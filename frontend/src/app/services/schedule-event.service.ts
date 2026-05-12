import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScheduleEvent } from '../models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ScheduleEventService {

  private readonly baseUrl = environment.apiUrl + '/schedule';

  constructor(private http: HttpClient) {}

  getAll(range?: { start?: string; end?: string }): Observable<ScheduleEvent[]> {
    let params = new HttpParams();
    if (range?.start) {
      params = params.set('start', range.start);
    }
    if (range?.end) {
      params = params.set('end', range.end);
    }
    return this.http.get<ScheduleEvent[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<ScheduleEvent> {
    return this.http.get<ScheduleEvent>(`${this.baseUrl}/${id}`);
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
