import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Person } from '../models';
import { environment } from '../../environments/environment.development';

@Injectable({ providedIn: 'root' })
export class PersonService {

  private readonly baseUrl = environment.apiUrl + '/persons';

  constructor(private http: HttpClient) {}

  searchByPhone(phone: string): Observable<Person | null> {
    return this.http.get<Person>(`${this.baseUrl}/search/phone/${encodeURIComponent(phone)}`).pipe(
      catchError(() => of(null))
    );
  }

  searchByEpic(epic: string): Observable<Person | null> {
    return this.http.get<Person>(`${this.baseUrl}/search/epic/${encodeURIComponent(epic)}`).pipe(
      catchError(() => of(null))
    );
  }

  searchByName(q: string): Observable<Person[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Person[]>(`${this.baseUrl}/search/name`, { params }).pipe(
      catchError(() => of([]))
    );
  }

  searchByDistrict(district: string): Observable<Person[]> {
    return this.http.get<Person[]>(`${this.baseUrl}/search/district/${encodeURIComponent(district)}`).pipe(
      catchError(() => of([]))
    );
  }

  getById(id: number): Observable<Person | null> {
    return this.http.get<Person>(`${this.baseUrl}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  create(person: Partial<Person>): Observable<Person> {
    return this.http.post<Person>(this.baseUrl, person);
  }
}
