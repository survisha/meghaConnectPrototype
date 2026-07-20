import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReferenceDataDto {
  code: string;
  value: string;
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
  assemblyconstituencynumber?: string;
  assemblyconstituencyname?: string;
  assemblyConstituencyNumber?: string;
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
  canProceed?: boolean;
  kycStatus?: string;
  kycProvider?: string;
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

  retryKyc(visitorId: number, payload: { name: string; epicNumber: string }): Observable<{
    success: boolean;
    message: string;
    kycStatus: string;
    kycProvider?: string;
    requestId?: string;
    canProceed?: boolean;
    hardFailure?: boolean;
    profile?: Record<string, unknown>;
  }> {
    return this.http.post<{
      success: boolean;
      message: string;
      kycStatus: string;
      kycProvider?: string;
      requestId?: string;
      canProceed?: boolean;
      hardFailure?: boolean;
      profile?: Record<string, unknown>;
    }>(`${environment.apiUrl}/visitor/auth/profile/${visitorId}/kyc/retry`, payload);
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
