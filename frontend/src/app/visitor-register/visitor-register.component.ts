import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { VisitorKycService } from '../services/visitor-kyc.service';

type KycStep = 'id-entry' | 'otp-verification' | 'photo-capture' | 'kyc-complete';

interface VisitorRegistrationForm {
  fullName: string;
  phoneNumber: string;
  email: string;
  address: string;
  district: string;
  constituency: string;
  idType: 'EPIC' | 'AADHAAR' | '';
  epicNumber: string;
  aadhaarNumber: string;
  otp: string;
  livePhoto: string;
  photoFromId: string;
  kycStatus?: string;
}

@Component({
  selector: 'app-visitor-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './visitor-register.component.html',
  styleUrls: ['./visitor-register.component.scss'],
})
export class VisitorRegisterComponent implements OnDestroy {
  // Multi-step KYC flow
  currentStep: KycStep = 'id-entry';
  
  form: VisitorRegistrationForm = {
    fullName: '',
    phoneNumber: '',
    email: '',
    address: '',
    district: '',
    constituency: '',
    idType: '',
    epicNumber: '',
    aadhaarNumber: '',
    otp: '',
    livePhoto: '',
    photoFromId: '',
  };

  // UI state
  errorMsg = '';
  successMsg = '';
  loading = false;
  submitted = false;
  
  // KYC state
  idValidated = false;
  otpVerified = false;
  photoCaptured = false;
  kycStatus: 'PHOTO_MATCHED' | 'DEMOGRAPHIC_MATCHED' | 'FAILED' | '' = '';
  kycStatusMessage = '';
  maskedPhone = '';
  otpCode = '';
  
  // Camera state
  videoStream: MediaStream | null = null;
  showCamera = false;
  isCameraActive = false;
  capturedPhotoUrl = '';

  constructor(
    private http: HttpClient, 
    private router: Router,
    private kycService: VisitorKycService
  ) {}

  ngOnDestroy() {
    this.stopCamera();
  }

  // ── STEP 1: ID VALIDATION ───────────────────────────────────────────────

  get idNumber(): string {
    return this.form.idType === 'EPIC' ? this.form.epicNumber : this.form.aadhaarNumber;
  }

  get canValidateId(): boolean {
    if (this.form.idType === 'EPIC') {
      return /^[A-Za-z]{3}[0-9]{7}$/.test(this.form.epicNumber);
    }
    if (this.form.idType === 'AADHAAR') {
      return /^\d{12}$/.test(this.form.aadhaarNumber);
    }
    return false;
  }

  validateId() {
    if (!this.canValidateId) {
      this.errorMsg = 'Please enter a valid ID number.';
      return;
    }

    this.errorMsg = '';
    this.loading = true;

    this.kycService.validateVisitorId({
      idType: this.form.idType as 'EPIC' | 'AADHAAR',
      idNumber: this.idNumber,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success && res.otpSent) {
          this.idValidated = true;
          this.currentStep = 'otp-verification';
          this.maskedPhone = res.phoneNumber || '';
          this.successMsg = `OTP sent to ${this.maskedPhone}`;
        } else {
          this.errorMsg = res.message || 'ID validation failed.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Failed to validate ID. Please try again.';
      },
    });
  }

  // ── STEP 2: OTP VERIFICATION ────────────────────────────────────────────

  get canVerifyOtp(): boolean {
    return this.form.otp.length === 6;
  }

  verifyOtp() {
    if (!this.canVerifyOtp) {
      this.errorMsg = 'Please enter a 6-digit OTP.';
      return;
    }

    this.errorMsg = '';
    this.loading = true;

    this.kycService.verifyOtp({
      idNumber: this.idNumber,
      otp: this.form.otp,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success && res.demographics) {
          this.otpVerified = true;
          this.currentStep = 'photo-capture';
          
          // Auto-populate demographics
          this.form.fullName = res.demographics.name;
          this.form.address = res.demographics.address;
          this.form.district = res.demographics.district;
          this.form.constituency = res.demographics.constituency;
          this.form.photoFromId = res.demographics.photoFromId || '';
          
          this.successMsg = 'OTP verified! Demographics auto-populated.';
        } else {
          this.errorMsg = res.message || 'Invalid OTP. Please try again.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'OTP verification failed.';
      },
    });
  }

  // ── STEP 3: LIVE PHOTO CAPTURE ──────────────────────────────────────────

  async openCamera() {
    try {
      this.errorMsg = '';
      this.videoStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } }
      });
      
      this.showCamera = true;
      
      // Wait for next tick to ensure video element exists
      setTimeout(() => {
        const videoElement = document.getElementById('camera-preview') as HTMLVideoElement;
        if (videoElement && this.videoStream) {
          videoElement.srcObject = this.videoStream;
          videoElement.play();
        }
      }, 100);
    } catch (error) {
      this.errorMsg = 'Camera access denied. Please allow camera permissions.';
    }
  }

  capturePhoto() {
    const videoElement = document.getElementById('camera-preview') as HTMLVideoElement;
    if (!videoElement) {
      this.errorMsg = 'Camera not initialized.';
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = videoElement.videoWidth;
    canvas.height = videoElement.videoHeight;
    
    const context = canvas.getContext('2d');
    if (!context) {
      this.errorMsg = 'Failed to capture photo.';
      return;
    }

    context.drawImage(videoElement, 0, 0);
    this.form.livePhoto = canvas.toDataURL('image/jpeg', 0.8);
    this.photoCaptured = true;
    this.stopCamera();
    this.showCamera = false;
    this.successMsg = 'Photo captured successfully!';
  }

  retakePhoto() {
    this.form.livePhoto = '';
    this.photoCaptured = false;
    this.openCamera();
  }

  stopCamera() {
    if (this.videoStream) {
      this.videoStream.getTracks().forEach(track => track.stop());
      this.videoStream = null;
    }
  }

  // ── STEP 4: FACE VALIDATION ─────────────────────────────────────────────

  validateFace() {
    if (!this.form.livePhoto) {
      this.errorMsg = 'Please capture your live photo first.';
      return;
    }

    this.errorMsg = '';
    this.loading = true;

    this.kycService.validateFace({
      idNumber: this.idNumber,
      livePhoto: this.form.livePhoto,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.kycStatus = res.kycStatus;
          this.currentStep = 'kyc-complete';
          this.successMsg = res.kycStatus === 'PHOTO_MATCHED' 
            ? 'KYC Verified (Photo Match)' 
            : 'KYC Verified (Demographic Match)';
          
          // Auto-submit registration
          setTimeout(() => this.submitRegistration(), 1500);
        } else {
          this.errorMsg = res.message || 'Face validation failed.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Face validation failed.';
      },
    });
  }

  // ── FINAL SUBMISSION ────────────────────────────────────────────────────

  submitRegistration() {
    this.loading = true;
    const payload: Record<string, string> = {
      fullName: this.form.fullName.trim(),
      phoneNumber: this.form.phoneNumber || this.maskedPhone.replace(/\*+/g, '').trim(),
      email: this.form.email.trim(),
      address: this.form.address.trim(),
      district: this.form.district.trim(),
      constituency: this.form.constituency.trim(),
      kycStatus: this.kycStatus,
    };

    if (this.form.idType === 'EPIC') {
      payload['epicNumber'] = this.form.epicNumber.trim().toUpperCase();
    } else if (this.form.idType === 'AADHAAR') {
      payload['aadhaarNumber'] = this.form.aadhaarNumber.trim();
    }

    if (this.form.livePhoto) {
      payload['livePhoto'] = this.form.livePhoto;
    }

    this.http.post<{ success: boolean; message: string }>('/api/v1/visitor/auth/register', payload).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.submitted = true;
          this.successMsg = res.message || 'Registration completed with KYC verification!';
        } else {
          this.errorMsg = res.message || 'Registration failed.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Registration failed.';
      },
    });
  }

  // ── NAVIGATION ──────────────────────────────────────────────────────────

  goToLogin() {
    this.router.navigate(['/public-login']);
  }

  // ── INPUT SANITIZATION ──────────────────────────────────────────────────

  sanitizeNumericInput(field: 'phoneNumber' | 'aadhaarNumber') {
    this.form[field] = this.form[field].replace(/\D/g, '');
  }

  sanitizeOtpInput() {
    this.otpCode = this.otpCode.replace(/\D/g, '');
  }

  sanitizeEpicInput() {
    this.form.epicNumber = this.form.epicNumber.toUpperCase();
  }

  resetForm() {
    this.currentStep = 'id-entry';
    this.idValidated = false;
    this.otpVerified = false;
    this.photoCaptured = false;
    this.kycStatus = '';
    this.form = {
      fullName: '',
      phoneNumber: '',
      email: '',
      address: '',
      district: '',
      constituency: '',
      idType: '',
      epicNumber: '',
      aadhaarNumber: '',
      otp: '',
      livePhoto: '',
      photoFromId: '',
    };
    this.errorMsg = '';
    this.successMsg = '';
  }
}
