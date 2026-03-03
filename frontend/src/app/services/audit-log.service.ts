import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuditEntry } from '../models';

export interface AuditPage {
  content: AuditEntry[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {

  private readonly baseUrl = '/api/v1/audit-logs';

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 50): Observable<AuditPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'timestamp,desc');
    return this.http.get<AuditPage>(this.baseUrl, { params }).pipe(
      catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size, number: page }))
    );
  }
}
