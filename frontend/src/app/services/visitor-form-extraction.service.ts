import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type ExtractionStatus = 'EXTRACTED' | 'NOT_FOUND' | 'UNREADABLE' | 'AMBIGUOUS' | 'CROSSED_OUT';
export type ExtractionConfidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';

export interface ExtractedVisitorField<T> {
  value: T | null;
  status: ExtractionStatus;
  confidence: ExtractionConfidence;
  reason: string | null;
  valid: boolean;
}

export interface VisitorFormExtractionResponse {
  success: boolean;
  name?: ExtractedVisitorField<string>;
  mobileNumber?: ExtractedVisitorField<string>;
  age?: ExtractedVisitorField<number>;
  address?: ExtractedVisitorField<string>;
  warnings: string[];
  requiresManualReview: boolean;
  imageQuality: { acceptable: boolean; issues: string[] };
  requestId: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class VisitorFormExtractionService {
  constructor(private readonly http: HttpClient) {}

  extract(image: File, languageHint?: string): Observable<VisitorFormExtractionResponse> {
    const data = new FormData();
    data.append('image', image);
    data.append('formType', 'VISITOR_REGISTRATION');
    if (languageHint) data.append('languageHint', languageHint);
    return this.http.post<VisitorFormExtractionResponse>(
      `${environment.apiUrl}/visitor-form-extraction/extract`, data
    );
  }
}
