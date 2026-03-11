import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ScheduleEvent } from '../models';
import { environment } from '../../environments/environment.development';

export interface CalendarEvent extends ScheduleEvent {
  color?: string;
  status?: 'approved' | 'pending' | 'conflict' | 'public';
  priority?: 'HIGH' | 'MEDIUM' | 'LOW';
  agenda?: string;
  travelTimeMinutes?: number;
}

export interface RescheduleRequest {
  eventId: number;
  newStartTime: string;
  newEndTime: string;
  remarks?: string;
}

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private base = `${environment.apiUrl}/schedule-events`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<CalendarEvent[]> {
    return this.http.get<CalendarEvent[]>(this.base).pipe(catchError(() => of([])));
  }

  getForDateRange(start: string, end: string): Observable<CalendarEvent[]> {
    return this.http.get<CalendarEvent[]>(`${this.base}?start=${start}&end=${end}`).pipe(
      catchError(() => of([]))
    );
  }

  create(event: Partial<CalendarEvent>): Observable<CalendarEvent> {
    return this.http.post<CalendarEvent>(this.base, event);
  }

  update(id: number, event: Partial<CalendarEvent>): Observable<CalendarEvent> {
    return this.http.put<CalendarEvent>(`${this.base}/${id}`, event);
  }

  reschedule(req: RescheduleRequest): Observable<CalendarEvent> {
    return this.http.post<CalendarEvent>(`${this.base}/${req.eventId}/reschedule`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  detectConflicts(events: CalendarEvent[]): CalendarEvent[] {
    return events.map(ev => {
      const start = new Date(ev.startTime).getTime();
      const end = new Date(ev.endTime).getTime();
      const conflict = events.some(other =>
        other.id !== ev.id &&
        new Date(other.startTime).getTime() < end &&
        new Date(other.endTime).getTime() > start
      );
      return { ...ev, isConflict: conflict };
    });
  }

  getEventColor(event: CalendarEvent): string {
    if (event.isConflict) return '#dc2626'; // red - conflict
    const statusColors: Record<string, string> = {
      approved: '#16a34a',  // green
      pending:  '#ca8a04',  // yellow
      conflict: '#dc2626',  // red
      public:   '#2563eb',  // blue
    };
    if (event.status && statusColors[event.status]) return statusColors[event.status];
    const typeColors: Record<string, string> = {
      A1: '#1565c0', A2: '#2e7d32', A3: '#f57f17',
      A4: '#c62828', B1: '#4527a0', B2: '#006064',
    };
    return typeColors[event.eventType] ?? '#6b7280';
  }
}
