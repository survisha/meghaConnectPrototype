import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { Tag } from 'primeng/tag';
import { AiChatbotComponent } from '../ai-chatbot/ai-chatbot.component';

interface VisitorProfile {
  fullName: string;
  phoneNumber: string;
  kycType: string;
  kycVerified: boolean;
  address: string;
  district: string;
  kycStatus?: string;
  kycConfidence?: number;
}

interface VisitorCard { label: string; value: string | number; icon: string; color: string; bg: string; }
interface ListEntry { id: string; title: string; status: string; date: string; extra?: string; }

@Component({
  selector: 'app-visitor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Tag, AiChatbotComponent],
  templateUrl: './visitor-dashboard.component.html',
  styleUrls: ['./visitor-dashboard.component.scss'],
})
export class VisitorDashboardComponent implements OnInit {
  cards: VisitorCard[] = [];
  myAppointments: ListEntry[] = [];
  mySchemes: ListEntry[] = [];
  myGrievances: ListEntry[] = [];
  loading = false;

  visitorProfile: VisitorProfile | null = null;

  totalVisits = 0;

  constructor(public auth: AuthService, private http: HttpClient) {}

  ngOnInit() {
    this.cards = [
      { label: 'My Appointments', value: 0, icon: 'pi-calendar',              color: '#1a237e', bg: '#e8eaf6' },
      { label: 'Total Visits',    value: 0, icon: 'pi-map-marker',             color: '#065f46', bg: '#d1fae5' },
      { label: 'Active Schemes',  value: 0, icon: 'pi-briefcase',              color: '#b45309', bg: '#fef3c7' },
      { label: 'Grievances',      value: 0, icon: 'pi-comments',               color: '#dc2626', bg: '#fee2e2' },
    ];

    const visitorId = sessionStorage.getItem('megha_visitor_id');
    if (visitorId) {
      this.loadProfile(visitorId);
    }
  }

  private loadProfile(visitorId: string) {
    this.loading = true;
    this.http.get<VisitorProfile & { success: boolean }>(`/api/v1/visitor/auth/profile/${visitorId}`).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.visitorProfile = res;
        }
      },
      error: () => { this.loading = false; }
    });
  }

  getStatusSeverity(s: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined {
    const m: Record<string, 'success' | 'info' | 'warn' | 'danger' | 'secondary'> = {
      SUBMITTED: 'info', CMO_REVIEW: 'warn', HCM_PENDING: 'danger', SCHEDULED: 'warn',
      COMPLETED: 'success', CANCELLED: 'secondary', UNDER_REVIEW: 'warn',
      APPROVED: 'success', REJECTED: 'danger', FORWARDED: 'warn', RESOLVED: 'success', CLOSED: 'secondary',
    };
    return m[s] ?? 'info';
  }

  /** R009: Get KYC confidence display */
  get kycConfidenceDisplay(): { score: number; label: string; color: string } | null {
    if (!this.visitorProfile?.kycStatus) return null;
    if (this.visitorProfile.kycStatus === 'PHOTO_MATCHED') {
      return { score: this.visitorProfile.kycConfidence ?? 92, label: 'Verified', color: '#16a34a' };
    }
    if (this.visitorProfile.kycStatus === 'DEMOGRAPHIC_MATCHED') {
      return { score: this.visitorProfile.kycConfidence ?? 75, label: 'Verified (Demographic)', color: '#ca8a04' };
    }
    if (this.visitorProfile.kycStatus === 'MANUAL_VERIFICATION_REQUIRED') {
      return { score: this.visitorProfile.kycConfidence ?? 45, label: 'Manual Verification Required', color: '#dc2626' };
    }
    return null;
  }
}
