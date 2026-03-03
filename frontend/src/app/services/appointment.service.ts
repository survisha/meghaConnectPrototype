import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment } from '../models';

export interface CreateAppointmentRequest {
  applicantId: number;
  eventType: string;
  agendaType: string;
  agendaBrief?: string;
  requestedLocation: string;
  mlaMdcApproved?: boolean;
  isWalkIn?: boolean;
}

export interface ApproveRejectRequest {
  remarks?: string;
}

export interface RescheduleRequest {
  scheduledDateTime: string;
  durationMinutes: number;
  remarks?: string;
}

export interface AppointmentPage {
  content: Appointment[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class AppointmentService {

  private readonly baseUrl = '/api/appointments';
  private readonly v1BaseUrl = '/api/v1/appointments';

  constructor(private http: HttpClient) {}

  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(this.v1BaseUrl, request);
  }

  getPendingAppointments(page = 0, size = 20): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('status', 'HCM_PENDING,APPROVER_REVIEW,CMO_REVIEW')
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<AppointmentPage>(this.v1BaseUrl, { params });
  }

  getAllAppointments(page = 0, size = 20): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<AppointmentPage>(this.v1BaseUrl, { params });
  }

  getAppointmentById(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`${this.v1BaseUrl}/${id}`);
  }

  approveAppointment(id: number, request: ApproveRejectRequest): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/${id}/approve`, request);
  }

  rejectAppointment(id: number, request: ApproveRejectRequest): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/${id}/reject`, request);
  }

  rescheduleAppointment(id: number, request: RescheduleRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.v1BaseUrl}/${id}/schedule`, {
      scheduledDateTime: request.scheduledDateTime,
      durationMinutes: request.durationMinutes,
    });
  }

  updateStatus(id: number, status: string, remarks?: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.v1BaseUrl}/${id}/status`, { status, remarks });
  }
}
