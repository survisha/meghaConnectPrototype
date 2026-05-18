import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScheduleEventService } from '../services/schedule-event.service';
import { AppointmentService } from '../services/appointment.service';
import { ReferenceDataService } from '../services/reference-data.service';
import { Appointment, AppointmentStatus, ScheduleEvent, EventType, Visitor } from '../models';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, provideNativeDateAdapter } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { apiErrorMessage } from '../shared/api-error.util';

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
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCardModule,
    MatCheckboxModule,
    MatSnackBarModule,
    DragDropModule
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './scheduling.component.html',
  styleUrls: ['./scheduling.component.scss'],
})
export class SchedulingComponent implements OnInit {
  events: ScheduleEvent[] = [];
  viewMode: 'day' | 'week' | 'month' = 'day';
  selectedEvent: ScheduleEvent | null = null;
  selectedDate: Date = new Date();
  showDialog = false;
  showAddDialog = false;
  loading = false;
  errorMsg = '';

  newEvent: Partial<ScheduleEvent> = {};
  newEventStartDate: Date | null = null;
  newEventEndDate: Date | null = null;
  newEventStartTime = '10:00';
  newEventEndTime = '11:00';
  publicDarbarCandidates: Appointment[] = [];
  publicDarbarCandidateIds = new Set<number>();
  publicDarbarCandidatesLoading = false;
  publicDarbarAssignmentLoading = false;
  publicDarbarAssignmentError = '';
  publicDarbarRemarks = 'Scheduled';
  private readonly followUpStatuses: AppointmentStatus[] = ['APPROVED', 'FOLLOWUP'];

  hours = Array.from({ length: 13 }, (_, i) => `${String(i + 8).padStart(2,'0')}:00`);

  eventTypes: Array<{ label: string; value: EventType }> = [];

  locations = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura', value: 'TURA' },
    { label: 'Delhi', value: 'DELHI' },
    { label: 'Others', value: 'OTHERS' },
  ];

  constructor(
    private scheduleEventService: ScheduleEventService,
    private appointmentService: AppointmentService,
    private referenceDataService: ReferenceDataService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadEventTypes();
    this.loadEvents();
  }

  private loadEventTypes() {
    this.referenceDataService.getByType('APPOINMENT_TYPES').subscribe({
      next: values => {
        this.eventTypes = (values ?? []).map(item => ({
          label: item.value,
          value: item.code as EventType,
        }));
      },
      error: error => {
        this.eventTypes = [];
        this.errorMsg = apiErrorMessage(error, 'Unable to load appointment types.');
      }
    });
  }

  private loadEvents() {
    this.loading = true;
    this.errorMsg = '';
    this.scheduleEventService.getAll().subscribe({
      next: events => {
        this.events = (events ?? []).filter(event => !!event.startTime && !!event.endTime);
        if (this.events.length > 0 && this.getEventsForSelectedDate().length === 0) {
          const firstEventDate = new Date(this.events[0].startTime);
          if (!Number.isNaN(firstEventDate.getTime())) {
            this.selectedDate = firstEventDate;
          }
        }
        this.loading = false;
      },
      error: error => {
        this.events = [];
        this.errorMsg = apiErrorMessage(error, 'Unable to load schedule events from API. Please try again.');
        this.loading = false;
      }
    });
  }

  onDateSelected(date: Date | null) {
    if (date) {
      this.selectedDate = date;
      // Automatically switch to day view to show events
      this.viewMode = 'day';
    }
  }

  getEventsForSelectedDate(): ScheduleEvent[] {
    const filtered = this.events.filter(event => {
      const eventDate = new Date(event.startTime);
      return this.isSameDay(eventDate, this.selectedDate);
    });
    
    return filtered;
  }

  isSameDay(date1: Date, date2: Date): boolean {
    return date1.getFullYear() === date2.getFullYear() &&
           date1.getMonth() === date2.getMonth() &&
           date1.getDate() === date2.getDate();
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
    const h = parseInt(hour, 10);
    const selectedDateEvents = this.getEventsForSelectedDate();
    return selectedDateEvents.filter(e => new Date(e.startTime).getHours() === h);
  }

  openEvent(event: ScheduleEvent) {
    this.selectedEvent = event;
    this.publicDarbarCandidates = [];
    this.publicDarbarCandidateIds.clear();
    this.publicDarbarAssignmentError = '';
    this.publicDarbarRemarks = 'Scheduled';
    this.showDialog = true;
    if (this.canAssignPublicDarbarAppointments(event)) {
      this.loadPublicDarbarCandidates();
    }
  }

  openAddEvent() {
    this.newEvent = {};
    this.newEventStartDate = new Date(this.selectedDate);
    this.newEventEndDate = new Date(this.selectedDate);
    this.newEventStartTime = '10:00';
    this.newEventEndTime = '11:00';
    this.showAddDialog = true;
  }

  formatTime(dt: string) {
    return new Date(dt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  formatDateTime(value?: string): string {
    if (!value) return '-';
    return new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
  }

  get selectedAppointments(): Appointment[] {
    return this.selectedEvent?.appointments?.length
      ? this.selectedEvent.appointments
      : this.selectedEvent?.appointment ? [this.selectedEvent.appointment] : [];
  }

  get selectedAppointment(): Appointment | null {
    return this.selectedAppointments[0] ?? null;
  }

  get selectedVisitor(): Visitor | null {
    return this.selectedAppointment?.applicant ?? null;
  }

  getStatusLabel(status?: string): string {
    if (status === 'FOLLOWUP' || status === 'SELECTED_FOR_PUBLIC_DARBAR') return 'FOLLOW-UP';
    if (status === 'SCHEDULED_FOR_PUBLIC_DARBAR') return 'SCHEDULED FOR PUBLIC DURBAR';
    return status ? status.replace(/_/g, ' ') : '-';
  }

  display(value: string | number | boolean | null | undefined): string {
    if (value === null || value === undefined || value === '') return '-';
    if (typeof value === 'boolean') return value ? 'Yes' : 'No';
    return String(value);
  }

  addEvent() {
    const startTime = this.combineDateAndTime(this.newEventStartDate, this.newEventStartTime);
    const endTime = this.combineDateAndTime(this.newEventEndDate, this.newEventEndTime);
    if (startTime && this.isPastDateTime(startTime)) {
      this.snackBar.open('Cannot schedule an event in the past.', 'Close', { duration: 4000 });
      return;
    }
    if (this.newEvent.title && this.newEvent.eventType && startTime && endTime && this.newEvent.location) {
      this.scheduleEventService.create({ ...this.newEvent, startTime, endTime }).subscribe({
        next: created => {
          this.events = [...this.events, created].sort((a, b) =>
            new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
          );
          this.newEvent = {};
          this.newEventStartDate = null;
          this.newEventEndDate = null;
          this.showAddDialog = false;
          this.snackBar.open('Event created successfully.', 'Close', { duration: 4000 });
        },
        error: error => this.errorMsg = apiErrorMessage(error, 'Unable to create schedule event.')
      });
    }
  }

  get canAddEvent() {
    return Boolean(
      this.newEvent.title &&
      this.newEvent.eventType &&
      this.newEvent.location &&
      this.combineDateAndTime(this.newEventStartDate, this.newEventStartTime) &&
      this.combineDateAndTime(this.newEventEndDate, this.newEventEndTime)
    );
  }

  getEventTypeLabel(value: string): string {
    return this.eventTypes.find(type => type.value === value)?.label ?? value;
  }

  canAssignPublicDarbarAppointments(event: ScheduleEvent | null = this.selectedEvent): boolean {
    return Boolean(event && event.sourceType !== 'APPOINTMENT');
  }

  loadPublicDarbarCandidates() {
    if (!this.selectedEvent) return;
    const assignedIds = new Set((this.selectedEvent.appointments ?? []).map(item => item.id));
    this.publicDarbarCandidatesLoading = true;
    this.publicDarbarAssignmentError = '';
    this.appointmentService.getAllAppointments(0, 1000, 'APPROVED,FOLLOWUP').subscribe({
      next: page => {
        this.publicDarbarCandidates = (page.content ?? [])
          .filter(appointment => this.isFollowUpStatus(appointment.status))
          .filter(appointment => !assignedIds.has(appointment.id));
        this.publicDarbarCandidateIds.clear();
        this.publicDarbarCandidatesLoading = false;
      },
      error: error => {
        this.publicDarbarCandidates = [];
        this.publicDarbarAssignmentError = apiErrorMessage(error, 'Unable to load approved/follow-up applications.');
        this.publicDarbarCandidatesLoading = false;
      }
    });
  }

  isPublicDarbarCandidateSelected(id: number) {
    return this.publicDarbarCandidateIds.has(id);
  }

  togglePublicDarbarCandidate(id: number, checked: boolean) {
    if (checked) {
      this.publicDarbarCandidateIds.add(id);
    } else {
      this.publicDarbarCandidateIds.delete(id);
    }
  }

  assignSelectedPublicDarbarAppointments() {
    if (!this.selectedEvent?.id || this.publicDarbarCandidateIds.size === 0) return;
    this.publicDarbarAssignmentLoading = true;
    this.publicDarbarAssignmentError = '';
    this.scheduleEventService.assignAppointments(this.selectedEvent.id, {
      appointmentIds: Array.from(this.publicDarbarCandidateIds),
      remarks: this.publicDarbarRemarks || 'Scheduled',
    }).subscribe({
      next: saved => {
        this.selectedEvent = saved;
        this.events = this.events.map(event => event.id === saved.id ? saved : event);
        this.publicDarbarCandidateIds.clear();
        this.loadPublicDarbarCandidates();
        this.publicDarbarAssignmentLoading = false;
      },
      error: error => {
        this.publicDarbarAssignmentError = apiErrorMessage(error, 'Unable to assign follow-up applications to this event.');
        this.publicDarbarAssignmentLoading = false;
      }
    });
  }

  private isFollowUpStatus(status: AppointmentStatus): boolean {
    return this.followUpStatuses.includes(status);
  }

  private combineDateAndTime(date: Date | null, time: string | null | undefined): string | null {
    if (!date || !time) return null;
    const [hours, minutes] = time.split(':').map(part => Number(part));
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null;
    const value = new Date(date);
    value.setHours(hours, minutes, 0, 0);
    return this.toLocalDateTime(value);
  }

  private toLocalDateTime(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  private toDateParam(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  // Drag and drop handler
  onEventDrop(event: CdkDragDrop<ScheduleEvent[]>, targetHour: string) {
    const droppedEvent = event.item.data as ScheduleEvent;
    if (!droppedEvent) {
      return;
    }

    const targetHourNum = parseInt(targetHour, 10);
    
    // Calculate new start and end times
    const duration = new Date(droppedEvent.endTime).getTime() - new Date(droppedEvent.startTime).getTime();
    
    // Create new date with selected date and target hour
    const newStart = new Date(this.selectedDate);
    newStart.setHours(targetHourNum, 0, 0, 0);
    if (this.isPastDateTime(newStart)) {
      this.snackBar.open('Cannot schedule or drag appointments to past dates.', 'Close', { duration: 5000 });
      return;
    }
    if (!window.confirm('Reschedule this appointment/event to the selected date and time?')) {
      return;
    }
    
    const newEnd = new Date(newStart.getTime() + duration);

    if (droppedEvent.sourceType === 'APPOINTMENT' && droppedEvent.appointmentId) {
      this.appointmentService.rescheduleAppointmentDate(droppedEvent.appointmentId, {
        scheduledDate: this.toDateParam(newStart),
        scheduledTime: `${String(newStart.getHours()).padStart(2, '0')}:00`,
      }).subscribe({
        next: () => {
          this.snackBar.open('Appointment rescheduled successfully.', 'Close', { duration: 5000 });
          this.loadEvents();
        },
        error: error => {
          this.errorMsg = apiErrorMessage(error, 'Unable to reschedule appointment.');
          this.loadEvents();
        }
      });
      return;
    }
    
    const updatedEvent: Partial<ScheduleEvent> = {
      title: droppedEvent.title,
      eventType: droppedEvent.eventType,
      startTime: this.toLocalDateTime(newStart),
      endTime: this.toLocalDateTime(newEnd),
      location: droppedEvent.location,
      travelTimeMinutes: droppedEvent.travelTimeMinutes,
      description: droppedEvent.description,
      shortNotes: droppedEvent.shortNotes,
      isConflict: droppedEvent.isConflict
    };

    this.scheduleEventService.update(droppedEvent.id, updatedEvent).subscribe({
      next: saved => {
        this.events = this.events.map(item => item.id === saved.id ? saved : item);
        this.snackBar.open('Appointment rescheduled successfully.', 'Close', { duration: 5000 });
      },
      error: error => {
        this.errorMsg = apiErrorMessage(error, 'Unable to update schedule event.');
        this.loadEvents();
      }
    });
  }

  // Check if a date has events (for calendar highlighting)
  dateHasEvents = (date: Date): boolean => {
    return this.events.some(event => {
      const eventDate = new Date(event.startTime);
      return this.isSameDay(eventDate, date);
    });
  }

  dateFilter = (date: Date | null): boolean => {
    return !date || !this.isPastCalendarDate(date);
  };

  // Custom date class for calendar styling
  dateClass = (date: Date): string => {
    const classes: string[] = [];
    if (this.isPastCalendarDate(date)) {
      classes.push('past-date');
    }
    const event = this.events.find(item => this.isSameDay(new Date(item.startTime), date));
    if (event) {
      classes.push('has-events', `has-event-${event.eventType.toLowerCase()}`);
    }
    return classes.join(' ');
  }

  private isPastCalendarDate(date: Date) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const value = new Date(date);
    value.setHours(0, 0, 0, 0);
    return value < today;
  }

  private isPastDateTime(value: string | Date) {
    const date = value instanceof Date ? value : new Date(value);
    return date.getTime() < Date.now();
  }
}

