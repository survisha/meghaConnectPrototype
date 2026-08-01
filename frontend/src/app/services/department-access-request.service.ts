import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CreateDepartmentAccessRequest {
  departmentCode: string;
  nodalOfficerName: string;
  officialEmail: string;
  officialMobile: string;
  requestPurpose: string;
  expectedUserCount: number;
  remarks?: string;
}

export interface DepartmentAccessRequest extends CreateDepartmentAccessRequest {
  id: number;
  departmentName: string;
  requestStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  submittedAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
  rejectionReason?: string;
}

export interface ApiEnvelope<T> { data: T; message: string; success: boolean; }
export interface PageResult<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }

@Injectable({ providedIn: 'root' })
export class DepartmentAccessRequestService {
  private readonly url = `${environment.apiUrl}/department-access-requests`;
  constructor(private readonly http: HttpClient) {}

  submit(request: CreateDepartmentAccessRequest): Observable<ApiEnvelope<DepartmentAccessRequest>> {
    return this.http.post<ApiEnvelope<DepartmentAccessRequest>>(this.url, request);
  }

  list(page: number, size: number, status?: string): Observable<ApiEnvelope<PageResult<DepartmentAccessRequest>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiEnvelope<PageResult<DepartmentAccessRequest>>>(this.url, { params });
  }

  get(id: number): Observable<ApiEnvelope<DepartmentAccessRequest>> {
    return this.http.get<ApiEnvelope<DepartmentAccessRequest>>(`${this.url}/${id}`);
  }

  approve(id: number): Observable<ApiEnvelope<unknown>> {
    return this.http.post<ApiEnvelope<unknown>>(`${this.url}/${id}/approve`, {});
  }

  reject(id: number, rejectionReason: string): Observable<ApiEnvelope<DepartmentAccessRequest>> {
    return this.http.post<ApiEnvelope<DepartmentAccessRequest>>(`${this.url}/${id}/reject`, { rejectionReason });
  }
}
