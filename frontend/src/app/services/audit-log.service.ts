import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuditEntry } from '../models';
import { environment } from '../../environments/environment';

export interface AuditPage {
  content: AuditEntry[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface AuditLogFilters {
  from?: string;
  to?: string;
  module?: string;
  action?: string;
  user?: string;
  role?: string;
  requestId?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {

  private readonly baseUrl = environment.apiUrl + '/audit-logs';

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 50, filters: AuditLogFilters = {}, sort = 'timestamp,desc'): Observable<AuditPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}`.trim()) {
        params = params.set(key, `${value}`.trim());
      }
    });

    return this.http.get<AuditPage>(this.baseUrl, { params }).pipe(
      map(response => this.normalizePage(response, page, size)),
      catchError(err => {
        return throwError(() => err);
      })
    );
  }

  private normalizePage(response: AuditPage, fallbackPage: number, fallbackSize: number): AuditPage {
    const raw: any = response ?? {};
    const content = Array.isArray(raw.content) ? raw.content.map((row: unknown) => this.normalizeEntry(row)) : [];
    return {
      content,
      totalElements: raw.totalElements ?? content.length,
      totalPages: raw.totalPages ?? 1,
      size: raw.size ?? fallbackSize,
      number: raw.number ?? fallbackPage,
    };
  }

  private normalizeEntry(row: unknown): AuditEntry {
    const raw: any = row ?? {};
    return {
      id: Number(raw.id ?? 0),
      timestamp: raw.timestamp ?? raw.createdAt ?? '',
      module: raw.module ?? raw.entity ?? raw.entityType ?? '',
      entity: raw.entity ?? raw.module ?? raw.entityType ?? '',
      entityType: raw.entityType ?? raw.module ?? raw.entity ?? '',
      entityId: Number(raw.entityId ?? 0),
      action: raw.action ?? '',
      user: raw.user ?? raw.performedBy ?? '',
      performedBy: raw.performedBy ?? raw.user ?? '',
      role: raw.role ?? raw.userRole ?? '',
      userRole: raw.userRole ?? raw.role ?? '',
      details: raw.details ?? raw.description ?? '',
      description: raw.description ?? raw.details ?? '',
      requestId: raw.requestId ?? '',
      oldValue: raw.oldValue ?? '',
      newValue: raw.newValue ?? '',
      status: raw.status ?? '',
      ipAddress: raw.ipAddress ?? '',
      endpoint: raw.endpoint ?? '',
    };
  }
}
