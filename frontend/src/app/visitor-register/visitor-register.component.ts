import { Component, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { VisitorKycService } from '../services/visitor-kyc.service';

type KycStep = 'id-entry' | 'otp-verification' | 'photo-capture' | 'additional-details' | 'kyc-complete';

interface VisitorRegistrationForm {
  fullName: string;
  phoneNumber: string;
  email: string;
  address: string;
  district: string;
  constituency: string;
  booth: string;
  village: string;
  designation: string;
  outsideState: boolean;
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
    booth: '',
    village: '',
    designation: '',
    outsideState: false,
    idType: '',
    epicNumber: '',
    aadhaarNumber: '',
    otp: '',
    livePhoto: '',
    photoFromId: '',
  };

  // Reference data
  designations = [
    'Govt Servant', 'Retd Govt Servant', 'Teacher', 'Political Leader',
    'Students', 'Religious Leader', 'Businessman', 'Media', 'General Public',
    'Organisation – Village Authority', 'Teachers Body', 'Civil Society / NGO',
    'Institute', 'Others'
  ];
  districts = [
    'East Khasi Hills', 'West Khasi Hills', 'South West Khasi Hills', 'Ri Bhoi',
    'East Jaintia Hills', 'West Jaintia Hills', 'East Garo Hills', 'West Garo Hills',
    'South Garo Hills', 'North Garo Hills', 'Eastern West Khasi Hills'
  ];

  // UI state
  errorMsg = '';
  successMsg = '';
  loading = false;
  submitted = false;
  
  // KYC state
  idValidated = false;
  otpSent = false;  // Flag to show OTP field
  otpVerified = false;
  photoCaptured = false;
  kycStatus: 'PHOTO_MATCHED' | 'DEMOGRAPHIC_MATCHED' | 'FAILED' | 'MANUAL_VERIFICATION_REQUIRED' | '' = '';
  kycStatusMessage = '';
  maskedPhone = '';
  otpCode = '';
  manualPhone = '';  // Optional mobile number for manual verification
  manualVerification = false;  // Flag if manual mobile was provided
  actualPhoneNumber = '';  // Store actual 10-digit phone number
  
  // Camera state
  videoStream: MediaStream | null = null;
  showCamera = false;
  isCameraActive = false;
  capturedPhotoUrl = '';

  constructor(
    private http: HttpClient, 
    private router: Router,
    private kycService: VisitorKycService,
    private cdr: ChangeDetectorRef
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

    // Check if manual phone number is provided and valid
    if (this.manualPhone && this.manualPhone.length !== 10) {
      this.errorMsg = 'Manual mobile number must be exactly 10 digits.';
      return;
    }

    this.errorMsg = '';
    this.loading = true;

    // Build request with optional phoneNumber
    const request: any = {
      idType: this.form.idType as 'EPIC' | 'AADHAAR',
      idNumber: this.idNumber,
    };

    // Add manual phone number if provided
    if (this.manualPhone && this.manualPhone.length === 10) {
      request.phoneNumber = this.manualPhone;
      this.manualVerification = true;
    }

    this.kycService.validateVisitorId(request).subscribe({
      next: res => {
        this.loading = false;
        if (res.success && res.otpSent) {
          this.idValidated = true;
          this.otpSent = true;  // Show OTP field
          this.maskedPhone = res.phoneNumber || '';
          this.actualPhoneNumber = res.actualPhoneNumber || this.manualPhone || '';  // Store actual phone
          this.manualVerification = res.manualVerification || false;
          this.successMsg = `OTP sent to ${this.maskedPhone}`;
          
          // Show manual verification warning if applicable
          if (this.manualVerification) {
            this.successMsg += ' (Manual verification required)';
          }
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

  resendOtp() {
    this.otpCode = '';
    this.otpSent = false;
    this.validateId();
  }

  // ── STEP 2: OTP VERIFICATION ────────────────────────────────────────────

  get canVerifyOtp(): boolean {
    return this.otpCode.length === 6;
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
      otp: this.otpCode,
    }).subscribe({
      next: res => {
        this.loading = false;
        
        if (res.success && res.demographics) {
          this.otpVerified = true;
          
          // Auto-populate demographics
          const demo = res.demographics;
          
          // Use Object.assign to update form to ensure Angular tracks changes
          Object.assign(this.form, {
            fullName: demo.fullName || '',
            address: demo.address || '',
            district: demo.district || '',
            constituency: demo.constituency || '',
            photoFromId: demo.photoFromId || '',
            phoneNumber: this.actualPhoneNumber  // Use stored actual phone number
          });
          
          this.successMsg = 'OTP verified! Demographics auto-populated.';
          
          // Force change detection and transition to next step
          this.cdr.detectChanges();
          setTimeout(() => {
            this.currentStep = 'photo-capture';
            this.cdr.detectChanges();
          }, 200);
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
      
      this.isCameraActive = true;  // Changed from showCamera to isCameraActive
      
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
    const photoData = canvas.toDataURL('image/jpeg', 0.8);
    this.form.livePhoto = photoData;
    this.capturedPhotoUrl = photoData;  // Set for display
    this.photoCaptured = true;
    this.stopCamera();
    this.isCameraActive = false;  // Changed from showCamera to isCameraActive
    this.successMsg = 'Photo captured successfully!';
  }

  retakePhoto() {
    this.form.livePhoto = '';
    this.capturedPhotoUrl = '';
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
          this.form.kycStatus = res.kycStatus;
          // If manual verification was used, override kycStatus
          if (this.manualVerification) {
            this.kycStatus = 'MANUAL_VERIFICATION_REQUIRED';
            this.form.kycStatus = 'MANUAL_VERIFICATION_REQUIRED';
          }
          this.currentStep = 'additional-details';
          this.successMsg = res.kycStatus === 'PHOTO_MATCHED'
            ? 'KYC Verified – Photo Match Successful'
            : 'KYC Verified – Demographic Match';
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
    const payload: Record<string, string | boolean> = {
      fullName: this.form.fullName.trim(),
      phoneNumber: this.form.phoneNumber || this.maskedPhone.replace(/\*+/g, '').trim(),
      email: this.form.email.trim(),
      address: this.form.address.trim(),
      district: this.form.outsideState ? 'NA' : this.form.district.trim(),
      constituency: this.form.outsideState ? 'NA' : this.form.constituency.trim(),
      booth: this.form.outsideState ? 'NA' : (this.form.booth || '').trim(),
      village: this.form.outsideState ? 'NA' : (this.form.village || '').trim(),
      designation: this.form.designation,
      kycStatus: this.form.kycStatus || this.kycStatus || 'PENDING',
    };

    if (this.form.idType === 'EPIC') {
      payload['epicNumber'] = this.form.epicNumber.trim().toUpperCase();
    } else if (this.form.idType === 'AADHAAR') {
      payload['aadhaarNumber'] = this.form.aadhaarNumber.trim();
    }

    if (this.form.livePhoto) {
      payload['livePhotoBase64'] = this.form.livePhoto;
    }

    if (this.manualVerification) {
      payload['manualVerification'] = true;
    }

    this.http.post<{ success: boolean; message: string }>('/api/v1/visitor/auth/register', payload).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.submitted = true;
          this.currentStep = 'kyc-complete';
          this.successMsg = 'Visitor registration completed successfully.';
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

  sanitizeManualPhone() {
    this.manualPhone = this.manualPhone.replace(/\D/g, '');
  }

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
    this.otpSent = false;
    this.otpVerified = false;
    this.photoCaptured = false;
    this.kycStatus = '';
    this.manualPhone = '';
    this.manualVerification = false;
    this.form = {
      fullName: '',
      phoneNumber: '',
      email: '',
      address: '',
      district: '',
      constituency: '',
      booth: '',
      village: '',
      designation: '',
      outsideState: false,
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
