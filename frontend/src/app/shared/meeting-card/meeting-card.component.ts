import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Tag } from 'primeng/tag';

export interface MeetingCardData {
  meetingId: string;
  title: string;
  dateTime: string;
  durationMinutes: number;
  location: string;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  attendeeName?: string;
  agendaBrief?: string;
  eventType?: string;
}

@Component({
  selector: 'app-meeting-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, Tag],
  template: `
    <div class="meeting-card" [class.active]="meeting.status === 'IN_PROGRESS'">
      <div class="meeting-header">
        <div class="meeting-id">{{ meeting.meetingId }}</div>
        <p-tag [value]="meeting.status" [severity]="getSeverity(meeting.status)" styleClass="text-xs"></p-tag>
      </div>
      <div class="meeting-title">{{ meeting.title }}</div>
      <div class="meeting-meta">
        <span><mat-icon class="meta-icon">event</mat-icon> {{ meeting.dateTime | date:'dd MMM yyyy, h:mm a' }}</span>
        <span><mat-icon class="meta-icon">schedule</mat-icon> {{ meeting.durationMinutes }} min</span>
        <span><mat-icon class="meta-icon">location_on</mat-icon> {{ meeting.location }}</span>
      </div>
      <div *ngIf="meeting.agendaBrief" class="meeting-agenda">{{ meeting.agendaBrief }}</div>
      <div class="meeting-actions" *ngIf="showActions">
        <button mat-stroked-button (click)="viewDetails.emit(meeting)"><mat-icon>info</mat-icon> Details</button>
        <button mat-raised-button color="primary" *ngIf="meeting.status === 'SCHEDULED'" (click)="startMeeting.emit(meeting)">
          <mat-icon>play_arrow</mat-icon> Start
        </button>
        <button mat-raised-button color="warn" *ngIf="meeting.status === 'IN_PROGRESS'" (click)="endMeeting.emit(meeting)">
          <mat-icon>stop</mat-icon> End
        </button>
      </div>
    </div>
  `,
  styles: [`
    .meeting-card { background: white; border-radius: 10px; padding: 1rem; box-shadow: 0 2px 6px rgba(0,0,0,0.08); border-left: 4px solid #1a237e; margin-bottom: 0.75rem; transition: box-shadow 0.2s; }
    .meeting-card.active { border-left-color: #16a34a; box-shadow: 0 2px 12px rgba(22,163,74,0.2); }
    .meeting-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.12); }
    .meeting-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .meeting-id { font-size: 0.75rem; color: #6b7280; font-family: monospace; background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
    .meeting-title { font-weight: 700; color: #1a237e; font-size: 0.95rem; margin-bottom: 0.5rem; }
    .meeting-meta { display: flex; flex-wrap: wrap; gap: 0.75rem; font-size: 0.8rem; color: #374151; margin-bottom: 0.5rem; }
    .meeting-meta span { display: flex; align-items: center; gap: 0.2rem; }
    .meta-icon { font-size: 0.9rem; height: 0.9rem; width: 0.9rem; color: #6b7280; }
    .meeting-agenda { font-size: 0.82rem; color: #6b7280; background: #f8fafc; border-radius: 4px; padding: 0.4rem 0.6rem; margin-bottom: 0.5rem; }
    .meeting-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.5rem; }
  `]
})
export class MeetingCardComponent {
  @Input() meeting!: MeetingCardData;
  @Input() showActions = true;
  @Output() viewDetails = new EventEmitter<MeetingCardData>();
  @Output() startMeeting = new EventEmitter<MeetingCardData>();
  @Output() endMeeting = new EventEmitter<MeetingCardData>();

  getSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    const m: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
      SCHEDULED: 'info', IN_PROGRESS: 'success', COMPLETED: 'secondary', CANCELLED: 'danger'
    };
    return m[status] ?? 'secondary';
  }
}
