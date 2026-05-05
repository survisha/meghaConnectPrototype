import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Appointment } from '../models';
import { environment } from '../../environments/environment';

export interface CreateAppointmentRequest {
  applicantId: number;
  eventType: string;
  subject?: string;
  department?: string;
  appointmentType?: string;
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

  private readonly baseUrl = environment.apiUrl + '/appointments';

  constructor(private http: HttpClient) {}

  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<unknown>(this.baseUrl, request)
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  getMyAppointments(): Observable<Appointment[]> {
    return this.http.get<unknown>(`${this.baseUrl}/my`).pipe(
      map(res => {
        const data = this.unwrapData<unknown>(res);
        const rows = Array.isArray(data) ? data : [];
        return rows.map(row => this.normalizeAppointment(row));
      })
    );
  }

  getDeoAppointments(page = 0, size = 100): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<unknown>(`${this.baseUrl}/deo`, { params })
      .pipe(map(res => this.normalizePage(res)));
  }

  getApproverAppointments(page = 0, size = 100): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<unknown>(`${this.baseUrl}/approver`, { params })
      .pipe(map(res => this.normalizePage(res)));
  }

  getPendingAppointments(page = 0, size = 20): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('status', 'HCM_PENDING,APPROVER_REVIEW,CMO_REVIEW')
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<unknown>(this.baseUrl, { params })
      .pipe(map(res => this.normalizePage(res)));
  }

  getAllAppointments(page = 0, size = 20): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<unknown>(this.baseUrl, { params })
      .pipe(map(res => this.normalizePage(res)));
  }

  getAppointmentById(id: number): Observable<Appointment> {
    return this.http.get<unknown>(`${this.baseUrl}/${id}`)
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  approveAppointment(id: number, request: ApproveRejectRequest): Observable<Appointment> {
    return this.http.put<unknown>(`${this.baseUrl}/${id}/approve`, request)
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  rejectAppointment(id: number, request: ApproveRejectRequest): Observable<Appointment> {
    return this.http.put<unknown>(`${this.baseUrl}/${id}/reject`, request)
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  rescheduleAppointment(id: number, request: RescheduleRequest): Observable<Appointment> {
    return this.http.post<unknown>(`${this.baseUrl}/${id}/schedule`, {
      scheduledDateTime: request.scheduledDateTime,
      durationMinutes: request.durationMinutes,
    }).pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  updateStatus(id: number, status: string, remarks?: string): Observable<Appointment> {
    return this.http.patch<unknown>(`${this.baseUrl}/${id}/status`, { status, remarks })
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  /**
   * Submit CMO review with remarks about pending information
   * Notifies applicant and DEO of any missing information
   */
  submitCmoReview(payload: {
    appointmentId: number;
    cmoRemarks: string;
    pendingInformation: string;
    status: string;
    notifyApplicant: boolean;
    notifyDeo: boolean;
  }): Observable<Appointment> {
    return this.http.post<unknown>(`${this.baseUrl}/${payload.appointmentId}/cmo-review`, {
      cmoRemarks: payload.cmoRemarks,
      pendingInformation: payload.pendingInformation,
      status: payload.status,
      notifyApplicant: payload.notifyApplicant,
      notifyDeo: payload.notifyDeo
    }).pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  private normalizePage(response: unknown): AppointmentPage {
    const data: any = this.unwrapData(response);
    const contentSource = Array.isArray(data) ? data : (data?.content ?? []);
    const content = (contentSource as unknown[]).map(row => this.normalizeAppointment(row));
    return {
      content,
      totalElements: data?.totalElements ?? content.length,
      totalPages: data?.totalPages ?? 1,
      size: data?.size ?? content.length,
      number: data?.number ?? 0,
    };
  }

  private normalizeAppointment(row: unknown): Appointment {
    const raw: any = row ?? {};
    const applicantRaw = raw.applicant ?? {};
    const applicant = {
      id: Number(applicantRaw.id ?? raw.applicantId ?? 0),
      fullName: applicantRaw.fullName ?? raw.applicantName ?? '—',
      phoneNumber: applicantRaw.phoneNumber ?? raw.applicantPhone ?? raw.applicantMobile ?? '',
      epicNumber: applicantRaw.epicNumber ?? '',
      photoUrl: applicantRaw.photoUrl,
      designation: applicantRaw.designation ?? '',
      district: applicantRaw.district ?? raw.district ?? '',
      constituency: applicantRaw.constituency ?? '',
      booth: applicantRaw.booth ?? '',
      village: applicantRaw.village,
      briefProfile: applicantRaw.briefProfile,
      kycStatus: applicantRaw.kycStatus,
    };

    return {
      id: Number(raw.id ?? raw.appointmentId ?? 0),
      applicationId: raw.applicationId ?? raw.id?.toString() ?? '',
      applicantId: raw.applicantId ?? applicant.id,
      applicant,
      applicantName: raw.applicantName ?? applicant.fullName,
      applicantPhone: raw.applicantPhone ?? applicant.phoneNumber,
      subject: raw.subject,
      department: raw.department,
      appointmentType: raw.appointmentType,
      agendaType: raw.agendaType ?? raw.appointmentType ?? raw.subject ?? '',
      agendaBrief: raw.agendaBrief ?? raw.description ?? '',
      status: raw.status ?? 'SUBMITTED',
      requestedLocation: raw.requestedLocation ?? 'OTHERS',
      scheduledDateTime: raw.scheduledDateTime,
      scheduledDurationMinutes: raw.scheduledDurationMinutes,
      eventType: raw.eventType ?? 'A4',
      mlaMdcApproved: raw.mlaMdcApproved,
      meetingCountLast6Months: raw.meetingCountLast6Months,
      cmoRemarks: raw.cmoRemarks,
      approverRemarks: raw.approverRemarks,
      hcmRemarks: raw.hcmRemarks,
      shortNotes: raw.shortNotes,
      directions: raw.directions,
      isWalkIn: raw.isWalkIn,
      createdAt: raw.createdAt,
      submittedAt: raw.submittedAt ?? raw.submittedDate ?? raw.createdAt,
      updatedAt: raw.updatedAt,
    } as Appointment;
  }

  private unwrapData<T = unknown>(response: unknown): T {
    const raw: any = response;
    if (raw && typeof raw === 'object' && 'data' in raw && 'success' in raw) {
      return raw.data as T;
    }
    return response as T;
  }
}
