import {
  Component, Input, Output, EventEmitter, OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CalendarEvent } from '../services/calendar.service';
import { EventType, Location } from '../models';

@Component({
  selector: 'app-calendar-event-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="ced-overlay" (click)="onOverlayClick($event)">
      <div class="ced-container" (click)="$event.stopPropagation()">
        <div class="ced-header">
          <h2>{{ event ? 'Edit Event' : 'New Event' }}</h2>
          <button mat-icon-button (click)="cancel()"><mat-icon>close</mat-icon></button>
        </div>
        <div class="ced-body">
          <mat-form-field appearance="outline" class="full-field">
            <mat-label>Title *</mat-label>
            <input matInput [(ngModel)]="form.title" placeholder="Event title" style="background:white!important"/>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-field">
            <mat-label>Event Type *</mat-label>
            <mat-select [(ngModel)]="form.eventType" style="background:white!important">
              <mat-option *ngFor="let t of eventTypes" [value]="t.value">{{ t.label }}</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="two-col">
            <mat-form-field appearance="outline">
              <mat-label>Start Time</mat-label>
              <input matInput type="datetime-local" [(ngModel)]="form.startTime" style="background:white!important"/>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>End Time</mat-label>
              <input matInput type="datetime-local" [(ngModel)]="form.endTime" style="background:white!important"/>
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-field">
            <mat-label>Location</mat-label>
            <mat-select [(ngModel)]="form.location" style="background:white!important">
              <mat-option *ngFor="let l of locations" [value]="l.value">{{ l.label }}</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-field">
            <mat-label>Travel Time (minutes)</mat-label>
            <input matInput type="number" [(ngModel)]="form.travelTimeMinutes" min="0" style="background:white!important"/>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-field">
            <mat-label>Status</mat-label>
            <mat-select [(ngModel)]="form.status" style="background:white!important">
              <mat-option value="approved">Approved</mat-option>
              <mat-option value="pending">Pending</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-field">
            <mat-label>Description</mat-label>
            <textarea matInput [(ngModel)]="form.description" rows="3" style="background:white!important"></textarea>
          </mat-form-field>
        </div>

        <div class="ced-footer">
          <button mat-stroked-button (click)="cancel()">Cancel</button>
          <button mat-raised-button color="primary" [disabled]="!isValid()" (click)="save()">
            <mat-icon>check</mat-icon> {{ event ? 'Update' : 'Create' }} Event
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .ced-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,.45);
      display: flex; align-items: center; justify-content: center; z-index: 1000;
    }
    .ced-container {
      background: white; border-radius: 12px; width: 540px; max-width: 96vw;
      max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,.25);
    }
    .ced-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 1.25rem 1.5rem; border-bottom: 1px solid #e5e7eb;
      h2 { margin: 0; font-size: 1.1rem; font-weight: 700; color: #1f2937; }
    }
    .ced-body {
      padding: 1.5rem; display: flex; flex-direction: column; gap: .75rem;
    }
    .full-field { width: 100%; }
    .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .ced-footer {
      padding: 1rem 1.5rem; border-top: 1px solid #e5e7eb;
      display: flex; justify-content: flex-end; gap: .75rem;
    }
    mat-form-field { background: white !important; }
  `]
})
export class CalendarEventDialogComponent implements OnInit {
  @Input() event?: CalendarEvent | null;
  @Input() date?: string;
  @Output() saved = new EventEmitter<Partial<CalendarEvent>>();
  @Output() cancelled = new EventEmitter<void>();

  form: Partial<CalendarEvent> = {};

  eventTypes = [
    { label: 'A1 – Cabinet / Union Minister / Media / Flight', value: 'A1' },
    { label: 'A2 – Event / Programme', value: 'A2' },
    { label: 'A3 – File Clearing / Birthday', value: 'A3' },
    { label: 'A4 – Individual Appointment', value: 'A4' },
    { label: 'B1 – Public Durbar', value: 'B1' },
    { label: 'B2 – Public Walk-in', value: 'B2' },
  ];

  locations = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura', value: 'TURA' },
    { label: 'Delhi', value: 'DELHI' },
    { label: 'Others', value: 'OTHERS' },
  ];

  ngOnInit() {
    if (this.event) {
      this.form = { ...this.event };
    } else {
      this.form = {
        title: '',
        eventType: 'A4' as EventType,
        startTime: this.date ?? new Date().toISOString().slice(0, 16),
        endTime: '',
        location: 'SHILLONG' as Location,
        travelTimeMinutes: 0,
        status: 'pending',
        description: '',
      };
    }
  }

  isValid(): boolean {
    return !!(this.form.title?.trim() && this.form.eventType && this.form.startTime && this.form.endTime);
  }

  save() {
    if (this.isValid()) this.saved.emit(this.form);
  }

  cancel() { this.cancelled.emit(); }

  onOverlayClick(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('ced-overlay')) this.cancelled.emit();
  }
}
