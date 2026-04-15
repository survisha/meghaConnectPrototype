import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ScheduleEventService } from '../services/schedule-event.service';
import { AuditLogService } from '../services/audit-log.service';
import { ScheduleEvent } from '../models';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { UIChart } from 'primeng/chart';
import { AiInsightsDashboardComponent } from '../ai-insights-dashboard/ai-insights-dashboard.component';

interface QuickAction { label: string; icon: string; matIcon?: string; route: string; severity: string; }

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, UIChart, AiInsightsDashboardComponent],
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

    // Initialize dummy data for demo purposes
    this.initializeDummyData();

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

    // Load today's schedule from real API, fallback to dummy if empty
    this.scheduleEventService.getAll().subscribe(events => {
      const today = new Date().toDateString();
      const todayEvents = events.filter(e => new Date(e.startTime).toDateString() === today);
      if (todayEvents.length > 0) {
        this.todaySchedule = todayEvents.map(e => ({
          time: new Date(e.startTime).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false }),
          title: e.title,
          type: e.eventType,
          location: e.location,
          badge: this.getEventBadge(e.eventType),
        }));
      }
      // Dummy data already initialized, no need to override if API returns data
    });

    // Load recent activity from real audit log API, fallback to dummy if empty
    this.auditLogService.getAll(0, 5).subscribe(page => {
      if (page.content && page.content.length > 0) {
        this.recentActivity = page.content.map(log => ({
          icon: this.getAuditIcon(log.action),
          matIcon: this.getAuditMatIcon(log.action),
          color: this.getAuditColor(log.action),
          text: `${log.action}: ${log.details ?? log.entityType + ' #' + log.entityId}`,
          time: new Date(log.timestamp).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }),
        }));
      }
      // Dummy data already initialized
    });
  }

  private initializeDummyData() {
    // Dummy KPI values with realistic counts
    const kpiUpdates: Record<string, number> = {
      "Today's Appointments": 12,
      'Pending Approvals': 8,
      'Active Scheme Apps': 34,
      'Pending Follow-ups': 5,
      'Walk-ins Today': 23,
      'CMO Reviews Due': 6
    };

    // Dummy today's schedule with AI-generated descriptions
    this.todaySchedule = [
      {
        time: '09:00',
        title: 'Weekly Cabinet Briefing - Policy Review & Budget Allocation Discussion',
        type: 'A1',
        location: 'Conference Hall A',
        badge: 'danger',
        aiSummary: 'Strategic session covering quarterly budget review, infrastructure project approvals, and upcoming legislative priorities.'
      },
      {
        time: '11:30',
        title: 'File Clearing Session - Pending Scheme Applications & Departmental Approvals',
        type: 'A3',
        location: 'CM Office',
        badge: 'secondary',
        aiSummary: 'Expedited review of 47 pending files including CMSDF applications, road construction proposals, and education grants.'
      },
      {
        time: '14:00',
        title: 'Public Durbar - Constituency Grievance Resolution & Direct Citizen Engagement',
        type: 'B1',
        location: 'Main Durbar Hall',
        badge: 'warn',
        aiSummary: 'Open public interaction session addressing water supply issues in Shillong East, land dispute resolutions, and healthcare accessibility concerns.'
      },
      {
        time: '16:30',
        title: 'Individual Appointment - Shri Ramesh Kumar - CM Care Fund Application Review',
        type: 'A4',
        location: 'Meeting Room 3',
        badge: 'info',
        aiSummary: 'One-on-one discussion regarding medical emergency assistance under CM Care scheme for cardiac surgery requiring ₹2.5L support.'
      },
      {
        time: '18:00',
        title: 'State Development Event - Launch of Digital Meghalaya Initiative',
        type: 'A2',
        location: 'State Secretariat Auditorium',
        badge: 'success',
        aiSummary: 'Inauguration ceremony for e-governance platform covering online citizen services, digital payment integration, and AI-powered grievance redressal system.'
      }
    ];

    // Dummy recent activity
    this.recentActivity = [
      {
        icon: 'pi-check-circle',
        matIcon: 'check_circle',
        color: '#16a34a',
        text: 'APPROVED: CM Care Fund Application #CM24-0845 - Medical Emergency Grant ₹50,000',
        time: '10 mins ago'
      },
      {
        icon: 'pi-arrow-right-arrow-left',
        matIcon: 'swap_horiz',
        color: '#1a237e',
        text: 'STATUS CHANGE: Appointment #APT-2024-1234 rescheduled from March 28 to March 30',
        time: '25 mins ago'
      },
      {
        icon: 'pi-check-circle',
        matIcon: 'check_circle',
        color: '#16a34a',
        text: 'CREATED: New scheme application CMSDF #SCH-2024-0567 - Rural Road Construction',
        time: '1 hour ago'
      },
      {
        icon: 'pi-user-plus',
        matIcon: 'login',
        color: '#1a237e',
        text: 'LOGIN: DEO Officer Priya Sharma accessed visitor registration module',
        time: '2 hours ago'
      },
      {
        icon: 'pi-bell',
        matIcon: 'notifications',
        color: '#b45309',
        text: 'REMINDER: 15 pending file approvals require attention by end of day',
        time: '3 hours ago'
      }
    ];

    // Update KPI values with dummy data
    setTimeout(() => {
      this.kpis.forEach(kpi => {
        if (kpiUpdates[kpi.label]) {
          kpi.value = kpiUpdates[kpi.label];
        }
      });
    }, 100);
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

  private getAuditMatIcon(action: string): string {
    if (action.includes('APPROVED') || action.includes('CREATED')) return 'check_circle';
    if (action.includes('CHANGE') || action.includes('UPDATE')) return 'swap_horiz';
    if (action.includes('LOGIN')) return 'login';
    return 'notifications';
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
      { label: "Today's Appointments", value: '–', icon: 'pi-calendar-plus', matIcon: 'event_available', color: '#1a237e', bg: '#e8eaf6',
        roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
      { label: 'Pending Approvals', value: '–', icon: 'pi-clock', matIcon: 'schedule', color: '#b45309', bg: '#fef3c7',
        roles: ['HCM','ADMIN','OSD','APPROVER'] },
      { label: 'Active Scheme Apps', value: '–', icon: 'pi-briefcase', matIcon: 'work', color: '#065f46', bg: '#d1fae5',
        roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
      { label: 'Pending Follow-ups', value: '–', icon: 'pi-exclamation-triangle', matIcon: 'warning', color: '#991b1b', bg: '#fee2e2',
        roles: ['HCM','ADMIN','OSD'] },
      { label: 'Walk-ins Today', value: '–', icon: 'pi-sign-in', matIcon: 'directions_walk', color: '#0369a1', bg: '#e0f2fe',
        roles: ['DATA_ENTRY_OPERATOR','ADMIN','OSD'] },
      { label: 'CMO Reviews Due', value: '–', icon: 'pi-file-edit', matIcon: 'rate_review', color: '#7c3aed', bg: '#ede9fe',
        roles: ['CMO_OFFICER'] },
    ];
    this.kpis = all.filter(k => !role || k.roles.includes(role as any));
  }

  private buildQuickActions() {
    const role = this.auth.user()?.role;
    const all: (QuickAction & { roles: string[], matIcon?: string })[] = [
      { label: 'New Appointment', icon: 'pi pi-plus', matIcon: 'add', route: '/appointments/new', severity: '',
        roles: ['ADMIN','OSD','DATA_ENTRY_OPERATOR'] },
      { label: 'Walk-in Counter', icon: 'pi pi-sign-in', matIcon: 'login', route: '/appointments/walkin', severity: 'success',
        roles: ['ADMIN','OSD','DATA_ENTRY_OPERATOR'] },
      { label: 'Register Visitor', icon: 'pi pi-user-plus', matIcon: 'person_add', route: '/deo/register-visitor', severity: 'success',
        roles: ['DATA_ENTRY_OPERATOR','ADMIN','OSD'] },
      { label: 'Apply for Scheme', icon: 'pi pi-briefcase', matIcon: 'work', route: '/schemes/apply', severity: 'warning',
        roles: ['ADMIN','OSD'] },
      { label: 'Identify Person', icon: 'pi pi-id-card', matIcon: 'badge', route: '/identify', severity: 'info',
        roles: ['HCM','ADMIN','OSD','DATA_ENTRY_OPERATOR'] },
      { label: 'Scheme Heatmap', icon: 'pi pi-map', matIcon: 'map', route: '/reports/heatmap', severity: 'secondary',
        roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
      { label: 'View Reports', icon: 'pi pi-chart-bar', matIcon: 'bar_chart', route: '/reports', severity: 'info',
        roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
      { label: 'Manage Users', icon: 'pi pi-shield', matIcon: 'admin_panel_settings', route: '/admin/users', severity: 'secondary',
        roles: ['HCM','ADMIN','OSD'] },
      { label: 'Audit Trail', icon: 'pi pi-history', matIcon: 'history', route: '/reports/audit', severity: 'secondary',
        roles: ['ADMIN'] },
    ];
    this.quickActions = all.filter(a => !role || a.roles.includes(role));
  }

  getEventClass(type: string) {
    const m: Record<string,string> = { A1:'event-a1', A2:'event-a2', A3:'event-a3', A4:'event-a4', B1:'event-b1', B2:'event-b2' };
    return m[type] ?? '';
  }

  get showSchedule() {
    return this.auth.hasRole('HCM','ADMIN','OSD','APPROVER','CMO_OFFICER');
  }

  get showCharts() {
    return this.auth.hasRole('HCM','ADMIN','OSD','APPROVER','CMO_OFFICER');
  }
}
