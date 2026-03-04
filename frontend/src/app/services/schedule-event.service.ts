import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ScheduleEvent } from '../models';
import { environment } from '../../environments/environment.development';

@Injectable({ providedIn: 'root' })
export class ScheduleEventService {

  private readonly baseUrl = environment.apiUrl + '/schedule';

  constructor(private http: HttpClient) {}

  getAll(): Observable<ScheduleEvent[]> {
    return this.http.get<ScheduleEvent[]>(this.baseUrl).pipe(
      catchError(() => of([]))
    );
  }

  getById(id: number): Observable<ScheduleEvent | null> {
    return this.http.get<ScheduleEvent>(`${this.baseUrl}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  create(event: Partial<ScheduleEvent>): Observable<ScheduleEvent> {
    return this.http.post<ScheduleEvent>(this.baseUrl, event);
  }

  update(id: number, event: Partial<ScheduleEvent>): Observable<ScheduleEvent> {
    return this.http.put<ScheduleEvent>(`${this.baseUrl}/${id}`, event);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
