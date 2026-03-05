import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScheduleEventService } from '../services/schedule-event.service';
import { ScheduleEvent, EventType } from '../models';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-scheduling',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MatFormFieldModule, 
    MatInputModule, 
    MatSelectModule, 
    MatButtonModule, 
    MatIconModule
  ],
  templateUrl: './scheduling.component.html',
  styleUrls: ['./scheduling.component.scss'],
})
export class SchedulingComponent implements OnInit {
  events: ScheduleEvent[] = [];
  viewMode: 'day' | 'week' | 'month' = 'day';
  selectedEvent: ScheduleEvent | null = null;
  showDialog = false;
  showAddDialog = false;
  loading = false;

  newEvent: Partial<ScheduleEvent> = {};

  hours = Array.from({ length: 13 }, (_, i) => `${String(i + 8).padStart(2,'0')}:00`);

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

  constructor(private scheduleEventService: ScheduleEventService) {}

  ngOnInit() {
    this.loading = true;
    this.scheduleEventService.getAll().subscribe({
      next: events => { this.events = events; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  getEventClass(type: EventType): string {
    const m: Record<string,string> = { A1:'event-a1', A2:'event-a2', A3:'event-a3', A4:'event-a4', B1:'event-b1', B2:'event-b2' };
    return 'cal-slot ' + (m[type] ?? '');
  }

  getEventColor(type: EventType) {
    const m: Record<string,string> = { A1:'#1565c0', A2:'#2e7d32', A3:'#f57f17', A4:'#c62828', B1:'#4527a0', B2:'#006064' };
    return m[type] ?? '#666';
  }

  getStartHour(event: ScheduleEvent): number {
    return new Date(event.startTime).getHours();
  }

  getEventsForHour(hour: string): ScheduleEvent[] {
    const h = parseInt(hour);
    return this.events.filter(e => new Date(e.startTime).getHours() === h);
  }

  openEvent(event: ScheduleEvent) { this.selectedEvent = event; this.showDialog = true; }

  formatTime(dt: string) {
    return new Date(dt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  addEvent() {
    if (this.newEvent.title) {
      this.scheduleEventService.create(this.newEvent).subscribe({
        next: created => {
          this.events.push(created);
          this.newEvent = {};
          this.showAddDialog = false;
        },
        error: () => {}
      });
    }
  }
}
