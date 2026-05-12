import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { AppointmentService } from '../services/appointment.service';
import { GrievanceService } from '../services/grievance.service';
import { Tag } from 'primeng/tag';
import { AiChatbotComponent } from '../ai-chatbot/ai-chatbot.component';
import { MatIconModule } from '@angular/material/icon';
import { apiErrorMessage } from '../shared/api-error.util';

interface VisitorProfile {
  fullName: string;
  phoneNumber: string;
  designation?: string;
  kycType: string;
  kycVerified: boolean;
  address: string;
  district: string;
  kycStatus?: string;
  kycConfidence?: number;
  livePhotoPath?: string;
  photoStoragePath?: string;
  photoPath?: string;
  livePhotoBase64?: string;
  photoBase64?: string;
  photoUrl?: string;
}

interface VisitorCard { label: string; value: string | number; icon: string; color: string; bg: string; }
interface ListEntry { id: string; title: string; status: string; date: string; extra?: string; }

@Component({
  selector: 'app-visitor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Tag, AiChatbotComponent, MatIconModule],
  templateUrl: './visitor-dashboard.component.html',
  styleUrls: ['./visitor-dashboard.component.scss'],
})
export class VisitorDashboardComponent implements OnInit {
  cards: VisitorCard[] = [];
  myAppointments: ListEntry[] = [];
  mySchemes: ListEntry[] = [];
  myGrievances: ListEntry[] = [];
  loading = false;
  errorMsg = '';

  visitorProfile: VisitorProfile | null = null;
  visitorPhotoUrl = '';
  photoLoadFailed = false;
  photoPreviewOpen = false;

  totalVisits = 0;

  constructor(
    public auth: AuthService,
    private http: HttpClient,
    private appointmentService: AppointmentService,
    private grievanceService: GrievanceService
  ) {}

  ngOnInit() {
    const user = this.auth.user();
    this.visitorProfile = {
      fullName: user?.fullName ?? 'Visitor',
      phoneNumber: user?.username ?? '',
      address: '',
      district: '',
      kycType: 'NONE',
      kycVerified: false,
      kycStatus: 'PENDING',
      kycConfidence: 0,
      livePhotoPath: ''
    };
    this.updateVisitorPhotoUrl();
    this.myAppointments = [];
    this.mySchemes = [];
    this.myGrievances = [];
    this.totalVisits = 0;
    this.updateCards();

    const visitorId = sessionStorage.getItem('megha_visitor_id');
    if (visitorId) {
      this.loadProfile(visitorId);
      this.loadGrievances(Number(visitorId));
    }
    this.loadAppointments();
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
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load visitor profile.');
        this.loading = false;
      }
    });
  }

  private loadAppointments() {
    this.loading = true;
    this.appointmentService.getMyAppointments().subscribe({
      next: appointments => {
        this.myAppointments = appointments.map(a => ({
          id: a.applicationId,
          title: a.subject || a.agendaBrief || a.agendaType || 'Appointment request',
          status: a.status,
          date: this.formatDate(a.scheduledDateTime || a.submittedAt || a.createdAt),
        }));
        this.totalVisits = this.myAppointments.length;
        this.updateCards();
        this.loading = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load your appointments.');
        this.myAppointments = [];
        this.totalVisits = 0;
        this.updateCards();
        this.loading = false;
      }
    });
  }

  private loadGrievances(visitorId: number) {
    this.grievanceService.getByVisitor(visitorId, 0, 100).subscribe({
      next: page => {
        this.myGrievances = page.content.map(g => ({
          id: g.ticketId,
          title: g.subject,
          status: g.status,
          date: this.formatDate(g.submittedAt),
        }));
        this.updateCards();
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load your grievances.');
        this.myGrievances = [];
        this.updateCards();
      }
    });
  }

  private updateCards() {
    this.cards = [
      { label: 'My Appointments', value: this.myAppointments.length, icon: 'event', color: '#1a237e', bg: '#e8eaf6' },
      { label: 'Total Visits', value: this.totalVisits, icon: 'place', color: '#065f46', bg: '#d1fae5' },
      { label: 'Active Schemes', value: this.mySchemes.length, icon: 'work', color: '#b45309', bg: '#fef3c7' },
      { label: 'Grievances', value: this.myGrievances.length, icon: 'chat', color: '#dc2626', bg: '#fee2e2' },
    ];
  }

  private formatDate(value?: string) {
    if (!value) return 'Not scheduled';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
  }

  private updateVisitorPhotoUrl() {
    this.photoLoadFailed = false;
    this.photoPreviewOpen = false;
    const inlinePhoto = (
      this.visitorProfile?.livePhotoBase64
      || this.visitorProfile?.photoBase64
      || this.visitorProfile?.photoUrl
      || ''
    ).trim();
    if (inlinePhoto) {
      this.visitorPhotoUrl = this.normalizePhotoSource(inlinePhoto);
      return;
    }

    const path = (
      this.visitorProfile?.livePhotoPath
      || this.visitorProfile?.photoStoragePath
      || this.visitorProfile?.photoPath
      || ''
    ).trim();
    if (!path) {
      this.visitorPhotoUrl = '';
      return;
    }

    this.visitorPhotoUrl = this.normalizePhotoSource(path);
  }

  private normalizePhotoSource(value: string): string {
    const source = value.trim();
    if (!source) {
      return '';
    }
    if (source.startsWith('data:image/') || source.startsWith('blob:') || /^https?:\/\//i.test(source)) {
      return source;
    }

    // Best-effort: apiUrl is usually `${origin}/api/v1`, while uploads live at `${origin}/uploads`.
    const origin = environment.apiUrl.replace(/\/api\/v1\/?$/i, '');
    const cleanPath = source.replace(/^\/+/, '');
    return `${origin}/${cleanPath.startsWith('uploads/') ? cleanPath : `uploads/${cleanPath}`}`;
  }

  get canViewPhoto(): boolean {
    return !!this.visitorPhotoUrl && !this.photoLoadFailed;
  }

  togglePhotoPreview(): void {
    if (!this.canViewPhoto) {
      return;
    }
    this.photoPreviewOpen = !this.photoPreviewOpen;
  }

  closePhotoPreview(): void {
    this.photoPreviewOpen = false;
  }

  onPhotoLoadError(): void {
    this.photoLoadFailed = true;
    this.photoPreviewOpen = false;
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
