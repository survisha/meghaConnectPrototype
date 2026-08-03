import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../environments/environment';
import { Visitor } from '../models';

export interface FaceSearchResult {
  success: boolean;
  matched: boolean;
  enrollmentId?: string;
  score?: number;
  distance?: number;
  message: string;
  visitor?: Visitor;
}

export interface FaceSearchRequest {
  photo: string;
  latitude?: number;
  longitude?: number;
  includeMatchedPhoto: boolean;
}

export function normalizeFacePhoto(photo: string): string {
  const value = photo?.trim();
  if (!value) throw new Error('Face photo is required.');
  const supportedPrefix = /^data:image\/(jpeg|png);base64,/i;
  if (/^data:/i.test(value) && !supportedPrefix.test(value)) {
    throw new Error('Only JPEG or PNG face images are supported.');
  }
  const normalized = value.replace(supportedPrefix, '');
  if (!normalized) throw new Error('Face photo is required.');
  return normalized;
}

@Injectable({ providedIn: 'root' })
export class FaceRecognitionService {
  constructor(private http: HttpClient) {}

  search(photo: string, latitude?: number, longitude?: number): Observable<FaceSearchResult> {
    const request: FaceSearchRequest = {
      photo: normalizeFacePhoto(photo),
      includeMatchedPhoto: false,
      ...(latitude !== undefined ? { latitude } : {}),
      ...(longitude !== undefined ? { longitude } : {}),
    };
    return this.http.post<FaceSearchResult>(`${environment.apiUrl}/face-recognition/search`, request)
      .pipe(timeout(15000));
  }
}
