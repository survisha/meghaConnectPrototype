import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface AiExtractedFields {
  projectName?: string;
  projectCategory?: string;
  estimatedCost?: string;
  location?: string;
  beneficiaries?: string;
  schemeRequested?: string;
  applicantName?: string;
  justification?: string;
}

export interface AiDocumentAnalysisResponse {
  success: boolean;
  summary: string;
  extractedFields: AiExtractedFields;
  priorityLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  priorityReason: string;
  duplicateFlag: boolean;
  duplicateDetails?: {
    previousApplicationId: string;
    schemeName: string;
    dateSubmitted: string;
  };
}

export interface DuplicateCheckRequest {
  epicNumber: string;
  phoneNumber: string;
  agendaType: string;
  schemeType?: string;
  projectName?: string;
}

export interface DuplicateCheckResponse {
  isDuplicate: boolean;
  previousApplicationId?: string;
  schemeName?: string;
  dateSubmitted?: string;
}

@Injectable({ providedIn: 'root' })
export class AiDocumentService {

  constructor(private http: HttpClient) {}

  analyzeDocument(file: File): Observable<AiDocumentAnalysisResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AiDocumentAnalysisResponse>('/api/ai/analyze-document', formData).pipe(
      catchError(() => of(this.getMockAnalysis(file.name)))
    );
  }

  checkDuplicate(request: DuplicateCheckRequest): Observable<DuplicateCheckResponse> {
    return this.http.post<DuplicateCheckResponse>('/api/ai/check-duplicate', request).pipe(
      catchError(() => of({ isDuplicate: false }))
    );
  }

  suggestPriority(agendaType: string, agendaBrief: string): Observable<{ level: 'HIGH' | 'MEDIUM' | 'LOW'; reason: string }> {
    return this.http.post<{ level: 'HIGH' | 'MEDIUM' | 'LOW'; reason: string }>(
      '/api/ai/suggest-priority', { agendaType, agendaBrief }
    ).pipe(catchError(() => of(this.getMockPriority(agendaType))));
  }

  /** Local mock for demo/offline mode */
  getMockAnalysis(_fileName: string): AiDocumentAnalysisResponse {
    return {
      success: true,
      summary:
        'Project: Community Hall Construction\n' +
        'Location: East Khasi Hills\n' +
        'Estimated Cost: ₹25,00,000\n' +
        'Beneficiaries: 650 villagers\n' +
        'Purpose: Construction of community gathering space',
      extractedFields: {
        projectName: 'Community Hall Construction',
        projectCategory: 'Community Hall',
        estimatedCost: '2500000',
        location: 'East Khasi Hills',
        beneficiaries: '501 to 1000',
        schemeRequested: 'CMSDF',
        applicantName: '',
        justification: 'The village lacks a proper gathering space for community events.',
      },
      priorityLevel: 'MEDIUM',
      priorityReason: 'Infrastructure project with community benefit',
      duplicateFlag: false,
    };
  }

  getMockPriority(agendaType: string): { level: 'HIGH' | 'MEDIUM' | 'LOW'; reason: string } {
    if (agendaType?.toLowerCase().includes('medical') || agendaType?.toLowerCase().includes('care')) {
      return { level: 'HIGH', reason: 'Medical case – requires urgent attention' };
    }
    if (agendaType?.toLowerCase().includes('grievance') || agendaType?.toLowerCase().includes('governance')) {
      return { level: 'MEDIUM', reason: 'Public grievance – moderate priority' };
    }
    return { level: 'LOW', reason: 'General discussion or political matter' };
  }
}
