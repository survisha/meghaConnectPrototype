import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Visitor } from '../models';

export interface RegisterVisitorRequest {
  fullName: string;
  phoneNumber: string;
  epicNumber?: string;
  designation?: string;
  addressLine?: string;
  district?: string;
  constituency?: string;
  booth?: string;
  boothVillage?: string;
  village?: string;
  outsideMeghalaya?: boolean;
  location?: string;
  livePhotoBase64?: string;
}

export interface OtpLoginRequest {
  mobile: string;
  otp: string;
}

export interface OtpLoginResponse {
  token: string;
  visitorId: number;
  fullName: string;
}

export interface VisitorHistory {
  appointments: unknown[];
  schemeApplications: unknown[];
  grievances: unknown[];
}

@Injectable({ providedIn: 'root' })
export class VisitorService {

  private readonly baseUrl = '/api/visitors';

  constructor(private http: HttpClient) {}

  register(request: RegisterVisitorRequest): Observable<Visitor> {
    return this.http.post<Visitor>(`${this.baseUrl}/register`, request);
  }

  loginWithOtp(request: OtpLoginRequest): Observable<OtpLoginResponse> {
    return this.http.post<OtpLoginResponse>(`${this.baseUrl}/login-otp`, request);
  }

  getById(id: number): Observable<Visitor> {
    return this.http.get<Visitor>(`${this.baseUrl}/${id}`);
  }

  getHistory(id: number): Observable<VisitorHistory> {
    return this.http.get<VisitorHistory>(`${this.baseUrl}/history/${id}`);
  }
}
