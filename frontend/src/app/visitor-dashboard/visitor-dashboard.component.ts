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
interface ListEntry { id: string; title: string; status: string; date: string; extra?: string; }

@Component({
  selector: 'app-visitor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Button, Tag, Timeline, Divider],
  templateUrl: './visitor-dashboard.component.html',
  styleUrls: ['./visitor-dashboard.component.scss'],
})
export class VisitorDashboardComponent implements OnInit {
  cards: VisitorCard[] = [];
  myAppointments: ListEntry[] = [];
  mySchemes: ListEntry[] = [];
  myGrievances: ListEntry[] = [];

  statusTimeline = [
    { label: 'Application Submitted', date: '10 Mar 2024', icon: 'pi pi-send', color: '#1a237e' },
    { label: 'CMO Verification', date: '11 Mar 2024', icon: 'pi pi-eye', color: '#b45309' },
    { label: 'Approver Review', date: '12 Mar 2024', icon: 'pi pi-check', color: '#1565c0' },
    { label: 'HCM Decision Pending', date: '–', icon: 'pi pi-clock', color: '#dc2626' },
  ];

  constructor(public auth: AuthService, public mock: MockDataService) {}

  ngOnInit() {
    this.myAppointments = this.mock.appointments.slice(0, 2).map(a => ({
      id: a.applicationId,
      title: a.agendaBrief.substring(0, 60) + (a.agendaBrief.length > 60 ? '…' : ''),
      status: a.status,
      date: a.scheduledDateTime ? new Date(a.scheduledDateTime).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Pending',
    }));

    this.mySchemes = this.mock.schemeApplications.slice(0, 2).map(s => ({
      id: `SC-2024-${String(s.id).padStart(4, '0')}`,
      title: s.projectName,
      status: s.status,
      date: s.schemeType,
      extra: `₹${(s.hcmApprovedCost ?? s.estimatedCost).toLocaleString('en-IN')}`,
    }));

    this.myGrievances = this.mock.grievances.slice(0, 2).map(g => ({
      id: g.ticketId,
      title: g.subject,
      status: g.status,
      date: new Date(g.submittedAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }),
    }));

    this.cards = [
      { label: 'My Appointments', value: this.myAppointments.length, icon: 'pi-calendar', color: '#1a237e', bg: '#e8eaf6' },
      { label: 'Scheme Applications', value: this.mySchemes.length, icon: 'pi-briefcase', color: '#065f46', bg: '#d1fae5' },
      { label: 'Grievances Raised', value: this.myGrievances.length, icon: 'pi-comments', color: '#b45309', bg: '#fef3c7' },
      { label: 'Pending Actions', value: this.mock.appointments.filter(a => a.status === 'SUBMITTED' || a.status === 'HCM_PENDING').length, icon: 'pi-exclamation-triangle', color: '#dc2626', bg: '#fee2e2' },
    ];
  }

  getStatusSeverity(s: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | null | undefined {
    const m: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
      SUBMITTED: 'info', CMO_REVIEW: 'warn', HCM_PENDING: 'danger', SCHEDULED: 'warn',
      COMPLETED: 'success', CANCELLED: 'secondary', UNDER_REVIEW: 'warn',
      APPROVED: 'success', REJECTED: 'danger', FORWARDED: 'warn', RESOLVED: 'success', CLOSED: 'secondary',
    };
    return m[s] ?? 'info';
  }
}
