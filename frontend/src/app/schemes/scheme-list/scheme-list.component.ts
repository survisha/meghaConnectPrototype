import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ReferenceDataDto, SchemeService } from '../../services/scheme.service';
import { ReportAnalyticsService } from '../../services/report-analytics.service';
import { VisitorService } from '../../services/visitor.service';
import { SchemeApplication, SchemeType } from '../../models';
import { apiErrorMessage } from '../../shared/api-error.util';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../shared/toast/toast.service';
import { finalize } from 'rxjs/operators';

interface SchemeOption {
  label: string;
  value: '' | SchemeType | string;
}

type SortDirection = 'asc' | 'desc';
type SchemeSortColumn =
  | 'applicant'
  | 'scheme'
  | 'project'
  | 'category'
  | 'estCost'
  | 'hcmApproved'
  | 'status'
  | 'createdAt';

@Component({
  selector: 'app-scheme-list',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterLink, 
    MatTableModule, 
    MatButtonModule, 
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatDialogModule,
    MatDividerModule,
  ],
  templateUrl: './scheme-list.component.html',
  styleUrls: ['./scheme-list.component.scss'],
})
export class SchemeListComponent implements OnInit {
  schemes: SchemeApplication[] = [];
  selected: SchemeApplication | null = null;
  showDetail = false;
  filterScheme = '';
  loading = false;
  schemeOptionsLoading = false;
  errorMsg = '';
  selectedVisitorPhotoUrl = '';
  selectedVisitorPhotoLoading = false;
  selectedVisitorPhotoError = '';
  actionUpdating = false;
  displayedColumns: string[] = ['applicant', 'scheme', 'project', 'category', 'estCost', 'hcmApproved', 'createdAt', 'status', 'actions'];
  pageSizeOptions = [10, 25, 50];
  pageSize = 10;
  pageIndex = 0;
  sortColumn: SchemeSortColumn = 'createdAt';
  sortDirection: SortDirection = 'desc';

  schemeOptions: SchemeOption[] = [{ label: 'All Schemes', value: '' }];

  schemeStats: Array<{ name: string; code: string; total: number; approved: number; pending: number; rejected: number; budget: string }> = [];

  constructor(
    private schemeService: SchemeService,
    private reportAnalytics: ReportAnalyticsService,
    private visitorService: VisitorService,
    public auth: AuthService,
    private snackBar: ToastService,
  ) {}

  ngOnInit() {
    this.loadSchemeOptions();
    this.loadApplications();
    this.loadStatistics();
  }

  private loadApplications(): void {
    this.loading = true;
    this.schemeService.getAllApplications({ page: 0, size: 100 }).subscribe({
      next: (res: any) => {
        const content = res.content ?? res ?? [];
        
        this.schemes = content || [];
        this.errorMsg = '';
        this.loading = false;
      },
      error: (err) => { 
        this.errorMsg = apiErrorMessage(err, 'Unable to load scheme applications.');
        this.schemes = [];
        this.loading = false;
      }
    });
  }

  private loadSchemeOptions(): void {
    this.schemeOptionsLoading = true;
    this.schemeService.getSchemeTypes().subscribe({
      next: (data) => {
        const referenceOptions = this.toSchemeOptions(data);
        this.schemeOptions = [{ label: 'All Schemes', value: '' }, ...referenceOptions];
        if (this.filterScheme && !referenceOptions.some(option => option.value === this.filterScheme)) {
          this.filterScheme = '';
        }
        this.schemeOptionsLoading = false;
      },
      error: (err) => {
        console.warn('[SchemeListComponent] Failed to load CM_SCHEME reference data:', err);
        this.schemeOptionsLoading = false;
      }
    });
  }

  private toSchemeOptions(data: ReferenceDataDto[] | null | undefined): SchemeOption[] {
    const unique = new Map<string, SchemeOption>();
    (data ?? [])
      .filter(item => item?.code)
      .forEach(item => {
        unique.set(item.code, {
          value: item.code,
          label: item.value || this.formatSchemeName(item.code),
        });
      });
    return Array.from(unique.values());
  }

  private loadStatistics(): void {
    this.reportAnalytics.load().subscribe({
      next: analytics => {
        const grouped = new Map<string, { total: number; approved: number; rejected: number }>();
        analytics.schemeDistricts.forEach(row => {
          const value = grouped.get(row.scheme) ?? { total: 0, approved: 0, rejected: 0 };
          value.total += Number(row.total); value.approved += Number(row.approved); value.rejected += Number(row.rejected);
          grouped.set(row.scheme, value);
        });
        this.schemeStats = [...grouped.entries()].map(([code, value]) => ({
          name: this.formatSchemeName(code), code, total: value.total, approved: value.approved,
          rejected: value.rejected, pending: value.total - value.approved - value.rejected, budget: ''
        }));
      },
      error: () => { this.schemeStats = []; }
    });
  }

  formatSchemeName(schemeType: string): string {
    if (schemeType === 'CM_CARE') return 'CM Care';
    if (schemeType === 'CM_CONNECT') return 'CM Connect';
    if (schemeType === 'CM_ELEVATE') return 'CM Elevate';
    if (schemeType === 'FOCUS_PLUS') return 'Focus+';
    return schemeType;
  }

  get filtered() {
    return this.schemes.filter(s => !this.filterScheme || s.schemeType === this.filterScheme);
  }

  applicantName(scheme: SchemeApplication | null) {
    return scheme?.applicant?.fullName || scheme?.applicantName || '-';
  }

  applicantPhone(scheme: SchemeApplication | null) {
    return scheme?.applicant?.phoneNumber || '-';
  }

  applicantEpic(scheme: SchemeApplication | null) {
    return scheme?.applicant?.epicNumber || '-';
  }

  applicantConstituency(scheme: SchemeApplication | null) {
    return scheme?.applicant?.constituency || '-';
  }

  get sortedSchemes() {
    return [...this.filtered].sort((left, right) => this.compareValues(
      this.schemeSortValue(left, this.sortColumn),
      this.schemeSortValue(right, this.sortColumn)
    ) * (this.sortDirection === 'asc' ? 1 : -1));
  }

  get pagedSchemes() {
    const start = this.pageIndex * this.pageSize;
    return this.sortedSchemes.slice(start, start + this.pageSize);
  }

  get totalPages() {
    return Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
  }

  get pageStart() {
    return this.filtered.length === 0 ? 0 : this.pageIndex * this.pageSize + 1;
  }

  get pageEnd() {
    return Math.min(this.filtered.length, (this.pageIndex + 1) * this.pageSize);
  }

  applyFilter() {
    this.pageIndex = 0;
  }

  setSort(column: SchemeSortColumn) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = column === 'createdAt' ? 'desc' : 'asc';
    }
    this.pageIndex = 0;
  }

  sortIcon(column: SchemeSortColumn) {
    if (this.sortColumn !== column) return 'unfold_more';
    return this.sortDirection === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  changePageSize(size: number) {
    this.pageSize = Number(size);
    this.pageIndex = 0;
  }

  previousPage() {
    if (this.pageIndex > 0) {
      this.pageIndex--;
    }
  }

  nextPage() {
    if (this.pageIndex < this.totalPages - 1) {
      this.pageIndex++;
    }
  }

  view(s: SchemeApplication) {
    this.selected = s;
    this.showDetail = true;
    this.loadVisitorPhoto(s);
  }

  closeDetail() {
    this.showDetail = false;
    this.selected = null;
    this.clearVisitorPhotoState();
  }

  canUseSchemeDecisionActions(scheme: SchemeApplication | null) {
    return !!scheme &&
      this.auth.hasRole('HCM', 'ADMIN', 'APPROVER') &&
      !this.isFinalSchemeStatus(scheme.status);
  }

  canUseSchemeFollowUpAction(scheme: SchemeApplication | null) {
    return !!scheme &&
      this.auth.hasRole('APPROVER') &&
      !this.isFinalSchemeStatus(scheme.status) &&
      scheme.status !== 'FOLLOWUP';
  }

  canUseSchemeCmoActions(scheme: SchemeApplication | null) {
    return !!scheme &&
      this.auth.hasRole('HCM', 'ADMIN', 'APPROVER') &&
      !this.isFinalSchemeStatus(scheme.status);
  }

  approveScheme(scheme: SchemeApplication) {
    this.updateSchemeStatus(scheme, 'APPROVED', 'Scheme application approved.');
  }

  rejectScheme(scheme: SchemeApplication) {
    this.updateSchemeStatus(scheme, 'REJECTED', 'Scheme application rejected.');
  }

  markSchemeFollowUp(scheme: SchemeApplication) {
    this.updateSchemeStatus(scheme, 'FOLLOWUP', 'Marked for follow-up.');
  }

  requestSchemeMissingInfo(scheme: SchemeApplication) {
    this.updateSchemeStatus(scheme, 'CMO_REVIEW', 'Missing information requested.');
  }

  forwardSchemeToApprover(scheme: SchemeApplication) {
    this.updateSchemeStatus(scheme, 'HCM_PENDING', 'Forwarded for approval.');
  }

  getSeverity(status: string): string {
    const m: Record<string, string> = { 
      APPROVED: 'success', 
      HCM_PENDING: 'danger', 
      SCHEDULED: 'warn', 
      SUBMITTED: 'info', 
      REJECTED: 'danger' 
    };
    return m[status] ?? 'info';
  }

  getStatusClass(status: string): string {
    const severity = this.getSeverity(status);
    return `status-badge status-${severity}`;
  }

  getSchemeBadgeColor(type: string) {
    const m: Record<string,string> = { 
      CMSDF:'#1565c0', 
      CMSG:'#0288d1', 
      CM_CARE:'#2e7d32', 
      CM_CONNECT:'#4527a0', 
      CM_ELEVATE:'#f57f17', 
      FOCUS_PLUS:'#c62828' 
    };
    return m[type] ?? '#374151';
  }

  private updateSchemeStatus(scheme: SchemeApplication, status: string, successMessage: string) {
    if (this.actionUpdating) return;
    this.actionUpdating = true;
    this.schemeService.updateApplicationStatus(scheme.id, { status })
      .pipe(finalize(() => this.actionUpdating = false))
      .subscribe({
        next: updated => {
          this.replaceScheme(updated);
          this.selected = updated;
          this.loadStatistics();
          this.snackBar.open(successMessage, 'Close', { duration: 4000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Unable to update scheme application.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  private replaceScheme(updated: SchemeApplication) {
    const index = this.schemes.findIndex(scheme => scheme.id === updated.id);
    if (index >= 0) {
      this.schemes[index] = updated;
    } else {
      this.schemes = [updated, ...this.schemes];
    }
  }

  private isFinalSchemeStatus(status?: string) {
    const normalized = (status || '').toUpperCase();
    return normalized === 'APPROVED' || normalized === 'REJECTED' || normalized === 'HCM_REJECTED';
  }

  private schemeSortValue(scheme: SchemeApplication, column: SchemeSortColumn): string | number {
    switch (column) {
      case 'applicant':
        return this.applicantName(scheme);
      case 'scheme':
        return this.formatSchemeName(scheme.schemeType);
      case 'project':
        return scheme.projectName || '';
      case 'category':
        return scheme.projectCategory || '';
      case 'estCost':
        return Number(scheme.estimatedCost ?? 0);
      case 'hcmApproved':
        return Number(scheme.hcmApprovedCost ?? 0);
      case 'status':
        return scheme.status || '';
      case 'createdAt':
        return this.dateSortValue(scheme.createdAt);
      default:
        return '';
    }
  }

  private compareValues(left: string | number, right: string | number) {
    if (typeof left === 'number' && typeof right === 'number') {
      return left - right;
    }
    return String(left).toLowerCase().localeCompare(String(right).toLowerCase());
  }

  private dateSortValue(value?: string) {
    if (!value) return 0;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 0 : date.getTime();
  }

  private loadVisitorPhoto(scheme: SchemeApplication) {
    this.selectedVisitorPhotoUrl = this.resolveVisitorPhoto(scheme.applicant);
    this.selectedVisitorPhotoError = '';
    const visitorId = scheme.applicantId || scheme.applicant?.id;

    if (!visitorId) {
      this.selectedVisitorPhotoLoading = false;
      if (!this.selectedVisitorPhotoUrl) {
        this.selectedVisitorPhotoError = 'No photo captured.';
      }
      return;
    }

    this.selectedVisitorPhotoLoading = true;
    this.visitorService.getById(visitorId).subscribe({
      next: visitor => {
        if (this.selected?.id !== scheme.id) return;
        const photoUrl = this.resolveVisitorPhoto(visitor);
        if (photoUrl) {
          this.selectedVisitorPhotoUrl = photoUrl;
        } else if (!this.selectedVisitorPhotoUrl) {
          this.selectedVisitorPhotoError = 'No photo captured.';
        }
        this.selectedVisitorPhotoLoading = false;
      },
      error: error => {
        if (this.selected?.id !== scheme.id) return;
        if (!this.selectedVisitorPhotoUrl) {
          this.selectedVisitorPhotoError = apiErrorMessage(error, 'Photo unavailable.');
        }
        this.selectedVisitorPhotoLoading = false;
      }
    });
  }

  private resolveVisitorPhoto(visitor?: { photoUrl?: string; livePhotoBase64?: string; photoBase64?: string } | null) {
    return visitor?.photoUrl || visitor?.livePhotoBase64 || visitor?.photoBase64 || '';
  }

  private clearVisitorPhotoState() {
    this.selectedVisitorPhotoUrl = '';
    this.selectedVisitorPhotoLoading = false;
    this.selectedVisitorPhotoError = '';
  }
}
