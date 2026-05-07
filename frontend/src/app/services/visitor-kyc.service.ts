import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface IdValidationRequest {
  idType: 'EPIC' | 'AADHAAR';
  idNumber: string;
  phoneNumber?: string; // Optional - for manual verification fallback
}

export interface IdValidationResponse {
  success: boolean;
  message: string;
  otpSent: boolean;
  phoneNumber?: string; // Masked phone number for display
  actualPhoneNumber?: string; // Actual 10-digit phone number for storage
  manualVerification?: boolean; // Flag if manual phone number was provided
  otp?: string; // DEMO ONLY - OTP for testing
}

export interface OtpVerificationRequest {
  idNumber: string;
  otp: string;
  phoneNumber: string;
  idType: string;
}

export interface VisitorDemographics {
  fullName: string;
  address: string;
  district: string;
  constituency: string;
  photoFromId?: string; // Base64 encoded photo from ID
}

export interface OtpVerificationResponse {
  success: boolean;
  message: string;
  demographics?: VisitorDemographics;
}

export interface FaceValidationRequest {
  idNumber: string;
  livePhoto: string; // Base64 encoded image
}

export interface FaceValidationResponse {
  success: boolean;
  kycStatus: 'PHOTO_MATCHED' | 'DEMOGRAPHIC_MATCHED' | 'FAILED';
  message: string;
  matchScore?: number;
}

export interface ReferenceDataDto {
  code: string;
  value: string;
}



export interface AadhaarQrResponse {
  success: boolean;
  txnId: string;
  qrDataUri: string;
  maskedMobile?: string;
  errorMessage?: string;
}

export interface EpicVerificationRequest {
  epicNumber: string;           // EPIC voter ID (e.g., BCV0259184)
  visitorName: string;          // Name as on voter card (for matching)
  phoneNumber?: string;         // Optional: mobile for OTP
}

export interface PollingDetailsResponse {
  pollingpartno?: string;
  pollingstationaddress?: string;
  pollingstationpartname?: string;
  pollingPartNo?: string;
  pollingStationAddress?: string;
  pollingStationPartName?: string;
}

export interface EpicVerificationData {
  state?: string;
  district?: string;
  verifiedName?: string;
  idFound?: boolean;
  voteridnumber?: string;
  borrowernameonvoteridcard?: string;
  relativemameonvoterid?: string;
  relativenameonvoterid?: string;
  borrowergender?: string;
  borrowerdateofbirth?: string;
  borroweraddressstate?: string;
  borroweraddressdistrict?: string;
  assemblyconstituencyname?: string;
  assemblyConstituencyName?: string;
  borroweraddresshousenumber?: string;
  borroweraddresssectionnumber?: string;
  accountnumber?: string;
  namematchscore?: number;
  voteridverificationstatus?: string;
  sourceinformation?: string;
  pollingdetails?: PollingDetailsResponse;
  voteridverificationrequestid?: string;
  voteridverificationcompletiontimestamp?: string;
}

export interface EpicVerificationResponse {
  code: string;                 // HTTP status code (200, 400, 503)
  message: string;              // "Success" or error message
  data?: EpicVerificationData;  // Strongly-typed response data
  requestId?: string;
  state?: string;
  success?: boolean;
  district?: string;
  verifiedName?: string;
  nameMatchScore?: number;
  idFound?: boolean;
}

export interface KycDataResponse {
  error: boolean;
  errorCode?: string;
  errorMessage?: string;
  appId?: string;
  clientTxnId?: string;
  txnId?: string;
  residentName?: string;
  localResidentName?: string;
  dob?: string;
  gender?: string;
  mobile?: string;
  maskedMobile?: string;
  residentImage?: string;
  photoBase64?: string;
  regionalAddress?: string;
  address?: string;
  fullAddress?: string;
  address1?: string;
  city?: string;
  state?: string;
  pincode?: string;
  claimData?: {
    residentName?: string;
    localResidentName?: string;
    dob?: string;
    gender?: string;
    mobile?: string;
    maskedMobile?: string;
    email?: string;
    maskedEmail?: string;
    residentImage?: string;
    photoBase64?: string;
    address?: string;
    regionalAddress?: string;
    address1?: string;
    city?: string;
    state?: string;
    pincode?: string;
  };
  claims?: {
    residentName?: string;
    localResidentName?: string;
    dob?: string;
    gender?: string;
    mobile?: string;
    maskedMobile?: string;
    email?: string;
    maskedEmail?: string;
    residentImage?: string;  // Base64 JPEG
    photoBase64?: string;
    address?: string;
    regionalAddress?: string;
    address1?: string;
    city?: string;
    state?: string;
    pincode?: string;
  };
  receivedAtMillis?: number;
}

@Injectable({ providedIn: 'root' })
export class VisitorKycService {
  
  constructor(private http: HttpClient) {}

  /**
   * Step 1: Validate EPIC or Aadhaar ID Type
   * - Sends OTP to the mobile number registered with the ID
   * - If phoneNumber is provided manually, OTP is sent to that number (manual verification)
   * - Backend calls external EPIC/Aadhaar service to get registered mobile number
   */
  validateVisitorId(request: IdValidationRequest): Observable<IdValidationResponse> {
    return this.http.post<IdValidationResponse>(`${environment.apiUrl}/visitor/validate-idType`, request);
  }

  /**
   * Step 2: Verify OTP entered by user
   * Returns demographic details if OTP is valid
   */
  verifyOtp(request: OtpVerificationRequest): Observable<OtpVerificationResponse> {
    return this.http.post<OtpVerificationResponse>(`${environment.apiUrl}/visitor/verify-otp`, request);
  }

  /**
   * Backward-compatible face validation endpoint. The EPIC registration flow
   * does not call this because EPIC does not provide a reference photo.
   */
  validateFace(request: FaceValidationRequest): Observable<FaceValidationResponse> {
    return this.http.post<FaceValidationResponse>(`${environment.apiUrl}/visitor/validate-face`, request);
  }

  getCitizenDesignations(): Observable<ReferenceDataDto[]> {
    return this.http.get<ReferenceDataDto[]>(`${environment.apiUrl}/reference/CITIZEN_DESIGNATION`);
  }

  /**
   * Verify EPIC (Voter ID) against Election Commission API.
   * Called when user enters EPIC number and name on voter card.
   * Returns verification status, district, state, and name match score.
   */
  verifyEpic(request: EpicVerificationRequest): Observable<EpicVerificationResponse> {
    return this.http.post<EpicVerificationResponse>(`${environment.apiUrl}/kyc/verify/epic`, request);
  }

  /**
   * Generate OVSE QR code for Aadhaar verification.
   * User scans the QR with their Aadhaar app on mobile.
   * Returns transaction ID and QR image (base64 data URI).
   */
  generateAadhaarQr(): Observable<AadhaarQrResponse> {
    return this.http.post<AadhaarQrResponse>(`${environment.apiUrl}/kyc/aadhaar/generate-qr`, {});
  }

  /**
   * Poll for Aadhaar KYC verification result.
   * Called after QR generation to wait for user to scan and verify.
   * Returns null/404 while waiting, returns KycData when ready.
   */
  getAadhaarKycResult(txnId: string): Observable<KycDataResponse> {
    return this.http.get<KycDataResponse>(`${environment.apiUrl}/kyc/aadhaar/result/${txnId}`);
  }

  /**
   * Utility: Capture photo from browser camera
   * Returns base64 encoded image
   */
  async capturePhoto(): Promise<string> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: { 
          facingMode: 'user',
          width: { ideal: 640 },
          height: { ideal: 480 }
        } 
      });
      
      return new Promise((resolve, reject) => {
        const video = document.createElement('video');
        video.srcObject = stream;
        video.play();

        video.addEventListener('loadedmetadata', () => {
          const canvas = document.createElement('canvas');
          canvas.width = video.videoWidth;
          canvas.height = video.videoHeight;
          
          const context = canvas.getContext('2d');
          if (!context) {
            reject(new Error('Failed to get canvas context'));
            return;
          }

          context.drawImage(video, 0, 0);
          const base64Image = canvas.toDataURL('image/jpeg', 0.8);
          
          // Stop camera
          stream.getTracks().forEach(track => track.stop());
          
          resolve(base64Image);
        });
      });
    } catch {
      throw new Error('Camera access denied or not available');
    }
  }
}
