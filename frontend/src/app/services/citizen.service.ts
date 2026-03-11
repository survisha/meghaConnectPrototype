import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Person } from '../models';
import { environment } from '../../environments/environment.development';

export interface CitizenSearchParams {
  phoneNumber?: string;
  epicNumber?: string;
  name?: string;
  district?: string;
  village?: string;
}

@Injectable({ providedIn: 'root' })
export class CitizenService {
  private base = `${environment.apiUrl}/persons`;

  constructor(private http: HttpClient) {}

  search(params: CitizenSearchParams): Observable<Person[]> {
    let p = new HttpParams();
    if (params.phoneNumber) p = p.set('phoneNumber', params.phoneNumber);
    if (params.epicNumber) p = p.set('epicNumber', params.epicNumber);
    if (params.name) p = p.set('name', params.name);
    if (params.district) p = p.set('district', params.district);
    if (params.village) p = p.set('village', params.village);
    return this.http.get<Person[]>(`${this.base}/search`, { params: p }).pipe(
      catchError(() => of([]))
    );
  }

  getById(id: number): Observable<Person> {
    return this.http.get<Person>(`${this.base}/${id}`).pipe(
      catchError(err => { throw err; })
    );
  }

  create(person: Partial<Person>): Observable<Person> {
    return this.http.post<Person>(this.base, person);
  }

  update(id: number, person: Partial<Person>): Observable<Person> {
    return this.http.put<Person>(`${this.base}/${id}`, person);
  }
}
