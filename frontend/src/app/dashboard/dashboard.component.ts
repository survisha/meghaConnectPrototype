import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../services/mock-data.service';
import { AuthService } from '../services/auth.service';
import { Button } from 'primeng/button';
import { UIChart } from 'primeng/chart';
import { Tag } from 'primeng/tag';
import { Badge, BadgeDirective } from 'primeng/badge';
import { Timeline } from 'primeng/timeline';

interface QuickAction { label: string; icon: string; route: string; severity: string; }

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Button, UIChart, Tag, Badge, BadgeDirective, Timeline],
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

  constructor(public mock: MockDataService, public auth: AuthService) {}

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

    this.todaySchedule = [
      { time: '09:00', title: 'Cabinet Meeting', type: 'A1', location: 'Shillong', badge: 'danger' },
      { time: '10:00', title: 'Appointment – Ramsing Marak', type: 'A4', location: 'Tura', badge: 'info' },
      { time: '11:00', title: 'Public Durbar – West Garo Hills', type: 'B1', location: 'Tura', badge: 'warn' },
      { time: '14:00', title: 'District Development Programme', type: 'A2', location: 'Tura', badge: 'success' },
      { time: '16:30', title: 'File Clearing', type: 'A3', location: 'Office', badge: 'secondary' },
    ];

    this.recentActivity = [
      { icon: 'pi-check-circle', color: '#16a34a', text: 'CM Care approved for Bijoy Momin (₹3,00,000)', time: '10:45 AM' },
      { icon: 'pi-arrow-right-arrow-left', color: '#1a237e', text: 'Appointment MC-2024-00002 moved to HCM Pending', time: '11:30 AM' },
      { icon: 'pi-user-plus', color: '#0369a1', text: 'New walk-in: Deibok Lyngdoh registered by DEO', time: '02:15 PM' },
      { icon: 'pi-bell', color: '#b45309', text: 'Direction pending follow-up: Community Hall CMSDF', time: '03:00 PM' },
    ];
  }

  private buildKpis() {
    const role = this.auth.user()?.role;
    const all = [
      { label: "Today's Appointments", value: 6, icon: 'pi-calendar-plus', color: '#1a237e', bg: '#e8eaf6',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
      { label: 'Pending Approvals', value: 3, icon: 'pi-clock', color: '#b45309', bg: '#fef3c7',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY'] },
      { label: 'Active Scheme Apps', value: 12, icon: 'pi-briefcase', color: '#065f46', bg: '#d1fae5',
        roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
      { label: 'Pending Follow-ups', value: 5, icon: 'pi-exclamation-triangle', color: '#991b1b', bg: '#fee2e2',
        roles: ['HCM','ADMIN','SAIDUL_OSD'] },
      { label: 'Walk-ins Today', value: 4, icon: 'pi-sign-in', color: '#0369a1', bg: '#e0f2fe',
        roles: ['DATA_ENTRY_OPERATOR','ADMIN','SAIDUL_OSD'] },
      { label: 'CMO Reviews Due', value: 7, icon: 'pi-file-edit', color: '#7c3aed', bg: '#ede9fe',
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
