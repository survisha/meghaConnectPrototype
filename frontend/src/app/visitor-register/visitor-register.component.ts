import { Component, OnDestroy, ChangeDetectorRef, OnInit, ElementRef, ViewChild } from '@angular/core';
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
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';
import { apiErrorMessage } from '../shared/api-error.util';
import { CameraCaptureService, CameraDeviceOption, CameraFacingMode } from '../shared/camera-capture.service';
import { CameraLivenessService } from '../shared/camera-liveness.service';
import {
  FACE_LIVENESS_INITIAL_RESULT,
  FaceLivenessCaptureMetadata,
  FaceLivenessResult,
} from '../shared/face-liveness-result.model';
import { ReferenceDataService } from '../services/reference-data.service';

type KycStep = 'id-entry' | 'otp-verification' | 'photo-capture' | 'additional-details' | 'kyc-complete';
type MobileValidationType = 'warning' | 'error' | 'success' | '';

interface VisitorRegistrationForm {
  fullName: string;
  phoneNumber: string;
  email: string;
  address: string;
  fullAddress: string;
  address1: string;
  addressLine: string;
  city: string;
  state: string;
  pincode: string;
  district: string;
  constituency: string;
  booth: string;
  boothVillage: string;
  village: string;
  location: string;
  designation: string;
  gender: string;
  dateOfBirth: string;
  outsideState: boolean;
  idType: 'EPIC' | 'AADHAAR' | 'NONE' | '';
  epicNumber: string;
  visitorName: string;     // Name as on voter card (for EPIC verification)
  aadhaarNumber: string;
  otp: string;
  livePhoto: string;
  photoFromId: string;
  kycStatus?: string;
  kycProvider?: string;
  kycFailureReason?: string;
  kycRequestId?: string;
  borrowerAddressHouseNumber?: string;
  borrowerAddressSectionNumber?: string;
  assemblyConstituencyNumber?: string;
  assemblyConstituencyName?: string;
  relativeNameOnVoterId?: string;
  pollingPartNo?: string;
  pollingStationAddress?: string;
  voterIdVerificationRequestId?: string;
  voterIdVerificationCompletionTimestamp?: string;
  nameMatchScore?: number;
  idFound?: boolean;
  aadhaarClientTxnId?: string;
  aadhaarAppId?: string;
  maskedIdentityNumber?: string;
  agendaType?: string;
  briefDescription?: string;
}

interface RegistrationCheckResponse {
  success: boolean;
  mobileExists: boolean;
  epicMobileExists: boolean;
  epicExists: boolean;
  message: string;
}

interface VerifiedKycData {
  kycVerified: boolean;
  kycType: 'EPIC' | 'AADHAAR' | 'NONE';
  kycReferenceId?: string;
  visitorName?: string;
  mobile?: string;
  gender?: string;
  dob?: string;
  state?: string;
  district?: string;
  constituency?: string;
  boothVillage?: string;
  address?: string;
  fullAddress?: string;
  address1?: string;
  city?: string;
  pincode?: string;
  epicNumber?: string;
  maskedIdentityNumber?: string;
  houseNumber?: string;
  sectionNumber?: string;
  assemblyConstituencyNumber?: string;
  assemblyConstituencyName?: string;
  relativeName?: string;
  pollingPartNo?: string;
  pollingStationAddress?: string;
  agendaType?: string;
  briefDescription?: string;
  nameMatchScore?: number;
  idFound?: boolean;
  voterIdVerificationCompletionTimestamp?: string;
  aadhaarClientTxnId?: string;
  aadhaarAppId?: string;
  kycFailureReason?: string;
  kycRequestId?: string;
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
    MatStepperModule,
    TranslateModule,
    LanguageSelectorComponent
  ],
  templateUrl: './visitor-register.component.html',
  styleUrls: ['./visitor-register.component.scss'],
})
export class VisitorRegisterComponent implements OnInit, OnDestroy {
  @ViewChild('visitorNameInput') visitorNameInput?: ElementRef<HTMLInputElement>;

  private readonly epicPattern = /^[A-Z]{3}[0-9]{7}$/;
  private readonly namePattern = /^[A-Za-z]+(?: [A-Za-z]+)*$/;

  // Multi-step KYC flow
  currentStep: KycStep = 'id-entry';
  
  form: VisitorRegistrationForm = {
    fullName: '',
    phoneNumber: '',
    email: '',
    address: '',
    fullAddress: '',
    address1: '',
    addressLine: '',
    city: '',
    state: '',
    pincode: '',
    district: '',
    constituency: '',
    booth: '',
    boothVillage: '',
    village: '',
    location: '',
    designation: '',
    gender: '',
    dateOfBirth: '',
    outsideState: false,
    idType: '',
    epicNumber: '',
    visitorName: '',
    aadhaarNumber: '',
    otp: '',
    livePhoto: '',
    photoFromId: '',
    agendaType: '',
    briefDescription: '',
  };

  // Reference data
  designations: { code: string; value: string }[] = [];
  agendaTypes: string[] = [];

  // UI state
  errorMsg = '';
  successMsg = '';
  loading = false;
  submitted = false;
  mobileValidationMsg = '';
  mobileValidationType: MobileValidationType = '';
  mobileCheckLoading = false;
  epicTouched = false;
  nameTouched = false;
  mobileTouched = false;
  epicRejectedInput = false;
  nameRejectedInput = false;
  duplicateRegistrationBlocked = false;
  verifiedKycData: VerifiedKycData | null = null;
  districtAutoPopulated = false;
  constituencyAutoPopulated = false;
  boothVillageAutoPopulated = false;
  isAadhaarFlow = false;
  hasAadhaarResidentImage = false;
  aadhaarResidentImage = '';
  
  // KYC state
  idValidated = false;
  otpSent = false;  // Flag to show OTP field
  otpVerified = false;
  verifiedMobileNumber: string | null = null;
  otpValidatedAt: Date | null = null;
  photoCaptured = false;
  kycStatus: 'PHOTO_MATCHED' | 'DEMOGRAPHIC_MATCHED' | 'FAILED' | 'MANUAL_VERIFICATION_REQUIRED' | 'KYC_PENDING' | '' = '';
  kycStatusMessage = '';
  maskedPhone = '';
  otpCode = '';
  manualPhone = '';  // Optional mobile number for manual verification
  manualVerification = false;  // Flag if manual mobile was provided
  actualPhoneNumber = '';  // Store actual 10-digit phone number
  kycPendingAllowed = false;
  kycPendingProvider: 'EPIC' | 'AADHAAR' | '' = '';
  kycPendingReason = '';
  kycPendingRequestId = '';
  
  // Camera state
  videoStream: MediaStream | null = null;
  showCamera = false;
  isCameraActive = false;
  capturedPhotoUrl = '';
  faceLivenessResult: FaceLivenessResult = { ...FACE_LIVENESS_INITIAL_RESULT };
  faceLivenessMetadata: FaceLivenessCaptureMetadata | null = null;
  cameraFacingMode: CameraFacingMode = 'user';
  cameraOptions: CameraDeviceOption[] = [];
  selectedCameraDeviceId = '';
  private livenessFrameId: number | null = null;
  private livenessAnalysisInProgress = false;
  private lastLivenessAnalysisAt = 0;
  private registrationOtpRequestId = 0;
  private readonly handleCameraDeviceChange = () => {
    void this.loadCameraDevices(true);
  };

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
    private cdr: ChangeDetectorRef,
    private translate: TranslateService,
    private cameraCapture: CameraCaptureService,
    private cameraLiveness: CameraLivenessService,
    private referenceDataService: ReferenceDataService
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
    this.loadAgendaTypes();
    this.loadCameraDevices();
    navigator.mediaDevices?.addEventListener?.('devicechange', this.handleCameraDeviceChange);
  }

  ngOnDestroy() {
    navigator.mediaDevices?.removeEventListener?.('devicechange', this.handleCameraDeviceChange);
    this.stopCamera();
  }

  private t(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
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
      const hasValidEpic = this.epicPattern.test(this.form.epicNumber);
      const hasValidName = this.isValidName(this.form.visitorName);
      return hasValidEpic && !!hasValidName;
    }
    if (this.form.idType === 'AADHAAR') {
      return /^\d{12}$/.test(this.form.aadhaarNumber);
    }
    if (this.form.idType === 'NONE') {
      return this.isValidName(this.form.fullName) && this.isManualPhoneValid && !this.duplicateRegistrationBlocked;
    }
    return false;
  }

  loadAgendaTypes() {
    this.referenceDataService.getByType('CM_AGENDA_MEETING').subscribe({
      next: data => {
        this.agendaTypes = (data || []).map(item => item.value).filter(Boolean);
      },
      error: err => {
        this.agendaTypes = [];
        this.errorMsg = apiErrorMessage(err, 'Unable to load agenda types.');
      }
    });
  }

  get isManualPhoneValid(): boolean {
    return /^\d{10}$/.test(this.manualPhone);
  }

  get currentOtpMobileNumber(): string {
    return this.actualPhoneNumber || this.manualPhone || this.form.phoneNumber || '';
  }

  get isCurrentMobileOtpVerified(): boolean {
    return this.otpVerified && this.verifiedMobileNumber === this.currentOtpMobileNumber;
  }

  get hasVisibleErrorMessage(): boolean {
    return !!this.errorMsg || (this.mobileValidationType === 'error' && !!this.mobileValidationMsg);
  }

  get canSendEpicOtp(): boolean {
    return this.canValidateId
      && this.isManualPhoneValid
      && !this.duplicateRegistrationBlocked
      && !this.hasVisibleErrorMessage
      && !this.isCurrentMobileOtpVerified;
  }

  get mobileValidationIcon(): string {
    if (this.mobileValidationType === 'error') return 'error';
    if (this.mobileValidationType === 'warning') return 'warning';
    if (this.mobileValidationType === 'success') return 'check_circle';
    return 'info';
  }

  validateId() {
    if (!this.canValidateId) {
      this.markIdStepTouched();
      this.errorMsg = this.primaryValidationMessage || this.t('ERROR_INVALID_ID_AND_NAME');
      return;
    }

    if (this.form.idType === 'EPIC' && !this.isManualPhoneValid) {
      this.mobileValidationType = 'error';
      this.mobileValidationMsg = this.t('ERROR_VALID_10_DIGIT_MOBILE');
      this.errorMsg = this.t('ERROR_MOBILE_BEFORE_OTP');
      return;
    }

    this.errorMsg = '';
    this.loading = true;

    // Check if Aadhaar OVSE flow should be used
    if (this.form.idType === 'NONE') {
      this.checkRegistrationStatus(true);
      return;
    }

    // Check if Aadhaar OVSE flow should be used
    if (this.form.idType === 'AADHAAR') {
      this.generateAadhaarQr();
      return;
    }

    // Handle EPIC verification
    if (this.form.idType === 'EPIC') {
      this.checkRegistrationStatus(true);
      return;
    }

    // Fallback for other ID types (should not reach here)
    this.loading = false;
    this.errorMsg = this.t('ERROR_UNSUPPORTED_ID_TYPE');
  }

  /**
   * Verify EPIC number against Election Commission API
   */
  verifyEpic() {
    const requestId = ++this.registrationOtpRequestId;
    this.errorMsg = '';
    this.successMsg = '';
    this.otpSent = false;
    this.idValidated = false;

    const epicRequest = {
      epicNumber: this.form.epicNumber,
      visitorName: this.form.visitorName.toUpperCase(),
      phoneNumber: this.manualPhone || ''
    };

    this.kycService.verifyEpic(epicRequest).subscribe({
      next: res => {
        if (requestId !== this.registrationOtpRequestId) return;
        this.loading = false;
        
        if (res.code === '200' && res.data) {
          // EPIC verification successful with name match
          this.errorMsg = '';
          this.successMsg = this.t('EPIC_VERIFIED_SENDING_OTP');
          
          this.populateVisitorDetailsFromKycResponse(res, 'EPIC');
          this.kycConfidenceScore = Math.min(this.verifiedKycData?.nameMatchScore ?? 0, 100);
          
          this.idValidated = true;
          this.maskedPhone = this.maskPhone(this.manualPhone);
          this.actualPhoneNumber = this.manualPhone;
          this.form.phoneNumber = this.manualPhone;
          this.loading = true;
          this.generateRegistrationOtp(requestId);
        } else if (res.code === '400') {
          // Name mismatch or validation error
          this.errorMsg = res.message || this.t('ERROR_NAME_VERIFICATION_FAILED');
          this.successMsg = '';
          this.otpSent = false;
          this.idValidated = false;
        } else {
          // EPIC verification failed or other error
          const errorMsg = res.message || this.t('EPIC_VERIFICATION_FAILED');
          this.errorMsg = errorMsg;
          this.successMsg = '';
          this.otpSent = false;
          this.idValidated = false;
          this.loading = false;
        }
      },
      error: err => {
        if (requestId !== this.registrationOtpRequestId) return;
        this.loading = false;
        this.successMsg = '';
        this.otpSent = false;
        this.idValidated = false;
        if (this.isKycServiceUnavailable(err)) {
          this.offerKycPendingFallback('EPIC', apiErrorMessage(err, 'Election Commission API is currently unavailable'), err?.error?.requestId);
          return;
        }
        this.errorMsg = apiErrorMessage(err, this.t('ERROR_FAILED_VERIFY_EPIC_TRY'));
      }
    });
  }

  resendOtp() {
    this.resetOtpVerification();
    this.otpCode = '';
    this.errorMsg = '';
    this.successMsg = '';
    if ((this.form.idType !== 'EPIC' && this.form.idType !== 'NONE') || !this.isManualPhoneValid) {
      this.errorMsg = this.t('ERROR_MOBILE_BEFORE_OTP');
      return;
    }
    if (this.form.idType === 'EPIC' && !this.idValidated) {
      this.errorMsg = this.t('ERROR_FAILED_VERIFY_EPIC_TRY');
      return;
    }
    this.loading = true;
    this.generateRegistrationOtp(++this.registrationOtpRequestId);
  }

  private generateRegistrationOtp(requestId = ++this.registrationOtpRequestId) {
    this.http.post<{ success: boolean; otp?: string; message: string }>(
      `${environment.apiUrl}/visitor/auth/generate-otp`,
      {
        phoneNumber: this.actualPhoneNumber || this.manualPhone,
        purpose: 'REGISTRATION',
        registrationFlow: 'true',
      }
    ).subscribe({
      next: res => {
        if (requestId !== this.registrationOtpRequestId) return;
        this.loading = false;
        if (res.success) {
          this.resetOtpVerification();
          this.otpCode = '';
          this.otpSent = true;
          this.currentStep = 'otp-verification';
          this.successMsg = res.otp
            ? this.t('OTP_SENT_TO_DEMO', { phone: this.maskedPhone, otp: res.otp })
            : this.t('OTP_SENT_TO', { phone: this.maskedPhone });
        } else {
          this.errorMsg = res.message || this.t('ERROR_FAILED_GENERATE_OTP');
        }
      },
      error: err => {
        if (requestId !== this.registrationOtpRequestId) return;
        this.loading = false;
        this.successMsg = '';
        this.errorMsg = apiErrorMessage(err, this.t('ERROR_FAILED_GENERATE_OTP_TRY'));
      }
    });
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
          this.successMsg = this.t('QR_CODE_GENERATED');
          
          // Start polling for KYC result
          this.startPollingKycResult();
        } else {
          this.offerKycPendingFallback('AADHAAR', res.errorMessage || this.t('ERROR_FAILED_GENERATE_QR'), res.requestId);
        }
      },
      error: err => {
        this.loading = false;
        if (this.isKycServiceUnavailable(err)) {
          this.offerKycPendingFallback('AADHAAR', apiErrorMessage(err, this.t('ERROR_QR_GENERATION_FAILED')), err?.error?.requestId);
          return;
        }
        this.errorMsg = apiErrorMessage(err, this.t('ERROR_QR_GENERATION_FAILED'));
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
    this.successMsg = this.t('QR_CODE_READY_SCAN');
    this.errorMsg = '';

    this.pollingInterval = setInterval(() => {
      this.pollingAttempts++;
      this.pollingCountdown = (this.maxPollingAttempts - this.pollingAttempts) * 2;  // Remaining seconds

      if (this.pollingAttempts > this.maxPollingAttempts) {
        clearInterval(this.pollingInterval);
        this.errorMsg = this.t('QR_SCAN_TIMEOUT');
        this.showQrCode = false;
        return;
      }

      // Poll for result from OVSE callback
      this.kycService.getAadhaarKycResult(this.currentTxnId).subscribe({
        next: res => {
          if (this.hasAadhaarKycPayload(res)) {
            // KYC verification successful.
            clearInterval(this.pollingInterval);
            this.successMsg = this.t('AADHAAR_VERIFICATION_SUCCESS_LOADING');
            this.handleAadhaarKycSuccess(res);
          } else if (res && res.error) {
            // User rejected or error occurred.
            clearInterval(this.pollingInterval);
            this.errorMsg = this.t('ERROR_KYC_FAILED', { reason: res.errorMessage || res.errorCode });
            this.showQrCode = false;
          }
          // If no result yet (404 or null), keep polling
        },
        error: () => {
          // Continue polling on 404 (result not yet available)
          // Only stop on max attempts
          if (this.pollingAttempts >= this.maxPollingAttempts) {
            clearInterval(this.pollingInterval);
            this.errorMsg = this.t('ERROR_VERIFICATION_TIMEOUT');
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

    this.isAadhaarFlow = true;
    this.populateVisitorDetailsFromAadhaarResponse(kycData);

    this.aadhaarResidentImage = this.getAadhaarImage(kycData);
    this.hasAadhaarResidentImage = !!this.aadhaarResidentImage;
    if (this.hasAadhaarResidentImage) {
      const residentPhotoDataUri = this.toImageDataUri(this.aadhaarResidentImage);
      this.form.livePhoto = residentPhotoDataUri;
      this.capturedPhotoUrl = residentPhotoDataUri;
      this.photoCaptured = true;
      this.kycStatus = 'PHOTO_MATCHED';
      this.kycConfidenceScore = 90;
      this.kycConfidenceLabel = this.t('CONFIDENCE_VERIFIED');
      this.form.kycStatus = 'PHOTO_MATCHED';
    } else {
      this.form.livePhoto = '';
      this.capturedPhotoUrl = '';
      this.photoCaptured = false;
      this.kycStatus = '';
      this.kycConfidenceScore = 75;
      this.kycConfidenceLabel = this.t('CONFIDENCE_DEMOGRAPHIC');
      this.form.kycStatus = '';
    }

    // Clear QR display
    this.showQrCode = false;
    this.successMsg = this.t('AADHAAR_KYC_VERIFIED_PREFILLED');
    this.errorMsg = '';

    // Move to next step
    this.cdr.detectChanges();
    setTimeout(() => {
      this.currentStep = 'photo-capture';
      this.cdr.detectChanges();
    }, 500);
  }

  populateVisitorDetailsFromKycResponse(response: any, kycType: 'EPIC' | 'AADHAAR') {
    if (kycType === 'AADHAAR') {
      this.populateVisitorDetailsFromAadhaarResponse(response);
      return;
    }

    const verified = this.mapEpicKycResponse(response);

    if (!verified.kycVerified) {
      this.verifiedKycData = null;
      this.errorMsg = this.t('ERROR_KYC_INCOMPLETE');
      return;
    }

    this.verifiedKycData = verified;
    this.patchVisitorDetailsFromKyc(verified);
  }

  populateVisitorDetailsFromAadhaarResponse(response: any) {
    const verified = this.mapAadhaarKycResponse(response);

    if (!verified.kycVerified) {
      this.verifiedKycData = null;
      this.errorMsg = this.t('ERROR_KYC_INCOMPLETE');
      return;
    }

    this.verifiedKycData = verified;
    this.patchVisitorDetailsFromKyc(verified);
  }

  private mapEpicKycResponse(response: any): VerifiedKycData {
    const data = response?.data || {};
    const polling = data.pollingdetails || data.pollingDetails || {};
    const state = data.borroweraddressstate || data.state || response?.state || '';
    const district = data.borroweraddressdistrict || data.district || response?.district || '';
    const constituency = data.assemblyconstituencyname || data.assemblyConstituencyName || data.constituency || response?.constituency || '';
    const assemblyConstituencyNumber = data.assemblyconstituencynumber || data.assemblyConstituencyNumber || '';
    const assemblyConstituencyName = data.assemblyconstituencyname || data.assemblyConstituencyName || '';
    const boothVillage = polling.pollingstationpartname || polling.pollingStationPartName || polling.pollingStationPartname || '';
    const houseNumber = data.borroweraddresshousenumber || '';
    const sectionNumber = data.borroweraddresssectionnumber || '';
    const nameMatchScore = Number(data.namematchscore ?? response?.nameMatchScore ?? 0);
    const verifiedName = data.verifiedName || response?.verifiedName || data.borrowernameonvoteridcard || '';
    const epicNumber = data.voteridnumber || this.form.epicNumber || '';

    return {
      kycVerified: !!verifiedName && !!epicNumber,
      kycType: 'EPIC',
      kycReferenceId: data.voteridverificationrequestid || response?.requestId || '',
      visitorName: verifiedName,
      gender: data.borrowergender || '',
      dob: this.normalizeDate(data.borrowerdateofbirth),
      state,
      district,
      constituency,
      assemblyConstituencyNumber,
      assemblyConstituencyName,
      boothVillage,
      address: this.composeEpicAddress(houseNumber, sectionNumber, district, state),
      epicNumber,
      maskedIdentityNumber: this.maskEpic(epicNumber),
      houseNumber,
      sectionNumber,
      relativeName: data.relativenameonvoterid || data.relativemameonvoterid || '',
      pollingPartNo: polling.pollingpartno || polling.pollingPartNo || '',
      pollingStationAddress: polling.pollingstationaddress
        || polling.pollingStationAddress
        || polling.pollingstationpartname
        || polling.pollingStationPartName
        || polling.pollingStationPartname
        || '',
      nameMatchScore: Number.isFinite(nameMatchScore) ? nameMatchScore : 0,
      idFound: Boolean(data.idFound ?? response?.idFound ?? false),
      voterIdVerificationCompletionTimestamp: data.voteridverificationcompletiontimestamp || '',
    };
  }

  private mapAadhaarKycResponse(response: any): VerifiedKycData {
    const residentName = this.getAadhaarValue(response, 'residentName', 'localResidentName', 'name');
    const mobile = this.getAadhaarValue(response, 'mobile', 'maskedMobile', 'phoneNumber');
    const clientTxnId = this.cleanString(response?.clientTxnId || response?.txnId);
    const fullAddress = this.getAadhaarValue(response, 'fullAddress', 'address', 'regionalAddress');
    const address1 = this.getAadhaarValue(response, 'address1') || this.extractAddress1FromFullAddress(fullAddress);
    const city = this.getAadhaarValue(response, 'city');
    const state = this.getAadhaarValue(response, 'state');
    const pincode = this.getAadhaarValue(response, 'pincode', 'pinCode', 'postalCode');

    return {
      kycVerified: !response?.error && !!residentName,
      kycType: 'AADHAAR',
      kycReferenceId: clientTxnId,
      visitorName: residentName,
      mobile,
      gender: this.getAadhaarValue(response, 'gender'),
      dob: this.normalizeDate(this.getAadhaarValue(response, 'dob', 'dateOfBirth')),
      address: fullAddress,
      fullAddress,
      address1,
      city,
      state,
      pincode,
      maskedIdentityNumber: this.form.aadhaarNumber ? this.maskAadhaar(this.form.aadhaarNumber) : '',
      aadhaarClientTxnId: clientTxnId,
      aadhaarAppId: this.cleanString(response?.appId),
      district: '',
      constituency: '',
      boothVillage: '',
      pollingPartNo: '',
      pollingStationAddress: '',
    };
  }

  private patchVisitorDetailsFromKyc(verified: VerifiedKycData) {
    const district = verified.district || this.form.district;
    const constituency = verified.kycType === 'AADHAAR'
      ? this.form.constituency
      : verified.constituency || this.form.constituency;
    const boothVillage = verified.boothVillage || this.form.boothVillage;

    Object.assign(this.form, {
      fullName: verified.visitorName || this.form.fullName,
      gender: verified.gender || this.form.gender,
      dateOfBirth: verified.dob || this.form.dateOfBirth,
      state: verified.state || this.form.state,
      city: verified.city || this.form.city,
      pincode: verified.pincode || this.form.pincode,
      district,
      constituency,
      booth: boothVillage,
      boothVillage,
      address: verified.address || this.form.address,
      fullAddress: verified.fullAddress || verified.address || this.form.fullAddress,
      address1: verified.address1 || verified.houseNumber || this.form.address1,
      addressLine: verified.address1 || verified.houseNumber || this.form.addressLine,
      epicNumber: verified.epicNumber || this.form.epicNumber,
      phoneNumber: this.form.idType === 'AADHAAR'
        ? this.extractUsableMobileFromAadhaar(verified) || this.form.phoneNumber
        : this.manualPhone || this.form.phoneNumber,
      kycStatus: this.form.kycStatus || (verified.kycType === 'EPIC' ? 'DEMOGRAPHIC_MATCHED' : 'PHOTO_MATCHED'),
      borrowerAddressHouseNumber: verified.houseNumber || '',
      borrowerAddressSectionNumber: verified.sectionNumber || '',
      assemblyConstituencyNumber: verified.assemblyConstituencyNumber || '',
      assemblyConstituencyName: verified.assemblyConstituencyName || verified.constituency || '',
      relativeNameOnVoterId: verified.relativeName || '',
      pollingPartNo: verified.pollingPartNo || '',
      pollingStationAddress: verified.pollingStationAddress || '',
      voterIdVerificationRequestId: verified.kycReferenceId || '',
      voterIdVerificationCompletionTimestamp: verified.voterIdVerificationCompletionTimestamp || '',
      nameMatchScore: verified.nameMatchScore,
      idFound: verified.idFound,
      aadhaarClientTxnId: verified.aadhaarClientTxnId || '',
      aadhaarAppId: verified.aadhaarAppId || '',
      maskedIdentityNumber: verified.maskedIdentityNumber || '',
    });

    this.form.outsideState = false;
    if (this.form.location === 'NA') {
      this.form.location = '';
    }
    this.districtAutoPopulated = !!verified.district;
    this.constituencyAutoPopulated = verified.kycType !== 'AADHAAR' && !!verified.constituency;
    this.boothVillageAutoPopulated = !!verified.boothVillage;
  }

  private extractUsableMobileFromAadhaar(verified: VerifiedKycData): string {
    const digits = this.cleanString(verified.mobile).replace(/\D/g, '');
    if (digits.length === 10) {
      return digits;
    }
    if (digits.length > 10) {
      return digits.substring(digits.length - 10);
    }
    return '';
  }

  private hasAadhaarKycPayload(response: any): boolean {
    return !!response
      && !response.error
      && !!(
        this.getAadhaarValue(response, 'residentName', 'localResidentName', 'name')
        || this.getAadhaarValue(response, 'address', 'regionalAddress', 'fullAddress')
        || this.getAadhaarImage(response)
      );
  }

  private isKycServiceUnavailable(error: any): boolean {
    const status = Number(error?.status ?? error?.error?.status ?? error?.error?.code ?? 0);
    const message = String(error?.error?.message || error?.error?.errorMessage || error?.message || '').toLowerCase();
    return status === 503
      || message.includes('unavailable')
      || message.includes('timeout')
      || message.includes('ovse')
      || message.includes('sdk')
      || message.includes('gateway')
      || message.includes('client error')
      || message.includes('provider');
  }

  private offerKycPendingFallback(provider: 'EPIC' | 'AADHAAR', reason: string, requestId?: string) {
    this.loading = false;
    this.kycPendingAllowed = true;
    this.kycPendingProvider = provider;
    this.kycPendingReason = reason || 'KYC service is temporarily unavailable.';
    this.kycPendingRequestId = requestId || '';
    this.errorMsg = 'KYC service is temporarily unavailable. You can continue with registration, but your KYC status will remain Pending.';
    this.successMsg = '';
  }

  continueWithKycPending() {
    if (!this.kycPendingAllowed || !this.form.idType) {
      return;
    }
    if (this.form.idType === 'EPIC' && !this.isManualPhoneValid) {
      this.errorMsg = this.t('ERROR_VALID_10_DIGIT_MOBILE');
      return;
    }

    this.stopCamera();
    this.actualPhoneNumber = this.form.idType === 'EPIC' ? this.manualPhone : this.form.phoneNumber;
    this.form.phoneNumber = this.actualPhoneNumber;
    this.form.fullName = this.form.fullName || this.form.visitorName;
    this.form.kycStatus = 'KYC_PENDING';
    this.form.kycProvider = this.kycPendingProvider || this.form.idType;
    this.form.kycFailureReason = this.kycPendingReason;
    this.form.kycRequestId = this.kycPendingRequestId;
    this.kycStatus = 'KYC_PENDING';
    this.kycConfidenceScore = 0;
    this.kycConfidenceLabel = 'Pending';
    this.verifiedKycData = {
      kycVerified: false,
      kycType: this.form.idType,
      kycReferenceId: this.kycPendingRequestId,
      visitorName: this.form.fullName,
      epicNumber: this.form.idType === 'EPIC' ? this.form.epicNumber : undefined,
      maskedIdentityNumber: this.form.idType === 'AADHAAR' ? this.maskAadhaar(this.form.aadhaarNumber) : this.maskEpic(this.form.epicNumber),
      kycFailureReason: this.kycPendingReason,
      kycRequestId: this.kycPendingRequestId,
    };
    this.idValidated = true;
    this.errorMsg = '';
    this.successMsg = 'Continue with registration. KYC status will remain Pending until verification is retried.';
    this.currentStep = 'photo-capture';
  }

  continueWithNoId() {
    if (!this.form.fullName.trim()) {
      this.loading = false;
      this.errorMsg = 'Full name is required.';
      return;
    }
    if (!this.isManualPhoneValid) {
      this.loading = false;
      this.errorMsg = this.t('ERROR_VALID_10_DIGIT_MOBILE');
      return;
    }
    this.actualPhoneNumber = this.manualPhone;
    this.form.phoneNumber = this.manualPhone;
    this.maskedPhone = this.maskPhone(this.manualPhone);
    this.form.kycStatus = 'KYC_PENDING';
    this.form.kycProvider = 'NONE';
    this.kycStatus = 'KYC_PENDING';
    this.kycConfidenceScore = 0;
    this.kycConfidenceLabel = 'Pending';
    this.verifiedKycData = {
      kycVerified: false,
      kycType: 'NONE',
      visitorName: this.form.fullName,
      mobile: this.form.phoneNumber,
    };
    this.idValidated = true;
    this.errorMsg = '';
    this.successMsg = 'Generating OTP for mobile verification.';
    this.generateRegistrationOtp();
  }

  private getAadhaarImage(response: any): string {
    return this.getAadhaarValue(
      response,
      'residentImage',
      'photoBase64',
      'residentPhotoBase64',
      'residentPhoto',
      'photo'
    );
  }

  private getAadhaarValue(response: any, ...keys: string[]): string {
    const sources = [
      response?.claimData,
      response?.claims,
      response,
    ];

    for (const source of sources) {
      const value = this.readObjectValue(source, keys);
      const cleaned = this.cleanString(value);
      if (cleaned) {
        return cleaned;
      }
    }
    return '';
  }

  private readObjectValue(source: any, keys: string[]): unknown {
    if (!source || typeof source !== 'object') {
      return '';
    }

    for (const key of keys) {
      if (source[key] !== null && source[key] !== undefined) {
        return source[key];
      }
    }

    const lowerKeys = keys.map(key => key.toLowerCase());
    const match = Object.keys(source).find(key => lowerKeys.includes(key.toLowerCase()));
    return match ? source[match] : '';
  }

  private cleanString(value: unknown): string {
    if (value === null || value === undefined) {
      return '';
    }
    return String(value).trim();
  }

  private extractAddress1FromFullAddress(fullAddress: string): string {
    return this.cleanString(fullAddress.split(',', 2)[0]);
  }

  private toImageDataUri(base64OrDataUri: string): string {
    const image = this.cleanString(base64OrDataUri);
    return image.startsWith('data:image/') ? image : `data:image/jpeg;base64,${image}`;
  }

  private composeEpicAddress(houseNumber: string, sectionNumber: string, district: string, state: string): string {
    return [
      houseNumber && houseNumber !== 'Not Available' ? houseNumber : '',
      sectionNumber ? `Section ${sectionNumber}` : '',
      district,
      state,
    ].filter(Boolean).join(', ');
  }

  private normalizeDate(value: string | null | undefined): string {
    if (!value) return '';
    const trimmed = value.trim();
    if (!trimmed || trimmed.toLowerCase() === 'not available') return '';
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
    return '';
  }

  private maskAadhaar(aadhaar: string): string {
    const digits = aadhaar.replace(/\D/g, '');
    return digits.length === 12 ? `XXXX-XXXX-${digits.substring(8)}` : '';
  }

  private maskEpic(epic: string): string {
    if (!epic || epic.length < 4) return '';
    return `${epic.substring(0, 3)}***${epic.substring(epic.length - 2)}`;
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

  get canSubmitRegistration(): boolean {
    const hasValidPhone = /^\d{10}$/.test(this.form.phoneNumber || '');
    return !this.loading
      && !!this.form.fullName.trim()
      && hasValidPhone
      && !!this.form.designation
      && !!this.form.livePhoto
      && (this.form.outsideState || !!this.form.district.trim());
  }

  get isDistrictReadOnly(): boolean {
    return this.districtAutoPopulated && !!this.form.district.trim();
  }

  get isConstituencyReadOnly(): boolean {
    return this.constituencyAutoPopulated && !!this.form.constituency.trim();
  }

  get isBoothVillageReadOnly(): boolean {
    return this.boothVillageAutoPopulated && !!this.form.boothVillage.trim();
  }

  validateOtp() {
    if (this.isCurrentMobileOtpVerified) {
      this.currentStep = 'photo-capture';
      this.successMsg = 'Mobile number already verified';
      return;
    }

    if (!this.canVerifyOtp) {
      this.errorMsg = this.t('ERROR_VALID_6_DIGIT_OTP');
      return;
    }

    this.errorMsg = '';
    this.loading = true;

    this.auth.validateOtp({
      otp: this.otpCode,
      phoneNumber: this.actualPhoneNumber || this.manualPhone,
      purpose: 'REGISTRATION',
      registrationFlow: true,
    }).subscribe({
      next: res => {
        this.loading = false;
        
        if (res.success) {
          this.otpVerified = true;
          this.verifiedMobileNumber = this.currentOtpMobileNumber;
          this.otpValidatedAt = new Date();

          if (!this.verifiedKycData) {
            this.verifiedKycData = {
              kycVerified: true,
              kycType: this.form.idType === 'EPIC' ? 'EPIC' : 'NONE',
              kycReferenceId: this.idNumber,
              visitorName: this.form.fullName,
              mobile: this.currentOtpMobileNumber,
            };
          }
          this.form.phoneNumber = this.currentOtpMobileNumber;

          this.successMsg = this.t('CONTINUE_WITH_PHOTO_CAPTURE');
          
          // Force change detection and transition to next step
          this.cdr.detectChanges();
          setTimeout(() => {
            this.currentStep = 'photo-capture';
            this.cdr.detectChanges();
          }, 200);
        } else {
          this.errorMsg = res.message || this.t('ERROR_INVALID_OTP_TRY');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = apiErrorMessage(err, this.t('ERROR_OTP_VERIFICATION_FAILED'));
      },
    });
  }

  // ── STEP 3: LIVE PHOTO CAPTURE ──────────────────────────────────────────

  async openCamera() {
    try {
      this.errorMsg = '';
      this.faceLivenessMetadata = null;
      this.faceLivenessResult = {
        ...FACE_LIVENESS_INITIAL_RESULT,
        loading: true,
        message: 'Preparing face validation...',
      };
      this.stopCamera();
      this.videoStream = await this.cameraCapture.open(this.cameraFacingMode, this.selectedCameraDeviceId || undefined);
      this.isCameraActive = true;  // Changed from showCamera to isCameraActive
      this.watchCameraDisconnect(this.videoStream);
      this.loadCameraDevices();
      
      // Wait for next tick to ensure video element exists
      setTimeout(() => {
        const videoElement = document.getElementById('camera-preview') as HTMLVideoElement;
        if (videoElement && this.videoStream) {
          this.cameraCapture.attach(videoElement, this.videoStream);
          this.startLivenessMonitoring(videoElement);
        }
      }, 100);
    } catch (err) {
      this.stopCamera();
      this.errorMsg = this.cameraErrorMessage(err);
    }
  }

  async capturePhoto() {
    const videoElement = document.getElementById('camera-preview') as HTMLVideoElement;
    if (!videoElement) {
      this.errorMsg = this.t('CAMERA_NOT_INITIALIZED');
      return;
    }

    this.faceLivenessResult = await this.cameraLiveness.analyze(videoElement);
    if (!this.faceLivenessResult.valid) {
      this.errorMsg = this.faceLivenessResult.message;
      return;
    }

    let photoData: string;
    try {
      photoData = this.cameraCapture.capture(videoElement);
    } catch {
      this.errorMsg = this.t('ERROR_FAILED_CAPTURE_PHOTO');
      return;
    }

    this.form.livePhoto = photoData;
    this.capturedPhotoUrl = photoData;  // Set for display
    this.faceLivenessMetadata = this.cameraLiveness.toCaptureMetadata(this.faceLivenessResult);
    this.photoCaptured = true;
    this.stopCamera();
    this.isCameraActive = false;  // Changed from showCamera to isCameraActive
    this.successMsg = this.t('PHOTO_CAPTURED_SUCCESS');
  }

  retakePhoto() {
    this.form.livePhoto = '';
    this.capturedPhotoUrl = '';
    this.faceLivenessMetadata = null;
    this.faceLivenessResult = { ...FACE_LIVENESS_INITIAL_RESULT };
    this.photoCaptured = false;
    this.openCamera();
  }

  stopCamera() {
    this.stopLivenessMonitoring();
    this.cameraCapture.stop(this.videoStream);
    this.videoStream = null;
    this.isCameraActive = false;
  }

  switchCamera() {
    this.selectedCameraDeviceId = '';
    this.cameraFacingMode = this.cameraCapture.toggle(this.cameraFacingMode);
    if (this.isCameraActive) {
      this.openCamera();
    }
  }

  onCameraDeviceChange(deviceId: string) {
    this.selectedCameraDeviceId = deviceId;
    if (this.isCameraActive) {
      this.openCamera();
    }
  }

  cameraDeviceLabel(device: CameraDeviceOption, index: number): string {
    return this.cameraCapture.deviceLabel(device, index);
  }

  private async loadCameraDevices(fromDeviceChange = false) {
    try {
      this.cameraOptions = await this.cameraCapture.listVideoDevices();
      if (!this.cameraOptions.length) {
        this.selectedCameraDeviceId = '';
        if (fromDeviceChange && this.isCameraActive) {
          this.errorMsg = 'No camera found';
          this.stopCamera();
        }
        return;
      }

      const selectedStillAvailable = this.cameraOptions.some(device => device.deviceId === this.selectedCameraDeviceId);
      if (!this.selectedCameraDeviceId || !selectedStillAvailable) {
        this.selectedCameraDeviceId = this.cameraOptions[0].deviceId;
        if (fromDeviceChange && this.isCameraActive && !selectedStillAvailable) {
          this.errorMsg = 'External camera disconnected';
          this.openCamera();
        }
      }
    } catch {
      this.cameraOptions = [];
      this.selectedCameraDeviceId = '';
    }
  }

  private watchCameraDisconnect(stream: MediaStream) {
    stream.getVideoTracks().forEach(track => {
      track.onended = () => {
        if (!this.videoStream) return;
        this.errorMsg = 'External camera disconnected';
        this.stopCamera();
      };
    });
  }

  private cameraErrorMessage(err: unknown): string {
    const name = err instanceof DOMException ? err.name : '';
    if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
      return 'Camera permission denied. Please allow camera access and try again.';
    }
    if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
      return 'No camera found';
    }
    if (name === 'NotReadableError' || name === 'TrackStartError') {
      return 'Unable to access selected camera. It may be busy in another application.';
    }
    if (name === 'OverconstrainedError' || name === 'ConstraintNotSatisfiedError') {
      return 'Unable to access selected camera. Please select another camera.';
    }
    return err instanceof Error && err.message ? err.message : this.t('CAMERA_ACCESS_DENIED');
  }

  get cameraFacingLabel(): string {
    return this.cameraCapture.label(this.cameraFacingMode);
  }

  // ── STEP 4: FACE VALIDATION ─────────────────────────────────────────────

  continueAfterPhotoCapture() {
    if (!this.verifiedKycData) {
      this.errorMsg = this.t('PLEASE_COMPLETE_ID_KYC');
      return;
    }

    if (!this.form.livePhoto) {
      this.errorMsg = this.t('PLEASE_CAPTURE_LIVE_PHOTO');
      return;
    }

    // Face validation skipped because EPIC does not provide reference photo.
    if (this.form.idType === 'EPIC') {
      this.kycStatus = this.manualVerification ? 'MANUAL_VERIFICATION_REQUIRED' : 'DEMOGRAPHIC_MATCHED';
      this.form.kycStatus = this.kycStatus;
      this.kycConfidenceScore = this.manualVerification ? 45 : 75;
      this.kycConfidenceLabel = this.manualVerification
        ? this.t('CONFIDENCE_MANUAL')
        : this.t('CONFIDENCE_DEMOGRAPHIC');
    } else if (this.form.idType === 'AADHAAR' && !this.hasAadhaarResidentImage) {
      this.kycStatus = 'DEMOGRAPHIC_MATCHED';
      this.form.kycStatus = 'DEMOGRAPHIC_MATCHED';
      this.kycConfidenceScore = this.kycConfidenceScore || 75;
      this.kycConfidenceLabel = this.kycConfidenceLabel || this.t('CONFIDENCE_DEMOGRAPHIC');
    } else if (this.form.idType === 'NONE') {
      this.kycStatus = 'KYC_PENDING';
      this.form.kycStatus = 'KYC_PENDING';
      this.kycConfidenceScore = 0;
      this.kycConfidenceLabel = 'Pending';
    } else if (!this.form.kycStatus) {
      this.kycStatus = 'PHOTO_MATCHED';
      this.form.kycStatus = 'PHOTO_MATCHED';
      this.kycConfidenceScore = this.kycConfidenceScore || 90;
      this.kycConfidenceLabel = this.kycConfidenceLabel || this.t('CONFIDENCE_VERIFIED');
    }

    this.errorMsg = '';
    this.successMsg = this.t('PHOTO_CAPTURED_CONTINUE_DETAILS');
    this.currentStep = 'additional-details';
  }

  // ── FINAL SUBMISSION ────────────────────────────────────────────────────

  submitRegistration() {
    if (this.duplicateRegistrationBlocked) {
      this.errorMsg = this.t('USER_ALREADY_REGISTERED');
      return;
    }

    if (!this.verifiedKycData) {
      this.errorMsg = this.t('COMPLETE_KYC_BEFORE_REGISTRATION');
      return;
    }

    if (!this.form.livePhoto) {
      this.errorMsg = this.t('PLEASE_CAPTURE_LIVE_PHOTO');
      return;
    }

    if (!this.form.outsideState && !this.form.district.trim()) {
      this.errorMsg = this.t('ERROR_DISTRICT_REQUIRED');
      return;
    }

    this.loading = true;
    const addressLine = (this.form.addressLine || this.form.address1 || this.form.address || '').trim();
    const fullAddress = (this.form.fullAddress || this.form.address || addressLine).trim();
    const boothVillage = (this.form.boothVillage || this.form.booth || '').trim();
    const payload: Record<string, string | boolean | number | null | undefined> = {
      fullName: this.form.fullName.trim(),
      phoneNumber: this.form.phoneNumber || this.maskedPhone.replace(/\*+/g, '').trim(),
      email: this.form.email.trim(),
      address: fullAddress,
      fullAddress,
      address1: addressLine,
      addressLine,
      houseNoColony: addressLine,
      state: this.form.state.trim(),
      city: this.form.city.trim(),
      pincode: this.form.pincode.trim(),
      district: this.form.outsideState ? 'NA' : this.form.district.trim(),
      constituency: this.form.outsideState ? 'NA' : this.form.constituency.trim(),
      booth: this.form.outsideState ? 'NA' : boothVillage,
      boothVillage: this.form.outsideState ? 'NA' : boothVillage,
      village: this.form.outsideState ? 'NA' : (this.form.village || '').trim(),
      outsideMeghalaya: this.form.outsideState,
      location: this.form.outsideState ? 'NA' : (this.form.location || '').trim(),
      designation: this.form.designation,
      gender: this.form.gender,
      dateOfBirth: this.form.dateOfBirth,
      kycStatus: this.form.kycStatus || this.kycStatus,
      kycProvider: this.form.kycProvider || this.form.idType,
      kycFailureReason: this.form.kycFailureReason || this.kycPendingReason,
      kycRequestId: this.form.kycRequestId || this.kycPendingRequestId,
      allowKycPending: this.form.kycStatus === 'KYC_PENDING',
      kycReferenceId: this.verifiedKycData.kycReferenceId,
      maskedIdentityNumber: this.form.maskedIdentityNumber,
      borrowerAddressHouseNumber: this.form.borrowerAddressHouseNumber,
      borrowerAddressSectionNumber: this.form.borrowerAddressSectionNumber,
      assemblyConstituencyNumber: this.form.assemblyConstituencyNumber,
      assemblyConstituencyName: this.form.assemblyConstituencyName,
      relativeNameOnVoterId: this.form.relativeNameOnVoterId,
      pollingPartNo: this.form.pollingPartNo,
      pollingStationAddress: this.form.pollingStationAddress,
      voterIdVerificationRequestId: this.form.voterIdVerificationRequestId,
      voterIdVerificationCompletionTimestamp: this.form.voterIdVerificationCompletionTimestamp,
      nameMatchScore: this.form.nameMatchScore,
      idFound: this.form.idFound,
      aadhaarClientTxnId: this.form.aadhaarClientTxnId,
      aadhaarAppId: this.form.aadhaarAppId,
      agendaType: this.form.agendaType,
      briefDescription: this.form.briefDescription,
      consentAccepted: true,
      consentVersion: '2026-05-25',
      consentTimestamp: new Date().toISOString(),
      privacyPolicyUrl: 'https://www.meghaconnect.com/privacy-policy',
      termsUrl: 'https://www.meghaconnect.com/terms',
    };

    if (this.form.idType === 'EPIC') {
      payload['epicNumber'] = this.form.epicNumber.trim().toUpperCase();
    } else if (this.form.idType === 'AADHAAR') {
      const aadhaarNumber = this.form.aadhaarNumber.trim();
      if (aadhaarNumber) {
        payload['aadhaarNumber'] = aadhaarNumber;
      }
    }

    if (this.form.livePhoto) {
      payload['livePhotoBase64'] = this.form.livePhoto;
      if (this.faceLivenessMetadata) {
        payload['faceCentered'] = this.faceLivenessMetadata.faceCentered;
        payload['frontFacing'] = this.faceLivenessMetadata.frontFacing;
        payload['multipleFacesDetected'] = this.faceLivenessMetadata.multipleFacesDetected;
        payload['livenessScore'] = this.faceLivenessMetadata.livenessScore;
        payload['capturedAt'] = this.faceLivenessMetadata.capturedAt;
      }
    }

    if (this.manualVerification) {
      payload['manualVerification'] = true;
    }

    this.http.post<{ success: boolean; message: string; visitorId?: number; kycStatus?: string; kycProvider?: string; requestId?: string; canProceed?: boolean }>(`${environment.apiUrl}/visitor/auth/register`, payload).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          if (res.kycStatus) {
            this.form.kycStatus = res.kycStatus;
          }
          this.submitted = true;
          this.currentStep = 'kyc-complete';
          this.successMsg = res.message || this.t('REGISTRATION_SUCCESS');
          if (this.isDeoMode && this.auth.hasRole('DATA_ENTRY_OPERATOR') && res.visitorId) {
            this.router.navigate(['/appointments/new'], {
              queryParams: { visitorId: res.visitorId, source: 'walkin', walkin: 'true' }
            });
          }
        } else {
          this.errorMsg = res.message || this.t('ERROR_REGISTRATION_FAILED');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = apiErrorMessage(err, this.t('ERROR_REGISTRATION_FAILED'));
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
    this.currentStep = this.form.idType === 'AADHAAR' ? 'id-entry' : 'otp-verification';
    this.errorMsg = '';
  }

  goBackFromPhotoCapture() {
    this.currentStep = this.form.idType === 'AADHAAR' ? 'id-entry' : 'otp-verification';
    this.errorMsg = '';
    if (this.isCurrentMobileOtpVerified) {
      this.successMsg = 'Mobile number already verified';
    }
  }

  goBackFromAdditionalDetails() {
    this.currentStep = 'photo-capture';
    this.errorMsg = '';
  }

  onOutsideMeghalayaChange(checked: boolean) {
    this.form.outsideState = checked;
    if (checked) {
      this.form.location = 'NA';
      return;
    }
    if (this.form.location === 'NA') {
      this.form.location = '';
    }
  }

  /**
   * Reset all KYC-related state when switching between EPIC and AADHAAR
   * This clears any previous errors, operations, and ongoing processes
   */
  onIdTypeChange(idType: VisitorRegistrationForm['idType']) {
    this.invalidatePendingRegistrationOtp();
    this.form.idType = idType;
    this.isAadhaarFlow = idType === 'AADHAAR';

    // Clear error and success messages
    this.errorMsg = '';
    this.successMsg = '';

    // Reset OTP-related state
    this.otpSent = false;
    this.otpCode = '';
    this.resetOtpVerification();
    this.maskedPhone = '';
    this.manualPhone = '';
    this.manualVerification = false;
    this.kycPendingAllowed = false;
    this.kycPendingProvider = '';
    this.kycPendingReason = '';
    this.kycPendingRequestId = '';
    this.actualPhoneNumber = '';
    this.mobileValidationMsg = '';
    this.mobileValidationType = '';
    this.mobileCheckLoading = false;
    this.epicTouched = false;
    this.nameTouched = false;
    this.mobileTouched = false;
    this.epicRejectedInput = false;
    this.nameRejectedInput = false;
    this.duplicateRegistrationBlocked = false;

    // Stop camera and clear photo capture state
    this.stopCamera();
    this.isCameraActive = false;
    this.capturedPhotoUrl = '';
    this.faceLivenessMetadata = null;
    this.faceLivenessResult = { ...FACE_LIVENESS_INITIAL_RESULT };
    this.photoCaptured = false;
    this.hasAadhaarResidentImage = false;
    this.aadhaarResidentImage = '';

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
    this.verifiedKycData = null;
    this.kycPendingAllowed = false;
    this.kycPendingProvider = '';
    this.kycPendingReason = '';
    this.kycPendingRequestId = '';

    // Clear ID-specific form fields
    this.form.epicNumber = '';
    this.form.visitorName = '';
    this.form.aadhaarNumber = '';
    if (idType === 'NONE') {
      this.form.kycStatus = 'KYC_PENDING';
      this.form.kycProvider = 'NONE';
      this.kycStatus = 'KYC_PENDING';
    } else {
      this.form.kycStatus = '';
      this.form.kycProvider = '';
    }
    this.form.livePhoto = '';
    this.form.photoFromId = '';
    this.resetLocationAutoPopulationLocks();

    // Clear auto-populated fields so user can enter fresh data
    // But preserve other form data like email, designation, etc.
    // Only reset fields that are auto-populated during KYC
    this.form.fullName = '';
    this.form.address = '';
    this.form.fullAddress = '';
    this.form.address1 = '';
    this.form.addressLine = '';
    this.form.city = '';
    this.form.state = '';
    this.form.pincode = '';
    this.form.district = '';
    this.form.constituency = '';
    this.form.booth = '';
    this.form.boothVillage = '';
    this.form.village = '';
    this.form.location = '';
    this.form.gender = '';
    this.form.dateOfBirth = '';
    this.form.borrowerAddressHouseNumber = '';
    this.form.borrowerAddressSectionNumber = '';
    this.form.assemblyConstituencyNumber = '';
    this.form.assemblyConstituencyName = '';
    this.form.relativeNameOnVoterId = '';
    this.form.pollingPartNo = '';
    this.form.pollingStationAddress = '';
    this.form.voterIdVerificationRequestId = '';
    this.form.voterIdVerificationCompletionTimestamp = '';
    this.form.nameMatchScore = undefined;
    this.form.idFound = undefined;
    this.form.aadhaarClientTxnId = '';
    this.form.aadhaarAppId = '';
    this.form.maskedIdentityNumber = '';

    // Keep the current step at 'id-entry' to show the form again
    this.currentStep = 'id-entry';
  }

  // ── INPUT SANITIZATION ──────────────────────────────────────────────────

  clearVisibleErrors() {
    this.errorMsg = '';
    if (this.mobileValidationType === 'error') {
      this.mobileValidationMsg = '';
      this.mobileValidationType = '';
    }
  }

  get epicValidationMessage(): string {
    if (this.form.idType !== 'EPIC' || !this.epicTouched) return '';
    if (this.epicRejectedInput) return 'EPIC number must be 3 letters followed by 7 digits.';
    if (!this.form.epicNumber) return 'EPIC number is required.';
    if (!this.epicPattern.test(this.form.epicNumber)) return 'EPIC number must be 3 letters followed by 7 digits.';
    return '';
  }

  get activeNameValue(): string {
    return this.form.idType === 'NONE' ? this.form.fullName : this.form.visitorName;
  }

  get nameValidationMessage(): string {
    if ((this.form.idType !== 'EPIC' && this.form.idType !== 'NONE') || !this.nameTouched) return '';
    const name = this.activeNameValue.trim();
    if (this.nameRejectedInput) return 'Name should contain only letters and spaces.';
    if (!name) return 'Name is required.';
    if (!this.isValidName(name)) return 'Name should contain only letters and spaces.';
    return '';
  }

  get mobileFieldValidationMessage(): string {
    if ((this.form.idType !== 'EPIC' && this.form.idType !== 'NONE') || !this.mobileTouched) return '';
    if (!this.manualPhone) return 'Mobile number is required.';
    if (!this.isManualPhoneValid) return 'Mobile number must be 10 digits.';
    return '';
  }

  get primaryValidationMessage(): string {
    return this.epicValidationMessage || this.nameValidationMessage || this.mobileFieldValidationMessage;
  }

  private markIdStepTouched() {
    if (this.form.idType === 'EPIC') this.epicTouched = true;
    if (this.form.idType === 'EPIC' || this.form.idType === 'NONE') {
      this.nameTouched = true;
      this.mobileTouched = true;
    }
  }

  private isValidName(value: string): boolean {
    return this.namePattern.test(this.normalizeName(value));
  }

  private normalizeName(value: string): string {
    return (value || '').replace(/\s+/g, ' ').trim();
  }

  private invalidatePendingRegistrationOtp() {
    this.registrationOtpRequestId++;
  }

  private cleanNameInput(value: string, uppercase = false): string {
    const clean = (value || '')
      .replace(/[^A-Za-z ]/g, '')
      .replace(/^\s+/, '')
      .replace(/\s{2,}/g, ' ');
    return uppercase ? clean.toUpperCase() : clean;
  }

  private isTextEditingShortcut(event: KeyboardEvent): boolean {
    return event.ctrlKey
      || event.metaKey
      || [
        'Backspace',
        'Delete',
        'Tab',
        'Enter',
        'Escape',
        'ArrowLeft',
        'ArrowRight',
        'ArrowUp',
        'ArrowDown',
        'Home',
        'End',
      ].includes(event.key);
  }

  allowDigitsOnly(event: KeyboardEvent) {
    if (this.isTextEditingShortcut(event)) return;
    if (event.key.length === 1 && !/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  allowNameOnly(event: KeyboardEvent) {
    if (this.isTextEditingShortcut(event)) return;
    if (event.key.length === 1 && !/^[A-Za-z ]$/.test(event.key)) {
      event.preventDefault();
      this.nameTouched = true;
      this.nameRejectedInput = true;
    }
  }

  private applySanitizedPaste(event: ClipboardEvent, value: string, maxLength?: number): string {
    event.preventDefault();
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? input.value.length;
    const end = input.selectionEnd ?? input.value.length;
    const next = `${input.value.slice(0, start)}${value}${input.value.slice(end)}`;
    const limited = typeof maxLength === 'number' ? next.slice(0, maxLength) : next;
    input.value = limited;
    const cursor = Math.min(start + value.length, limited.length);
    input.setSelectionRange(cursor, cursor);
    return limited;
  }

  pasteManualPhone(event: ClipboardEvent) {
    const digits = (event.clipboardData?.getData('text') || '').replace(/\D/g, '');
    this.manualPhone = this.applySanitizedPaste(event, digits, 10);
    this.sanitizeManualPhone();
  }

  pasteOtpCode(event: ClipboardEvent) {
    const digits = (event.clipboardData?.getData('text') || '').replace(/\D/g, '');
    this.otpCode = this.applySanitizedPaste(event, digits, 6);
    this.sanitizeOtpInput();
  }

  pasteVisitorName(event: ClipboardEvent) {
    const pasted = event.clipboardData?.getData('text') || '';
    const clean = this.cleanNameInput(pasted, true);
    this.nameRejectedInput = pasted.toUpperCase() !== clean;
    this.form.visitorName = this.applySanitizedPaste(event, clean);
    this.sanitizeVisitorName();
  }

  pasteFullName(event: ClipboardEvent) {
    const pasted = event.clipboardData?.getData('text') || '';
    const clean = this.cleanNameInput(pasted);
    this.nameRejectedInput = pasted !== clean;
    this.form.fullName = this.applySanitizedPaste(event, clean);
    this.sanitizeFullName();
  }

  sanitizeManualPhone() {
    this.mobileTouched = true;
    this.invalidatePendingRegistrationOtp();
    this.manualPhone = this.manualPhone.replace(/\D/g, '');
    this.form.phoneNumber = this.manualPhone;
    this.resetOtpVerification();
    this.otpSent = false;
    this.otpCode = '';
    this.errorMsg = '';
    this.mobileValidationMsg = '';
    this.mobileValidationType = '';
    this.mobileCheckLoading = false;
    this.duplicateRegistrationBlocked = false;
    this.resetLocationAutoPopulationLocks();
  }

  onMobileBlur() {
    this.mobileTouched = true;
    if (!this.manualPhone) {
      this.mobileValidationMsg = '';
      this.mobileValidationType = '';
      this.duplicateRegistrationBlocked = false;
      return;
    }

    if (!this.isManualPhoneValid) {
      this.mobileValidationType = 'error';
      this.mobileValidationMsg = this.t('ERROR_VALID_10_DIGIT_MOBILE');
      this.duplicateRegistrationBlocked = false;
      return;
    }

    this.checkRegistrationStatus(false);
  }

  private checkRegistrationStatus(proceedAfterCheck: boolean) {
    if (!this.isManualPhoneValid) {
      this.mobileValidationType = 'error';
      this.mobileValidationMsg = this.t('ERROR_VALID_10_DIGIT_MOBILE');
      this.loading = false;
      return;
    }

    const payload: Record<string, string> = {
      phoneNumber: this.manualPhone,
    };

    if (this.form.epicNumber && /^[A-Za-z]{3}[0-9]{7}$/.test(this.form.epicNumber)) {
      payload['epicNumber'] = this.form.epicNumber.trim().toUpperCase();
    }

    this.mobileCheckLoading = true;
    this.http.post<RegistrationCheckResponse>(`${environment.apiUrl}/visitor/auth/check-registration`, payload).subscribe({
      next: res => {
        this.mobileCheckLoading = false;
        this.applyRegistrationCheck(res);
        
        if (res.epicMobileExists || res.epicExists ) {
          this.loading = false;
          this.errorMsg = res.message || this.t('USER_ALREADY_REGISTERED');
          return;
        }

        if (proceedAfterCheck) {
          if (this.form.idType === 'NONE') {
            this.continueWithNoId();
            return;
          }
          this.verifyEpic();
        }
      },
      error: err => {
        this.mobileCheckLoading = false;
        this.loading = false;
        this.mobileValidationType = 'error';
        this.mobileValidationMsg = apiErrorMessage(err, this.t('ERROR_UNABLE_VALIDATE_MOBILE'));
      }
    });
  }

  private applyRegistrationCheck(res: RegistrationCheckResponse) {
    this.duplicateRegistrationBlocked = !!res.epicMobileExists;

    if (res.epicMobileExists || res.epicExists) {
      this.mobileValidationType = 'error';
      this.mobileValidationMsg = res.message || this.t('USER_ALREADY_REGISTERED');
      return;
    }

    if (res.mobileExists) {
      this.mobileValidationType = 'warning';
      this.mobileValidationMsg = res.message || this.t('WARNING_MOBILE_EXISTS');
      return;
    }

    this.mobileValidationType = 'success';
    this.mobileValidationMsg = this.t('MOBILE_AVAILABLE');
  }

  sanitizeNumericInput(field: 'phoneNumber' | 'aadhaarNumber') {
    this.form[field] = this.form[field].replace(/\D/g, '');
    if (field === 'phoneNumber') {
      this.resetOtpVerification();
      this.otpCode = '';
    }
    this.clearVisibleErrors();
  }

  sanitizePincode() {
    this.form.pincode = this.form.pincode.replace(/\D/g, '');
    this.clearVisibleErrors();
  }

  sanitizeOtpInput() {
    this.otpCode = this.otpCode.replace(/\D/g, '');
    this.clearVisibleErrors();
  }

  sanitizeEpicInput() {
    this.epicTouched = true;
    this.invalidatePendingRegistrationOtp();
    const original = this.form.epicNumber || '';
    const raw = original.toUpperCase().replace(/[^A-Z0-9]/g, '');
    let next = '';
    for (const char of raw) {
      if (next.length < 3) {
        if (/[A-Z]/.test(char)) next += char;
      } else if (next.length < 10) {
        if (/\d/.test(char)) next += char;
      }
      if (next.length === 10) break;
    }
    this.epicRejectedInput = original.toUpperCase() !== next && !this.epicPattern.test(next);
    this.form.epicNumber = next;
    this.resetOtpVerification();
    this.otpSent = false;
    this.otpCode = '';
    this.errorMsg = '';
    this.duplicateRegistrationBlocked = false;
    this.idValidated = false;
    this.mobileValidationMsg = '';
    this.mobileValidationType = '';
    if (this.form.epicNumber.length === 10 && this.epicPattern.test(this.form.epicNumber)) {
      setTimeout(() => this.visitorNameInput?.nativeElement.focus());
    }
  }

  sanitizeVisitorName() {
    this.nameTouched = true;
    this.invalidatePendingRegistrationOtp();
    const original = this.form.visitorName || '';
    const next = this.cleanNameInput(original, true);
    this.nameRejectedInput = original.toUpperCase() !== next;
    this.form.visitorName = next;
    this.resetOtpVerification();
    this.otpSent = false;
    this.otpCode = '';
    this.idValidated = false;
    this.clearVisibleErrors();
  }

  sanitizeFullName() {
    this.nameTouched = true;
    this.invalidatePendingRegistrationOtp();
    const original = this.form.fullName || '';
    const next = this.cleanNameInput(original);
    this.nameRejectedInput = original !== next;
    this.form.fullName = next;
    this.clearVisibleErrors();
  }

  trimVisitorName() {
    this.form.visitorName = this.normalizeName(this.form.visitorName).toUpperCase();
  }

  trimFullName() {
    this.form.fullName = this.normalizeName(this.form.fullName);
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
    this.invalidatePendingRegistrationOtp();
    this.currentStep = 'id-entry';
    this.idValidated = false;
    this.otpSent = false;
    this.otpVerified = false;
    this.verifiedMobileNumber = null;
    this.otpValidatedAt = null;
    this.photoCaptured = false;
    this.faceLivenessMetadata = null;
    this.faceLivenessResult = { ...FACE_LIVENESS_INITIAL_RESULT };
    this.kycStatus = '';
    this.isAadhaarFlow = false;
    this.hasAadhaarResidentImage = false;
    this.aadhaarResidentImage = '';
    this.manualPhone = '';
    this.manualVerification = false;
    this.mobileValidationMsg = '';
    this.mobileValidationType = '';
    this.mobileCheckLoading = false;
    this.duplicateRegistrationBlocked = false;
    this.epicTouched = false;
    this.nameTouched = false;
    this.mobileTouched = false;
    this.epicRejectedInput = false;
    this.nameRejectedInput = false;
    this.verifiedKycData = null;
    this.form = {
      fullName: '',
      phoneNumber: '',
      email: '',
      address: '',
      fullAddress: '',
      address1: '',
      addressLine: '',
      city: '',
      state: '',
      pincode: '',
      district: '',
      constituency: '',
      booth: '',
      boothVillage: '',
      village: '',
      location: '',
      designation: '',
      gender: '',
      dateOfBirth: '',
      outsideState: false,
      idType: '',
      epicNumber: '',
      visitorName: '',
      aadhaarNumber: '',
      otp: '',
      livePhoto: '',
      photoFromId: '',
      agendaType: '',
      briefDescription: '',
    };
    this.errorMsg = '';
    this.successMsg = '';
    this.resetLocationAutoPopulationLocks();
  }

  resetOtpVerification() {
    this.otpVerified = false;
    this.verifiedMobileNumber = null;
    this.otpValidatedAt = null;
  }

  private resetLocationAutoPopulationLocks() {
    this.districtAutoPopulated = false;
    this.constituencyAutoPopulated = false;
    this.boothVillageAutoPopulated = false;
  }

  private startLivenessMonitoring(videoElement: HTMLVideoElement) {
    this.stopLivenessMonitoring();

    const analyzeFrame = async () => {
      if (!this.isCameraActive || !this.videoStream) {
        return;
      }

      const now = performance.now();
      if (!this.livenessAnalysisInProgress && now - this.lastLivenessAnalysisAt > 250) {
        this.livenessAnalysisInProgress = true;
        this.lastLivenessAnalysisAt = now;
        try {
          this.faceLivenessResult = await this.cameraLiveness.analyze(videoElement);
          this.cdr.detectChanges();
        } finally {
          this.livenessAnalysisInProgress = false;
        }
      }

      this.livenessFrameId = requestAnimationFrame(analyzeFrame);
    };

    this.livenessFrameId = requestAnimationFrame(analyzeFrame);
  }

  private stopLivenessMonitoring() {
    if (this.livenessFrameId !== null) {
      cancelAnimationFrame(this.livenessFrameId);
      this.livenessFrameId = null;
    }
    this.livenessAnalysisInProgress = false;
  }
}
