import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { AppointmentService } from '../services/appointment.service';
import { GrievanceService } from '../services/grievance.service';
import { VisitorKycService } from '../services/visitor-kyc.service';
import { SchemeService } from '../services/scheme.service';
import { Appointment, SchemeApplication } from '../models';
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
  kycProvider?: string;
  kycFailureReason?: string;
  kycRequestId?: string;
  livePhotoPath?: string;
  photoStoragePath?: string;
  photoPath?: string;
  livePhotoBase64?: string;
  photoBase64?: string;
  photoUrl?: string;
}

interface VisitorCard { label: string; value: string | number; icon: string; color: string; bg: string; }
interface ListEntry { id: string; title: string; status: string; date: string; extra?: string; }
interface SchemeEntry extends ListEntry {
  application: SchemeApplication;
  applicationNumber: string;
  submittedDate: string;
}

type VisitorSchemeApplication = SchemeApplication & {
  applicationNumber?: string;
  applicationId?: string | number;
  schemeName?: string;
  schemeCode?: string;
  remarks?: string;
  submittedDate?: string;
  lastUpdated?: string;
};

@Component({
  selector: 'app-visitor-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Tag, AiChatbotComponent, MatIconModule],
  templateUrl: './visitor-dashboard.component.html',
  styleUrls: ['./visitor-dashboard.component.scss'],
})
export class VisitorDashboardComponent implements OnInit {
  cards: VisitorCard[] = [];
  myAppointments: Appointment[] = [];
  mySchemes: SchemeEntry[] = [];
  myGrievances: ListEntry[] = [];
  loading = false;
  errorMsg = '';
  successMsg = '';
  selectedAppointment: Appointment | null = null;
  selectedScheme: SchemeEntry | null = null;
  downloadingPassId: number | null = null;
  retryingKyc = false;
  kycRetryOpen = false;
  kycRetrySubmitted = false;
  kycVerifiedGraphic = false;
  kycRetryForm = {
    name: '',
    epicNumber: '',
  };

  visitorProfile: VisitorProfile | null = null;
  visitorPhotoUrl = '';
  photoLoadFailed = false;
  photoPreviewOpen = false;

  totalVisits = 0;

  constructor(
    public auth: AuthService,
    private http: HttpClient,
    private appointmentService: AppointmentService,
    private grievanceService: GrievanceService,
    private visitorKycService: VisitorKycService,
    private schemeService: SchemeService
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
      this.loadSchemes(Number(visitorId));
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
        this.myAppointments = appointments;
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

  private loadSchemes(visitorId: number) {
    if (!visitorId) {
      this.mySchemes = [];
      this.updateCards();
      return;
    }

    this.schemeService.getApplicationsByVisitor(visitorId).subscribe({
      next: applications => {
        this.mySchemes = (applications || []).map(application => this.toSchemeEntry(application));
        this.updateCards();
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load your scheme applications.');
        this.mySchemes = [];
        this.updateCards();
      }
    });
  }

  private toSchemeEntry(application: SchemeApplication): SchemeEntry {
    const scheme = application as VisitorSchemeApplication;
    const applicationNumber = this.schemeApplicationNumber(scheme);
    return {
      id: applicationNumber,
      applicationNumber,
      title: this.schemeName(scheme),
      status: scheme.status || 'SUBMITTED',
      date: this.formatDate(scheme.submittedDate || scheme.createdAt),
      submittedDate: this.formatDate(scheme.submittedDate || scheme.createdAt),
      extra: scheme.projectName || undefined,
      application,
    };
  }

  private updateCards() {
    this.cards = [
      { label: 'My Appointments', value: this.myAppointments.length, icon: 'event', color: '#1a237e', bg: '#e8eaf6' },
      { label: 'Total Visits', value: this.totalVisits, icon: 'place', color: '#065f46', bg: '#d1fae5' },
      { label: 'Active Schemes', value: this.mySchemes.length, icon: 'work', color: '#b45309', bg: '#fef3c7' },
      { label: 'Grievances', value: this.myGrievances.length, icon: 'chat', color: '#dc2626', bg: '#fee2e2' },
    ];
  }

  formatDate(value?: string) {
    if (!value) return 'Not scheduled';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
  }

  appointmentTitle(appointment: Appointment): string {
    return appointment.subject || appointment.agendaBrief || appointment.agendaType || 'Appointment request';
  }

  displayStatus(appointment: Appointment): string {
    if (appointment.status === 'APPROVER_REVIEW') {
      return 'Under Review';
    }
    if (appointment.status === 'APPROVED' && !appointment.scheduledDateTime) {
      return 'Approved - Waiting for Schedule';
    }
    return appointment.status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }

  canDownloadPass(appointment: Appointment): boolean {
    if (!appointment?.scheduledDateTime) {
      return false;
    }
    return ['SCHEDULED', 'HCM_ACCEPTED', 'APPROVED', 'APPROVED_WITH_DATE_TIME', 'SCHEDULED_FOR_PUBLIC_DARBAR']
      .includes(appointment.status);
  }

  viewAppointment(appointment: Appointment): void {
    this.selectedAppointment = appointment;
  }

  closeAppointmentDetails(): void {
    this.selectedAppointment = null;
  }

  viewScheme(scheme: SchemeEntry): void {
    this.selectedScheme = scheme;
  }

  closeSchemeDetails(): void {
    this.selectedScheme = null;
  }

  schemeApplicationNumber(application: SchemeApplication | null | undefined): string {
    const value = application as VisitorSchemeApplication | null | undefined;
    return String(value?.applicationNumber || value?.applicationId || application?.id || '—');
  }

  schemeName(application: SchemeApplication | null | undefined): string {
    const value = application as VisitorSchemeApplication | null | undefined;
    const raw = value?.schemeName || value?.schemeCode || application?.schemeType || 'Scheme Application';
    return raw.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }

  schemeRemarks(application: SchemeApplication | null | undefined): string {
    const value = application as VisitorSchemeApplication | null | undefined;
    return value?.remarks || application?.justification || '—';
  }

  downloadVisitorPass(appointment: Appointment): void {
    if (!this.canDownloadPass(appointment) || this.downloadingPassId) {
      return;
    }
    this.errorMsg = '';
    this.successMsg = '';
    this.downloadingPassId = appointment.id;
    this.appointmentService.downloadVisitorPass(appointment.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `visitor-pass-${appointment.applicationId || appointment.id}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
        this.successMsg = 'Visitor pass downloaded successfully.';
        this.downloadingPassId = null;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to download visitor pass.');
        this.downloadingPassId = null;
      }
    });
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

  get isKycPending(): boolean {
    return this.visitorProfile?.kycStatus === 'KYC_PENDING'
      || (!this.visitorProfile?.kycVerified && this.visitorProfile?.kycStatus === 'PENDING');
  }

  get kycStatusLabel(): string {
    if (this.visitorProfile?.kycVerified) return 'VERIFIED';
    return (this.visitorProfile?.kycStatus || 'PENDING').replace(/_/g, ' ');
  }

  openKycRetryPanel(): void {
    this.errorMsg = '';
    this.successMsg = '';
    this.kycRetrySubmitted = false;
    this.kycRetryForm = {
      name: this.visitorProfile?.fullName || '',
      epicNumber: '',
    };
    this.kycRetryOpen = true;
  }

  closeKycRetryPanel(): void {
    if (this.retryingKyc) {
      return;
    }
    this.kycRetryOpen = false;
    this.kycRetrySubmitted = false;
  }

  onKycNameInput(): void {
    this.kycRetryForm.name = this.kycRetryForm.name.replace(/[^A-Za-z ]/g, '').replace(/\s{2,}/g, ' ');
  }

  onKycEpicInput(): void {
    const clean = this.kycRetryForm.epicNumber.toUpperCase().replace(/[^A-Z0-9]/g, '');
    let next = '';
    for (const char of clean) {
      if (next.length < 3 && /[A-Z]/.test(char)) {
        next += char;
      } else if (next.length >= 3 && next.length < 10 && /[0-9]/.test(char)) {
        next += char;
      }
      if (next.length === 10) break;
    }
    this.kycRetryForm.epicNumber = next;
  }

  get kycNameError(): string {
    const name = this.kycRetryForm.name.trim();
    if (!name) return 'Name is required.';
    if (!/^[A-Za-z ]+$/.test(name)) return 'Name should contain only letters and spaces.';
    return '';
  }

  get kycEpicError(): string {
    const epic = this.kycRetryForm.epicNumber.trim().toUpperCase();
    if (!epic) return 'EPIC number is required.';
    if (!/^[A-Z]{3}[0-9]{7}$/.test(epic)) return 'EPIC number must be 3 letters followed by 7 digits.';
    return '';
  }

  get isKycRetryFormValid(): boolean {
    return !this.kycNameError && !this.kycEpicError;
  }

  retryKycVerification(): void {
    const visitorId = Number(sessionStorage.getItem('megha_visitor_id') || 0);
    this.kycRetrySubmitted = true;
    this.onKycNameInput();
    this.onKycEpicInput();
    if (!visitorId || this.retryingKyc || !this.isKycRetryFormValid) {
      return;
    }
    this.errorMsg = '';
    this.successMsg = '';
    this.retryingKyc = true;
    this.visitorKycService.retryKyc(visitorId, {
      name: this.kycRetryForm.name.trim(),
      epicNumber: this.kycRetryForm.epicNumber.trim().toUpperCase(),
    }).subscribe({
      next: res => {
        this.retryingKyc = false;
        if (res.success) {
          if (res.profile) {
            this.visitorProfile = res.profile as unknown as VisitorProfile;
          } else if (this.visitorProfile) {
            this.visitorProfile = {
              ...this.visitorProfile,
              fullName: this.kycRetryForm.name.trim(),
              kycStatus: res.kycStatus,
              kycProvider: res.kycProvider,
              kycType: 'EPIC',
              kycVerified: true,
            };
          }
          this.kycRetryOpen = false;
          this.kycVerifiedGraphic = true;
          this.successMsg = res.message || 'KYC verification completed successfully.';
          this.loadProfile(String(visitorId));
          return;
        }
        this.errorMsg = res.message || 'Unable to verify EPIC details. Please check the EPIC number and name.';
      },
      error: err => {
        this.retryingKyc = false;
        this.errorMsg = apiErrorMessage(err, 'Unable to retry KYC verification.');
      }
    });
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
    if (this.visitorProfile.kycStatus === 'KYC_PENDING') {
      return { score: 0, label: 'KYC Pending', color: '#b45309' };
    }
    return null;
  }
}
