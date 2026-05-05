import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
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
  livePhotoPath?: string;
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
  visitorPhotoUrl = '';
  photoLoadFailed = false;

  totalVisits = 0;

  constructor(public auth: AuthService, private http: HttpClient) {}

  ngOnInit() {
    // Initialize dummy data for demo purposes
    this.visitorProfile = {
      fullName: 'Rajesh Kumar Sharma',
      phoneNumber: '+91-9876543210',
      address: 'Ward No. 5, Police Bazar Road',
      district: 'East Khasi Hills',
      kycType: 'AADHAAR',
      kycVerified: true,
      kycStatus: 'PHOTO_MATCHED',
      kycConfidence: 94,
      livePhotoPath: ''
    };
    this.updateVisitorPhotoUrl();

    this.myAppointments = [
      { id: 'APT-2024-001', title: 'Meeting with CM regarding Road Development', status: 'SCHEDULED', date: '2024-03-28 11:00 AM' },
      { id: 'APT-2024-002', title: 'Discussion on Education Policy', status: 'COMPLETED', date: '2024-03-15 10:00 AM' },
      { id: 'APT-2024-003', title: 'Healthcare Infrastructure Proposal', status: 'CMO_REVIEW', date: '2024-03-20 02:00 PM' }
    ];

    this.mySchemes = [
      { id: 'SCH-2024-045', title: 'CM Self-Development Fund', status: 'APPROVED', date: '2024-03-10', extra: '₹50,000' },
      { id: 'SCH-2024-078', title: 'CM Care Program', status: 'UNDER_REVIEW', date: '2024-03-22', extra: '₹25,000' },
      { id: 'SCH-2024-091', title: 'CM Elevate Scholarship', status: 'APPROVED', date: '2024-02-28', extra: '₹15,000' }
    ];

    this.myGrievances = [
      { id: 'GRV-2024-012', title: 'Water Supply Issue in Ward 5', status: 'RESOLVED', date: '2024-03-05' },
      { id: 'GRV-2024-034', title: 'Street Light Maintenance Request', status: 'UNDER_REVIEW', date: '2024-03-18' }
    ];

    this.totalVisits = 7;

    this.cards = [
      { label: 'My Appointments', value: this.myAppointments.length, icon: 'pi-calendar',              color: '#1a237e', bg: '#e8eaf6' },
      { label: 'Total Visits',    value: this.totalVisits, icon: 'pi-map-marker',             color: '#065f46', bg: '#d1fae5' },
      { label: 'Active Schemes',  value: this.mySchemes.length, icon: 'pi-briefcase',              color: '#b45309', bg: '#fef3c7' },
      { label: 'Grievances',      value: this.myGrievances.length, icon: 'pi-comments',               color: '#dc2626', bg: '#fee2e2' },
    ];

    const visitorId = sessionStorage.getItem('megha_visitor_id');
    if (visitorId) {
      this.loadProfile(visitorId);
    }
  }

  private loadProfile(visitorId: string) {
    this.loading = true;
    this.http.get<VisitorProfile & { success: boolean }>(`${environment.apiUrl}/visitor/auth/profile/${visitorId}`).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.visitorProfile = res;
          this.updateVisitorPhotoUrl();
        }
      },
      error: () => { this.loading = false; }
    });
  }

  private updateVisitorPhotoUrl() {
    this.photoLoadFailed = false;
    const path = (this.visitorProfile?.livePhotoPath || '').trim();
    if (!path) {
      this.visitorPhotoUrl = '';
      return;
    }

    // Best-effort: apiUrl is usually `${origin}/api/v1`, while uploads live at `${origin}/uploads`.
    const origin = environment.apiUrl.replace(/\/api\/v1\/?$/i, '');
    this.visitorPhotoUrl = `${origin}/uploads/${path}`;
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
