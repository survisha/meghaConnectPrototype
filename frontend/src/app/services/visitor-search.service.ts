import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Visitor } from '../models';
import { environment } from '../../environments/environment';

export interface PublicIdentificationHistory {
  citizenId: number;
  citizenName: string;
  photoUrl?: string;
  visitCount: number;
  lastVisitedAt?: string;
  schemes: CitizenSchemeHistory[];
  appointments: CitizenAppointmentHistory[];
}

export interface AssociateCitizen {
  id?: number;
  citizenId: number;
  fullName: string;
  mobileNumber?: string;
  epicReference?: string;
  aadhaarReference?: string;
  addressSummary?: string;
  photoUrl?: string;
  kycStatus?: string;
  status?: string;
  relationship?: string;
  remarks?: string;
  role?: 'PRIMARY' | 'ASSOCIATE' | string;
  createdAt?: string;
}

export interface CitizenSchemeHistory {
  id: number;
  schemeName: string;
  projectName?: string;
  appliedDate?: string;
  status: string;
  amount?: number;
  remarks?: string;
}

export interface CitizenAppointmentHistory {
  appointmentId: number;
  applicationId?: string;
  dateTime?: string;
  department?: string;
  officerName?: string;
  purpose?: string;
  status: string;
  remarks?: string;
  role?: 'PRIMARY' | 'ASSOCIATE' | string;
  primaryVisitorName?: string;
  groupMembers?: AssociateCitizen[];
}

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

  getPublicIdentificationHistory(citizenId: number): Observable<PublicIdentificationHistory> {
    return this.http.get<PublicIdentificationHistory>(
      `${environment.apiUrl}/public-identification/citizens/${citizenId}/full-history`
    );
  }

  searchAssociateCitizens(query: string): Observable<AssociateCitizen[]> {
    const params = new HttpParams().set('query', query.trim());
    return this.http.get<AssociateCitizen[]>(`${this.baseUrl}/associate-search`, { params });
  }

  create(visitor: Partial<Visitor>): Observable<Visitor> {
    return this.http.post<Visitor>(this.baseUrl, visitor);
  }

  update(id: number, visitor: Partial<Visitor> & { livePhotoBase64?: string; photoBase64?: string }): Observable<Visitor> {
    return this.http.put<Visitor>(`${this.baseUrl}/${id}`, visitor);
  }

  private nullOnNotFound(error: HttpErrorResponse): Observable<null> {
    if (error.status === 404) {
      return of(null);
    }

    return throwError(() => error);
  }
}
