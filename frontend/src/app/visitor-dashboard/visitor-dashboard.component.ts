import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MockDataService } from '../services/mock-data.service';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { Timeline } from 'primeng/timeline';
import { Divider } from 'primeng/divider';

interface VisitorCard { label: string; value: string | number; icon: string; color: string; bg: string; }

@Component({
  selector: 'app-visitor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Button, Tag, Timeline, Divider],
  templateUrl: './visitor-dashboard.component.html',
  styleUrls: ['./visitor-dashboard.component.scss'],
})
export class VisitorDashboardComponent implements OnInit {
  cards: VisitorCard[] = [];

  myAppointments = [
    { id: 'MC-2024-00042', agenda: 'CMSDF Application – Community Road', status: 'HCM_PENDING', date: '15 Mar 2024' },
    { id: 'MC-2024-00031', agenda: 'Public Grievance – Water Supply', status: 'COMPLETED', date: '20 Feb 2024' },
  ];

  mySchemes = [
    { id: 'SC-2024-0012', scheme: 'CMSDF', project: 'Community Hall – Dalu', status: 'UNDER_REVIEW', amount: '₹2.5L' },
    { id: 'SC-2024-0003', scheme: 'CM Care', project: 'Medical Assistance', status: 'APPROVED', amount: '₹50K' },
  ];

  myGrievances = [
    { id: 'GRV-2024-005', subject: 'Road repair request', status: 'FORWARDED', date: '10 Mar 2024' },
  ];

  statusTimeline = [
    { label: 'Application Submitted', date: '10 Mar 2024', icon: 'pi pi-send', color: '#1a237e' },
    { label: 'CMO Verification', date: '11 Mar 2024', icon: 'pi pi-eye', color: '#b45309' },
    { label: 'Approver Review', date: '12 Mar 2024', icon: 'pi pi-check', color: '#1565c0' },
    { label: 'HCM Decision Pending', date: '–', icon: 'pi pi-clock', color: '#dc2626' },
  ];

  constructor(public auth: AuthService, public mock: MockDataService) {}

  ngOnInit() {
    this.cards = [
      { label: 'My Appointments', value: this.myAppointments.length, icon: 'pi-calendar', color: '#1a237e', bg: '#e8eaf6' },
      { label: 'Scheme Applications', value: this.mySchemes.length, icon: 'pi-briefcase', color: '#065f46', bg: '#d1fae5' },
      { label: 'Grievances Raised', value: this.myGrievances.length, icon: 'pi-comments', color: '#b45309', bg: '#fef3c7' },
      { label: 'Pending Actions', value: 1, icon: 'pi-exclamation-triangle', color: '#dc2626', bg: '#fee2e2' },
    ];
  }

  getAppointmentSeverity(s: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | null | undefined {
    const m: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
      SUBMITTED: 'info', CMO_REVIEW: 'warn', HCM_PENDING: 'danger', SCHEDULED: 'warn', COMPLETED: 'success', CANCELLED: 'secondary',
    };
    return m[s] ?? 'info';
  }

  getSchemeSeverity(s: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | null | undefined {
    const m: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
      SUBMITTED: 'info', UNDER_REVIEW: 'warn', APPROVED: 'success', REJECTED: 'danger',
    };
    return m[s] ?? 'info';
  }

  getGrievanceSeverity(s: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | null | undefined {
    const m: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
      SUBMITTED: 'info', UNDER_REVIEW: 'warn', FORWARDED: 'warn', RESOLVED: 'success', CLOSED: 'secondary',
    };
    return m[s] ?? 'info';
  }
}
