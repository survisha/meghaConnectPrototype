import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ScheduleEventService } from '../services/schedule-event.service';
import { AuditLogService } from '../services/audit-log.service';
import { AppointmentService } from '../services/appointment.service';
import { SchemeService } from '../services/scheme.service';
import { GrievanceService } from '../services/grievance.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { UIChart } from 'primeng/chart';
import { AiInsightsDashboardComponent } from '../ai-insights-dashboard/ai-insights-dashboard.component';
import { Appointment, AuditEntry, EventType, ScheduleEvent, SchemeApplication } from '../models';
import { apiErrorMessage } from '../shared/api-error.util';


interface QuickAction { label: string; matIcon: string; route: string; severity: string; }

interface DashboardKpi {
  label: string;
  value: number | string;
  matIcon: string;
  color: string;
  bg: string;
  roles: string[];
}

interface DashboardScheduleItem {
  time: string;
  title: string;
  type: string;
  location: string;
  aiSummary?: string;
}

interface DashboardActivityItem {
  matIcon: string;
  color: string;
  text: string;
  time: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, UIChart, AiInsightsDashboardComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  kpis: DashboardKpi[] = [];
  quickActions: QuickAction[] = [];

  appointmentTypeData: any;
  schemeData: any;
  chartOptions: any;

  todaySchedule: DashboardScheduleItem[] = [];
  recentActivity: DashboardActivityItem[] = [];
  currentDateLabel = '';
  errorMsg = '';

  private readonly appointmentTypeKeys: EventType[] = ['A1', 'A2', 'A3', 'A4', 'B1', 'B2'];
  private readonly appointmentTypeLabels = [
    'A1: Cabinet/Flight',
    'A2: Events',
    'A3: Files',
    'A4: Individual',
    'B1: Public Durbar',
    'B2: Walk-in',
  ];
  private readonly appointmentTypeColors = ['#1565c0', '#2e7d32', '#f57f17', '#c62828', '#4527a0', '#006064'];
  private readonly schemeTypeKeys = ['CMSDF', 'CMSG', 'CM_CARE', 'CM_CONNECT', 'CM_ELEVATE', 'FOCUS_PLUS'];
  private readonly schemeTypeLabels = ['CMSDF', 'CMSG', 'CM Care', 'CM Connect', 'CM Elevate', 'Focus+'];
  private readonly schemeTypeColors = ['#1a237e', '#1565c0', '#0288d1', '#00838f', '#2e7d32', '#558b2f'];
  private readonly pendingApprovalStatuses = new Set(['PENDING_APPROVER_REVIEW', 'APPROVER_REVIEW', 'HCM_PENDING']);
  private readonly inactiveSchemeStatuses = new Set(['REJECTED', 'HCM_REJECTED', 'CANCELLED', 'CANCELED', 'COMPLETED', 'CLOSED']);
  private readonly resolvedGrievanceStatuses = new Set(['RESOLVED', 'CLOSED']);

  constructor(
    public auth: AuthService,
    private scheduleEventService: ScheduleEventService,
    private auditLogService: AuditLogService,
    private appointmentService: AppointmentService,
    private schemeService: SchemeService,
    private grievanceService: GrievanceService,
  ) {}

  ngOnInit() {
    this.currentDateLabel = this.formatDisplayDate(new Date());
    this.buildKpis();
    this.buildQuickActions();
    this.initializeCharts();
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    if (this.showCharts || this.hasVisibleKpi("Today's Appointments") || this.hasVisibleKpi('Pending Approvals') ||
        this.hasVisibleKpi('Walk-ins Today') || this.hasVisibleKpi('CMO Reviews Due')) {
      this.loadAppointmentMetrics();
    }

    if (this.showSchedule) {
      this.loadTodaySchedule();
    }

    if (this.showCharts || this.hasVisibleKpi('Active Scheme Apps')) {
      this.loadSchemeMetrics();
    }

    if (this.hasVisibleKpi('Pending Follow-ups')) {
      this.loadPendingFollowUps();
    }

    if (this.auth.hasRole('ADMIN')) {
      this.loadRecentActivity();
    }
  }

  private loadAppointmentMetrics(): void {
    this.appointmentService.getAllAppointments(0, 1000).subscribe({
      next: page => {
        const appointments = page.content ?? [];
        this.setKpiValue("Today's Appointments", appointments.filter(a => this.isToday(a.scheduledDateTime)).length);
        this.setKpiValue('Pending Approvals', appointments.filter(a => this.pendingApprovalStatuses.has(a.status)).length);
        this.setKpiValue('Walk-ins Today', appointments.filter(a => Boolean(a.isWalkIn) && this.isToday(this.firstDate(a.createdAt, a.submittedAt, a.scheduledDateTime))).length);
        this.setKpiValue('CMO Reviews Due', appointments.filter(a => a.status === 'CMO_REVIEW').length);
        this.updateAppointmentTypeChart(appointments);
      },
      error: err => this.addError(err, 'Unable to load appointment dashboard metrics.'),
    });
  }

  private loadTodaySchedule(): void {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    const end = new Date(start);
    end.setDate(end.getDate() + 1);

    this.scheduleEventService.getAll({
      start: this.toLocalDateTimeParam(start),
      end: this.toLocalDateTimeParam(end),
    }).subscribe({
      next: events => {
        this.todaySchedule = (events ?? [])
          .filter(event => this.isToday(event.startTime))
          .sort((a, b) => this.dateTimeValue(a.startTime) - this.dateTimeValue(b.startTime))
          .map(event => this.mapScheduleEvent(event));
      },
      error: err => this.addError(err, 'Unable to load today schedule.'),
    });
  }

  private loadSchemeMetrics(): void {
    this.schemeService.getAllApplications({ page: 0, size: 1000 }).subscribe({
      next: response => {
        const applications = this.normalizeSchemeApplications(response);
        this.setKpiValue('Active Scheme Apps', applications.filter(app => !this.inactiveSchemeStatuses.has(this.normalizeStatus(app.status))).length);
        this.updateSchemeChart(applications);
      },
      error: err => this.addError(err, 'Unable to load scheme application dashboard metrics.'),
    });
  }

  private loadPendingFollowUps(): void {
    this.grievanceService.getAll(0, 1000).subscribe({
      next: page => {
        const grievances = page.content ?? [];
        this.setKpiValue('Pending Follow-ups', grievances.filter(g => !this.resolvedGrievanceStatuses.has(g.status)).length);
      },
      error: err => this.addError(err, 'Unable to load pending follow-up metrics.'),
    });
  }

  private loadRecentActivity(): void {
    this.auditLogService.getAll(0, 5).subscribe({
      next: page => {
        this.recentActivity = (page.content ?? []).map(log => this.mapAuditEntry(log));
      },
      error: err => this.addError(err, 'Unable to load recent activity.'),
    });
  }

  private initializeCharts(): void {
    this.appointmentTypeData = {
      labels: this.appointmentTypeLabels,
      datasets: [{ data: this.appointmentTypeKeys.map(() => 0), backgroundColor: this.appointmentTypeColors }],
    };

    this.schemeData = {
      labels: this.schemeTypeLabels,
      datasets: [{
        label: 'Applications',
        data: this.schemeTypeKeys.map(() => 0),
        backgroundColor: this.schemeTypeColors,
      }],
    };

    this.chartOptions = {
      plugins: { legend: { position: 'bottom' } },
      responsive: true,
      maintainAspectRatio: false,
    };
  }

  private updateAppointmentTypeChart(appointments: Appointment[]): void {
    const counts = new Map<string, number>(this.appointmentTypeKeys.map(key => [key, 0]));
    appointments
      .filter(appointment => this.isCurrentMonth(this.firstDate(appointment.createdAt, appointment.submittedAt, appointment.scheduledDateTime, appointment.updatedAt)))
      .forEach(appointment => {
        const key = appointment.eventType;
        if (counts.has(key)) {
          counts.set(key, (counts.get(key) ?? 0) + 1);
        }
      });

    this.appointmentTypeData = {
      labels: this.appointmentTypeLabels,
      datasets: [{ data: this.appointmentTypeKeys.map(key => counts.get(key) ?? 0), backgroundColor: this.appointmentTypeColors }],
    };
  }

  private updateSchemeChart(applications: SchemeApplication[]): void {
    const counts = new Map<string, number>(this.schemeTypeKeys.map(key => [key, 0]));
    applications
      .filter(application => this.isCurrentMonth(this.firstDate(application.createdAt, application.updatedAt)))
      .forEach(application => {
        const key = this.normalizeSchemeType(application.schemeType);
        if (counts.has(key)) {
          counts.set(key, (counts.get(key) ?? 0) + 1);
        }
      });

    this.schemeData = {
      labels: this.schemeTypeLabels,
      datasets: [{
        label: 'Applications',
        data: this.schemeTypeKeys.map(key => counts.get(key) ?? 0),
        backgroundColor: this.schemeTypeColors,
      }],
    };
  }

  private mapScheduleEvent(event: ScheduleEvent): DashboardScheduleItem {
    return {
      time: this.formatTime(event.startTime),
      title: event.title || event.appointment?.subject || 'Scheduled event',
      type: event.eventType,
      location: event.location || 'Not specified',
      aiSummary: event.shortNotes || event.appointment?.shortNotes,
    };
  }

  private mapAuditEntry(log: AuditEntry): DashboardActivityItem {
    const entity = log.entityType && log.entityId ? `${log.entityType} #${log.entityId}` : log.entityType || 'Record';
    return {
      matIcon: this.getAuditMatIcon(log.action),
      color: this.getAuditColor(log.action),
      text: `${log.action || 'ACTIVITY'}: ${log.details || log.description || entity}`,
      time: this.formatRelativeTime(log.timestamp),
    };
  }

  private normalizeSchemeApplications(response: unknown): SchemeApplication[] {
    const data = this.unwrapData<unknown>(response);
    const raw: any = data ?? {};
    const rows = Array.isArray(raw) ? raw : (raw.content ?? []);
    return Array.isArray(rows) ? rows as SchemeApplication[] : [];
  }

  private unwrapData<T = unknown>(response: unknown): T {
    const raw: any = response;
    if (raw && typeof raw === 'object' && 'data' in raw && 'success' in raw) {
      return raw.data as T;
    }
    return response as T;
  }

  private hasVisibleKpi(label: string): boolean {
    return this.kpis.some(kpi => kpi.label === label);
  }

  private setKpiValue(label: string, value: number): void {
    const kpi = this.kpis.find(item => item.label === label);
    if (kpi) {
      kpi.value = value;
    }
  }

  private addError(error: unknown, fallbackMessage: string): void {
    const message = apiErrorMessage(error, fallbackMessage);
    if (!this.errorMsg.includes(message)) {
      this.errorMsg = this.errorMsg ? `${this.errorMsg} ${message}` : message;
    }
  }

  private firstDate(...values: Array<string | undefined>): string | undefined {
    return values.find(value => Boolean(value));
  }

  private parseDate(value?: string): Date | null {
    if (!value) {
      return null;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private isToday(value?: string): boolean {
    const date = this.parseDate(value);
    if (!date) {
      return false;
    }
    const now = new Date();
    return date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate();
  }

  private isCurrentMonth(value?: string): boolean {
    const date = this.parseDate(value);
    if (!date) {
      return false;
    }
    const now = new Date();
    return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
  }

  private dateTimeValue(value?: string): number {
    return this.parseDate(value)?.getTime() ?? 0;
  }

  private formatDisplayDate(date: Date): string {
    return new Intl.DateTimeFormat('en-IN', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    }).format(date);
  }

  private formatTime(value?: string): string {
    const date = this.parseDate(value);
    if (!date) {
      return '--:--';
    }
    return date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  private formatRelativeTime(value?: string): string {
    const date = this.parseDate(value);
    if (!date) {
      return '';
    }

    const diffMs = Date.now() - date.getTime();
    if (diffMs < 60_000) {
      return 'Just now';
    }

    const minutes = Math.floor(diffMs / 60_000);
    if (minutes < 60) {
      return `${minutes} min${minutes === 1 ? '' : 's'} ago`;
    }

    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      return `${hours} hour${hours === 1 ? '' : 's'} ago`;
    }

    const days = Math.floor(hours / 24);
    if (days < 7) {
      return `${days} day${days === 1 ? '' : 's'} ago`;
    }

    return date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  private toLocalDateTimeParam(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  private normalizeStatus(status?: string): string {
    return (status ?? '').trim().toUpperCase();
  }

  private normalizeSchemeType(schemeType?: string): string {
    const normalized = (schemeType ?? '').trim().toUpperCase().replace(/\s+/g, '_').replace(/-/g, '_').replace(/\+/g, '_PLUS');
    const aliases: Record<string, string> = {
      CMCARE: 'CM_CARE',
      CM_CARE: 'CM_CARE',
      CMCONNECT: 'CM_CONNECT',
      CM_CONNECT: 'CM_CONNECT',
      CMELEVATE: 'CM_ELEVATE',
      CM_ELEVATE: 'CM_ELEVATE',
      FOCUSPLUS: 'FOCUS_PLUS',
      FOCUS_PLUS: 'FOCUS_PLUS',
    };
    return aliases[normalized] ?? normalized;
  }

  private getAuditMatIcon(action: string): string {
    const normalized = this.normalizeStatus(action);
    if (normalized.includes('APPROVED') || normalized.includes('CREATED')) return 'check_circle';
    if (normalized.includes('CHANGE') || normalized.includes('UPDATE')) return 'swap_horiz';
    if (normalized.includes('LOGIN')) return 'login';
    return 'notifications';
  }

  private getAuditColor(action: string): string {
    const normalized = this.normalizeStatus(action);
    if (normalized.includes('APPROVED') || normalized.includes('CREATED')) return '#16a34a';
    if (normalized.includes('REJECT')) return '#dc2626';
    if (normalized.includes('CHANGE') || normalized.includes('UPDATE')) return '#1a237e';
    return '#b45309';
  }

  private buildKpis() {
    const role = this.auth.user()?.role;
    const all: DashboardKpi[] = [
      { label: "Today's Appointments", value: '-', matIcon: 'event_available', color: '#1a237e', bg: '#e8eaf6',
        roles: ['HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER', 'DATA_ENTRY_OPERATOR'] },
      { label: 'Pending Approvals', value: '-', matIcon: 'schedule', color: '#b45309', bg: '#fef3c7',
        roles: ['HCM', 'ADMIN', 'OSD', 'APPROVER'] },
      { label: 'Active Scheme Apps', value: '-', matIcon: 'work', color: '#065f46', bg: '#d1fae5',
        roles: ['HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER'] },
      { label: 'Pending Follow-ups', value: '-', matIcon: 'warning', color: '#991b1b', bg: '#fee2e2',
        roles: ['HCM', 'ADMIN', 'OSD'] },
      { label: 'Walk-ins Today', value: '-', matIcon: 'directions_walk', color: '#0369a1', bg: '#e0f2fe',
        roles: ['DATA_ENTRY_OPERATOR', 'ADMIN', 'OSD'] },
      { label: 'CMO Reviews Due', value: '-', matIcon: 'rate_review', color: '#7c3aed', bg: '#ede9fe',
        roles: ['CMO_OFFICER'] },
    ];
    this.kpis = all.filter(k => !role || k.roles.includes(role));
  }

  private buildQuickActions() {
    const role = this.auth.user()?.role;
    const all: (QuickAction & { roles: string[] })[] = [
      { label: 'New Appointment', matIcon: 'add', route: '/appointments/new', severity: '',
        roles: ['ADMIN', 'OSD'] },
      { label: 'Walk-in Counter', matIcon: 'login', route: '/appointments/walkin', severity: 'success',
        roles: ['ADMIN', 'OSD', 'DATA_ENTRY_OPERATOR'] },
      { label: 'Register Visitor', matIcon: 'person_add', route: '/deo/register-visitor', severity: 'success',
        roles: ['DATA_ENTRY_OPERATOR', 'ADMIN', 'OSD'] },
      { label: 'Apply for Scheme', matIcon: 'work', route: '/schemes/apply', severity: 'warning',
        roles: ['ADMIN', 'OSD'] },
      { label: 'Identify Person', matIcon: 'badge', route: '/identify', severity: 'info',
        roles: ['HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER', 'DATA_ENTRY_OPERATOR'] },
      { label: 'Scheme Heatmap', matIcon: 'map', route: '/reports/heatmap', severity: 'secondary',
        roles: ['HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER'] },
      { label: 'View Reports', matIcon: 'bar_chart', route: '/reports', severity: 'info',
        roles: ['HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER'] },
      { label: 'Manage Users', matIcon: 'admin_panel_settings', route: '/admin/users', severity: 'secondary',
        roles: ['HCM', 'ADMIN', 'OSD'] },
      { label: 'Audit Trail', matIcon: 'history', route: '/reports/audit', severity: 'secondary',
        roles: ['ADMIN'] },
    ];
    this.quickActions = all.filter(a => !role || a.roles.includes(role));
  }

  getEventClass(type: string) {
    const m: Record<string, string> = { A1: 'event-a1', A2: 'event-a2', A3: 'event-a3', A4: 'event-a4', B1: 'event-b1', B2: 'event-b2' };
    return m[type] ?? '';
  }

  get showSchedule() {
    return this.auth.hasRole('HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER');
  }

  get showCharts() {
    return this.auth.hasRole('HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER');
  }
}
