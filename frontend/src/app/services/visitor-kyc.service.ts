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
   * Step 3: Validate face by comparing live photo with ID photo
   * Returns KYC status (PHOTO_MATCHED or DEMOGRAPHIC_MATCHED)
   */
  validateFace(request: FaceValidationRequest): Observable<FaceValidationResponse> {
    return this.http.post<FaceValidationResponse>(`${environment.apiUrl}/visitor/validate-face`, request);
  }

  getCitizenDesignations(): Observable<ReferenceDataDto[]> {
    return this.http.get<ReferenceDataDto[]>(`${environment.apiUrl}/reference/CITIZEN_DESIGNATION`);
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
    } catch (error) {
      throw new Error('Camera access denied or not available');
    }
  }
}
