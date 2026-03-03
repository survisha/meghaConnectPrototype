import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Tag } from 'primeng/tag';
import { Timeline } from 'primeng/timeline';

interface VisitorCard { label: string; value: string | number; icon: string; color: string; bg: string; }
interface ListEntry { id: string; title: string; status: string; date: string; extra?: string; }

@Component({
  selector: 'app-visitor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Tag, Timeline],
  templateUrl: './visitor-dashboard.component.html',
  styleUrls: ['./visitor-dashboard.component.scss'],
})
export class VisitorDashboardComponent implements OnInit {
  cards: VisitorCard[] = [];
  myAppointments: ListEntry[] = [];
  mySchemes: ListEntry[] = [];
  myGrievances: ListEntry[] = [];
  loading = false;

  statusTimeline = [
    { label: 'Application Submitted', date: '–', icon: 'pi pi-send', color: '#1a237e' },
    { label: 'CMO Verification', date: '–', icon: 'pi pi-eye', color: '#b45309' },
    { label: 'Approver Review', date: '–', icon: 'pi pi-check', color: '#1565c0' },
    { label: 'HCM Decision Pending', date: '–', icon: 'pi pi-clock', color: '#dc2626' },
  ];

  constructor(public auth: AuthService) {}

  ngOnInit() {
    this.cards = [
      { label: 'My Appointments', value: 0, icon: 'pi-calendar', color: '#1a237e', bg: '#e8eaf6' },
      { label: 'Scheme Applications', value: 0, icon: 'pi-briefcase', color: '#065f46', bg: '#d1fae5' },
      { label: 'Grievances Raised', value: 0, icon: 'pi-comments', color: '#b45309', bg: '#fef3c7' },
      { label: 'Pending Actions', value: 0, icon: 'pi-exclamation-triangle', color: '#dc2626', bg: '#fee2e2' },
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
