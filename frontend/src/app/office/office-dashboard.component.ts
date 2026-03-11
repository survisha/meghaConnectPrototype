import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Dialog } from 'primeng/dialog';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { interval, Subscription } from 'rxjs';
import { AppointmentService } from '../services/appointment.service';
import { NotificationService } from '../services/notification.service';
import { Appointment, EventType } from '../models';
import { MeetingCardComponent, MeetingCardData } from '../shared/meeting-card/meeting-card.component';

export interface ScheduledMeeting {
  id: number;
  meetingId: string;
  appointmentId: number;
  title: string;
  dateTime: string;
  durationMinutes: number;
  location: string;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  attendeeName: string;
  phoneNumber: string;
  agendaBrief: string;
  eventType: EventType;
}

interface ActiveTimer {
  remaining: number;
  subscription: Subscription;
  isExpired: boolean;
}

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined;

@Component({
  selector: 'app-office-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TableModule,
    Tag,
    Dialog,
    Toast,
    MeetingCardComponent,
  ],
  providers: [MessageService],
  templateUrl: './office-dashboard.component.html',
  styleUrls: ['./office-dashboard.component.scss'],
})
export class OfficeDashboardComponent implements OnInit, OnDestroy {

  meetings: ScheduledMeeting[] = [];
  acceptedAppointments: Appointment[] = [];
  scheduleView: 'brief' | 'detailed' = 'brief';
  showScheduleDialog = false;
  showDetailsDialog = false;
  selectedMeeting: ScheduledMeeting | null = null;
  loading = false;
  submitting = false;

  scheduleForm!: FormGroup;

  /** keyed by meeting.id */
  private activeTimers = new Map<number, ActiveTimer>();
  private idCounter = 1;

  /** Sentinel value used when "Other / Walk-in" is selected (no linked appointment). */
  static readonly WALKIN_SENTINEL = -1;

  readonly locationOptions = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura',     value: 'TURA' },
    { label: 'Delhi',    value: 'DELHI' },
    { label: 'Others',   value: 'OTHERS' },
  ];

  readonly durationOptions = [
    { label: '15 minutes',  value: 15 },
    { label: '30 minutes',  value: 30 },
    { label: '45 minutes',  value: 45 },
    { label: '60 minutes',  value: 60 },
    { label: '90 minutes',  value: 90 },
    { label: '120 minutes', value: 120 },
  ];

  private static readonly DEFAULT_EVENT_TYPE: EventType = 'A4';

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private notificationService: NotificationService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadAcceptedAppointments();
    this.seedDemoMeetings();
  }

  ngOnDestroy(): void {
    this.activeTimers.forEach(t => t.subscription.unsubscribe());
    this.activeTimers.clear();
  }

  // ─── Computed getters ────────────────────────────────────────────────────

  get todaysMeetings(): ScheduledMeeting[] {
    const today = new Date().toDateString();
    return this.meetings.filter(m => new Date(m.dateTime).toDateString() === today);
  }

  get inProgressMeetings(): ScheduledMeeting[] {
    return this.meetings.filter(m => m.status === 'IN_PROGRESS');
  }

  get completedCount(): number {
    return this.todaysMeetings.filter(m => m.status === 'COMPLETED').length;
  }

  get pendingCount(): number {
    return this.todaysMeetings.filter(m => m.status === 'SCHEDULED').length;
  }

  get hasActiveMeetings(): boolean {
    return this.inProgressMeetings.length > 0;
  }

  get today(): Date {
    return new Date();
  }

  // ─── Data loading ────────────────────────────────────────────────────────

  private buildForm(): void {
    this.scheduleForm = this.fb.group({
      appointmentId: [null, Validators.required],
      date:          ['', Validators.required],
      time:          ['', Validators.required],
      durationMinutes: [30, [Validators.required, Validators.min(5)]],
      location:      ['SHILLONG', Validators.required],
      notes:         [''],
    });
  }

  loadAcceptedAppointments(): void {
    this.loading = true;
    this.appointmentService.getAllAppointments(0, 100).subscribe({
      next: page => {
        this.acceptedAppointments = page.content.filter(
          a => a.status === 'HCM_ACCEPTED' || a.status === 'SCHEDULED',
        );
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.messageService.add({
          severity: 'warn',
          summary: 'Network',
          detail: 'Could not load appointments – working offline.',
          life: 4000,
        });
      },
    });
  }

  /** Seed a few demo meetings so the UI is not empty on first load. */
  private seedDemoMeetings(): void {
    const base = new Date();
    const pad  = (n: number) => String(n).padStart(2, '0');
    const todayStr = `${base.getFullYear()}-${pad(base.getMonth() + 1)}-${pad(base.getDate())}`;

    this.meetings = [
      {
        id: 1,
        meetingId: this.buildMeetingId(1),
        appointmentId: 101,
        title: 'Infrastructure Grant Review',
        dateTime: `${todayStr}T09:30:00`,
        durationMinutes: 30,
        location: 'SHILLONG',
        status: 'COMPLETED',
        attendeeName: 'Bah Pynskhemborlang Marbaniang',
        phoneNumber: '9876543210',
        agendaBrief: 'Review of road construction grant application for Mawphlang constituency.',
        eventType: 'A4',
      },
      {
        id: 2,
        meetingId: this.buildMeetingId(2),
        appointmentId: 102,
        title: 'CM Social Development Fund Discussion',
        dateTime: `${todayStr}T11:00:00`,
        durationMinutes: 45,
        location: 'SHILLONG',
        status: 'SCHEDULED',
        attendeeName: 'Kong Rishailang Lyngdoh',
        phoneNumber: '9876543211',
        agendaBrief: 'CMSDF application for self-help group – handloom weaving project.',
        eventType: 'A4',
      },
      {
        id: 3,
        meetingId: this.buildMeetingId(3),
        appointmentId: 103,
        title: 'Public Durbar – Tura Circuit',
        dateTime: `${todayStr}T14:00:00`,
        durationMinutes: 60,
        location: 'TURA',
        status: 'SCHEDULED',
        attendeeName: 'Shri Tengkan A. Sangma',
        phoneNumber: '9876543212',
        agendaBrief: 'Public durbar for western Garo Hills grievance redressal.',
        eventType: 'B1',
      },
    ];

    this.idCounter = this.meetings.length + 1;
  }

  // ─── Meeting ID generation ────────────────────────────────────────────────

  generateMeetingId(): string {
    return this.buildMeetingId(this.idCounter);
  }

  private buildMeetingId(seq: number): string {
    const d   = new Date();
    const yr  = d.getFullYear();
    const mo  = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `MCM-${yr}${mo}${day}-${String(seq).padStart(4, '0')}`;
  }

  // ─── Scheduling dialog ────────────────────────────────────────────────────

  openScheduleDialog(): void {
    this.scheduleForm.reset({
      appointmentId: null,
      date: '',
      time: '',
      durationMinutes: 30,
      location: 'SHILLONG',
      notes: '',
    });
    this.showScheduleDialog = true;
  }

  scheduleMeeting(): void {
    if (this.scheduleForm.invalid) {
      this.scheduleForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const { appointmentId, date, time, durationMinutes, location, notes } = this.scheduleForm.value as {
      appointmentId: number;
      date: string;
      time: string;
      durationMinutes: number;
      location: string;
      notes: string;
    };

    const dateTime = `${date}T${time}:00`;
    const isWalkIn   = appointmentId === OfficeDashboardComponent.WALKIN_SENTINEL;
    const appointment = isWalkIn
      ? undefined
      : this.acceptedAppointments.find(a => a.id === appointmentId);

    if (!isWalkIn && !appointment) {
      this.submitting = false;
      this.messageService.add({
        severity: 'error',
        summary: 'Appointment Not Found',
        detail: 'The selected appointment could not be found. Please refresh and try again.',
        life: 5000,
      });
      return;
    }

    // Persist to backend only for a real appointment (reschedule = office formalising the slot)
    if (!isWalkIn && appointment) {
      this.appointmentService.rescheduleAppointment(appointmentId, { scheduledDateTime: dateTime, durationMinutes })
        .subscribe({
          next: () => this.finaliseSchedule(appointment, dateTime, durationMinutes, location, notes),
          error: () => this.finaliseSchedule(appointment, dateTime, durationMinutes, location, notes),
        });
    } else {
      this.finaliseSchedule(undefined, dateTime, durationMinutes, location, notes);
    }
  }

  private finaliseSchedule(
    appointment: Appointment | undefined,
    dateTime: string,
    durationMinutes: number,
    location: string,
    notes: string,
  ): void {
    const meeting: ScheduledMeeting = {
      id: this.idCounter,
      meetingId: this.generateMeetingId(),
      appointmentId: appointment?.id ?? 0,
      title: appointment
        ? `${appointment.agendaType} – ${appointment.applicant.fullName}`
        : 'Office Meeting',
      dateTime,
      durationMinutes,
      location,
      status: 'SCHEDULED',
      attendeeName: appointment?.applicant.fullName ?? 'Invitee',
      phoneNumber: appointment?.applicant.phoneNumber ?? '',
      agendaBrief: notes || appointment?.agendaBrief || '',
      eventType: appointment?.eventType ?? OfficeDashboardComponent.DEFAULT_EVENT_TYPE,
    };

    this.idCounter++;
    this.meetings = [...this.meetings, meeting];
    this.showScheduleDialog = false;
    this.submitting = false;

    this.sendMeetingNotification(meeting);
    this.messageService.add({
      severity: 'success',
      summary: 'Meeting Scheduled',
      detail: `${meeting.meetingId} created. Notifications sent.`,
      life: 5000,
    });
  }

  // ─── Timer system ─────────────────────────────────────────────────────────

  startMeeting(meeting: ScheduledMeeting | MeetingCardData): void {
    const m = this.findMeeting(meeting.meetingId);
    if (!m || m.status !== 'SCHEDULED') return;

    m.status = 'IN_PROGRESS';
    m.dateTime = new Date().toISOString();
    this.meetings = [...this.meetings]; // trigger change detection

    this.startTimer(m.id, m.durationMinutes);
    this.messageService.add({
      severity: 'info',
      summary: 'Meeting Started',
      detail: `${m.meetingId} – timer started for ${m.durationMinutes} minutes.`,
      life: 3000,
    });
  }

  endMeeting(meeting: ScheduledMeeting | MeetingCardData): void {
    const m = this.findMeeting(meeting.meetingId);
    if (!m) return;

    m.status = 'COMPLETED';
    this.meetings = [...this.meetings];
    this.stopTimer(m.id);

    this.messageService.add({
      severity: 'success',
      summary: 'Meeting Ended',
      detail: `${m.meetingId} marked as Completed.`,
      life: 3000,
    });
  }

  cancelMeeting(meeting: ScheduledMeeting): void {
    meeting.status = 'CANCELLED';
    this.meetings = [...this.meetings];
    this.stopTimer(meeting.id);
    this.showDetailsDialog = false;
  }

  private startTimer(meetingId: number, durationMinutes: number): void {
    this.stopTimer(meetingId); // clear any existing timer
    const totalSeconds = durationMinutes * 60;
    const timerState: ActiveTimer = { remaining: totalSeconds, subscription: new Subscription(), isExpired: false };

    timerState.subscription = interval(1000).subscribe(() => {
      timerState.remaining--;
      if (timerState.remaining <= 0) {
        timerState.remaining  = 0;
        timerState.isExpired  = true;
        timerState.subscription.unsubscribe();
        this.onTimerExpired(meetingId);
      }
    });

    this.activeTimers.set(meetingId, timerState);
  }

  private stopTimer(meetingId: number): void {
    const timer = this.activeTimers.get(meetingId);
    if (timer) {
      timer.subscription.unsubscribe();
      this.activeTimers.delete(meetingId);
    }
  }

  private onTimerExpired(meetingId: number): void {
    const m = this.meetings.find(x => x.id === meetingId);
    this.messageService.add({
      severity: 'warn',
      summary: '⏰ TIME UP',
      detail: m ? `Meeting ${m.meetingId} – ${m.attendeeName} – time has elapsed!` : 'Meeting time has elapsed!',
      sticky: true,
    });
  }

  getTimerDisplay(meetingId: number): string {
    const timer = this.activeTimers.get(meetingId);
    if (!timer) return '--:--';
    const mins = Math.floor(timer.remaining / 60);
    const secs = timer.remaining % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  isTimerExpired(meetingId: number): boolean {
    return this.activeTimers.get(meetingId)?.isExpired ?? false;
  }

  getTimerPercent(meetingId: number, durationMinutes: number): number {
    const timer = this.activeTimers.get(meetingId);
    if (!timer) return 0;
    return Math.round(((durationMinutes * 60 - timer.remaining) / (durationMinutes * 60)) * 100);
  }

  // ─── Notifications ────────────────────────────────────────────────────────

  private sendMeetingNotification(meeting: ScheduledMeeting): void {
    if (!meeting.phoneNumber) return;

    const msg = `Dear ${meeting.attendeeName}, your meeting with the Hon'ble Chief Minister has been scheduled.\n` +
      `Meeting ID: ${meeting.meetingId}\n` +
      `Date & Time: ${new Date(meeting.dateTime).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}\n` +
      `Location: ${meeting.location}\n` +
      `Duration: ${meeting.durationMinutes} minutes\n` +
      `Agenda: ${meeting.agendaBrief}\n\n` +
      `Please arrive 15 minutes early. – CMO, Meghalaya`;

    this.notificationService.sendWhatsApp(meeting.phoneNumber, msg).subscribe();
    this.notificationService.sendSms(meeting.phoneNumber, msg).subscribe();
  }

  // ─── View helpers ─────────────────────────────────────────────────────────

  toMeetingCardData(m: ScheduledMeeting): MeetingCardData {
    return {
      meetingId:       m.meetingId,
      title:           m.title,
      dateTime:        m.dateTime,
      durationMinutes: m.durationMinutes,
      location:        m.location,
      status:          m.status,
      attendeeName:    m.attendeeName,
      agendaBrief:     m.agendaBrief,
      eventType:       m.eventType,
    };
  }

  viewMeetingDetails(meeting: ScheduledMeeting | MeetingCardData): void {
    this.selectedMeeting = this.findMeeting(meeting.meetingId) ?? null;
    if (this.selectedMeeting) this.showDetailsDialog = true;
  }

  getStatusSeverity(status: string): TagSeverity {
    const m: Record<string, TagSeverity> = {
      SCHEDULED:   'info',
      IN_PROGRESS: 'success',
      COMPLETED:   'secondary',
      CANCELLED:   'danger',
    };
    return m[status] ?? 'secondary';
  }

  formatMeetingTime(dateTime: string): string {
    return new Date(dateTime).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  private findMeeting(meetingId: string): ScheduledMeeting | undefined {
    return this.meetings.find(m => m.meetingId === meetingId);
  }
}
