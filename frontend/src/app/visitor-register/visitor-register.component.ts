import { Component, OnDestroy, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { VisitorKycService } from '../services/visitor-kyc.service';
import { AuthService } from '../services/auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';

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
  visitorName: string;     // Name as on voter card (for EPIC verification)
  aadhaarNumber: string;
  otp: string;
  livePhoto: string;
  photoFromId: string;
  kycStatus?: string;
}

@Component({
  selector: 'app-visitor-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatRadioModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatStepperModule
  ],
  templateUrl: './visitor-register.component.html',
  styleUrls: ['./visitor-register.component.scss'],
})
export class VisitorRegisterComponent implements OnInit, OnDestroy {
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
    visitorName: '',
    aadhaarNumber: '',
    otp: '',
    livePhoto: '',
    photoFromId: '',
  };

  // Reference data
  designations: { code: string; value: string }[] = [];
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

  // KYC confidence indicator (R009)
  kycConfidenceScore = 0;
  kycConfidenceLabel = '';

  // DEO mode (R011) – true when accessed from /deo/register-visitor
  isDeoMode = false;

  // Hide left panel for staff members entering visitor data
  hideLeftPanel = false;

  // OVSE QR state
  showQrCode = false;
  qrDataUri = '';
  currentTxnId = '';
  pollingInterval: any = null;
  maxPollingAttempts = 60;  // 60 attempts × 2 seconds = 120 seconds (2 minutes)
  pollingAttempts = 0;
  pollingCountdown = 0;  // Display countdown timer

  constructor(
    private http: HttpClient, 
    private router: Router,
    private route: ActivatedRoute,
    private kycService: VisitorKycService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    // Detect DEO mode from route snapshot URL segments
    this.isDeoMode = this.route.snapshot.url.some(segment => segment.path === 'register-visitor');
    
    // Hide left panel if staff member (non-PUBLIC) is logged in and accessing through DEO route
    if (this.isDeoMode && this.auth.isLoggedIn()) {
      const userRole = this.auth.user()?.role;
      this.hideLeftPanel = userRole !== 'PUBLIC' && userRole !== undefined;
    }
  }

  ngOnInit() {
    this.loadDesignations();
  }

  ngOnDestroy() {
    this.stopCamera();
  }

  loadDesignations() {
    this.kycService.getCitizenDesignations().subscribe({
      next: res => {
        this.designations = Array.isArray(res) ? res : [];
      },
      error: () => {
        this.designations = [];
      }
    });
  }

  // ── STEP 1: ID VALIDATION ───────────────────────────────────────────────

  get idNumber(): string {
    return this.form.idType === 'EPIC' ? this.form.epicNumber : this.form.aadhaarNumber;
  }

  get canValidateId(): boolean {
    if (this.form.idType === 'EPIC') {
      const hasValidEpic = /^[A-Za-z]{3}[0-9]{7}$/.test(this.form.epicNumber);
      const hasValidName = this.form.visitorName && this.form.visitorName.trim().length > 0;
      return hasValidEpic && !!hasValidName;
    }
    if (this.form.idType === 'AADHAAR') {
      return /^\d{12}$/.test(this.form.aadhaarNumber);
    }
    return false;
  }

  validateId() {
    if (!this.canValidateId) {
      this.errorMsg = 'Please enter a valid ID number and visitor name.';
      return;
    }

    // Check if manual phone number is provided and valid
    if (this.manualPhone && this.manualPhone.length !== 10) {
      this.errorMsg = 'Manual mobile number must be exactly 10 digits.';
      return;
    }

    this.errorMsg = '';
    this.loading = true;

    // Check if Aadhaar OVSE flow should be used
    if (this.form.idType === 'AADHAAR') {
      this.generateAadhaarQr();
      return;
    }

    // Handle EPIC verification
    if (this.form.idType === 'EPIC') {
      this.verifyEpic();
      return;
    }

    // Fallback for other ID types (should not reach here)
    this.loading = false;
    this.errorMsg = 'Unsupported ID type';
  }

  /**
   * Verify EPIC number against Election Commission API
   */
  verifyEpic() {
    const epicRequest = {
      epicNumber: this.form.epicNumber,
      visitorName: this.form.visitorName.toUpperCase(),
      phoneNumber: this.manualPhone || ''
    };

    this.kycService.verifyEpic(epicRequest).subscribe({
      next: res => {
        this.loading = false;
        
        if (res.code === '200' && res.data) {
          // EPIC verification successful with name match
          this.errorMsg = '';
          this.successMsg = '✓ EPIC verified successfully';
          
          // Extract verified details from response data
          const verifiedName = res.data?.borrowernameonvoteridcard || this.form.visitorName;
          const district = res.data?.borroweraddressdistrict || '';
          const state = res.data?.borroweraddressstate || '';
          const nameMatchScore = res.data?.namematchscore || 0;
          
          // Auto-populate form with verified details
          this.form.fullName = verifiedName;
          this.form.district = district;
          this.form.address = state + (district ? ', ' + district : '');
          
          // Log AI KYC score for display
          this.kycConfidenceScore = Math.min(nameMatchScore * 10, 100);
          
          // If manual phone was provided, require manual verification
          if (this.manualPhone && this.manualPhone.length === 10) {
            this.manualVerification = true;
            this.maskedPhone = this.maskPhone(this.manualPhone);
            this.actualPhoneNumber = this.manualPhone;
            this.successMsg += ' (Manual verification required - OTP will be sent)';
            this.otpSent = true;
            this.currentStep = 'otp-verification';
          } else {
            // No manual phone: automated OTP sending would happen here
            // For now, move to next step after EPIC verification
            this.idValidated = true;
            this.currentStep = 'photo-capture';  // Skip OTP if registered phone is available
          }
        } else if (res.code === '400') {
          // Name mismatch or validation error
          this.errorMsg = '✗ Name Verification Failed: ' + (res.message || 'Name does not match EPIC card');
          this.successMsg = '';
          // Show verified details for user reference (if available)
          if (res.data) {
            const verifiedName = res.data.borrowernameonvoteridcard || '';
            const verificationStatus = res.data.voteridverificationstatus || '';
            const matchScore = res.data.namematchscore || 0;
            this.errorMsg += `\n\nVerified Name: ${verifiedName}\nMatch Score: ${matchScore}%\nEPIC Status: ${verificationStatus}`;
          }
        } else {
          // EPIC verification failed or other error
          const errorMsg = res.message || 'EPIC verification failed';
          this.errorMsg = '✗ ' + errorMsg;
          this.successMsg = '';
          this.loading = false;
        }
      },
      error: err => {
        this.loading = false;
        const errorMsg = err?.error?.message || err?.message || 'Failed to verify EPIC. Please try again.';
        this.errorMsg = '✗ ' + errorMsg;
      }
    });
  }

  resendOtp() {
    this.otpCode = '';
    this.otpSent = false;
    this.validateId();
  }

  // ── OVSE AADHAAR QR FLOW ────────────────────────────────────────────────

  /**
   * Generate OVSE QR code for Aadhaar verification.
   * QR is generic and can be scanned by any user with the Aadhaar app.
   * No Aadhaar or mobile number required for QR generation.
   */
  generateAadhaarQr() {
    this.errorMsg = '';
    this.successMsg = '';
    this.loading = true;

    // Generate QR code (backend will use OVSE SDK with appId + txnId configuration)
    this.kycService.generateAadhaarQr().subscribe({
      next: res => {
        this.loading = false;
        if (res.success && res.qrDataUri) {
          this.qrDataUri = res.qrDataUri;
          this.currentTxnId = res.txnId;
          this.maskedPhone = res.maskedMobile || '';
          this.showQrCode = true;
          this.idValidated = true;
          this.successMsg = 'QR code generated! Scan with your Aadhaar app to verify your identity.';
          
          // Start polling for KYC result
          this.startPollingKycResult();
        } else {
          this.errorMsg = res.errorMessage || 'Failed to generate QR code. Please try again.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.errorMessage || 'QR code generation failed. OVSE service may be unavailable.';
      },
    });
  }

  /**
   * Poll for Aadhaar KYC verification result.
   * Called repeatedly until result is available or max attempts reached.
   * Waits for user to scan QR with Aadhaar app and complete verification.
   */
  startPollingKycResult() {
    this.pollingAttempts = 0;
    this.successMsg = 'QR code ready! Please scan with your Aadhaar app...';
    this.errorMsg = '';

    this.pollingInterval = setInterval(() => {
      this.pollingAttempts++;
      this.pollingCountdown = (this.maxPollingAttempts - this.pollingAttempts) * 2;  // Remaining seconds

      if (this.pollingAttempts > this.maxPollingAttempts) {
        clearInterval(this.pollingInterval);
        this.errorMsg = 'QR scan timeout (2 minutes). Please try again or use manual verification.';
        this.showQrCode = false;
        return;
      }

      // Poll for result from OVSE callback
      this.kycService.getAadhaarKycResult(this.currentTxnId).subscribe({
        next: res => {
          if (res && !res.error && res.claims) {
            // ✓ KYC verification successful!
            clearInterval(this.pollingInterval);
            this.successMsg = '✓ Aadhaar verification successful! Loading your details...';
            this.handleAadhaarKycSuccess(res);
          } else if (res && res.error) {
            // ✗ User rejected or error occurred
            clearInterval(this.pollingInterval);
            this.errorMsg = `KYC verification failed: ${res.errorMessage || res.errorCode}`;
            this.showQrCode = false;
          }
          // If no result yet (404 or null), keep polling
        },
        error: err => {
          // Continue polling on 404 (result not yet available)
          // Only stop on max attempts
          if (this.pollingAttempts >= this.maxPollingAttempts) {
            clearInterval(this.pollingInterval);
            this.errorMsg = 'Verification timeout. Please try again or contact support.';
            this.showQrCode = false;
          }
        },
      });
    }, 2000);  // Poll every 2 seconds for faster feedback
  }

  /**
   * Handle successful Aadhaar KYC verification.
   * Populate form with disclosed claims from Aadhaar app.
   */
  handleAadhaarKycSuccess(kycData: any) {
    clearInterval(this.pollingInterval);

    // Extract claims from KYC data
    const claims = kycData.claims || {};

    // Populate form with verified data
    Object.assign(this.form, {
      fullName: claims.residentName || claims.localResidentName || '',
      phoneNumber: claims.mobile || claims.maskedMobile || this.maskedPhone || '',
      email: claims.email || claims.maskedEmail || '',
      address: claims.address || claims.regionalAddress || '',
      kycStatus: 'PHOTO_MATCHED',
      livePhoto: claims.residentImage ? `data:image/jpeg;base64,${claims.residentImage}` : '',
    });

    // Set captured photo URL for display
    if (claims.residentImage) {
      this.capturedPhotoUrl = `data:image/jpeg;base64,${claims.residentImage}`;
      this.photoCaptured = true;
    }

    // Set KYC status
    this.kycStatus = 'PHOTO_MATCHED';
    this.kycConfidenceScore = 90;
    this.kycConfidenceLabel = 'Verified';
    this.form.kycStatus = 'PHOTO_MATCHED';

    // Clear QR display
    this.showQrCode = false;
    this.successMsg = 'Aadhaar KYC verified successfully! Your details have been pre-filled.';
    this.errorMsg = '';

    // Move to next step
    this.cdr.detectChanges();
    setTimeout(() => {
      this.currentStep = 'additional-details';
      this.cdr.detectChanges();
    }, 500);
  }

  /**
   * Cancel Aadhaar QR verification and go back.
   */
  cancelAadhaarQr() {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
    this.showQrCode = false;
    this.qrDataUri = '';
    this.currentTxnId = '';
    this.currentStep = 'id-entry';
    this.errorMsg = '';
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
      phoneNumber: this.actualPhoneNumber,
      idType: this.form.idType,
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
          // R009: set KYC confidence score and label
          this.kycConfidenceScore = (res as any).confidenceScore ?? (this.kycStatus === 'PHOTO_MATCHED' ? 92 : this.kycStatus === 'DEMOGRAPHIC_MATCHED' ? 75 : 45);
          this.kycConfidenceLabel = this.kycStatus === 'PHOTO_MATCHED' ? 'Verified' : this.kycStatus === 'DEMOGRAPHIC_MATCHED' ? 'Verified (Demographic)' : 'Manual Verification Required';
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

    this.http.post<{ success: boolean; message: string }>(`${environment.apiUrl}/visitor/auth/register`, payload).subscribe({
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
    if (this.isDeoMode) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/public-login']);
    }
  }

  goBackToIdEntry() {
    this.currentStep = 'id-entry';
    this.otpSent = false;
    this.otpCode = '';
    this.errorMsg = '';
  }

  goBackToOtpVerification() {
    this.currentStep = 'otp-verification';
    this.errorMsg = '';
  }

  /**
   * Reset all KYC-related state when switching between EPIC and AADHAAR
   * This clears any previous errors, operations, and ongoing processes
   */
  onIdTypeChange(idType: VisitorRegistrationForm['idType']) {
    this.form.idType = idType;

    // Clear error and success messages
    this.errorMsg = '';
    this.successMsg = '';

    // Reset OTP-related state
    this.otpSent = false;
    this.otpCode = '';
    this.otpVerified = false;
    this.maskedPhone = '';
    this.manualPhone = '';
    this.manualVerification = false;
    this.actualPhoneNumber = '';

    // Stop camera and clear photo capture state
    this.stopCamera();
    this.isCameraActive = false;
    this.capturedPhotoUrl = '';
    this.photoCaptured = false;

    // Clear AADHAAR QR-related state
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
    this.showQrCode = false;
    this.qrDataUri = '';
    this.currentTxnId = '';
    this.pollingAttempts = 0;
    this.pollingCountdown = 0;

    // Reset ID validation and KYC state
    this.idValidated = false;
    this.kycStatus = '';
    this.kycConfidenceScore = 0;
    this.kycConfidenceLabel = '';
    this.loading = false;

    // Clear ID-specific form fields
    this.form.epicNumber = '';
    this.form.visitorName = '';
    this.form.aadhaarNumber = '';
    this.form.livePhoto = '';
    this.form.photoFromId = '';

    // Clear auto-populated fields so user can enter fresh data
    // But preserve other form data like email, designation, etc.
    // Only reset fields that are auto-populated during KYC
    this.form.fullName = '';
    this.form.address = '';

    // Keep the current step at 'id-entry' to show the form again
    this.currentStep = 'id-entry';
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

  /**
   * Mask phone number for display: XXXX-XXXX-5678
   * Shows only last 4 digits
   */
  maskPhone(phone: string): string {
    if (!phone || phone.length < 4) return '****-****-****';
    return 'XXXX-XXXX-' + phone.substring(phone.length - 4);
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
      visitorName: '',
      aadhaarNumber: '',
      otp: '',
      livePhoto: '',
      photoFromId: '',
    };
    this.errorMsg = '';
    this.successMsg = '';
  }
}
