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
import { ToastService } from '../shared/toast/toast.service';
import { MatTooltipModule } from '@angular/material/tooltip';
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
    MatTooltipModule,
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
  showConflictDialog = false;
  loading = false;
  errorMsg = '';
  conflictDialog = {
    message: '',
    requested: '',
    conflicting: '',
  };

  newEvent: Partial<ScheduleEvent> = {};
  editingEvent: ScheduleEvent | null = null;
  newEventStartDate: Date | null = null;
  newEventEndDate: Date | null = null;
  newEventStartTime = '10:00';
  newEventEndTime = '11:00';
  readonly today = this.startOfDay(new Date());
  publicDarbarCandidates: Appointment[] = [];
  publicDarbarCandidateIds = new Set<number>();
  publicDarbarCandidatesLoading = false;
  publicDarbarAssignmentLoading = false;
  publicDarbarAssignmentError = '';
  publicDarbarRemarks = 'Scheduled';
  selectedCandidateDetail: Appointment | null = null;
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
    private snackBar: ToastService
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
      if (this.isPastCalendarDate(date)) {
        this.snackBar.open('Previous dates cannot be selected for scheduling.', 'Close', { duration: 4000 });
        return;
      }
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
    const defaultDate = this.defaultEventDate();
    const start = this.defaultStartTime();
    const end = this.addMinutesToClock(start, 60);
    this.newEvent = {};
    this.editingEvent = null;
    this.newEventStartDate = defaultDate;
    this.newEventEndDate = new Date(defaultDate);
    this.newEventStartTime = start;
    this.newEventEndTime = end;
    this.showAddDialog = true;
  }

  openEditEvent(event: ScheduleEvent) {
    const start = new Date(event.startTime);
    const end = new Date(event.endTime);
    this.editingEvent = event;
    this.newEvent = { ...event };
    this.newEventStartDate = Number.isNaN(start.getTime()) ? this.defaultEventDate() : start;
    this.newEventEndDate = Number.isNaN(end.getTime()) ? this.newEventStartDate : end;
    this.newEventStartTime = Number.isNaN(start.getTime()) ? this.defaultStartTime() : this.clockValue(start);
    this.newEventEndTime = Number.isNaN(end.getTime()) ? this.addMinutesToClock(this.newEventStartTime, 60) : this.clockValue(end);
    this.showDialog = false;
    this.showAddDialog = true;
  }

  closeAddDialog() {
    this.showAddDialog = false;
    this.editingEvent = null;
    this.newEvent = {};
    this.newEventStartDate = null;
    this.newEventEndDate = null;
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
    this.saveEvent();
  }

  saveEvent() {
    const startTime = this.combineDateAndTime(this.newEventStartDate, this.newEventStartTime);
    const endTime = this.combineDateAndTime(this.newEventEndDate, this.newEventEndTime);
    const validation = this.validateEventTimes(startTime, endTime);
    if (validation) {
      this.snackBar.open(validation, 'Close', { duration: 4000 });
      return;
    }
    if (this.newEvent.title && this.newEvent.eventType && startTime && endTime && this.newEvent.location) {
      const payload = { ...this.newEvent, startTime, endTime };
      const request = this.editingEvent?.id
        ? this.scheduleEventService.update(this.editingEvent.id, payload)
        : this.scheduleEventService.create(payload);
      const wasEdit = Boolean(this.editingEvent?.id);
      request.subscribe({
        next: saved => {
          this.events = this.editingEvent?.id
            ? this.events.map(event => event.id === saved.id ? saved : event)
            : [...this.events, saved];
          this.events = this.events.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
          this.newEvent = {};
          this.newEventStartDate = null;
          this.newEventEndDate = null;
          this.editingEvent = null;
          this.showAddDialog = false;
          this.snackBar.open(wasEdit ? 'Event updated successfully.' : 'Event created successfully.', 'Close', { duration: 4000 });
        },
        error: error => {
          if (this.isMeetingConflict(error)) {
            this.showMeetingConflict(error);
            this.loadEvents();
            return;
          }
          this.errorMsg = apiErrorMessage(error, this.editingEvent ? 'Failed to update event.' : 'Failed to create event.');
        }
      });
    }
  }

  get canAddEvent() {
    return Boolean(
      this.newEvent.title &&
      this.newEvent.eventType &&
      this.newEvent.location &&
      this.combineDateAndTime(this.newEventStartDate, this.newEventStartTime) &&
      this.combineDateAndTime(this.newEventEndDate, this.newEventEndTime) &&
      !this.validateEventTimes(
        this.combineDateAndTime(this.newEventStartDate, this.newEventStartTime),
        this.combineDateAndTime(this.newEventEndDate, this.newEventEndTime)
      )
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
        this.selectedCandidateDetail = null;
        this.loadPublicDarbarCandidates();
        this.publicDarbarAssignmentLoading = false;
        this.snackBar.open('Application added to event.', 'Close', { duration: 4000 });
      },
      error: error => {
        if (this.isMeetingConflict(error)) {
          this.showMeetingConflict(error);
          this.loadEvents();
          this.publicDarbarAssignmentError = this.conflictDialog.message;
        } else {
          this.publicDarbarAssignmentError = apiErrorMessage(error, 'Unable to assign follow-up applications to this event.');
        }
        this.publicDarbarAssignmentLoading = false;
      }
    });
  }

  viewCandidateDetails(appointment: Appointment) {
    this.selectedCandidateDetail = appointment;
    this.publicDarbarCandidateIds.clear();
    this.publicDarbarCandidateIds.add(appointment.id);
  }

  closeCandidateDetails() {
    this.selectedCandidateDetail = null;
  }

  addCandidateFromDetails(appointment: Appointment) {
    this.publicDarbarCandidateIds.clear();
    this.publicDarbarCandidateIds.add(appointment.id);
    this.assignSelectedPublicDarbarAppointments();
  }

  removeAssignedAppointment(appointment: Appointment) {
    if (!this.selectedEvent?.id || !appointment.id) return;
    if (!window.confirm('Remove this application from the event and return it to follow-up?')) return;
    this.publicDarbarAssignmentLoading = true;
    this.publicDarbarAssignmentError = '';
    this.scheduleEventService.removeAppointment(this.selectedEvent.id, appointment.id).subscribe({
      next: saved => {
        this.selectedEvent = saved;
        this.events = this.events.map(event => event.id === saved.id ? saved : event);
        this.loadPublicDarbarCandidates();
        this.publicDarbarAssignmentLoading = false;
        this.snackBar.open('Application removed from event and moved back to follow-up.', 'Close', { duration: 4000 });
      },
      error: error => {
        this.publicDarbarAssignmentError = apiErrorMessage(error, 'Failed to remove application from event.');
        this.publicDarbarAssignmentLoading = false;
      }
    });
  }

  priorityInsight(appointment: Appointment): { level: 'High' | 'Medium' | 'Low'; message: string } {
    const applicantId = appointment.applicantId || appointment.applicant?.id;
    const related = [...this.publicDarbarCandidates, ...this.selectedAppointments]
      .filter(item => (item.applicantId || item.applicant?.id) === applicantId);
    const pendingCount = related.filter(item => this.isFollowUpStatus(item.status)).length;
    const ageDays = appointment.createdAt
      ? Math.max(0, Math.floor((Date.now() - new Date(appointment.createdAt).getTime()) / 86400000))
      : 0;
    const meetings = appointment.meetingCountLast6Months ?? 0;
    if (pendingCount >= 3 || meetings > 0 || ageDays >= 14) {
      return {
        level: 'High',
        message: `High priority: ${pendingCount || 1} pending appointment(s), ${meetings} recent CM visit(s), follow-up age ${ageDays} day(s).`,
      };
    }
    if (pendingCount >= 2 || ageDays >= 7) {
      return {
        level: 'Medium',
        message: `Medium priority: Follow-up pending for ${ageDays} day(s) with ${pendingCount || 1} active request(s).`,
      };
    }
    return {
      level: 'Low',
      message: 'Low priority: First or recent follow-up request.',
    };
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

  private formatLocalDate(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  private formatLocalTime(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private parseClockValue(value: string): { hours: number; minutes: number } | null {
    const [hoursRaw, minutesRaw = '0'] = value.split(':');
    const hours = Number(hoursRaw);
    const minutes = Number(minutesRaw);
    if (!Number.isInteger(hours) || !Number.isInteger(minutes) || hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
      return null;
    }
    return { hours, minutes };
  }

  // Drag and drop handler
  onEventDrop(event: CdkDragDrop<ScheduleEvent[]>, targetHour: string) {
    const droppedEvent = event.item.data as ScheduleEvent;
    if (!droppedEvent) {
      return;
    }

    const targetClock = this.parseClockValue(targetHour);
    if (!targetClock) {
      this.snackBar.open('Invalid drop time.', 'Close', { duration: 4000 });
      this.loadEvents();
      return;
    }
    
    // Calculate new start and end times
    const duration = new Date(droppedEvent.endTime).getTime() - new Date(droppedEvent.startTime).getTime();
    
    // Create new date with selected date and target hour
    const newStart = new Date(this.selectedDate);
    newStart.setHours(targetClock.hours, targetClock.minutes, 0, 0);
    if (this.isPastDateTime(newStart)) {
      this.snackBar.open('Cannot schedule or drag appointments to past dates.', 'Close', { duration: 5000 });
      this.loadEvents();
      return;
    }
    if (!window.confirm('Reschedule this appointment/event to the selected date and time?')) {
      this.loadEvents();
      return;
    }
    
    const newEnd = new Date(newStart.getTime() + duration);
    const payloadStart = this.toLocalDateTime(newStart);
    const payloadEnd = this.toLocalDateTime(newEnd);

    if (droppedEvent.sourceType === 'APPOINTMENT' && droppedEvent.appointmentId) {
      const scheduledDate = this.formatLocalDate(newStart);
      const scheduledTime = this.formatLocalTime(newStart);
      this.appointmentService.rescheduleAppointmentDate(droppedEvent.appointmentId, {
        scheduledDate,
        scheduledTime,
      }).subscribe({
        next: saved => {
          this.events = this.events
            .map(item => item === droppedEvent || item.appointmentId === droppedEvent.appointmentId
              ? {
                  ...item,
                  startTime: payloadStart,
                  endTime: payloadEnd,
                  appointment: item.appointment ? { ...item.appointment, ...saved } : item.appointment,
                }
              : item)
            .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
          this.snackBar.open('Appointment rescheduled successfully.', 'Close', { duration: 5000 });
          this.loadEvents();
        },
        error: error => {
          if (this.isMeetingConflict(error)) {
            this.showMeetingConflict(error);
          } else {
            this.errorMsg = apiErrorMessage(error, 'Unable to reschedule appointment.');
          }
          this.loadEvents();
        }
      });
      return;
    }
    if (!droppedEvent.id) {
      this.snackBar.open('Unable to update schedule event: missing event id.', 'Close', { duration: 4000 });
      return;
    }
    
    const updatedEvent: Partial<ScheduleEvent> = {
      title: droppedEvent.title,
      eventType: droppedEvent.eventType,
      startTime: payloadStart,
      endTime: payloadEnd,
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
        if (this.isMeetingConflict(error)) {
          this.showMeetingConflict(error);
        } else {
          this.errorMsg = apiErrorMessage(error, 'Unable to update schedule event.');
        }
        this.loadEvents();
      }
    });
  }

  closeConflictDialog() {
    this.showConflictDialog = false;
  }

  private isMeetingConflict(error: any): boolean {
    const body = error?.error ?? {};
    return error?.status === 409 && (body.code === 'MEETING_CONFLICT' || body.errorCode === 'MEETING_CONFLICT');
  }

  private showMeetingConflict(error: any) {
    const body = error?.error ?? {};
    const details = body.details ?? {};
    this.conflictDialog = {
      message: body.message || 'Meeting Conflict Detected.',
      requested: this.formatConflictRange(details.requestedStart, details.requestedEnd),
      conflicting: this.formatConflictRange(details.conflictingStart, details.conflictingEnd),
    };
    this.errorMsg = this.conflictDialog.message;
    this.showConflictDialog = true;
    this.snackBar.open('Meeting conflict detected. Please choose another time.', 'Close', { duration: 5000 });
  }

  private formatConflictRange(start?: string, end?: string): string {
    if (!start) {
      return '';
    }
    const startDate = new Date(start);
    const endDate = end ? new Date(end) : null;
    if (Number.isNaN(startDate.getTime())) {
      return '';
    }
    const startText = startDate.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
    if (!endDate || Number.isNaN(endDate.getTime())) {
      return startText;
    }
    return `${startText} - ${endDate.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true })}`;
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
    const today = this.startOfDay(new Date());
    const value = new Date(date);
    value.setHours(0, 0, 0, 0);
    return value < today;
  }

  private isPastDateTime(value: string | Date) {
    const date = value instanceof Date ? value : new Date(value);
    return date.getTime() < Date.now();
  }

  onEventDateChange(kind: 'start' | 'end') {
    if (kind === 'start' && this.newEventStartDate && this.isPastCalendarDate(this.newEventStartDate)) {
      this.newEventStartDate = this.today;
      this.snackBar.open('Start date cannot be in the past.', 'Close', { duration: 4000 });
    }
    if (kind === 'end' && this.newEventEndDate && this.isPastCalendarDate(this.newEventEndDate)) {
      this.newEventEndDate = this.newEventStartDate ?? this.today;
      this.snackBar.open('End date cannot be in the past.', 'Close', { duration: 4000 });
    }
    if (this.newEventStartDate && this.newEventEndDate && this.startOfDay(this.newEventEndDate) < this.startOfDay(this.newEventStartDate)) {
      this.newEventEndDate = new Date(this.newEventStartDate);
      this.snackBar.open('End date cannot be before start date.', 'Close', { duration: 4000 });
    }
  }

  private validateEventTimes(startTime: string | null, endTime: string | null): string | null {
    if (!startTime || !endTime) return 'Start and end date/time are required.';
    const start = new Date(startTime);
    const end = new Date(endTime);
    if (this.isPastCalendarDate(start)) return 'Start date cannot be in the past.';
    if (this.isPastDateTime(start)) return 'Start time cannot be in the past.';
    if (this.isPastCalendarDate(end)) return 'End date cannot be in the past.';
    if (end < start) return 'End date cannot be before start date.';
    if (end.getTime() === start.getTime()) return 'End time must be after start time.';
    return null;
  }

  private startOfDay(date: Date): Date {
    const value = new Date(date);
    value.setHours(0, 0, 0, 0);
    return value;
  }

  private defaultEventDate(): Date {
    const selected = this.startOfDay(this.selectedDate);
    return selected < this.today ? new Date(this.today) : new Date(selected);
  }

  private defaultStartTime(): string {
    const date = this.defaultEventDate();
    if (!this.isSameDay(date, new Date())) return '10:00';
    const next = new Date();
    next.setMinutes(0, 0, 0);
    next.setHours(next.getHours() + 1);
    return this.clockValue(next);
  }

  private clockValue(date: Date): string {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  }

  private addMinutesToClock(clock: string, minutesToAdd: number): string {
    const [hours, minutes] = clock.split(':').map(Number);
    const date = new Date();
    date.setHours(hours, minutes + minutesToAdd, 0, 0);
    return this.clockValue(date);
  }
}

