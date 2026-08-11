import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GrievanceService } from '../services/grievance.service';
import { VisitorService } from '../services/visitor.service';
import { AuthService } from '../services/auth.service';
import { Grievance, GrievanceCategory, GrievanceStatus, Visitor } from '../models';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatStepperModule } from '@angular/material/stepper';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { apiErrorMessage } from '../shared/api-error.util';

@Component({
  selector: 'app-grievances',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDialogModule,
    MatStepperModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule
  ],
  templateUrl: './grievances.component.html',
  styleUrls: ['./grievances.component.scss'],
})
export class GrievancesComponent implements OnInit {
  grievances: Grievance[] = [];
  filtered: Grievance[] = [];
  search = '';
  filterStatus = '';
  filterCategory = '';
  loading = false;
  errorMsg = '';

  // Mat-table columns
  private readonly staffColumns: string[] = ['ticketId', 'applicant', 'district', 'category', 'subject', 'submitted', 'status', 'actions'];
  private readonly publicColumns: string[] = ['ticketId', 'subject', 'submitted', 'status', 'actions'];
  displayedColumns: string[] = this.staffColumns;

  showForm = false;
  showDetail = false;
  selectedGrievance: Grievance | null = null;
  editingGrievance: Grievance | null = null;
  visitorProfile: Visitor | null = null;
  visitorId: number | null = null;

  step = 0;
  formSteps = [{ label: 'Personal Info' }, { label: 'Grievance Details' }, { label: 'Review & Submit' }];

  form = {
    applicantName: '', phoneNumber: '', district: '', constituency: '', designation: '',
    subject: '', description: '',
  };

  statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'Acknowledged', value: 'ACKNOWLEDGED' },
    { label: 'Under Review', value: 'UNDER_REVIEW' },
    { label: 'Forwarded', value: 'FORWARDED' },
    { label: 'Resolved', value: 'RESOLVED' },
    { label: 'Closed', value: 'CLOSED' },
  ];

  categoryOptions = [
    { label: 'All Categories', value: '' },
    { label: 'Public Services', value: 'PUBLIC_SERVICES' },
    { label: 'Infrastructure', value: 'INFRASTRUCTURE' },
    { label: 'Health', value: 'HEALTH' },
    { label: 'Education', value: 'EDUCATION' },
    { label: 'Employment', value: 'EMPLOYMENT' },
    { label: 'Welfare Scheme', value: 'WELFARE_SCHEME' },
    { label: 'Law & Order', value: 'LAW_ORDER' },
    { label: 'Others', value: 'OTHERS' },
  ];

  districts = ['East Khasi Hills', 'West Khasi Hills', 'South West Khasi Hills', 'Ri Bhoi',
    'East Jaintia Hills', 'West Jaintia Hills', 'East Garo Hills', 'West Garo Hills',
    'South Garo Hills', 'North Garo Hills', 'Eastern West Khasi Hills'];

  constructor(
    private grievanceService: GrievanceService,
    private visitorService: VisitorService,
    public auth: AuthService
  ) {}

  ngOnInit() {
    this.displayedColumns = this.isPublic ? this.publicColumns : this.staffColumns;
    if (this.isPublic) {
      this.visitorId = this.resolveVisitorId();
      this.applyVisitorProfileToForm();
      if (this.visitorId) {
        this.loadVisitorProfile(this.visitorId);
        this.loadVisitorGrievances(this.visitorId);
      }
      return;
    }

    if (!this.isStaff) {
      return;
    }

    this.loadAllGrievances();
  }

  private loadAllGrievances() {
    this.loading = true;
    this.grievanceService.getAll(0, 100).subscribe({
      next: page => {
        this.grievances = page.content;
        this.applyFilter();
        this.errorMsg = '';
        this.loading = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load grievances.');
        this.loading = false;
      }
    });
  }

  private loadVisitorGrievances(visitorId: number) {
    this.loading = true;
    this.grievanceService.getByVisitor(visitorId, 0, 100).subscribe({
      next: page => {
        this.grievances = page.content;
        this.applyFilter();
        this.errorMsg = '';
        this.loading = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load your grievances.');
        this.loading = false;
      }
    });
  }

  private loadVisitorProfile(visitorId: number) {
    this.visitorService.getById(visitorId).subscribe({
      next: profile => {
        this.visitorProfile = profile;
        this.applyVisitorProfileToForm();
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load visitor profile.');
        this.applyVisitorProfileToForm();
      }
    });
  }

  applyFilter() {
    const data = this.grievances;

    // Apply search and filter criteria
    const q = this.search.trim().toLowerCase();
    this.filtered = data.filter(g =>
      (!q || this.matchesSearch(g, q)) &&
      (!this.filterStatus || g.status === this.filterStatus) &&
      (!this.filterCategory || g.category === this.filterCategory)
    );
  }

  openDetail(g: Grievance) {
    this.grievanceService.getById(g.id).subscribe({
      next: detail => {
        this.selectedGrievance = detail ?? g;
        this.errorMsg = '';
        this.showDetail = true;
      },
      error: err => this.errorMsg = apiErrorMessage(err, 'Unable to load grievance details.')
    });
  }

  openCreateForm() {
    this.editingGrievance = null;
    this.step = 0;
    this.errorMsg = '';
    this.resetForm();
    this.showForm = true;
  }

  openEdit(g: Grievance) {
    if (!this.canModify(g)) {
      return;
    }
    this.grievanceService.getById(g.id).subscribe({
      next: detail => {
        const grievance = detail ?? g;
        this.editingGrievance = grievance;
        this.errorMsg = '';
        this.resetForm();
        this.form.subject = grievance.subject;
        this.form.description = grievance.description;
        this.step = 1;
        this.showForm = true;
      },
      error: err => this.errorMsg = apiErrorMessage(err, 'Unable to load grievance details.')
    });
  }

  closeForm() {
    this.showForm = false;
    this.editingGrievance = null;
    this.step = 0;
    this.resetForm();
  }

  nextStep() {
    if (this.step === 0 && (!this.form.applicantName.trim() || !this.form.phoneNumber.trim())) {
      return;
    }
    if (this.step === 1 && (!this.form.subject.trim() || !this.form.description.trim())) {
      return;
    }
    if (this.step < this.formSteps.length - 1) this.step++;
  }
  prevStep() { if (this.step > 0) this.step--; }

  submitGrievance() {
    const request = {
      visitorId: this.visitorId ?? undefined,
      subject: this.form.subject,
      description: this.form.description,
    };

    if (this.editingGrievance) {
      this.grievanceService.update(this.editingGrievance.id, request).subscribe({
        next: updated => {
          const idx = this.grievances.findIndex(x => x.id === updated.id);
          if (idx >= 0) this.grievances[idx] = updated;
          this.applyFilter();
          this.errorMsg = '';
          this.closeForm();
        },
        error: err => this.errorMsg = apiErrorMessage(err, 'Failed to update grievance.')
      });
      return;
    }

    this.grievanceService.create(request).subscribe({
      next: newGrievance => {
        this.grievances = [newGrievance, ...this.grievances];
        this.applyFilter();
        this.errorMsg = '';
        this.closeForm();
      },
      error: err => this.errorMsg = apiErrorMessage(err, 'Failed to submit grievance.')
    });
  }

  updateStatus(g: Grievance, status: GrievanceStatus) {
    this.grievanceService.updateStatus(g.id, status).subscribe({
      next: updated => {
        const idx = this.grievances.findIndex(x => x.id === updated.id);
        if (idx >= 0) this.grievances[idx] = updated;
        this.applyFilter();
        this.errorMsg = '';
        this.showDetail = false;
      },
      error: err => this.errorMsg = apiErrorMessage(err, 'Failed to update status.')
    });
  }

  deleteGrievance(g: Grievance) {
    if (!this.canModify(g) || !window.confirm(`Delete grievance ${g.ticketId}?`)) {
      return;
    }
    this.grievanceService.delete(g.id).subscribe({
      next: () => {
        this.grievances = this.grievances.filter(x => x.id !== g.id);
        this.applyFilter();
        this.errorMsg = '';
      },
      error: err => this.errorMsg = apiErrorMessage(err, 'Failed to delete grievance.')
    });
  }

  canModify(g: Grievance): boolean {
    return this.isPublic && g.status !== 'RESOLVED' && g.status !== 'CLOSED';
  }

  getStatusSeverity(s: GrievanceStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined {
    const m: Record<GrievanceStatus, 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast'> = {
      SUBMITTED: 'info', ACKNOWLEDGED: 'info', UNDER_REVIEW: 'warn',
      FORWARDED: 'warn', RESOLVED: 'success', CLOSED: 'secondary',
    };
    return m[s] ?? 'info';
  }

  getStatusColor(s: GrievanceStatus): string {
    const colorMap: Record<GrievanceStatus, string> = {
      SUBMITTED: '#3b82f6',
      ACKNOWLEDGED: '#6366f1',
      UNDER_REVIEW: '#f59e0b',
      FORWARDED: '#f97316',
      RESOLVED: '#10b981',
      CLOSED: '#6b7280',
    };
    return colorMap[s] ?? '#6b7280';
  }

  getCategoryLabel(c?: GrievanceCategory | ''): string {
    if (!c) return '—';
    const m: Record<GrievanceCategory, string> = {
      PUBLIC_SERVICES: 'Public Services', INFRASTRUCTURE: 'Infrastructure',
      HEALTH: 'Health', EDUCATION: 'Education', EMPLOYMENT: 'Employment',
      WELFARE_SCHEME: 'Welfare Scheme', LAW_ORDER: 'Law & Order', OTHERS: 'Others',
    };
    return m[c] ?? c;
  }

  private resolveVisitorId(): number | null {
    const fromAuth = this.auth.user()?.visitorId;
    if (fromAuth) {
      return fromAuth;
    }
    const stored = sessionStorage.getItem('megha_visitor_id');
    const parsed = stored ? Number(stored) : NaN;
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private resetForm() {
    this.form = {
      applicantName: '',
      phoneNumber: '',
      district: '',
      constituency: '',
      designation: '',
      subject: '',
      description: '',
    };
    this.applyVisitorProfileToForm();
  }

  private applyVisitorProfileToForm() {
    const subject = this.form.subject;
    const description = this.form.description;
    const user = this.auth.user();
    this.form = {
      applicantName: this.visitorProfile?.fullName || user?.fullName || '',
      phoneNumber: this.visitorProfile?.phoneNumber || user?.username || '',
      district: this.visitorProfile?.district || '',
      constituency: this.visitorProfile?.constituency || '',
      designation: this.visitorProfile?.designation || '',
      subject,
      description,
    };
  }

  private matchesSearch(g: Grievance, q: string): boolean {
    return [
      g.applicantName,
      g.phoneNumber,
      g.ticketId,
      g.subject,
      g.description,
      g.district,
      g.constituency,
      g.visitorDesignation,
    ].some(value => (value ?? '').toLowerCase().includes(q));
  }

  get isStaff() { return this.auth.hasRole('HCM', 'ADMIN', 'APPROVER', 'DEO'); }
  get isPublic() { return this.auth.hasRole('PUBLIC'); }
}
