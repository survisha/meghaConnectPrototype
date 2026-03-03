import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ScheduleEventService } from '../services/schedule-event.service';
import { AuditLogService } from '../services/audit-log.service';
import { ScheduleEvent } from '../models';
import { Button } from 'primeng/button';
import { UIChart } from 'primeng/chart';

interface QuickAction { label: string; icon: string; route: string; severity: string; }

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Button, UIChart],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  kpis: any[] = [];
  quickActions: QuickAction[] = [];

  appointmentTypeData: any;
  schemeData: any;
  chartOptions: any;

  todaySchedule: any[] = [];
  recentActivity: any[] = [];

  constructor(
    public auth: AuthService,
    private scheduleEventService: ScheduleEventService,
    private auditLogService: AuditLogService,
  ) {}

  ngOnInit() {
    this.buildKpis();
    this.buildQuickActions();

    this.appointmentTypeData = {
      labels: ['A1: Cabinet/Flight', 'A2: Events', 'A3: Files', 'A4: Individual', 'B1: Public Durbar', 'B2: Walk-in'],
      datasets: [{ data: [2, 5, 3, 18, 8, 6], backgroundColor: ['#1565c0','#2e7d32','#f57f17','#c62828','#4527a0','#006064'] }]
    };

    this.schemeData = {
      labels: ['CMSDF', 'CMSG', 'CM Care', 'CM Connect', 'CM Elevate', 'Focus+'],
      datasets: [{
        label: 'Applications',
        data: [45, 32, 28, 19, 15, 22],
        backgroundColor: ['#1a237e','#1565c0','#0288d1','#00838f','#2e7d32','#558b2f'],
      }]
    };

    this.chartOptions = { plugins: { legend: { position: 'bottom' } }, responsive: true };

    // Load today's schedule from real API
    this.scheduleEventService.getAll().subscribe(events => {
      const today = new Date().toDateString();
      const todayEvents = events.filter(e => new Date(e.startTime).toDateString() === today);
      this.todaySchedule = todayEvents.map(e => ({
        time: new Date(e.startTime).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false }),
        title: e.title,
        type: e.eventType,
        location: e.location,
        badge: this.getEventBadge(e.eventType),
      }));
    });

    // Load recent activity from real audit log API
    this.auditLogService.getAll(0, 5).subscribe(page => {
      this.recentActivity = page.content.map(log => ({
        icon: this.getAuditIcon(log.action),
        color: this.getAuditColor(log.action),
        text: `${log.action}: ${log.details ?? log.entityType + ' #' + log.entityId}`,
        time: new Date(log.timestamp).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }),
      }));
    });
  }

  private getEventBadge(type: string): string {
    const m: Record<string,string> = { A1:'danger', A2:'success', A3:'secondary', A4:'info', B1:'warn', B2:'info' };
    return m[type] ?? 'info';
  }

  private getAuditIcon(action: string): string {
    if (action.includes('APPROVED') || action.includes('CREATED')) return 'pi-check-circle';
    if (action.includes('CHANGE') || action.includes('UPDATE')) return 'pi-arrow-right-arrow-left';
    if (action.includes('LOGIN')) return 'pi-user-plus';
    return 'pi-bell';
  }

  private getAuditColor(action: string): string {
    if (action.includes('APPROVED') || action.includes('CREATED')) return '#16a34a';
    if (action.includes('REJECT')) return '#dc2626';
    if (action.includes('CHANGE') || action.includes('UPDATE')) return '#1a237e';
    return '#b45309';
  }

  private buildKpis() {
    const role = this.auth.user()?.role;
    const all = [
      { label: "Today's Appointments", value: '–', icon: 'pi-calendar-plus', color: '#1a237e', bg: '#e8eaf6',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
      { label: 'Pending Approvals', value: '–', icon: 'pi-clock', color: '#b45309', bg: '#fef3c7',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY'] },
      { label: 'Active Scheme Apps', value: '–', icon: 'pi-briefcase', color: '#065f46', bg: '#d1fae5',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
      { label: 'Pending Follow-ups', value: '–', icon: 'pi-exclamation-triangle', color: '#991b1b', bg: '#fee2e2',
        roles: ['HCM','ADMIN','SAIDUL_OSD'] },
      { label: 'Walk-ins Today', value: '–', icon: 'pi-sign-in', color: '#0369a1', bg: '#e0f2fe',
        roles: ['DATA_ENTRY_OPERATOR','ADMIN','SAIDUL_OSD'] },
      { label: 'CMO Reviews Due', value: '–', icon: 'pi-file-edit', color: '#7c3aed', bg: '#ede9fe',
        roles: ['CMO_OFFICER'] },
    ];
    this.kpis = all.filter(k => !role || k.roles.includes(role as any));
  }

  private buildQuickActions() {
    const role = this.auth.user()?.role;
    const all: (QuickAction & { roles: string[] })[] = [
      { label: 'New Appointment', icon: 'pi pi-plus', route: '/appointments/new', severity: '',
        roles: ['ADMIN','SAIDUL_OSD','DATA_ENTRY_OPERATOR'] },
      { label: 'Walk-in Counter', icon: 'pi pi-sign-in', route: '/appointments/walkin', severity: 'success',
        roles: ['ADMIN','SAIDUL_OSD','DATA_ENTRY_OPERATOR'] },
      { label: 'Apply for Scheme', icon: 'pi pi-briefcase', route: '/schemes/apply', severity: 'warning',
        roles: ['ADMIN','SAIDUL_OSD'] },
      { label: 'Identify Person', icon: 'pi pi-id-card', route: '/identify', severity: 'info',
        roles: ['HCM','ADMIN','SAIDUL_OSD','DATA_ENTRY_OPERATOR'] },
      { label: 'Scheme Heatmap', icon: 'pi pi-map', route: '/reports/heatmap', severity: 'secondary',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
      { label: 'View Reports', icon: 'pi pi-chart-bar', route: '/reports', severity: 'info',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
      { label: 'Manage Users', icon: 'pi pi-shield', route: '/admin/users', severity: 'secondary',
        roles: ['HCM','ADMIN','SAIDUL_OSD'] },
      { label: 'Audit Trail', icon: 'pi pi-history', route: '/reports/audit', severity: 'secondary',
        roles: ['ADMIN'] },
    ];
    this.quickActions = all.filter(a => !role || a.roles.includes(role));
  }

  getEventClass(type: string) {
    const m: Record<string,string> = { A1:'event-a1', A2:'event-a2', A3:'event-a3', A4:'event-a4', B1:'event-b1', B2:'event-b2' };
    return m[type] ?? '';
  }

  get showSchedule() {
    return this.auth.hasRole('HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER');
  }

  get showCharts() {
    return this.auth.hasRole('HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER');
  }
}
