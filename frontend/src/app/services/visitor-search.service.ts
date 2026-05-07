import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Visitor } from '../models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VisitorSearchService {

  private readonly baseUrl = environment.apiUrl + '/visitors';

  constructor(private http: HttpClient) {}

  search(criteria: { mobile?: string; epic?: string; referenceId?: string }): Observable<Visitor[]> {
    let params = new HttpParams();
    const mobile = criteria.mobile?.trim();
    const epic = criteria.epic?.trim();
    const referenceId = criteria.referenceId?.trim();
    if (mobile) params = params.set('mobile', mobile);
    if (epic) params = params.set('epic', epic.toUpperCase());
    if (referenceId) params = params.set('referenceId', referenceId);
    return this.http.get<Visitor[]>(`${this.baseUrl}/search`, { params });
  }

  searchByPhone(phone: string): Observable<Visitor[]> {
    return this.http.get<Visitor[]>(`${this.baseUrl}/search/phone/${encodeURIComponent(phone)}`);
  }

  searchByEpic(epic: string): Observable<Visitor | null> {
    return this.http.get<Visitor>(`${this.baseUrl}/search/epic/${encodeURIComponent(epic)}`).pipe(
      catchError(error => this.nullOnNotFound(error))
    );
  }

  searchByName(q: string): Observable<Visitor[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Visitor[]>(`${this.baseUrl}/search/name`, { params });
  }

  searchByDistrict(district: string): Observable<Visitor[]> {
    return this.http.get<Visitor[]>(`${this.baseUrl}/search/district/${encodeURIComponent(district)}`);
  }

  getById(id: number): Observable<Visitor | null> {
    return this.http.get<Visitor>(`${this.baseUrl}/${id}`).pipe(
      catchError(error => this.nullOnNotFound(error))
    );
  }

  create(visitor: Partial<Visitor>): Observable<Visitor> {
    return this.http.post<Visitor>(this.baseUrl, visitor);
  }

  private nullOnNotFound(error: HttpErrorResponse): Observable<null> {
    if (error.status === 404) {
      return of(null);
    }

    return throwError(() => error);
  }
}
