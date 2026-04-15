import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Visitor } from '../models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VisitorSearchService {

  private readonly baseUrl = environment.apiUrl + '/visitors';

  constructor(private http: HttpClient) {}

  searchByPhone(phone: string): Observable<Visitor | null> {
    return this.http.get<Visitor>(`${this.baseUrl}/search/phone/${encodeURIComponent(phone)}`).pipe(
      catchError(() => of(null))
    );
  }

  searchByEpic(epic: string): Observable<Visitor | null> {
    return this.http.get<Visitor>(`${this.baseUrl}/search/epic/${encodeURIComponent(epic)}`).pipe(
      catchError(() => of(null))
    );
  }

  searchByName(q: string): Observable<Visitor[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Visitor[]>(`${this.baseUrl}/search/name`, { params }).pipe(
      catchError(() => of([]))
    );
  }

  searchByDistrict(district: string): Observable<Visitor[]> {
    return this.http.get<Visitor[]>(`${this.baseUrl}/search/district/${encodeURIComponent(district)}`).pipe(
      catchError(() => of([]))
    );
  }

  getById(id: number): Observable<Visitor | null> {
    return this.http.get<Visitor>(`${this.baseUrl}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  create(visitor: Partial<Visitor>): Observable<Visitor> {
    return this.http.post<Visitor>(this.baseUrl, visitor);
  }
}
