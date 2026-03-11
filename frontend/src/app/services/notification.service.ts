import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment.development';

export interface NotificationRequest {
  phoneNumber: string;
  message: string;
  channel: 'SMS' | 'WHATSAPP';
}

export interface MeetingNotification {
  appointmentId: number;
  phoneNumber: string;
  meetingId: string;
  dateTime: string;
  location: string;
  agendaBrief: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private base = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  send(req: NotificationRequest): Observable<any> {
    return this.http.post(`${this.base}/send`, req).pipe(catchError(() => of(null)));
  }

  sendMeetingConfirmation(meeting: MeetingNotification): Observable<any> {
    return this.http.post(`${this.base}/meeting-confirmation`, meeting).pipe(catchError(() => of(null)));
  }

  sendSms(phoneNumber: string, message: string): Observable<any> {
    return this.send({ phoneNumber, message, channel: 'SMS' });
  }

  sendWhatsApp(phoneNumber: string, message: string): Observable<any> {
    return this.send({ phoneNumber, message, channel: 'WHATSAPP' });
  }
}
