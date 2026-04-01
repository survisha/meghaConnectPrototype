import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ScheduleEvent } from '../models';
import { environment } from '../../environments/environment.development';
import { MockDataService } from './mock-data.service';

@Injectable({ providedIn: 'root' })
export class ScheduleEventService {

  private readonly baseUrl = environment.apiUrl + '/schedule';

  constructor(
    private http: HttpClient,
    private mockDataService: MockDataService
  ) {}

  getAll(): Observable<ScheduleEvent[]> {
    return this.http.get<ScheduleEvent[]>(this.baseUrl).pipe(
      map(events => {
        // If API returns empty array or null, use dummy data for demo purposes
        if (!events || events.length === 0) {
          console.log('[ScheduleEventService] API returned no data, using dummy data for demo');
          return this.mockDataService.scheduleEvents;
        }
        return events;
      }),
      catchError(err => {
        console.error('[ScheduleEventService] API call failed, using dummy data for demo:', err);
        return of(this.mockDataService.scheduleEvents);
      })
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
