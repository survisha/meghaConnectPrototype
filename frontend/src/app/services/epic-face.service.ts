import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EpicFaceResult {
  matched: boolean; epicNumber?: string; name?: string; address?: string;
  serialNumber?: string; partNumber?: string; partName?: string; acpcName?: string;
  district?: string; pincode?: string; epicPhoto?: string;
  source: 'EPIC_FACE_1N' | 'EPIC_FACE_11'; providerStatus: string;
}

export interface EpicRegistrationPrefill extends EpicFaceResult {
  liveCapturedPhoto: string;
  faceMatched: boolean;
}

@Injectable({ providedIn: 'root' })
export class EpicFaceService {
  private readonly url = `${environment.apiUrl}/epic/face`;
  constructor(private readonly http: HttpClient) {}
  search(photo: string): Observable<EpicFaceResult> {
    return this.http.post<EpicFaceResult>(`${this.url}/search`, { photo, source: 'WALK_IN' });
  }
  verify(epicNumber: string, photo: string): Observable<EpicFaceResult> {
    return this.http.post<EpicFaceResult>(`${this.url}/verify`, { epicNumber, photo });
  }
}
