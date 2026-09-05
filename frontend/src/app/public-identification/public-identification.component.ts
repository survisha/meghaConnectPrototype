import { Component, OnDestroy } from '@angular/core';
import { animate, style, transition, trigger } from '@angular/animations';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CitizenAppointmentHistory,
  CitizenSchemeHistory,
  PublicIdentificationHistory,
  VisitorSearchService
} from '../services/visitor-search.service';
import { Visitor } from '../models';
import { apiErrorMessage } from '../shared/api-error.util';
import { AuthenticatedPhotoComponent } from '../shared/authenticated-photo.component';
import { resolvePhotoUrl } from '../shared/photo-url.util';
import { CameraCaptureService } from '../shared/camera-capture.service';
import { FaceRecognitionService } from '../services/face-recognition.service';
import { from, interval, mergeMap, Subscription, toArray } from 'rxjs';
import { AutoFaceDetection, CameraLivenessService } from '../shared/camera-liveness.service';
import { ToastService } from '../shared/toast/toast.service';
import { AUTO_FACE_RESULT_EXPIRY_TICK_MS, AUTO_FACE_RESULT_TIMEOUT_MS } from '../config/public-identification.constants';
import { LegacyPersonCandidate, LegacyPersonSearchService } from '../services/legacy-person-search.service';
import { AppointmentService } from '../services/appointment.service';
import { AuthService } from '../services/auth.service';

// Angular Material
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-public-identification',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatInputModule, MatSelectModule, MatChipsModule, MatDividerModule,
    MatTableModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    AuthenticatedPhotoComponent,
  ],
  templateUrl: './public-identification.component.html',
  styleUrls: ['./public-identification.component.scss'],
  animations: [
    trigger('faceResultLifecycle', [
      transition(':enter', [style({ opacity: 0, transform: 'translateY(8px)' }), animate('180ms ease-out')]),
      transition(':leave', [animate('180ms ease-in', style({ opacity: 0, transform: 'translateY(-8px)' }))]),
    ]),
  ],
})
export class PublicIdentificationComponent implements OnDestroy {
  searchPhone = '';
  searchEpic = '';
  searchName = '';
  searchDistrict = '';
  results: Visitor[] = [];
  selected: Visitor | null = null;
  searched = false;
  searching = false;
  errorMessage = '';
  selectedPhotoLoadFailed = false;
  selectedPhotoPreviewOpen = false;
  historyLoading = false;
  historyError = '';
  fullHistoryOpen = false;
  citizenHistory: PublicIdentificationHistory | null = null;
  legacyMatches: LegacyPersonCandidate[] = [];
  legacyLoading = false;
  legacyUnavailable = false;
  faceCameraStream: MediaStream | null = null;
  faceCameraActive = false;
  facePhoto = '';
  faceSearching = false;
  readonly maxFaces = 6;
  readonly maxConcurrentFaceSearches = 5;
  readonly faceCaptureRetryMs = 15000;
  readonly faceDisappearGraceMs = 2500;
  faceDetections: FaceIdentificationItem[] = [];
  selectedFaceTrackingId: string | null = null;
  private detectionTimer: ReturnType<typeof setTimeout> | null = null;
  private detectionRunning = false;
  private nextTrackingId = 1;
  private trackedFaces = new Map<string, TrackedFace>();
  private faceQueue: QueuedFace[] = [];
  private activeFaceSearches = 0;
  private faceSearchSubscriptions = new Set<Subscription>();
  private resultExpirySubscription: Subscription | null = null;
  private historySubscription: Subscription | null = null;

  districts = ['East Khasi Hills','West Khasi Hills','Ri Bhoi','East Jaintia Hills','West Jaintia Hills','East Garo Hills','West Garo Hills','South Garo Hills','North Garo Hills'];

  schemeHistoryColumns: string[] = ['scheme', 'appliedDate', 'amount', 'status'];
  meetingHistoryColumns: string[] = ['date', 'department', 'purpose', 'status'];
  schemeHistory: CitizenSchemeHistory[] = [];
  meetingHistory: CitizenAppointmentHistory[] = [];
  lastVisited: CitizenAppointmentHistory | null = null;
  lastVisitedAtDisplay = '';
  upcomingAppointment: CitizenAppointmentHistory | null = null;
  selectedPendingAppointment: CitizenAppointmentHistory | null = null;
  pendingAppointmentRemarks = '';
  pendingAppointmentSaving = false;

  get canManagePendingHistory(): boolean { return this.auth.hasRole('APPROVER'); }
  get visibleMeetingHistoryColumns(): string[] {
    return this.canManagePendingHistory
      ? [...this.meetingHistoryColumns, 'actions']
      : this.meetingHistoryColumns;
  }

  constructor(
    private visitorSearchService: VisitorSearchService,
    private cameraCapture: CameraCaptureService,
    private faceRecognition: FaceRecognitionService,
    private cameraLiveness: CameraLivenessService,
    private toast: ToastService,
    private legacySearch: LegacyPersonSearchService,
    private appointmentService: AppointmentService,
    private auth: AuthService
  ) {}

  openPendingAppointment(appointment: CitizenAppointmentHistory): void {
    if (!this.canManagePendingHistory || appointment.status !== 'PENDING') return;
    this.selectedPendingAppointment = appointment;
    this.pendingAppointmentRemarks = appointment.remarks || '';
  }

  closePendingAppointment(): void {
    if (this.pendingAppointmentSaving) return;
    this.selectedPendingAppointment = null;
    this.pendingAppointmentRemarks = '';
  }

  savePendingAppointmentRemarks(): void {
    const appointment = this.selectedPendingAppointment;
    const remarks = this.pendingAppointmentRemarks.trim();
    if (!appointment || !remarks || this.pendingAppointmentSaving) {
      if (!remarks) this.toast.error('Enter remarks before saving.');
      return;
    }
    this.pendingAppointmentSaving = true;
    this.appointmentService.addRemark(appointment.appointmentId, { hcmRemarks: remarks }).subscribe({
      next: () => {
        this.pendingAppointmentSaving = false;
        appointment.remarks = remarks;
        this.pendingAppointmentRemarks = '';
        this.toast.success('Remarks saved. Appointment remains PENDING.');
        if (this.selected) this.loadCitizenHistory(this.selected.id);
      },
      error: error => {
        this.pendingAppointmentSaving = false;
        this.toast.error(apiErrorMessage(error, 'Unable to save remarks.'));
      }
    });
  }

  completePendingAppointment(): void {
    const appointment = this.selectedPendingAppointment;
    if (!appointment || this.pendingAppointmentSaving ||
        !confirm('Complete this pending appointment?')) return;
    this.pendingAppointmentSaving = true;
    this.appointmentService.completeAppointment(appointment.appointmentId).subscribe({
      next: () => {
        this.pendingAppointmentSaving = false;
        this.selectedPendingAppointment = null;
        this.toast.success('Appointment completed successfully.');
        if (this.selected) this.loadCitizenHistory(this.selected.id);
      },
      error: error => {
        this.pendingAppointmentSaving = false;
        this.toast.error(apiErrorMessage(error, 'Unable to complete appointment.'));
      }
    });
  }

  ngOnDestroy(): void {
    this.stopFaceCamera(true);
    this.stopResultExpiryClock();
    this.historySubscription?.unsubscribe();
  }

  async startFaceIdentification(): Promise<void> {
    this.errorMessage = '';
    this.resetFaceSession();
    try {
      this.faceCameraStream = await this.cameraCapture.open('environment');
      this.faceCameraActive = true;
      setTimeout(() => {
        const video = document.getElementById('publicFaceVideo') as HTMLVideoElement | null;
        if (video && this.faceCameraStream) {
          this.cameraCapture.attach(video, this.faceCameraStream);
          this.scheduleFaceDetection(video);
        }
      });
    } catch { this.errorMessage = 'Camera access was blocked.'; }
  }

  captureAndIdentify(): void {
    const video = document.getElementById('publicFaceVideo') as HTMLVideoElement | null;
    if (!video) return;
    try {
      this.facePhoto = this.cameraCapture.capture(video);
      this.stopFaceCamera();
      this.faceSearching = true;
      this.faceRecognition.search(this.facePhoto).subscribe({
        next: response => {
          this.faceSearching = false;
          this.searched = true;
          this.results = response.matched && response.visitor ? [response.visitor] : [];
          if (this.results[0]) this.select(this.results[0]);
        },
        error: error => { this.faceSearching = false; this.handleSearchError(error); }
      });
    } catch { this.errorMessage = 'Unable to capture a clear photo.'; }
  }

  identifyFaceCrops(event: Event): void {
    const files = Array.from((event.target as HTMLInputElement).files || []).slice(0, this.maxFaces);
    if (!files.length) return;
    this.faceSearching = true;
    this.errorMessage = '';
    from(files.map((file, index) => ({ file, index }))).pipe(
      mergeMap(item => from(this.readImage(item.file)).pipe(
        mergeMap(photo => this.faceRecognition.search(photo)),
        mergeMap(response => [{ response, index: item.index }])
      ), 3),
      toArray()
    ).subscribe({
      next: indexedResponses => {
        const unique = new Map<number, Visitor>();
        indexedResponses.sort((a, b) => a.index - b.index).forEach(({ response }) => {
          if (response.matched && response.visitor?.id) unique.set(response.visitor.id, response.visitor);
        });
        this.results = Array.from(unique.values());
        this.searched = true;
        this.faceSearching = false;
        if (this.results[0]) this.select(this.results[0]);
      },
      error: error => { this.faceSearching = false; this.handleSearchError(error); }
    });
  }

  clearFaceIdentification(): void {
    this.stopFaceCamera();
    this.facePhoto = '';
  }

  closeIdentificationCamera(): void {
    this.stopFaceCamera();
  }

  private stopFaceCamera(cancelPendingSearches = false): void {
    if (this.detectionTimer) clearTimeout(this.detectionTimer);
    this.detectionTimer = null;
    this.detectionRunning = false;
    this.faceQueue = [];
    if (cancelPendingSearches) {
      this.faceSearchSubscriptions.forEach(subscription => subscription.unsubscribe());
      this.faceSearchSubscriptions.clear();
      this.activeFaceSearches = 0;
    }
    this.trackedFaces.clear();
    this.cameraCapture.stop(this.faceCameraStream);
    const video = document.getElementById('publicFaceVideo') as HTMLVideoElement | null;
    if (video) {
      video.pause();
      video.srcObject = null;
    }
    this.faceCameraStream = null;
    this.faceCameraActive = false;
    this.stopResultExpiryClock();
  }

  private resetFaceSession(): void {
    this.faceQueue = [];
    this.faceDetections = [];
    this.selectedFaceTrackingId = null;
    this.trackedFaces.clear();
    this.nextTrackingId = 1;
  }

  private scheduleFaceDetection(video: HTMLVideoElement): void {
    if (!this.faceCameraActive) return;
    this.detectionTimer = setTimeout(async () => {
      if (!this.faceCameraActive || this.detectionRunning) return;
      this.detectionRunning = true;
      try {
        const detections = await this.cameraLiveness.analyzeFaces(video);
        this.processDetectedFaces(video, detections);
      } catch {
        this.errorMessage = 'Automatic face detection is unavailable.';
      } finally {
        this.detectionRunning = false;
        this.scheduleFaceDetection(video);
      }
    }, 350);
  }

  private processDetectedFaces(video: HTMLVideoElement, detections: AutoFaceDetection[]): void {
    const now = Date.now();
    const seenTracks = new Set<string>();
    for (const detection of detections.filter(face => face.valid)) {
      const centerX = detection.box.left + detection.box.width / 2;
      const centerY = detection.box.top + detection.box.height / 2;
      let track = Array.from(this.trackedFaces.values()).find(candidate => candidate.active &&
        Math.hypot(candidate.centerX - centerX, candidate.centerY - centerY) < 0.16 &&
        this.descriptorDistance(candidate.descriptor, detection.descriptor) < 0.18);
      track ??= Array.from(this.trackedFaces.values()).find(candidate => !candidate.active &&
        now - candidate.lastSeen < this.faceCaptureRetryMs &&
        this.descriptorDistance(candidate.descriptor, detection.descriptor) < 0.12);
      if (!track) {
        track = {
          id: `Face ${this.nextTrackingId++}`, centerX, centerY, descriptor: detection.descriptor,
          lastSeen: now, active: true, captured: false
        };
        this.trackedFaces.set(track.id, track);
      }
      track.centerX = centerX;
      track.centerY = centerY;
      track.descriptor = detection.descriptor;
      track.lastSeen = now;
      track.active = true;
      seenTracks.add(track.id);
      if (!track.captured) {
        track.captured = true;
        const photo = this.cameraCapture.captureCrop(video, detection.box);
        this.faceDetections.push({
          trackingId: track.id, capturedImage: photo, status: 'QUEUED',
          matchScore: detection.score, recognitionTime: new Date(now), createdTime: now,
          expiryTime: 0, selected: false
        });
        this.faceQueue.push({ trackingId: track.id, photo });
      }
    }
    for (const [id, track] of this.trackedFaces) {
      if (!seenTracks.has(id) && now - track.lastSeen > this.faceDisappearGraceMs) track.active = false;
      if (!track.active && now - track.lastSeen >= this.faceCaptureRetryMs) this.trackedFaces.delete(id);
    }
    this.drainFaceQueue();
  }

  private drainFaceQueue(): void {
    while (this.faceCameraActive && this.activeFaceSearches < this.maxConcurrentFaceSearches && this.faceQueue.length) {
      const queued = this.faceQueue.shift()!;
      const item = this.faceDetections.find(face => face.trackingId === queued.trackingId && face.status === 'QUEUED');
      if (item) item.status = 'SEARCHING';
      this.activeFaceSearches++;
      const subscription = this.faceRecognition.search(queued.photo).subscribe({
        next: response => {
          if (item) {
            item.status = response.matched && response.visitor ? 'MATCHED' : 'NOT_REGISTERED';
            item.visitor = response.visitor;
            item.enrollmentId = response.enrollmentId;
            item.matchScore = response.score ?? item.matchScore;
            item.message = response.message;
            item.recognitionTime = new Date();
            item.expiryTime = Date.now() + AUTO_FACE_RESULT_TIMEOUT_MS;
            this.startResultExpiryClock();
          }
          if (response.matched && response.visitor?.id && !this.results.some(v => v.id === response.visitor!.id)) {
            this.results = [...this.results, response.visitor];
            if (!this.selected) this.select(response.visitor);
          }
          this.searched = true;
        },
        error: error => {
          if (item) {
            item.status = error?.name === 'TimeoutError' ? 'TIMEOUT' : 'UNAVAILABLE';
            item.message = item.status === 'TIMEOUT' ? 'Search timed out.' : 'Face recognition service is unavailable.';
            item.recognitionTime = new Date();
            item.expiryTime = Date.now() + AUTO_FACE_RESULT_TIMEOUT_MS;
            this.startResultExpiryClock();
          }
          this.toast.error('Face Recognition Service Unavailable');
          this.finishFaceSearch(subscription);
        },
        complete: () => this.finishFaceSearch(subscription)
      });
      this.faceSearchSubscriptions.add(subscription);
    }
    this.faceSearching = this.activeFaceSearches > 0 || this.faceQueue.length > 0;
  }

  private descriptorDistance(left: number[], right: number[]): number {
    if (!left.length || left.length !== right.length) return Number.POSITIVE_INFINITY;
    return left.reduce((sum, value, index) => sum + Math.abs(value - right[index]), 0) / left.length;
  }

  private finishFaceSearch(subscription: Subscription): void {
    this.faceSearchSubscriptions.delete(subscription);
    this.activeFaceSearches = Math.max(0, this.activeFaceSearches - 1);
    this.faceSearching = this.activeFaceSearches > 0 || this.faceQueue.length > 0;
    this.drainFaceQueue();
  }

  selectFaceResult(face: FaceIdentificationItem): void {
    this.faceDetections.forEach(item => item.selected = item.trackingId === face.trackingId);
    this.selectedFaceTrackingId = face.trackingId;
    if (face.status === 'MATCHED' && face.visitor) {
      this.select(face.visitor);
    } else {
      this.clearSelectedProfile();
    }
  }

  getStatusClass(status: FaceIdentificationStatus): string {
    switch (status) {
      case 'MATCHED': return 'face-status-success';
      case 'NOT_REGISTERED': return 'face-status-warning';
      case 'FAILED': return 'face-status-error';
      case 'TIMEOUT': return 'face-status-timeout';
      case 'UNAVAILABLE': return 'face-status-unavailable';
      default: return 'face-status-searching';
    }
  }

  getFaceStatusIcon(status: FaceIdentificationStatus): string {
    switch (status) {
      case 'MATCHED': return 'check_circle';
      case 'NOT_REGISTERED': return 'warning';
      case 'FAILED': return 'error';
      case 'TIMEOUT': return 'schedule';
      case 'UNAVAILABLE': return 'cloud_off';
      default: return 'sync';
    }
  }

  faceStatusLabel(status: FaceIdentificationStatus): string {
    if (status === 'NOT_REGISTERED') return 'Not Registered';
    if (status === 'UNAVAILABLE') return 'Service Unavailable';
    if (status === 'QUEUED' || status === 'SEARCHING') return 'Searching';
    return this.statusLabel(status);
  }

  trackFaceResult(_: number, face: FaceIdentificationItem): string { return face.trackingId; }

  private startResultExpiryClock(): void {
    if (this.resultExpirySubscription) return;
    this.resultExpirySubscription = interval(AUTO_FACE_RESULT_EXPIRY_TICK_MS).subscribe(() => this.removeExpiredFaceResults());
  }

  private removeExpiredFaceResults(): void {
    const now = Date.now();
    const expiredSelected = this.faceDetections.some(face =>
      face.trackingId === this.selectedFaceTrackingId && face.expiryTime > 0 && face.expiryTime <= now);
    this.faceDetections = this.faceDetections.filter(face => !face.expiryTime || face.expiryTime > now);
    if (expiredSelected) {
      this.selectedFaceTrackingId = null;
      this.clearSelectedProfile();
    }
    if (!this.faceDetections.some(face => face.expiryTime > 0)) this.stopResultExpiryClock();
  }

  private stopResultExpiryClock(): void {
    this.resultExpirySubscription?.unsubscribe();
    this.resultExpirySubscription = null;
  }

  private clearSelectedProfile(): void {
    this.historySubscription?.unsubscribe();
    this.historySubscription = null;
    this.selected = null;
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.selectedPendingAppointment = null;
    this.populateHistory();
  }

  private readImage(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  populateHistory() {
    this.schemeHistory = [];
    this.meetingHistory = [];
    this.lastVisited = null;
    this.lastVisitedAtDisplay = '';
    this.upcomingAppointment = null;
    this.citizenHistory = null;
    this.historyError = '';
    this.historyLoading = false;
    this.fullHistoryOpen = false;
  }

  search() {
    const phone = this.searchPhone.trim();
    const epic = this.searchEpic.trim();
    const name = this.searchName.trim();
    const district = this.searchDistrict.trim();

    this.searched = true;
    this.searching = false;
    this.errorMessage = '';
    this.results = [];
    this.selected = null;
    this.populateHistory();

    if (!phone && !epic && !name && !district) {
      this.errorMessage = 'Enter at least one search criteria.';
      return;
    }

    this.searching = true;

    if (phone) {
      this.visitorSearchService.searchByPhone(phone).subscribe({
        next: visitors => this.setResults(visitors, { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    } else if (epic) {
      this.visitorSearchService.searchByEpic(epic).subscribe({
        next: visitor => this.setResults(visitor ? [visitor] : [], { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    } else if (name) {
      this.visitorSearchService.searchByName(name).subscribe({
        next: visitors => this.setResults(visitors, { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    } else if (district) {
      this.visitorSearchService.searchByDistrict(district).subscribe({
        next: visitors => this.setResults(visitors, { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    }
  }

  select(p: Visitor) {
    this.selected = p;
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.populateHistory();
    this.loadCitizenHistory(p.id);
    this.loadLegacyHistory(p);
  }

  private loadLegacyHistory(visitor: Visitor): void {
    this.legacyMatches=[];this.legacyUnavailable=false;this.legacyLoading=true;
    this.legacySearch.search({epic:visitor.epicNumber,name:visitor.fullName,mobile:visitor.phoneNumber,village:visitor.village,address:visitor.fullAddress||visitor.address||visitor.addressLine||visitor.address1,district:visitor.district,constituency:visitor.constituency}).subscribe({
      next:r=>{this.legacyMatches=r.matches;this.legacyLoading=false;},
      error:()=>{this.legacyLoading=false;this.legacyUnavailable=true;}
    });
  }

  clearSearch() {
    this.searchPhone = '';
    this.searchEpic = '';
    this.searchName = '';
    this.searchDistrict = '';
    this.results = [];
    this.selected = null;
    this.searched = false;
    this.searching = false;
    this.errorMessage = '';
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.populateHistory();
    this.legacyMatches=[];
    this.legacyUnavailable=false;
  }

  initial(name?: string | null): string {
    return (name?.trim().charAt(0) || '?').toUpperCase();
  }

  displayValue(value?: string | number | null): string {
    const formatted = value?.toString().trim();
    return formatted || '-';
  }

  formatDateTime(value?: string | null): string {
    if (!value) {
      return '-';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
  }

  formatCurrency(value?: number | null): string {
    if (value === null || value === undefined) {
      return '-';
    }
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  statusLabel(status?: string | null): string {
    return this.displayValue(status?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()));
  }

  statusClass(status?: string | null): string {
    const normalized = (status || '').toUpperCase();
    if (['APPROVED', 'COMPLETED', 'RESOLVED', 'HCM_ACCEPTED', 'APPROVED_WITH_DATE_TIME'].includes(normalized)) {
      return 'status-success';
    }
    if (['REJECTED', 'CANCELLED', 'HCM_REJECTED'].includes(normalized)) {
      return 'status-danger';
    }
    if (['PENDING', 'SUBMITTED', 'CMO_REVIEW', 'APPROVER_REVIEW', 'HCM_PENDING', 'SCHEDULED'].includes(normalized)) {
      return 'status-warn';
    }
    return 'status-info';
  }

  get latestSchemes(): CitizenSchemeHistory[] {
    return this.schemeHistory.slice(0, 3);
  }

  get latestMeetings(): CitizenAppointmentHistory[] {
    return this.meetingHistory.slice(0, 3);
  }

  get hasAnyHistory(): boolean {
    return this.schemeHistory.length > 0 || this.meetingHistory.length > 0 || !!this.citizenHistory?.lastVisitedAt;
  }

  get groupVisitHistory(): CitizenAppointmentHistory[] {
    return this.meetingHistory.filter(item => item.role === 'ASSOCIATE' || (item.groupMembers?.length || 0) > 0);
  }

  toggleFullHistory(): void {
    this.fullHistoryOpen = !this.fullHistoryOpen;
  }

  get selectedPhotoUrl(): string {
    return this.getVisitorPhotoUrl(this.selected);
  }

  formatAppointmentDateTime(appointment?: CitizenAppointmentHistory | null): string {
    return this.formatDateTime(appointment ? this.getAppointmentDateTime(appointment) : null);
  }

  toggleSelectedPhotoPreview(): void {
    if (!this.selectedPhotoUrl || this.selectedPhotoLoadFailed) {
      return;
    }
    this.selectedPhotoPreviewOpen = !this.selectedPhotoPreviewOpen;
  }

  closeSelectedPhotoPreview(): void {
    this.selectedPhotoPreviewOpen = false;
  }

  onSelectedPhotoError(): void {
    this.selectedPhotoLoadFailed = true;
    this.selectedPhotoPreviewOpen = false;
  }

  private setResults(visitors: Visitor[], criteria: SearchCriteria): void {
    this.results = (visitors || []).filter(visitor => this.matchesCriteria(visitor, criteria));
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.searching = false;
    if (this.results[0]) {
      this.select(this.results[0]);
    } else {
      this.selected = null;
      this.populateHistory();
    }
  }

  private handleSearchError(error: unknown): void {
    this.results = [];
    this.selected = null;
    this.searching = false;
    this.errorMessage = apiErrorMessage(error, 'Unable to search visitor records right now. Please try again.');
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.populateHistory();
  }

  private matchesCriteria(visitor: Visitor, criteria: SearchCriteria): boolean {
    const phoneDigits = this.onlyDigits(criteria.phone);
    const visitorPhoneDigits = this.onlyDigits(visitor.phoneNumber);
    if (phoneDigits && !visitorPhoneDigits.includes(phoneDigits)) {
      return false;
    }

    const epic = criteria.epic.toUpperCase();
    if (epic && !this.text(visitor.epicNumber).toUpperCase().includes(epic)) {
      return false;
    }

    const name = criteria.name.toLowerCase();
    if (name && !this.text(visitor.fullName).toLowerCase().includes(name)) {
      return false;
    }

    const district = criteria.district.toLowerCase();
    if (district && this.text(visitor.district).toLowerCase() !== district) {
      return false;
    }

    return true;
  }

  private text(value?: string | null): string {
    return (value || '').trim();
  }

  private onlyDigits(value?: string | null): string {
    return (value || '').replace(/\D/g, '');
  }

  private loadCitizenHistory(citizenId: number): void {
    this.historySubscription?.unsubscribe();
    this.historyLoading = true;
    this.historyError = '';
    this.historySubscription = this.visitorSearchService.getPublicIdentificationHistory(citizenId).subscribe({
      next: history => {
        if (this.selected?.id !== citizenId) return;
        this.historyLoading = false;
        this.citizenHistory = history;
        this.schemeHistory = history.schemes || [];
        this.meetingHistory = history.appointments || [];
        this.mapFullHistoryResponse(history);
        if (this.selected && this.selected.id === citizenId && history.photoUrl) {
          this.selected = { ...this.selected, photoUrl: history.photoUrl };
          this.selectedPhotoLoadFailed = false;
        }
      },
      error: error => {
        if (this.selected?.id !== citizenId) return;
        this.historyLoading = false;
        this.historyError = apiErrorMessage(error, 'Unable to load citizen history. Please try again.');
        this.schemeHistory = [];
        this.meetingHistory = [];
        this.lastVisited = null;
        this.lastVisitedAtDisplay = '';
        this.upcomingAppointment = null;
        this.citizenHistory = null;
      },
    });
  }

  private mapFullHistoryResponse(history: PublicIdentificationHistory): void {
    this.lastVisitedAtDisplay = history.lastVisitedAt ? this.formatDateTime(history.lastVisitedAt) : '';
    this.processVisitHistory(history.appointments || []);
  }

  private processVisitHistory(records: CitizenAppointmentHistory[]): void {
    const now = new Date();
    const datedRecords = (records || [])
      .map(record => ({ record, date: this.parseHistoryDate(this.getAppointmentDateTime(record)) }))
      .filter((item): item is { record: CitizenAppointmentHistory; date: Date } => !!item.date);

    const pastRecords = datedRecords
      .filter(item => item.date.getTime() < now.getTime())
      .filter(item => this.isPastVisitStatus(item.record.status))
      .sort((a, b) => b.date.getTime() - a.date.getTime());

    const upcomingRecords = datedRecords
      .filter(item => item.date.getTime() >= now.getTime())
      .filter(item => (item.record.status || '').toUpperCase() === 'SCHEDULED')
      .sort((a, b) => a.date.getTime() - b.date.getTime());

    this.lastVisited = pastRecords[0]?.record || null;
    this.upcomingAppointment = upcomingRecords[0]?.record || null;
  }

  getAppointmentDateTime(appointment: CitizenAppointmentHistory): string | null {
    const dateOnly = this.firstNonBlank(
      appointment.appointmentDate,
      appointment.visitDate,
      appointment.eventDate,
      appointment.meetingDate
    );
    const timeOnly = this.firstNonBlank(appointment.startTime);
    const combinedDateTime = dateOnly && timeOnly ? `${dateOnly}T${timeOnly}` : dateOnly;
    return this.firstNonBlank(
      appointment.scheduledAt,
      appointment.appointmentDateTime,
      appointment.dateTime,
      combinedDateTime
    ) || null;
  }

  private parseHistoryDate(value?: string | null): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private isPastVisitStatus(status?: string | null): boolean {
    const normalized = (status || '').toUpperCase();
    if (!normalized) return true;
    return ['COMPLETED', 'VISITED', 'CLOSED', 'EXITED', 'RESOLVED'].includes(normalized);
  }

  getVisitorPhotoUrl(visitor?: Visitor | null): string {
    if (!visitor) {
      return '';
    }

    const canonicalPhoto = this.firstNonBlank(visitor.photoUrl, visitor.photoStoragePath, visitor.photoPath);
    if (canonicalPhoto) {
      return this.normalizePhotoSource(canonicalPhoto);
    }

    const fallbackPhoto = this.firstNonBlank(visitor.livePhotoPath, visitor.livePhotoBase64, visitor.photoBase64);
    return fallbackPhoto ? this.normalizePhotoSource(fallbackPhoto) : '';
  }

  private normalizePhotoSource(value: string): string {
    return resolvePhotoUrl(value) || '';
  }

  private firstNonBlank(...values: Array<string | null | undefined>): string {
    return values.map(value => value?.trim() || '').find(Boolean) || '';
  }
}

interface SearchCriteria {
  phone: string;
  epic: string;
  name: string;
  district: string;
}

interface TrackedFace {
  id: string;
  centerX: number;
  centerY: number;
  lastSeen: number;
  descriptor: number[];
  active: boolean;
  captured: boolean;
}

interface QueuedFace {
  trackingId: string;
  photo: string;
}

interface FaceIdentificationItem {
  trackingId: string;
  capturedImage: string;
  status: FaceIdentificationStatus;
  visitor?: Visitor;
  enrollmentId?: string;
  matchScore?: number;
  recognitionTime: Date;
  createdTime: number;
  expiryTime: number;
  selected: boolean;
  message?: string;
}

type FaceIdentificationStatus = 'QUEUED' | 'SEARCHING' | 'MATCHED' | 'NOT_REGISTERED' | 'FAILED' | 'TIMEOUT' | 'UNAVAILABLE';
