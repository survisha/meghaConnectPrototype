import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuditLogFilters, AuditLogService } from '../../services/audit-log.service';
import { AuditEntry } from '../../models';
import { apiErrorMessage } from '../../shared/api-error.util';

interface AuditFilters {
  from: Date | null;
  to: Date | null;
  module: string;
  action: string;
  user: string;
  role: string;
  requestId: string;
  status: string;
}

@Component({
  selector: 'app-audit-trail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatDatepickerModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './audit-trail.component.html',
  styleUrls: ['./audit-trail.component.scss'],
})
export class AuditTrailComponent implements OnInit, OnDestroy {
  logs: AuditEntry[] = [];
  displayedColumns = ['timestamp', 'module', 'action', 'user', 'role', 'entityId', 'description', 'status', 'requestId'];
  expandedLog: AuditEntry | null = null;

  loading = false;
  errorMsg = '';
  totalElements = 0;
  pageIndex = 0;
  pageSize = 100;
  sortActive = 'timestamp';
  sortDirection: 'asc' | 'desc' = 'desc';

  filters: AuditFilters = {
    from: null,
    to: null,
    module: '',
    action: '',
    user: '',
    role: '',
    requestId: '',
    status: '',
  };

  moduleOptions: string[] = [];
  actionOptions: string[] = [];
  readonly roleOptions = ['ADMIN', 'HCM', 'APPROVER', 'DEO', 'PUBLIC'];
  readonly statusOptions = ['SUCCESS', 'FAILED', 'PENDING'];

  private filterTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private auditLogService: AuditLogService) {}

  ngOnInit() {
    this.loadLogs();
  }

  ngOnDestroy() {
    if (this.filterTimer) {
      clearTimeout(this.filterTimer);
    }
  }

  loadLogs(page = this.pageIndex) {
    this.loading = true;
    this.errorMsg = '';
    this.pageIndex = page;

    const sort = `${this.sortActive || 'timestamp'},${this.sortDirection || 'desc'}`;
    this.auditLogService.getAll(this.pageIndex, this.pageSize, this.buildApiFilters(), sort).subscribe({
      next: pageData => {
        this.logs = pageData.content;
        this.totalElements = pageData.totalElements;
        this.pageSize = pageData.size || this.pageSize;
        this.pageIndex = pageData.number || 0;
        this.expandedLog = null;
        this.captureFilterOptions(pageData.content);
        this.loading = false;
      },
      error: err => {
        this.logs = [];
        this.totalElements = 0;
        this.errorMsg = apiErrorMessage(err, 'Unable to load audit logs. Please try again.');
        this.loading = false;
      },
    });
  }

  onPage(event: PageEvent) {
    this.pageSize = event.pageSize;
    this.loadLogs(event.pageIndex);
  }

  onSort(sort: Sort) {
    this.sortActive = sort.active || 'timestamp';
    this.sortDirection = (sort.direction || 'desc') as 'asc' | 'desc';
    this.loadLogs(0);
  }

  onFilterChange(immediate = false) {
    if (this.filterTimer) {
      clearTimeout(this.filterTimer);
    }
    if (immediate) {
      this.loadLogs(0);
      return;
    }
    this.filterTimer = setTimeout(() => this.loadLogs(0), 350);
  }

  clearFilters() {
    this.filters = {
      from: null,
      to: null,
      module: '',
      action: '',
      user: '',
      role: '',
      requestId: '',
      status: '',
    };
    this.loadLogs(0);
  }

  toggleRow(row: AuditEntry) {
    this.expandedLog = this.expandedLog?.id === row.id ? null : row;
  }

  isExpanded(row: AuditEntry): boolean {
    return this.expandedLog?.id === row.id;
  }

  trackById(_: number, row: AuditEntry) {
    return row.id;
  }

  displayModule(row: AuditEntry): string {
    return row.module || row.entity || row.entityType || '-';
  }

  displayUser(row: AuditEntry): string {
    return row.user || row.performedBy || '-';
  }

  displayDescription(row: AuditEntry): string {
    return this.maskSensitive(row.description || row.details || '-');
  }

  shortText(value: string | undefined, limit = 80): string {
    const safe = this.maskSensitive(value || '-');
    return safe.length > limit ? `${safe.slice(0, limit)}...` : safe;
  }

  actionClass(action: string | undefined): string {
    const value = (action || '').toUpperCase();
    if (value.includes('DELETE') || value.includes('REJECT')) return 'badge-danger';
    if (value.includes('CREATE') || value.includes('APPROVE') || value.includes('LOGIN')) return 'badge-success';
    if (value.includes('UPDATE') || value.includes('CHANGE') || value.includes('SCHEDULE')) return 'badge-info';
    return 'badge-neutral';
  }

  prettyJson(value: string | undefined): string {
    const masked = this.maskSensitive(value || '');
    if (!masked) {
      return '-';
    }
    try {
      return JSON.stringify(JSON.parse(masked), null, 2);
    } catch {
      return masked;
    }
  }

  private buildApiFilters(): AuditLogFilters {
    return {
      from: this.formatDateParam(this.filters.from, false),
      to: this.formatDateParam(this.filters.to, true),
      module: this.filters.module,
      action: this.filters.action,
      user: this.filters.user,
      role: this.filters.role,
      requestId: this.filters.requestId,
      status: this.filters.status,
    };
  }

  private captureFilterOptions(rows: AuditEntry[]) {
    this.moduleOptions = this.mergeUnique(this.moduleOptions, rows.map(row => this.displayModule(row)));
    this.actionOptions = this.mergeUnique(this.actionOptions, rows.map(row => row.action));
  }

  private mergeUnique(existing: string[], incoming: Array<string | undefined>): string[] {
    const values = new Set(existing.filter(Boolean));
    incoming.filter(Boolean).forEach(value => values.add(value as string));
    return Array.from(values).sort((a, b) => a.localeCompare(b));
  }

  private formatDateParam(date: Date | null, endOfDay: boolean): string | undefined {
    if (!date) {
      return undefined;
    }
    const value = new Date(date);
    value.setHours(endOfDay ? 23 : 0, endOfDay ? 59 : 0, endOfDay ? 59 : 0, endOfDay ? 999 : 0);
    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`;
  }

  private maskSensitive(value: string): string {
    if (!value) {
      return '';
    }
    return value
      .replace(/("(?:aadhaar|aadhar|otp|password|passwd|pwd|token|authorization|secret)"\s*:\s*)"[^"]*"/gi, '$1"***"')
      .replace(/\b\d{4}\s?\d{4}\s?\d{4}\b/g, '**** **** ****')
      .replace(/(otp|password|aadhaar|aadhar|token|secret)(\s*[=:]\s*)\S+/gi, '$1$2***');
  }
}
