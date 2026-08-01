import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CitizenAppointmentHistory,
  CitizenSchemeHistory,
  PublicIdentificationHistory,
  VisitorSearchService
} from '../services/visitor-search.service';
import { Visitor } from '../models';
import { environment } from '../../environments/environment';
import { apiErrorMessage } from '../shared/api-error.util';
import { CameraCaptureService } from '../shared/camera-capture.service';
import { FaceRecognitionService } from '../services/face-recognition.service';
import { from, mergeMap, toArray } from 'rxjs';

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
  ],
  templateUrl: './public-identification.component.html',
  styleUrls: ['./public-identification.component.scss'],
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
  faceCameraStream: MediaStream | null = null;
  faceCameraActive = false;
  facePhoto = '';
  faceSearching = false;
  readonly maxFaces = 6;

  districts = ['East Khasi Hills','West Khasi Hills','Ri Bhoi','East Jaintia Hills','West Jaintia Hills','East Garo Hills','West Garo Hills','South Garo Hills','North Garo Hills'];

  schemeHistoryColumns: string[] = ['scheme', 'appliedDate', 'amount', 'status'];
  meetingHistoryColumns: string[] = ['date', 'department', 'purpose', 'status'];
  schemeHistory: CitizenSchemeHistory[] = [];
  meetingHistory: CitizenAppointmentHistory[] = [];
  lastVisited: CitizenAppointmentHistory | null = null;
  lastVisitedAtDisplay = '';
  upcomingAppointment: CitizenAppointmentHistory | null = null;

  constructor(
    private visitorSearchService: VisitorSearchService,
    private cameraCapture: CameraCaptureService,
    private faceRecognition: FaceRecognitionService
  ) {}

  ngOnDestroy(): void { this.stopFaceCamera(); }

  async startFaceIdentification(): Promise<void> {
    this.errorMessage = '';
    try {
      this.faceCameraStream = await this.cameraCapture.open('user');
      this.faceCameraActive = true;
      setTimeout(() => {
        const video = document.getElementById('publicFaceVideo') as HTMLVideoElement | null;
        if (video && this.faceCameraStream) this.cameraCapture.attach(video, this.faceCameraStream);
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

  private stopFaceCamera(): void {
    this.cameraCapture.stop(this.faceCameraStream);
    this.faceCameraStream = null;
    this.faceCameraActive = false;
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
    this.historyLoading = true;
    this.historyError = '';
    this.visitorSearchService.getPublicIdentificationHistory(citizenId).subscribe({
      next: history => {
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

  private getVisitorPhotoUrl(visitor?: Visitor | null): string {
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
    const source = value.trim();
    if (!source) {
      return '';
    }
    if (source.startsWith('data:image/') || source.startsWith('blob:') || /^https?:\/\//i.test(source)) {
      return source;
    }

    const origin = environment.apiUrl.replace(/\/api\/v1\/?$/i, '');
    const path = source.replace(/^\/+/, '');
    return `${origin}/${path.startsWith('uploads/') ? path : `uploads/${path}`}`;
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
