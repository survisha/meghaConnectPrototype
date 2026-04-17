import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface AppointmentApproval {
  appointmentId: number;
  applicantName: string;
  applicantPhone: string;
  agendaType: string;
  requestedLocation: string;
  agendaBrief: string;
  status: string; // SUBMITTED, CMO_REVIEW, APPROVER_REVIEW, HCM_PENDING, SCHEDULED
  submittedDate: string;
  projectName?: string;
  schemeType?: string;
  estimatedCost?: number;
  mlaMdcApproved?: boolean;
  associates?: any[];
  documents?: any[];
  cmoRemarks?: string;
  approverRemarks?: string;
  scheduledDate?: string;
  scheduledTime?: string;
}

export interface ScheduleEvent {
  eventId?: number;
  appointmentId: number;
  title: string;
  startTime: string; // ISO format
  endTime: string;
  location: string;
  eventType: 'APPOINTMENT' | 'TRAVEL_BUFFER' | 'BLOCKED_TIME';
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class AppointmentApprovalService {
  private apiUrl = '/api/v1/appointments';
  private pendingAppointments$ = new BehaviorSubject<AppointmentApproval[]>([]);
  private scheduleEvents$ = new BehaviorSubject<ScheduleEvent[]>([]);

  constructor(private http: HttpClient) {}

  /**
   * Get pending appointments for CMO Officer
   */
  getPendingAppointments(role: string = 'CMO_OFFICER'): Observable<AppointmentApproval[]> {
    return this.http.get<AppointmentApproval[]>(`${this.apiUrl}/pending?role=${role}`).pipe(
      tap(appointments => this.pendingAppointments$.next(appointments))
    );
  }

  /**
   * Get appointment details for approval view
   */
  getAppointmentDetails(appointmentId: number): Observable<AppointmentApproval> {
    return this.http.get<AppointmentApproval>(`${this.apiUrl}/${appointmentId}`);
  }

  /**
   * Submit appointment from form to CMO for review
   */
  submitForApproval(appointmentData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit-for-approval`, appointmentData).pipe(
      tap(response => {
        console.log('Appointment submitted for approval:', response);
      })
    );
  }

  /**
   * CMO approves and forwards to Joint Secretary
   */
  approveAndForward(appointmentId: number, remarks: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${appointmentId}/cmo-approve`, {
      remarks,
      nextAction: 'FORWARD_TO_APPROVER'
    });
  }

  /**
   * CMO rejects appointment
   */
  rejectAppointment(appointmentId: number, rejectReason: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${appointmentId}/reject`, {
      rejectReason,
      status: 'REJECTED'
    });
  }

  /**
   * Get available schedule slots for day and location
   */
  getAvailableSlots(date: string, location: string): Observable<ScheduleEvent[]> {
    return this.http.get<ScheduleEvent[]>(`${this.apiUrl}/available-slots?date=${date}&location=${location}`);
  }

  /**
   * Schedule appointment in calendar
   */
  scheduleAppointment(appointmentId: number, scheduleData: {
    startTime: string;
    endTime: string;
    location: string;
    remarks?: string;
  }): Observable<any> {
    return this.http.put(`${this.apiUrl}/${appointmentId}/schedule`, scheduleData).pipe(
      tap(response => {
        console.log('Appointment scheduled:', response);
      })
    );
  }

  /**
   * Reschedule existing appointment
   */
  rescheduleAppointment(appointmentId: number, newScheduleData: {
    startTime: string;
    endTime: string;
    location: string;
    rescheduledReason?: string;
  }): Observable<any> {
    return this.http.put(`${this.apiUrl}/${appointmentId}/reschedule`, newScheduleData).pipe(
      tap(response => {
        console.log('Appointment rescheduled:', response);
      })
    );
  }

  /**
   * Get all schedule events for a date/location
   */
  getScheduleEvents(startDate: string, endDate: string, location: string): Observable<ScheduleEvent[]> {
    return this.http.get<ScheduleEvent[]>(
      `${this.apiUrl}/schedule-events?startDate=${startDate}&endDate=${endDate}&location=${location}`
    ).pipe(
      tap(events => this.scheduleEvents$.next(events))
    );
  }

  /**
   * Get pending appointments as observable
   */
  getPendingAppointmentsObservable(): Observable<AppointmentApproval[]> {
    return this.pendingAppointments$.asObservable();
  }

  /**
   * Get schedule events as observable
   */
  getScheduleEventsObservable(): Observable<ScheduleEvent[]> {
    return this.scheduleEvents$.asObservable();
  }

  /**
   * Check for scheduling conflicts
   */
  checkConflicts(startTime: string, endTime: string, location: string, appointmentId?: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/check-conflicts`, {
      startTime,
      endTime,
      location,
      excludeAppointmentId: appointmentId
    });
  }
}
