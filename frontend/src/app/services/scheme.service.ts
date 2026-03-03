import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SchemeApplication } from '../models';

export interface Scheme {
  schemeId: number;
  schemeCode: string;
  schemeName: string;
  description: string;
  active: boolean;
}

export interface CreateSchemeApplicationRequest {
  applicantId: number;
  appointmentId?: number;
  schemeType: string;
  projectName: string;
  projectCategory?: string;
  beneficiaryType?: string;
  beneficiaryCount?: string;
  estimatedCost?: number;
  communityContribution?: number;
  justification?: string;
}

export interface UpdateSchemeStatusRequest {
  status: string;
  remarks?: string;
  hcmApprovedCost?: number;
}

@Injectable({ providedIn: 'root' })
export class SchemeService {

  private readonly schemesUrl = '/api/schemes';
  private readonly applicationsUrl = '/api/scheme-applications';

  constructor(private http: HttpClient) {}

  // Scheme catalog
  getSchemes(): Observable<Scheme[]> {
    return this.http.get<Scheme[]>(this.schemesUrl);
  }

  createScheme(scheme: Partial<Scheme>): Observable<Scheme> {
    return this.http.post<Scheme>(this.schemesUrl, scheme);
  }

  // Scheme applications
  createApplication(request: CreateSchemeApplicationRequest): Observable<SchemeApplication> {
    return this.http.post<SchemeApplication>(this.applicationsUrl, request);
  }

  getApplicationsByVisitor(visitorId: number): Observable<SchemeApplication[]> {
    return this.http.get<SchemeApplication[]>(`${this.applicationsUrl}/${visitorId}`);
  }

  updateApplicationStatus(id: number, request: UpdateSchemeStatusRequest): Observable<SchemeApplication> {
    return this.http.put<SchemeApplication>(`${this.applicationsUrl}/${id}/status`, request);
  }

  getAllApplications(params?: { status?: string; page?: number; size?: number }): Observable<unknown> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params?.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get(this.applicationsUrl, { params: httpParams });
  }
}
