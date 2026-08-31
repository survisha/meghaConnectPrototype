import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReportAnalytics {
  meetingDates: Array<{ meetingDate: string; scheduled: number; completed: number }>;
  statusCounts: Array<{ status: string; total: number }>;
  topConstituencies: Array<{ name: string; total: number; approved: number; rejected: number }>;
  schemeDistricts: Array<{ scheme: string; district: string; total: number; approved: number; rejected: number }>;
}

@Injectable({ providedIn: 'root' })
export class ReportAnalyticsService {
  constructor(private readonly http: HttpClient) {}
  load(): Observable<ReportAnalytics> {
    return this.http.get<ReportAnalytics>(`${environment.apiUrl}/reports/appointments/analytics`);
  }
}
