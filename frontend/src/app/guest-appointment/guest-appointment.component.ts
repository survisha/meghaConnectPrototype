import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule, provideNativeDateAdapter } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { AppointmentService, GuestAppointmentRequest } from '../services/appointment.service';
import { ReferenceDataDto, ReferenceDataService } from '../services/reference-data.service';
import { apiErrorMessage } from '../shared/api-error.util';
import { CameraCaptureService, CameraFacingMode } from '../shared/camera-capture.service';

@Component({
  selector: 'app-guest-appointment',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './guest-appointment.component.html',
  styleUrls: ['./guest-appointment.component.scss'],
})
export class GuestAppointmentComponent implements OnInit, OnDestroy {
  referredOffices: ReferenceDataDto[] = [];
  visitorCategories: ReferenceDataDto[] = [];
  submitting = false;
  errorMsg = '';
  referenceId = '';
  supportingDocument: File | null = null;
  preferredDate: Date | null = null;
  videoStream: MediaStream | null = null;
  isCameraActive = false;
  capturedPhotoUrl = '';
  cameraFacingMode: CameraFacingMode = 'user';

  form: GuestAppointmentRequest = {
    fullName: '',
    mobileNumber: '',
    address: '',
    referredOffice: '',
    reasonForAppointment: '',
  };

  constructor(
    private appointmentService: AppointmentService,
    private referenceDataService: ReferenceDataService,
    private cameraCapture: CameraCaptureService
  ) {}

  ngOnInit(): void {
    this.referenceDataService.getByType('GUEST_REFERRED_OFFICE').subscribe({
      next: values => this.referredOffices = values ?? [],
      error: error => this.errorMsg = apiErrorMessage(error, 'Unable to load referred offices.'),
    });
    this.referenceDataService.getByType('GUEST_VISITOR_CATEGORY').subscribe({
      next: values => this.visitorCategories = values ?? [],
      error: error => this.errorMsg = apiErrorMessage(error, 'Unable to load visitor categories.'),
    });
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }

  submit(): void {
    this.errorMsg = '';
    if (!this.isValid()) return;
    this.submitting = true;
    this.appointmentService.createGuestAppointment({
      ...this.form,
      preferredDate: this.preferredDate ? this.toDateParam(this.preferredDate) : undefined,
      livePhotoBase64: this.capturedPhotoUrl,
      supportingDocument: this.supportingDocument,
    }).subscribe({
      next: response => {
        this.referenceId = response.referenceId;
        this.stopCamera();
        this.submitting = false;
      },
      error: error => {
        this.errorMsg = apiErrorMessage(error, 'Unable to submit guest appointment.');
        this.submitting = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.supportingDocument = input.files?.[0] ?? null;
  }

  reset(): void {
    this.referenceId = '';
    this.supportingDocument = null;
    this.preferredDate = null;
    this.stopCamera();
    this.capturedPhotoUrl = '';
    this.form = {
      fullName: '',
      mobileNumber: '',
      address: '',
      referredOffice: '',
      reasonForAppointment: '',
    };
  }

  async openCamera(): Promise<void> {
    try {
      this.errorMsg = '';
      this.stopCamera();
      this.videoStream = await this.cameraCapture.open(this.cameraFacingMode);
      this.isCameraActive = true;
      setTimeout(() => {
        const videoElement = document.getElementById('guest-camera-preview') as HTMLVideoElement;
        if (videoElement && this.videoStream) {
          this.cameraCapture.attach(videoElement, this.videoStream);
        }
      }, 100);
    } catch {
      this.errorMsg = 'Camera access was denied or is unavailable.';
      this.isCameraActive = false;
    }
  }

  capturePhoto(): void {
    const videoElement = document.getElementById('guest-camera-preview') as HTMLVideoElement;
    if (!videoElement) {
      this.errorMsg = 'Camera is not initialized.';
      return;
    }
    try {
      this.capturedPhotoUrl = this.cameraCapture.capture(videoElement);
      this.stopCamera();
    } catch {
      this.errorMsg = 'Unable to capture photo. Please try again.';
    }
  }

  retakePhoto(): void {
    this.capturedPhotoUrl = '';
    this.openCamera();
  }

  closeCamera(): void {
    this.stopCamera();
  }

  switchCamera(): void {
    this.cameraFacingMode = this.cameraCapture.toggle(this.cameraFacingMode);
    if (this.isCameraActive) {
      this.openCamera();
    }
  }

  get cameraFacingLabel(): string {
    return this.cameraCapture.label(this.cameraFacingMode);
  }

  private isValid(): boolean {
    if (!this.form.fullName.trim() || !this.form.mobileNumber.trim() || !this.form.address.trim()
      || !this.form.referredOffice || !this.form.reasonForAppointment.trim()) {
      this.errorMsg = 'Please fill all required fields.';
      return false;
    }
    if (!/^[6-9]\d{9}$/.test(this.form.mobileNumber.trim())) {
      this.errorMsg = 'Please enter a valid 10-digit mobile number.';
      return false;
    }
    if (this.form.reasonForAppointment.trim().length < 10) {
      this.errorMsg = 'Reason for appointment must be at least 10 characters.';
      return false;
    }
    if (!this.capturedPhotoUrl) {
      this.errorMsg = 'Please capture guest photo before submitting.';
      return false;
    }
    return true;
  }

  private stopCamera(): void {
    this.cameraCapture.stop(this.videoStream);
    this.videoStream = null;
    this.isCameraActive = false;
  }

  private toDateParam(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }
}
