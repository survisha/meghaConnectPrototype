import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, retry, timeout } from 'rxjs';
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

@Injectable({ providedIn: 'root' })
export class FaceRecognitionService {
  constructor(private http: HttpClient) {}

  search(photo: string): Observable<FaceSearchResult> {
    return this.http.post<FaceSearchResult>(`${environment.apiUrl}/face-recognition/search`, {
      photo,
      includeMatchedPhoto: false
    }).pipe(timeout(15000), retry(1));
  }
}
