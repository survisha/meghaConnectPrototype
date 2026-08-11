import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardSummary {
  pendingScheduled: number; scheduledUpcoming: number; liveWalkIns: number;
  completedAppointments: number; routedToOfficial: number; rejected: number;
  openFollowUps: number; overdueFollowUps: number;
}

export interface DirectionFollowUp {
  id: number; directionId: string; appointmentId: number; visitorId: number;
  departmentId: number; departmentName: string; responsibleOfficerName?: string;
  instruction: string; dueDate?: string; status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE';
  priority: string; evidenceRequired: boolean; daysOverdue: number;
}

@Injectable({ providedIn: 'root' })
export class DecisionSupportService {
  private readonly hcmUrl = `${environment.apiUrl}/hcm`;
  private readonly followUpUrl = `${environment.apiUrl}/follow-ups`;
  private readonly reportUrl = `${environment.apiUrl}/reports/appointments`;
  constructor(private http: HttpClient) {}
  dashboardSummary(): Observable<DashboardSummary> { return this.http.get<DashboardSummary>(`${this.hcmUrl}/dashboard-summary`); }
  citizenIntelligence(visitorId: number): Observable<any> { return this.http.get(`${this.hcmUrl}/citizen-intelligence/${visitorId}`); }
  followUps(params: Record<string, string | number | boolean | undefined> = {}): Observable<any> {
    let httpParams = new HttpParams(); Object.entries(params).forEach(([k,v]) => { if (v !== undefined) httpParams = httpParams.set(k, String(v)); });
    return this.http.get(this.followUpUrl, { params: httpParams });
  }
  updateFollowUp(id: number, status: 'IN_PROGRESS' | 'COMPLETED', remarks?: string): Observable<DirectionFollowUp> {
    return this.http.put<DirectionFollowUp>(`${this.followUpUrl}/${id}/status`, { status, remarks });
  }
  report(params: Record<string, string | number | undefined>): Observable<any> {
    let httpParams = new HttpParams(); Object.entries(params).forEach(([k,v]) => { if (v !== undefined && v !== '') httpParams = httpParams.set(k, String(v)); });
    return this.http.get(this.reportUrl, { params: httpParams });
  }
  exportReport(format: 'pdf' | 'xlsx', params: Record<string, string | number | undefined>): Observable<Blob> {
    let httpParams = new HttpParams(); Object.entries(params).forEach(([k,v]) => { if (v !== undefined && v !== '') httpParams = httpParams.set(k, String(v)); });
    return this.http.get(`${this.reportUrl}/export.${format}`, { params: httpParams, responseType: 'blob' });
  }
}
