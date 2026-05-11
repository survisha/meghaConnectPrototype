import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Grievance, GrievanceCategory, GrievanceStatus } from '../models';
import { environment } from '../../environments/environment';

export interface CreateGrievanceRequest {
  applicantName?: string;
  phoneNumber?: string;
  district?: string;
  constituency?: string;
  category?: GrievanceCategory;
  visitorId?: number;
  subject: string;
  description: string;
}

export interface UpdateGrievanceRequest {
  subject: string;
  description: string;
}

export interface GrievancePage {
  content: Grievance[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class GrievanceService {

  private readonly baseUrl = environment.apiUrl + '/grievances';

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 50): Observable<GrievancePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<GrievancePage>(this.baseUrl, { params }).pipe(
      catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size, number: page }))
    );
  }

  getByVisitor(visitorId: number, page = 0, size = 50): Observable<GrievancePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<GrievancePage>(`${this.baseUrl}/visitor/${visitorId}`, { params }).pipe(
      catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size, number: page }))
    );
  }

  getById(id: number): Observable<Grievance | null> {
    return this.http.get<Grievance>(`${this.baseUrl}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  create(request: CreateGrievanceRequest): Observable<Grievance> {
    return this.http.post<Grievance>(this.baseUrl, request);
  }

  update(id: number, request: UpdateGrievanceRequest): Observable<Grievance> {
    return this.http.put<Grievance>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, status: GrievanceStatus, remarks?: string): Observable<Grievance> {
    return this.http.patch<Grievance>(`${this.baseUrl}/${id}/status`, { status, remarks });
  }
}
