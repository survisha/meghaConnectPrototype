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

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Button, UIChart, Tag, Badge, BadgeDirective, Timeline],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  kpis = [
    { label: "Today's Appointments", value: 6, icon: 'pi-calendar-plus', color: '#1a237e', bg: '#e8eaf6' },
    { label: 'Pending Approvals', value: 3, icon: 'pi-clock', color: '#b45309', bg: '#fef3c7' },
    { label: 'Active Scheme Apps', value: 12, icon: 'pi-briefcase', color: '#065f46', bg: '#d1fae5' },
    { label: 'Pending Follow-ups', value: 5, icon: 'pi-exclamation-triangle', color: '#991b1b', bg: '#fee2e2' },
  ];

  appointmentTypeData: any;
  schemeData: any;
  chartOptions: any;

  todaySchedule: any[] = [];
  recentActivity: any[] = [];

  constructor(public mock: MockDataService, public auth: AuthService) {}

  ngOnInit() {
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

  getEventClass(type: string) {
    const m: Record<string,string> = { A1:'event-a1', A2:'event-a2', A3:'event-a3', A4:'event-a4', B1:'event-b1', B2:'event-b2' };
    return m[type] ?? '';
  }
}
