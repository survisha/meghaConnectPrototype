import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { VisitorSearchService } from '../../services/visitor-search.service';
import { AppointmentService } from '../../services/appointment.service';
import { VisitorKycService } from '../../services/visitor-kyc.service';
import { Visitor } from '../../models';
import { apiErrorMessage } from '../../shared/api-error.util';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatRadioModule } from '@angular/material/radio';
import { CameraCaptureService, CameraFacingMode } from '../../shared/camera-capture.service';
import { FaceRecognitionService } from '../../services/face-recognition.service';

@Component({
  selector: 'app-walkin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatChipsModule, MatIconModule, MatCardModule, MatTooltipModule, MatRadioModule],
  templateUrl: './walkin.component.html',
  styleUrls: ['./walkin.component.scss'],
})
export class WalkinComponent implements OnDestroy {
  phoneNumber = '';
  epicNumber = '';
  referenceId = '';
  matchingVisitors: Visitor[] = [];
  selectedVisitorId: number | null = null;
  selectedVisitor: Visitor | null = null;
  foundPerson: Visitor | null = null;
  notFound = false;
  showSearchResults = false;
  showUpdateDetailsPanel = false;
  showKycPanel = false;
  showAppointmentPanel = false;
  checkedIn = false;
  ticketId = '';
  agendaType = '';
  requestedLocation = 'SHILLONG';
  agendaBrief = '';
  errorMsg = '';
  associates: Visitor[] = [];
  searching = false;
  faceCameraStream: MediaStream | null = null;
  faceCameraActive = false;
  facePhoto = '';
  faceSearching = false;
  identifiedByFace = false;
  creating = false;
  visitorUpdateForm = this.emptyVisitorUpdateForm();
  visitorUpdatePhoto = '';
  visitorUpdateMsg = '';
  visitorUpdateError = '';
  savingVisitorUpdate = false;
  visitorCameraStream: MediaStream | null = null;
  visitorCameraActive = false;
  visitorCameraFacingMode: CameraFacingMode = 'user';
  kycRetrySubmitted = false;
  retryingKyc = false;
  kycRetryForm = {
    name: '',
    epicNumber: '',
  };

  agendaTypes = [
    { label: 'Scheme availment (CM)', value: 'Scheme availment (CM)' },
    { label: 'Governance', value: 'Governance' },
    { label: 'Trade & Commerce', value: 'Trade & Commerce' },
    { label: 'Political Discussion', value: 'Political Discussion' },
    { label: 'Public Grievance', value: 'Public Grievance' }
  ];
  locations = ['SHILLONG', 'TURA', 'DELHI', 'OTHERS'];

  constructor(
    private visitorSearchService: VisitorSearchService,
    private appointmentService: AppointmentService,
    private visitorKycService: VisitorKycService,
    private router: Router,
    private cameraCapture: CameraCaptureService,
    private faceRecognition: FaceRecognitionService
  ) {}

  ngOnDestroy() {
    this.stopVisitorCamera();
    this.stopFaceCamera();
  }

  async startFaceCamera(): Promise<void> {
    this.errorMsg = '';
    try {
      this.faceCameraStream = await this.cameraCapture.open('user');
      this.faceCameraActive = true;
      setTimeout(() => {
        const video = document.getElementById('walkinFaceVideo') as HTMLVideoElement | null;
        if (video && this.faceCameraStream) this.cameraCapture.attach(video, this.faceCameraStream);
      });
    } catch {
      this.errorMsg = 'Camera access was blocked. Please allow camera permission.';
    }
  }

  captureFace(): void {
    const video = document.getElementById('walkinFaceVideo') as HTMLVideoElement | null;
    if (!video) return;
    try {
      this.facePhoto = this.cameraCapture.capture(video);
      this.stopFaceCamera();
      this.searchByFace();
    } catch {
      this.errorMsg = 'Unable to capture a clear face photo. Please retake it.';
    }
  }

  searchByFace(): void {
    if (!this.facePhoto || this.faceSearching) return;
    this.faceSearching = true;
    this.errorMsg = '';
    this.faceRecognition.search(this.facePhoto).subscribe({
      next: result => {
        this.faceSearching = false;
        if (result.matched && result.visitor) {
          this.matchingVisitors = [result.visitor];
          this.showSearchResults = true;
          this.notFound = false;
          this.identifiedByFace = true;
          this.selectVisitor(result.visitor.id ?? null);
        } else {
          this.notFound = true;
          this.showSearchResults = false;
          this.identifiedByFace = false;
        }
      },
      error: err => {
        this.faceSearching = false;
        this.errorMsg = apiErrorMessage(err, 'Face search is unavailable. Please retry or use manual lookup.');
      }
    });
  }

  clearFaceSearch(): void {
    this.stopFaceCamera();
    this.facePhoto = '';
    this.faceSearching = false;
    this.identifiedByFace = false;
  }

  stopFaceCamera(): void {
    this.cameraCapture.stop(this.faceCameraStream);
    this.faceCameraStream = null;
    this.faceCameraActive = false;
  }

  continueNewRegistration(): void {
    this.router.navigate(['/deo/register-visitor'], { state: { faceSearchPhoto: this.facePhoto } });
  }

  search() {
    this.errorMsg = '';
    this.resetSearchSelectionState();
    this.notFound = false;
    this.searching = true;
    const phone = this.phoneNumber.trim();
    const epic = this.epicNumber.trim();
    const referenceId = this.referenceId.trim();

    if (!phone && !epic && !referenceId) {
      this.notFound = true;
      this.searching = false;
      this.errorMsg = 'Enter mobile, EPIC, or visitor reference ID to search.';
      return;
    }
    if (phone && !/^[0-9]{10}$/.test(phone)) {
      this.searching = false;
      this.errorMsg = 'Enter a valid 10-digit mobile number.';
      return;
    }

    this.visitorSearchService.search({ mobile: phone, epic, referenceId }).subscribe({
      next: results => {
        this.matchingVisitors = results || [];
        this.showSearchResults = this.matchingVisitors.length > 0;
        this.notFound = this.matchingVisitors.length === 0;
        this.visitorUpdateMsg = '';
        this.visitorUpdateError = '';
        this.stopVisitorCamera();
        if (this.matchingVisitors.length === 1) {
          this.selectVisitor(this.matchingVisitors[0].id);
        }
        this.searching = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to search visitor records.');
        this.notFound = false;
        this.searching = false;
      }
    });
  }

  onSearchPhoneInput() {
    this.phoneNumber = this.phoneNumber.replace(/\D/g, '').slice(0, 10);
  }

  selectVisitor(visitorId: number | null) {
    this.selectedVisitorId = visitorId;
    this.selectedVisitor = this.matchingVisitors.find(visitor => visitor.id === visitorId) ?? null;
    this.foundPerson = this.selectedVisitor;
    this.showUpdateDetailsPanel = false;
    this.showKycPanel = false;
    this.showAppointmentPanel = false;
    this.visitorUpdateMsg = '';
    this.visitorUpdateError = '';
    this.kycRetrySubmitted = false;
    this.stopVisitorCamera();
  }

  openUpdateDetailsPanel() {
    if (!this.selectedVisitor) {
      this.errorMsg = 'Select a visitor before updating details.';
      return;
    }
    this.hideActionPanels();
    this.foundPerson = this.selectedVisitor;
    this.initVisitorUpdateForm(this.selectedVisitor);
    this.showUpdateDetailsPanel = true;
  }

  openKycPanel() {
    if (!this.selectedVisitor) {
      this.errorMsg = 'Select a visitor before performing KYC.';
      return;
    }
    if (!this.isKycPending(this.selectedVisitor)) {
      this.errorMsg = 'KYC is already completed or matched for the selected visitor.';
      return;
    }
    this.hideActionPanels();
    this.foundPerson = this.selectedVisitor;
    this.kycRetryForm = {
      name: this.selectedVisitor.fullName || '',
      epicNumber: this.selectedVisitor.epicNumber || '',
    };
    this.onKycEpicInput();
    this.kycRetrySubmitted = false;
    this.showKycPanel = true;
  }

  openAppointmentPanel() {
    if (!this.selectedVisitor) {
      this.errorMsg = 'Select a visitor before creating an appointment.';
      return;
    }
    this.hideActionPanels();
    this.foundPerson = this.selectedVisitor;
    this.showAppointmentPanel = true;
  }

  continueToAppointmentForm() {
    this.errorMsg = '';
    const visitor = this.selectedVisitor || this.foundPerson;
    if (!visitor?.id) {
      this.errorMsg = 'Select an existing visitor before opening the appointment form.';
      return;
    }
    if (this.isKycPending(visitor)) {
      this.errorMsg = 'Citizen KYC is pending. Appointment can be created, but KYC should be verified later.';
    }
    this.router.navigate(['/appointments/new'], {
      queryParams: {
        visitorId: visitor.id,
        source: 'walkin',
        walkin: true,
      }
    });
  }

  isKycPending(visitor: Visitor | null): boolean {
    const status = visitor?.kycStatus?.trim().toUpperCase();
    return !status || status === 'KYC_PENDING' || status === 'PENDING';
  }

  kycStatusLabel(visitor: Visitor | null): string {
    if (!visitor) return 'PENDING';
    return (visitor.kycStatus || 'PENDING').replace(/_/g, ' ');
  }

  addAssociate() {
    // No-op without a real search; user must search separately
  }

  sanitizeVisitorEpic() {
    this.visitorUpdateForm.epicNumber = this.visitorUpdateForm.epicNumber.toUpperCase().replace(/[^A-Z0-9]/g, '');
  }

  sanitizeVisitorPhone() {
    this.visitorUpdateForm.phoneNumber = this.visitorUpdateForm.phoneNumber.replace(/\D/g, '').slice(0, 10);
  }

  onKycNameInput(): void {
    this.kycRetryForm.name = this.kycRetryForm.name.replace(/[^A-Za-z ]/g, '').replace(/\s{2,}/g, ' ');
  }

  onKycEpicInput(): void {
    const clean = this.kycRetryForm.epicNumber.toUpperCase().replace(/[^A-Z0-9]/g, '');
    let next = '';
    for (const char of clean) {
      if (next.length < 3 && /[A-Z]/.test(char)) {
        next += char;
      } else if (next.length >= 3 && next.length < 10 && /[0-9]/.test(char)) {
        next += char;
      }
      if (next.length === 10) break;
    }
    this.kycRetryForm.epicNumber = next;
  }

  get kycNameError(): string {
    const name = this.kycRetryForm.name.trim();
    if (!name) return 'Name is required.';
    if (!/^[A-Za-z ]+$/.test(name)) return 'Name should contain only letters and spaces.';
    return '';
  }

  get kycEpicError(): string {
    const epic = this.kycRetryForm.epicNumber.trim().toUpperCase();
    if (!epic) return 'EPIC number is required.';
    if (!/^[A-Z]{3}[0-9]{7}$/.test(epic)) return 'EPIC number must be 3 letters followed by 7 digits.';
    return '';
  }

  get isKycRetryFormValid(): boolean {
    return !this.kycNameError && !this.kycEpicError;
  }

  retryKycVerification(): void {
    this.kycRetrySubmitted = true;
    this.onKycNameInput();
    this.onKycEpicInput();
    const visitorId = this.selectedVisitorId || this.selectedVisitor?.id || 0;
    if (!visitorId || this.retryingKyc || !this.isKycRetryFormValid) {
      return;
    }
    this.errorMsg = '';
    this.visitorUpdateMsg = '';
    this.visitorUpdateError = '';
    this.retryingKyc = true;
    this.visitorKycService.retryKyc(visitorId, {
      name: this.kycRetryForm.name.trim(),
      epicNumber: this.kycRetryForm.epicNumber.trim().toUpperCase(),
    }).subscribe({
      next: res => {
        this.retryingKyc = false;
        if (!res.success) {
          this.visitorUpdateError = res.message || 'Unable to verify EPIC details. Please check the EPIC number and name.';
          return;
        }
        const updated = {
          ...(this.selectedVisitor || {}),
          ...(res.profile || {}),
          id: visitorId,
          fullName: String((res.profile as any)?.fullName || this.kycRetryForm.name.trim()),
          epicNumber: this.kycRetryForm.epicNumber.trim().toUpperCase(),
          kycStatus: res.kycStatus as any,
          kycProvider: res.kycProvider,
          kycType: 'EPIC',
        } as Visitor;
        this.selectedVisitor = updated;
        this.foundPerson = updated;
        this.matchingVisitors = this.matchingVisitors.map(visitor => visitor.id === visitorId ? { ...visitor, ...updated } : visitor);
        this.initVisitorUpdateForm(updated);
        this.showKycPanel = false;
        this.visitorUpdateMsg = res.message || 'KYC verification completed successfully.';
      },
      error: err => {
        this.retryingKyc = false;
        this.visitorUpdateError = apiErrorMessage(err, 'Unable to retry KYC verification.');
      }
    });
  }

  sanitizeVisitorPincode() {
    this.visitorUpdateForm.pincode = this.visitorUpdateForm.pincode.replace(/\D/g, '').slice(0, 6);
  }

  async openVisitorCamera() {
    try {
      this.visitorUpdateError = '';
      this.stopVisitorCamera();
      this.visitorCameraStream = await this.cameraCapture.open(this.visitorCameraFacingMode);
      this.visitorCameraActive = true;
      setTimeout(() => {
        const video = document.getElementById('deo-camera-preview') as HTMLVideoElement;
        if (video && this.visitorCameraStream) {
          this.cameraCapture.attach(video, this.visitorCameraStream);
        }
      }, 100);
    } catch {
      this.visitorUpdateError = 'Camera access was blocked. Please allow camera permission and try again.';
    }
  }

  captureVisitorPhoto() {
    const video = document.getElementById('deo-camera-preview') as HTMLVideoElement;
    if (!video) {
      this.visitorUpdateError = 'Camera is not ready yet.';
      return;
    }

    try {
      this.visitorUpdatePhoto = this.cameraCapture.capture(video);
    } catch {
      this.visitorUpdateError = 'Unable to capture photo.';
      return;
    }

    this.stopVisitorCamera();
    this.visitorUpdateMsg = 'Photo captured. Save updates to attach it to this visitor.';
  }

  retakeVisitorPhoto() {
    this.visitorUpdatePhoto = '';
    this.openVisitorCamera();
  }

  stopVisitorCamera() {
    this.cameraCapture.stop(this.visitorCameraStream);
    this.visitorCameraStream = null;
    this.visitorCameraActive = false;
  }

  switchVisitorCamera() {
    this.visitorCameraFacingMode = this.cameraCapture.toggle(this.visitorCameraFacingMode);
    if (this.visitorCameraActive) {
      this.openVisitorCamera();
    }
  }

  get visitorCameraFacingLabel(): string {
    return this.cameraCapture.label(this.visitorCameraFacingMode);
  }

  get hasExistingVisitorPhoto(): boolean {
    return !!this.firstNonBlank(
      this.foundPerson?.livePhotoBase64,
      this.foundPerson?.photoBase64,
      this.foundPerson?.photoUrl,
      this.foundPerson?.livePhotoPath,
      this.foundPerson?.photoStoragePath,
      this.foundPerson?.photoPath
    );
  }

  saveVisitorUpdates() {
    this.visitorUpdateMsg = '';
    this.visitorUpdateError = '';
    if (!this.foundPerson?.id) {
      this.visitorUpdateError = 'Search and select a visitor before saving updates.';
      return;
    }

    this.savingVisitorUpdate = true;
    const addressLine = this.visitorUpdateForm.addressLine.trim();
    const payload = {
      fullName: this.visitorUpdateForm.fullName.trim(),
      phoneNumber: this.visitorUpdateForm.phoneNumber.trim(),
      epicNumber: this.visitorUpdateForm.epicNumber.trim(),
      designation: this.visitorUpdateForm.designation.trim(),
      address: addressLine,
      fullAddress: addressLine,
      address1: addressLine,
      addressLine,
      district: this.visitorUpdateForm.district.trim(),
      constituency: this.visitorUpdateForm.constituency.trim(),
      booth: this.visitorUpdateForm.booth.trim(),
      boothVillage: this.visitorUpdateForm.boothVillage.trim(),
      village: this.visitorUpdateForm.village.trim(),
      location: this.visitorUpdateForm.location.trim(),
      city: this.visitorUpdateForm.city.trim(),
      state: this.visitorUpdateForm.state.trim(),
      pincode: this.visitorUpdateForm.pincode.trim(),
      briefProfile: this.visitorUpdateForm.briefProfile.trim(),
      livePhotoBase64: this.visitorUpdatePhoto || undefined,
    };

    this.visitorSearchService.update(this.foundPerson.id, payload).subscribe({
      next: updated => {
        this.foundPerson = {
          ...this.foundPerson,
          ...updated,
          photoBase64: this.visitorUpdatePhoto || this.foundPerson?.photoBase64,
          livePhotoBase64: this.visitorUpdatePhoto || this.foundPerson?.livePhotoBase64,
        } as Visitor;
        this.selectedVisitor = this.foundPerson;
        this.selectedVisitorId = this.foundPerson.id;
        this.matchingVisitors = this.matchingVisitors.map(visitor => visitor.id === this.foundPerson?.id ? this.foundPerson as Visitor : visitor);
        this.initVisitorUpdateForm(this.foundPerson);
        this.visitorUpdatePhoto = '';
        this.visitorUpdateMsg = 'Visitor details updated.';
        this.savingVisitorUpdate = false;
      },
      error: err => {
        this.visitorUpdateError = apiErrorMessage(err, 'Unable to update visitor details.');
        this.savingVisitorUpdate = false;
      }
    });
  }

  private initVisitorUpdateForm(visitor: Visitor) {
    this.visitorUpdateForm = {
      fullName: visitor.fullName ?? '',
      phoneNumber: visitor.phoneNumber ?? '',
      epicNumber: visitor.epicNumber ?? '',
      designation: visitor.designation ?? '',
      addressLine: visitor.addressLine ?? visitor.address1 ?? visitor.fullAddress ?? visitor.address ?? '',
      district: visitor.district ?? '',
      constituency: visitor.constituency ?? '',
      booth: visitor.booth ?? '',
      boothVillage: visitor.boothVillage ?? visitor.booth ?? '',
      village: visitor.village ?? '',
      location: visitor.location ?? '',
      city: visitor.city ?? '',
      state: visitor.state ?? '',
      pincode: visitor.pincode ?? '',
      briefProfile: visitor.briefProfile ?? '',
    };
  }

  visitorInitial(visitor: Visitor | null): string {
    return (visitor?.fullName || 'V').trim().charAt(0).toUpperCase() || 'V';
  }

  visitorIdType(visitor: Visitor | null): string {
    const type = this.firstNonBlank(visitor?.kycType, visitor?.epicNumber ? 'EPIC' : 'No ID');
    return type === 'NONE' ? 'No ID' : type;
  }

  visitorIdentityLabel(visitor: Visitor | null): string {
    return this.firstNonBlank(visitor?.epicNumber, 'No ID');
  }

  private resetSearchSelectionState() {
    this.matchingVisitors = [];
    this.selectedVisitorId = null;
    this.selectedVisitor = null;
    this.foundPerson = null;
    this.showSearchResults = false;
    this.showUpdateDetailsPanel = false;
    this.showKycPanel = false;
    this.showAppointmentPanel = false;
    this.visitorUpdateForm = this.emptyVisitorUpdateForm();
    this.visitorUpdatePhoto = '';
    this.visitorUpdateMsg = '';
    this.visitorUpdateError = '';
    this.kycRetrySubmitted = false;
    this.kycRetryForm = { name: '', epicNumber: '' };
    this.stopVisitorCamera();
  }

  private hideActionPanels() {
    this.showUpdateDetailsPanel = false;
    this.showKycPanel = false;
    this.showAppointmentPanel = false;
    this.visitorUpdateMsg = '';
    this.visitorUpdateError = '';
    this.stopVisitorCamera();
  }

  private emptyVisitorUpdateForm() {
    return {
      fullName: '',
      phoneNumber: '',
      epicNumber: '',
      designation: '',
      addressLine: '',
      district: '',
      constituency: '',
      booth: '',
      boothVillage: '',
      village: '',
      location: '',
      city: '',
      state: '',
      pincode: '',
      briefProfile: '',
    };
  }

  private firstNonBlank(...values: Array<string | null | undefined>): string {
    return values.map(value => value?.trim() || '').find(Boolean) || '';
  }
}
