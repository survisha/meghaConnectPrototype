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
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';

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
  idType: 'EPIC' | 'AADHAAR' | '';
  epicNumber: string;
  visitorName: string;     // Name as on voter card (for EPIC verification)
  aadhaarNumber: string;
  otp: string;
  livePhoto: string;
  photoFromId: string;
  kycStatus?: string;
  borrowerAddressHouseNumber?: string;
  borrowerAddressSectionNumber?: string;
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
  kycType: 'EPIC' | 'AADHAAR';
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
  relativeName?: string;
  pollingPartNo?: string;
  pollingStationAddress?: string;
  nameMatchScore?: number;
  idFound?: boolean;
  voterIdVerificationCompletionTimestamp?: string;
  aadhaarClientTxnId?: string;
  aadhaarAppId?: string;
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
  mobileValidationMsg = '';
  mobileValidationType: MobileValidationType = '';
  mobileCheckLoading = false;
  duplicateRegistrationBlocked = false;
  verifiedKycData: VerifiedKycData | null = null;
  isAadhaarFlow = false;
  hasAadhaarResidentImage = false;
  aadhaarResidentImage = '';
  
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
    private cdr: ChangeDetectorRef,
    private translate: TranslateService
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
      const hasValidEpic = /^[A-Za-z]{3}[0-9]{7}$/.test(this.form.epicNumber);
      const hasValidName = this.form.visitorName && this.form.visitorName.trim().length > 0;
      return hasValidEpic && !!hasValidName;
    }
    if (this.form.idType === 'AADHAAR') {
      return /^\d{12}$/.test(this.form.aadhaarNumber);
    }
    return false;
  }

  get isManualPhoneValid(): boolean {
    return /^\d{10}$/.test(this.manualPhone);
  }

  get canSendEpicOtp(): boolean {
    return this.canValidateId && this.isManualPhoneValid && !this.duplicateRegistrationBlocked;
  }

  get mobileValidationIcon(): string {
    if (this.mobileValidationType === 'error') return 'error';
    if (this.mobileValidationType === 'warning') return 'warning';
    if (this.mobileValidationType === 'success') return 'check_circle';
    return 'info';
  }

  validateId() {
    if (!this.canValidateId) {
      this.errorMsg = this.t('ERROR_INVALID_ID_AND_NAME');
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
          this.successMsg = this.t('EPIC_VERIFIED_SENDING_OTP');
          
          this.populateVisitorDetailsFromKycResponse(res, 'EPIC');
          this.kycConfidenceScore = Math.min(this.verifiedKycData?.nameMatchScore ?? 0, 100);
          
          this.idValidated = true;
          this.maskedPhone = this.maskPhone(this.manualPhone);
          this.actualPhoneNumber = this.manualPhone;
          this.form.phoneNumber = this.manualPhone;
          this.loading = true;
          this.generateRegistrationOtp();
        } else if (res.code === '400') {
          // Name mismatch or validation error
          this.errorMsg = res.message || this.t('ERROR_NAME_VERIFICATION_FAILED');
          this.successMsg = '';
        } else {
          // EPIC verification failed or other error
          const errorMsg = res.message || this.t('EPIC_VERIFICATION_FAILED');
          this.errorMsg = errorMsg;
          this.successMsg = '';
          this.loading = false;
        }
      },
      error: err => {
        this.loading = false;
        const errorMsg = err?.error?.message || err?.message || this.t('ERROR_FAILED_VERIFY_EPIC_TRY');
        this.errorMsg = errorMsg;
      }
    });
  }

  resendOtp() {
    this.otpCode = '';
    this.otpSent = false;
    if (this.form.idType === 'EPIC' && this.isManualPhoneValid) {
      this.loading = true;
      this.generateRegistrationOtp();
    } else {
      this.validateId();
    }
  }

  private generateRegistrationOtp() {
    this.http.post<{ success: boolean; otp?: string; message: string }>(
      `${environment.apiUrl}/visitor/auth/generate-otp`,
      {
        phoneNumber: this.actualPhoneNumber || this.manualPhone,
        purpose: 'REGISTRATION',
        registrationFlow: 'true',
      }
    ).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
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
        this.loading = false;
        this.errorMsg = err?.error?.message || this.t('ERROR_FAILED_GENERATE_OTP_TRY');
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
          this.errorMsg = res.errorMessage || this.t('ERROR_FAILED_GENERATE_QR');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.errorMessage || this.t('ERROR_QR_GENERATION_FAILED');
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
      boothVillage,
      address: this.composeEpicAddress(houseNumber, sectionNumber, district, state),
      epicNumber,
      maskedIdentityNumber: this.maskEpic(epicNumber),
      houseNumber,
      sectionNumber,
      relativeName: data.relativenameonvoterid || data.relativemameonvoterid || '',
      pollingPartNo: polling.pollingpartno || polling.pollingPartNo || '',
      pollingStationAddress: polling.pollingstationaddress || polling.pollingStationAddress || '',
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
    Object.assign(this.form, {
      fullName: verified.visitorName || this.form.fullName,
      gender: verified.gender || this.form.gender,
      dateOfBirth: verified.dob || this.form.dateOfBirth,
      state: verified.state || this.form.state,
      city: verified.city || this.form.city,
      pincode: verified.pincode || this.form.pincode,
      district: verified.district || this.form.district,
      constituency: verified.kycType === 'AADHAAR' ? this.form.constituency : verified.constituency || this.form.constituency,
      booth: verified.boothVillage || this.form.booth,
      boothVillage: verified.boothVillage || this.form.boothVillage,
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

    if (verified.state && verified.state.toLowerCase() !== 'meghalaya') {
      this.form.outsideState = true;
      this.form.location = 'NA';
    }
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

  verifyOtp() {
    if (!this.canVerifyOtp) {
      this.errorMsg = this.t('ERROR_VALID_6_DIGIT_OTP');
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

          if (!this.verifiedKycData) {
            const demo = res.demographics;
            Object.assign(this.form, {
              fullName: demo.fullName || '',
              address: demo.address || '',
              district: demo.district || '',
              constituency: demo.constituency || '',
              photoFromId: demo.photoFromId || '',
            });
          }
          this.form.phoneNumber = this.actualPhoneNumber;

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
        this.errorMsg = err?.error?.message || this.t('ERROR_OTP_VERIFICATION_FAILED');
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
    } catch {
      this.errorMsg = this.t('CAMERA_ACCESS_DENIED');
    }
  }

  capturePhoto() {
    const videoElement = document.getElementById('camera-preview') as HTMLVideoElement;
    if (!videoElement) {
      this.errorMsg = this.t('CAMERA_NOT_INITIALIZED');
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = videoElement.videoWidth;
    canvas.height = videoElement.videoHeight;
    
    const context = canvas.getContext('2d');
    if (!context) {
      this.errorMsg = this.t('ERROR_FAILED_CAPTURE_PHOTO');
      return;
    }

    context.drawImage(videoElement, 0, 0);
    const photoData = canvas.toDataURL('image/jpeg', 0.8);
    this.form.livePhoto = photoData;
    this.capturedPhotoUrl = photoData;  // Set for display
    this.photoCaptured = true;
    this.stopCamera();
    this.isCameraActive = false;  // Changed from showCamera to isCameraActive
    this.successMsg = this.t('PHOTO_CAPTURED_SUCCESS');
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

  validateFace() {
    if (!this.verifiedKycData) {
      this.errorMsg = this.t('PLEASE_COMPLETE_ID_KYC');
      return;
    }

    if (!this.form.livePhoto) {
      this.errorMsg = this.t('PLEASE_CAPTURE_LIVE_PHOTO');
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
          this.kycConfidenceLabel = this.kycStatus === 'PHOTO_MATCHED'
            ? this.t('CONFIDENCE_VERIFIED')
            : this.kycStatus === 'DEMOGRAPHIC_MATCHED'
              ? this.t('CONFIDENCE_DEMOGRAPHIC')
              : this.t('CONFIDENCE_MANUAL');
          this.currentStep = 'additional-details';
          this.successMsg = res.kycStatus === 'PHOTO_MATCHED'
            ? this.t('KYC_VERIFIED_PHOTO_SHORT')
            : this.t('KYC_VERIFIED_DEMOGRAPHIC_SHORT');
        } else {
          this.errorMsg = res.message || this.t('FACE_VALIDATION_FAILED');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || this.t('FACE_VALIDATION_FAILED');
      },
    });
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
      district: this.form.district.trim() || (this.form.outsideState ? 'NA' : ''),
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
      kycReferenceId: this.verifiedKycData.kycReferenceId,
      maskedIdentityNumber: this.form.maskedIdentityNumber,
      borrowerAddressHouseNumber: this.form.borrowerAddressHouseNumber,
      borrowerAddressSectionNumber: this.form.borrowerAddressSectionNumber,
      relativeNameOnVoterId: this.form.relativeNameOnVoterId,
      pollingPartNo: this.form.pollingPartNo,
      pollingStationAddress: this.form.pollingStationAddress,
      voterIdVerificationRequestId: this.form.voterIdVerificationRequestId,
      voterIdVerificationCompletionTimestamp: this.form.voterIdVerificationCompletionTimestamp,
      nameMatchScore: this.form.nameMatchScore,
      idFound: this.form.idFound,
      aadhaarClientTxnId: this.form.aadhaarClientTxnId,
      aadhaarAppId: this.form.aadhaarAppId,
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
          this.successMsg = this.t('REGISTRATION_SUCCESS');
        } else {
          this.errorMsg = res.message || this.t('ERROR_REGISTRATION_FAILED');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || this.t('ERROR_REGISTRATION_FAILED');
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
    this.form.idType = idType;
    this.isAadhaarFlow = idType === 'AADHAAR';

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
    this.mobileValidationMsg = '';
    this.mobileValidationType = '';
    this.mobileCheckLoading = false;
    this.duplicateRegistrationBlocked = false;

    // Stop camera and clear photo capture state
    this.stopCamera();
    this.isCameraActive = false;
    this.capturedPhotoUrl = '';
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

  sanitizeManualPhone() {
    this.manualPhone = this.manualPhone.replace(/\D/g, '');
    this.form.phoneNumber = this.manualPhone;
    this.mobileValidationMsg = '';
    this.mobileValidationType = '';
    this.mobileCheckLoading = false;
    this.duplicateRegistrationBlocked = false;
  }

  onMobileBlur() {
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

        if (res.epicMobileExists) {
          this.loading = false;
          this.errorMsg = res.message || this.t('USER_ALREADY_REGISTERED');
          return;
        }

        if (proceedAfterCheck) {
          this.verifyEpic();
        }
      },
      error: err => {
        this.mobileCheckLoading = false;
        this.loading = false;
        this.mobileValidationType = 'error';
        this.mobileValidationMsg = err?.error?.message || this.t('ERROR_UNABLE_VALIDATE_MOBILE');
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
  }

  sanitizePincode() {
    this.form.pincode = this.form.pincode.replace(/\D/g, '');
  }

  sanitizeOtpInput() {
    this.otpCode = this.otpCode.replace(/\D/g, '');
  }

  sanitizeEpicInput() {
    this.form.epicNumber = this.form.epicNumber.toUpperCase();
    this.duplicateRegistrationBlocked = false;
    if (this.mobileValidationType === 'error') {
      this.mobileValidationMsg = '';
      this.mobileValidationType = '';
    }
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
    this.isAadhaarFlow = false;
    this.hasAadhaarResidentImage = false;
    this.aadhaarResidentImage = '';
    this.manualPhone = '';
    this.manualVerification = false;
    this.mobileValidationMsg = '';
    this.mobileValidationType = '';
    this.mobileCheckLoading = false;
    this.duplicateRegistrationBlocked = false;
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
    };
    this.errorMsg = '';
    this.successMsg = '';
  }
}
