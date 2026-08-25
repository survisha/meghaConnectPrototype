import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Appointment, AppointmentDocument, EventType, Location, ScheduleEvent } from '../models';
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

export interface GuestAppointmentRequest {
  fullName: string;
  mobileNumber: string;
  address: string;
  email?: string;
  organizationName?: string;
  designation?: string;
  visitorCategory?: string;
  referredOffice: string;
  referredByName?: string;
  reasonForAppointment: string;
  preferredDate?: string;
  remarks?: string;
  livePhotoBase64?: string;
  supportingDocument?: File | null;
}

export interface GuestAppointmentResponse {
  referenceId: string;
  status: string;
  message: string;
}

export type AiNotesStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'PARTIAL_SUCCESS' | 'FAILED';

export interface AppointmentDocumentAiNotes {
  id: number;
  appointmentId: number;
  documentId: number;
  fileName: string;
  aiSummary: string;
  importantDetails: string;
  missingInfo: string;
  riskFlags: string;
  status: AiNotesStatus;
  errorMessage?: string;
  modelName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AppointmentPriorityInsight {
  level: 'HIGH' | 'MEDIUM' | 'LOW';
  score: number;
  reasons: string[];
  recommendation: string;
}

export interface CmoReviewRequest {
  appointmentId: number;
  eventType?: EventType;
  requestedLocation?: Location;
  cmoRemarks?: string;
  pendingInformation?: string;
  status: string;
  notifyApplicant?: boolean;
  notifyDeo?: boolean;
}

export interface AppointmentRemark {
  id?: number;
  appointmentId?: number;
  hcmRemarks?: string;
  decision?: string;
  departmentCode?: string;
  departmentName?: string;
  createdBy?: string;
  createdByRole?: string;
  createdAt?: string;
}

export interface AppointmentPdfExportAuditRequest {
  selectedCount: number;
  appointmentIds: number[];
  filters: {
    fromDate?: string;
    toDate?: string;
    status?: string;
  };
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

  getAllAppointments(page = 0, size = 20, status?: string, options?: { source?: string; appointmentType?: 'NORMAL' | 'WALKIN'; referredOffice?: string; sort?: string }): Observable<AppointmentPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) {
      params = params.set('status', status);
    }
    if (options?.source) {
      params = params.set('source', options.source);
    }
    if (options?.appointmentType) {
      params = params.set('appointmentType', options.appointmentType);
    }
    if (options?.referredOffice) {
      params = params.set('referredOffice', options.referredOffice);
    }
    if (options?.sort) {
      params = params.set('sort', options.sort);
    }
    return this.http.get<unknown>(this.baseUrl, { params })
      .pipe(map(res => this.normalizePage(res)));
  }

  createGuestAppointment(request: GuestAppointmentRequest): Observable<GuestAppointmentResponse> {
    const formData = new FormData();
    formData.append('fullName', request.fullName);
    formData.append('mobileNumber', request.mobileNumber);
    formData.append('address', request.address);
    formData.append('referredOffice', request.referredOffice);
    formData.append('reasonForAppointment', request.reasonForAppointment);
    this.appendIfPresent(formData, 'email', request.email);
    this.appendIfPresent(formData, 'organizationName', request.organizationName);
    this.appendIfPresent(formData, 'designation', request.designation);
    this.appendIfPresent(formData, 'visitorCategory', request.visitorCategory);
    this.appendIfPresent(formData, 'referredByName', request.referredByName);
    this.appendIfPresent(formData, 'preferredDate', request.preferredDate);
    this.appendIfPresent(formData, 'remarks', request.remarks);
    this.appendIfPresent(formData, 'livePhotoBase64', request.livePhotoBase64);
    if (request.supportingDocument) {
      formData.append('supportingDocument', request.supportingDocument, request.supportingDocument.name);
    }
    const guestUrl = environment.apiUrl.replace(/\/appointments$/, '') + '/guest-appointments';
    return this.http.post<GuestAppointmentResponse>(guestUrl, formData);
  }

  getAppointmentById(id: number): Observable<Appointment> {
    return this.http.get<unknown>(`${this.baseUrl}/${id}`)
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  getAppointmentDocuments(id: number): Observable<AppointmentDocument[]> {
    return this.http.get<unknown>(`${this.baseUrl}/${id}/documents`).pipe(
      map(res => {
        const data = this.unwrapData<unknown>(res);
        const rows = Array.isArray(data) ? data : [];
        return rows.map(row => this.normalizeDocument(row));
      })
    );
  }

  getAiNotesByAppointment(appointmentId: number): Observable<AppointmentDocumentAiNotes[]> {
    return this.http.get<unknown>(`${this.baseUrl}/${appointmentId}/ai-notes`).pipe(
      map(res => {
        const data = this.unwrapData<unknown>(res);
        const rows = Array.isArray(data) ? data : [];
        return rows.map(row => this.normalizeAiNotes(row));
      })
    );
  }

  regenerateAiNotes(documentId: number): Observable<AppointmentDocumentAiNotes> {
    return this.http.post<unknown>(`${this.baseUrl}/documents/${documentId}/ai-notes/regenerate`, {})
      .pipe(map(res => this.normalizeAiNotes(this.unwrapData(res))));
  }

  getAiPriorityInsight(appointmentId: number): Observable<AppointmentPriorityInsight> {
    return this.http.get<AppointmentPriorityInsight>(
      `${environment.apiUrl}/ai/appointments/${appointmentId}/priority-insight`
    );
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

  rescheduleAppointmentDate(id: number, request: { scheduledDate: string; scheduledTime: string; eventId?: number }): Observable<Appointment> {
    return this.http.post<unknown>(`${this.baseUrl}/${id}/reschedule`, request)
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  updateStatus(id: number, status: string, remarks?: string): Observable<Appointment> {
    return this.http.patch<unknown>(`${this.baseUrl}/${id}/status`, { status, remarks })
      .pipe(map(res => this.normalizeAppointment(this.unwrapData(res))));
  }

  assignAppointmentsToEvent(eventId: number, appointmentIds: number[], remarks = 'Scheduled'): Observable<ScheduleEvent> {
    return this.http.post<ScheduleEvent>(`${this.baseUrl}/assign-event`, { eventId, appointmentIds, remarks });
  }

  getHcmActionAppointments(date: string): Observable<Appointment[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<unknown>(`${this.baseUrl}/hcm-actions`, { params }).pipe(
      map(res => {
        const data = this.unwrapData<unknown>(res);
        const rows = Array.isArray(data) ? data : [];
        return rows.map(row => this.normalizeAppointment(row));
      })
    );
  }

  getRemarks(appointmentId: number): Observable<AppointmentRemark[]> {
    return this.http.get<unknown>(`${this.baseUrl}/${appointmentId}/remarks`).pipe(
      map(res => {
        const data = this.unwrapData<unknown>(res);
        return Array.isArray(data) ? data.map((row: any) => ({
          id: row.id,
          appointmentId: row.appointmentId,
          hcmRemarks: row.hcmRemarks,
          decision: row.decision,
          departmentCode: row.departmentCode,
          departmentName: row.departmentName,
          createdBy: row.createdBy,
          createdByRole: row.createdByRole,
          createdAt: row.createdAt,
        })) : [];
      })
    );
  }

  addRemark(appointmentId: number, payload: { hcmRemarks: string; decision?: string; departmentCode?: string }): Observable<AppointmentRemark> {
    return this.http.post<AppointmentRemark>(`${this.baseUrl}/${appointmentId}/remarks`, payload);
  }

  updateRemark(appointmentId: number, remarkId: number, payload: { hcmRemarks: string; decision?: string; departmentCode?: string }): Observable<AppointmentRemark> {
    return this.http.put<AppointmentRemark>(`${this.baseUrl}/${appointmentId}/remarks/${remarkId}`, payload);
  }

  auditPdfExport(payload: AppointmentPdfExportAuditRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/export-audit`, payload);
  }

  uploadSupportingDocument(appointmentId: number, file: File): Observable<AppointmentDocument> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<unknown>(`${this.baseUrl}/${appointmentId}/supporting-documents`, formData)
      .pipe(map(res => this.normalizeDocument(this.unwrapData(res))));
  }

  requestMissingInformation(appointmentId: number, remarks = ''): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.baseUrl}/${appointmentId}/request-missing-information`, { remarks }).pipe(map(item => this.normalizeAppointment(item)));
  }

  closeAppointment(appointmentId: number, remarks = ''): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.baseUrl}/${appointmentId}/close`, { remarks }).pipe(map(item => this.normalizeAppointment(item)));
  }

  downloadVisitorPass(appointmentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${appointmentId}/visitor-pass/download`, {
      responseType: 'blob',
    });
  }

  markForPublicDarbar(id: number, remarks = 'Follow-up'): Observable<unknown> {
    return this.http.post<unknown>(`${this.baseUrl}/approver/${id}/select-public-darbar`, { remarks });
  }

  /**
   * Submit CMO review with remarks about pending information
   * Notifies applicant and DEO of any missing information
   */
  submitCmoReview(payload: CmoReviewRequest): Observable<Appointment> {
    return this.http.post<unknown>(`${this.baseUrl}/${payload.appointmentId}/cmo-review`, {
      eventType: payload.eventType,
      requestedLocation: payload.requestedLocation,
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
      fullName: applicantRaw.fullName ?? raw.guestName ?? raw.applicantName ?? '—',
      phoneNumber: applicantRaw.phoneNumber ?? raw.guestMobile ?? raw.applicantPhone ?? raw.applicantMobile ?? '',
      epicNumber: applicantRaw.epicNumber ?? '',
      photoUrl: applicantRaw.photoUrl,
      livePhotoBase64: applicantRaw.livePhotoBase64,
      photoBase64: applicantRaw.photoBase64,
      livePhotoPath: applicantRaw.livePhotoPath,
      photoStoragePath: applicantRaw.photoStoragePath,
      photoPath: applicantRaw.photoPath,
      designation: applicantRaw.designation ?? raw.guestDesignation ?? raw.designation ?? '',
      address: applicantRaw.address ?? raw.address,
      fullAddress: applicantRaw.fullAddress ?? raw.fullAddress,
      address1: applicantRaw.address1 ?? raw.address1,
      addressLine: applicantRaw.addressLine ?? raw.addressLine,
      outsideMeghalaya: applicantRaw.outsideMeghalaya ?? raw.outsideMeghalaya,
      district: applicantRaw.district ?? raw.district ?? '',
      constituency: applicantRaw.constituency ?? '',
      booth: applicantRaw.booth ?? '',
      boothVillage: applicantRaw.boothVillage,
      partNumber: applicantRaw.partNumber ?? applicantRaw.pollingPartNo,
      pollingPartNo: applicantRaw.pollingPartNo,
      village: applicantRaw.village,
      agendaType: applicantRaw.agendaType,
      briefDescription: applicantRaw.briefDescription,
      briefProfile: applicantRaw.briefProfile,
      kycStatus: applicantRaw.kycStatus,
    };

    return {
      id: Number(raw.id ?? raw.appointmentId ?? 0),
      applicationId: raw.applicationId ?? raw.id?.toString() ?? '',
      applicantId: raw.applicantId ?? applicant.id,
      applicant,
      applicantName: raw.applicantName ?? raw.guestName ?? applicant.fullName,
      applicantPhone: raw.applicantPhone ?? raw.guestMobile ?? applicant.phoneNumber,
      subject: raw.subject,
      department: raw.department,
      appointmentType: raw.appointmentType,
      appointmentSource: raw.appointmentSource ?? 'CITIZEN',
      guestReferenceId: raw.guestReferenceId,
      guestName: raw.guestName,
      guestMobile: raw.guestMobile,
      guestAddress: raw.guestAddress,
      guestEmail: raw.guestEmail,
      organizationName: raw.organizationName,
      guestDesignation: raw.guestDesignation,
      visitorCategory: raw.visitorCategory,
      referredOffice: raw.referredOffice,
      referredByName: raw.referredByName,
      reasonForAppointment: raw.reasonForAppointment,
      preferredDate: raw.preferredDate,
      agendaType: raw.agendaType ?? raw.appointmentType ?? raw.subject ?? '',
      agendaBrief: raw.agendaBrief ?? raw.reasonForAppointment ?? raw.description ?? '',
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
      associates: Array.isArray(raw.associates) ? raw.associates.map((associate: any) => ({
        id: associate.id,
        citizenId: Number(associate.citizenId ?? 0),
        fullName: associate.fullName ?? '—',
        mobileNumber: associate.mobileNumber,
        epicReference: associate.epicReference,
        aadhaarReference: associate.aadhaarReference,
        addressSummary: associate.addressSummary,
        photoUrl: associate.photoUrl,
        kycStatus: associate.kycStatus,
        status: associate.status,
        relationship: associate.relationship,
        remarks: associate.remarks,
        role: associate.role,
      })) : [],
      isWalkIn: raw.isWalkIn,
      walkInTokenNumber: raw.walkInTokenNumber ?? raw.tokenNumber,
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

  private appendIfPresent(formData: FormData, key: string, value?: string | null): void {
    if (value !== undefined && value !== null && value !== '') {
      formData.append(key, value);
    }
  }

  private normalizeDocument(row: unknown): AppointmentDocument {
    const raw: any = row ?? {};
    return {
      id: raw.id,
      appointmentId: raw.appointmentId,
      documentType: raw.documentType,
      fileName: raw.fileName ?? raw.originalFilename ?? raw.documentType ?? 'Document',
      filePath: raw.filePath ?? '',
      fileSize: Number(raw.fileSize ?? raw.fileSizeBytes ?? 0),
      mimeType: raw.mimeType ?? raw.contentType,
      uploadedAt: raw.uploadedAt ?? raw.createdAt,
      isRequired: Boolean(raw.isRequired),
      status: raw.status ?? 'UPLOADED',
    } as AppointmentDocument;
  }

  private normalizeAiNotes(row: unknown): AppointmentDocumentAiNotes {
    const raw: any = row ?? {};
    return {
      id: Number(raw.id ?? 0),
      appointmentId: Number(raw.appointmentId ?? 0),
      documentId: Number(raw.documentId ?? 0),
      fileName: raw.fileName ?? 'Document',
      aiSummary: raw.aiSummary ?? '',
      importantDetails: raw.importantDetails ?? '',
      missingInfo: raw.missingInfo ?? '',
      riskFlags: raw.riskFlags ?? '',
      status: raw.status ?? 'PENDING',
      errorMessage: raw.errorMessage,
      modelName: raw.modelName,
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
    };
  }
}
