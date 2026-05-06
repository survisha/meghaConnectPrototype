import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AiSummaryRequest {
  appointmentId: number;
  agendaBrief: string;
  agendaType: string;
  applicantName: string;
  district: string;
}

export interface AiSummaryResponse {
  appointmentId: number;
  shortNotes: string;
}

@Injectable({ providedIn: 'root' })
export class AiSummaryService {

  private readonly apiUrl = `${environment.apiUrl}/api/ai/generate-summary`;

  constructor(private http: HttpClient) {}

  generateSummary(request: AiSummaryRequest): Observable<AiSummaryResponse> {
    return this.http.post<AiSummaryResponse>(this.apiUrl, request);
  }

  /** Offline / demo fallback: generate a short note locally */
  generateLocalSummary(request: AiSummaryRequest): AiSummaryResponse {
    const notes = `${request.applicantName} (${request.district}) – ${request.agendaType}: ${request.agendaBrief.slice(0, 120)}${request.agendaBrief.length > 120 ? '…' : ''}`;
    return { appointmentId: request.appointmentId, shortNotes: notes };
  }
}
