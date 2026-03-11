import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="status-badge" [style.background]="getBg()" [style.color]="getColor()">
      <span *ngIf="showDot" class="status-dot" [style.background]="getColor()"></span>
      {{ getLabel() }}
    </span>
  `,
  styles: [`
    .status-badge { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; border-radius: 999px; font-size: 0.72rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; }
    .status-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
  `]
})
export class StatusBadgeComponent {
  @Input() status = '';
  @Input() showDot = false;
  @Input() customLabel = '';

  private statusConfig: Record<string, { bg: string; color: string; label: string }> = {
    SUBMITTED:          { bg: '#eff6ff', color: '#1d4ed8', label: 'Submitted' },
    DEO_PROCESSED:      { bg: '#f0fdf4', color: '#15803d', label: 'DEO Processed' },
    CMO_REVIEW:         { bg: '#fefce8', color: '#a16207', label: 'CMO Review' },
    APPROVER_REVIEW:    { bg: '#fff7ed', color: '#c2410c', label: 'Approver Review' },
    HCM_PENDING:        { bg: '#faf5ff', color: '#7e22ce', label: 'HCM Pending' },
    HCM_ACCEPTED:       { bg: '#f0fdf4', color: '#15803d', label: 'HCM Accepted' },
    HCM_SNOOZED:        { bg: '#f8fafc', color: '#475569', label: 'Snoozed' },
    HCM_REJECTED:       { bg: '#fef2f2', color: '#b91c1c', label: 'Rejected' },
    SCHEDULED:          { bg: '#ecfdf5', color: '#059669', label: 'Scheduled' },
    COMPLETED:          { bg: '#f0fdf4', color: '#15803d', label: 'Completed' },
    CANCELLED:          { bg: '#fef2f2', color: '#b91c1c', label: 'Cancelled' },
    IN_PROGRESS:        { bg: '#ecfdf5', color: '#059669', label: 'In Progress' },
    approved:           { bg: '#f0fdf4', color: '#15803d', label: 'Approved' },
    pending:            { bg: '#fefce8', color: '#a16207', label: 'Pending' },
    conflict:           { bg: '#fef2f2', color: '#b91c1c', label: 'Conflict' },
    public:             { bg: '#eff6ff', color: '#1d4ed8', label: 'Public' },
  };

  getBg()    { return this.statusConfig[this.status]?.bg    ?? '#f3f4f6'; }
  getColor() { return this.statusConfig[this.status]?.color ?? '#374151'; }
  getLabel() { return this.customLabel || this.statusConfig[this.status]?.label || this.status; }
}
