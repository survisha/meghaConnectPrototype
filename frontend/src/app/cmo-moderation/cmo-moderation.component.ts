import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AppointmentService } from '../services/appointment.service';
import { DocumentService } from '../services/document.service';
import { Appointment, AppointmentDocument, EventType, Location } from '../models';
import { apiErrorMessage } from '../shared/api-error.util';

import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

type ChipSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

@Component({
  selector: 'app-cmo-moderation',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatBadgeModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './cmo-moderation.component.html',
  styleUrls: ['./cmo-moderation.component.scss'],
})
export class CmoModerationComponent implements OnInit {
  @ViewChild('reviewDialog') reviewDialog!: TemplateRef<unknown>;
  @ViewChild('modifyDialog') modifyDialog!: TemplateRef<unknown>;
  @ViewChild('missingInfoDialog') missingInfoDialog!: TemplateRef<unknown>;

  appointments: Appointment[] = [];
  selected: Appointment | null = null;
  documents: AppointmentDocument[] = [];
  documentsLoading = false;
  documentsError = '';
  downloadingDocumentId: number | null = null;
  loading = false;
  saving = false;

  modifyEventType: EventType = 'A4';
  modifyLocation: Location = 'SHILLONG';
  modifyRemarks = '';
  missingInfoNote = '';

  displayedColumns: string[] = [
    'applicationId',
    'applicant',
    'district',
    'agenda',
    'category',
    'location',
    'mlaApproved',
    'status',
    'actions',
  ];

  readonly eventTypeOptions: { label: string; value: EventType }[] = [
    { label: 'A1 - Cabinet / Minister / Media / Flight', value: 'A1' },
    { label: 'A2 - Event / Programme', value: 'A2' },
    { label: 'A3 - File Clearing / Birthday', value: 'A3' },
    { label: 'A4 - Individual Appointment', value: 'A4' },
    { label: 'B1 - Public Durbar', value: 'B1' },
    { label: 'B2 - Public Walk-in', value: 'B2' },
  ];

  readonly locationOptions: { label: string; value: Location }[] = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura', value: 'TURA' },
    { label: 'Delhi', value: 'DELHI' },
    { label: 'Others', value: 'OTHERS' },
  ];

  private readonly cmoQueueStatuses = new Set<string>([
    'CREATED',
    'SUBMITTED',
    'DEO_PROCESSED',
    'PENDING_APPROVER_REVIEW',
    'CMO_REVIEW',
  ]);

  constructor(
    private appointmentService: AppointmentService,
    private documentService: DocumentService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.loading = true;
    this.appointmentService.getAllAppointments(0, 100)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: page => {
          this.appointments = page.content
            .filter(appt => this.cmoQueueStatuses.has(appt.status))
            .sort((a, b) => this.toTime(b.submittedAt ?? b.createdAt) - this.toTime(a.submittedAt ?? a.createdAt));
        },
        error: error => {
          this.appointments = [];
          this.snackBar.open(apiErrorMessage(error, 'Failed to load CMO moderation appointments.'), 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
        },
      });
  }

  openReview(appt: Appointment): void {
    this.selected = appt;
    this.loadDocuments(appt.id);
    this.dialog.open(this.reviewDialog, {
      width: '960px',
      maxWidth: '96vw',
      autoFocus: false,
      panelClass: 'cmo-dialog-panel',
    });
  }

  openModify(appt: Appointment): void {
    this.selected = appt;
    this.modifyEventType = appt.eventType;
    this.modifyLocation = appt.requestedLocation;
    this.modifyRemarks = appt.cmoRemarks ?? '';
    this.dialog.closeAll();
    this.dialog.open(this.modifyDialog, {
      width: '620px',
      maxWidth: '94vw',
      autoFocus: false,
      panelClass: 'cmo-dialog-panel',
    });
  }

  openMissingInfo(appt: Appointment): void {
    this.selected = appt;
    this.missingInfoNote = appt.cmoRemarks ?? '';
    this.dialog.closeAll();
    this.dialog.open(this.missingInfoDialog, {
      width: '620px',
      maxWidth: '94vw',
      autoFocus: false,
      panelClass: 'cmo-dialog-panel',
    });
  }

  closeDialogs(): void {
    this.dialog.closeAll();
    this.selected = null;
    this.documents = [];
    this.documentsError = '';
    this.missingInfoNote = '';
  }

  saveModify(): void {
    if (!this.selected) return;

    this.saving = true;
    this.appointmentService.submitCmoReview({
      appointmentId: this.selected.id,
      eventType: this.modifyEventType,
      requestedLocation: this.modifyLocation,
      cmoRemarks: this.modifyRemarks,
      status: 'APPROVER_REVIEW',
      notifyApplicant: false,
      notifyDeo: false,
    }).pipe(finalize(() => this.saving = false))
      .subscribe({
        next: updated => {
          this.snackBar.open(`${updated.applicationId} forwarded to Approver.`, 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
          this.closeDialogs();
          this.loadAppointments();
        },
        error: error => {
          this.snackBar.open(apiErrorMessage(error, 'Failed to update appointment.'), 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
        },
      });
  }

  sendMissingInfoNote(): void {
    const note = this.missingInfoNote.trim();
    if (!this.selected || !note) {
      this.snackBar.open('Please enter the missing information note.', 'Close', {
        duration: 2500,
        horizontalPosition: 'end',
        verticalPosition: 'top',
      });
      return;
    }

    this.saving = true;
    this.appointmentService.submitCmoReview({
      appointmentId: this.selected.id,
      cmoRemarks: note,
      pendingInformation: note,
      status: 'CMO_REVIEW',
      notifyApplicant: true,
      notifyDeo: true,
    }).pipe(finalize(() => this.saving = false))
      .subscribe({
        next: updated => {
          this.snackBar.open(`Missing information note sent for ${updated.applicationId}.`, 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
          this.closeDialogs();
          this.loadAppointments();
        },
        error: error => {
          this.snackBar.open(apiErrorMessage(error, 'Failed to send missing information note.'), 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
        },
      });
  }

  forwardToApprover(appt: Appointment): void {
    this.saving = true;
    this.appointmentService.submitCmoReview({
      appointmentId: appt.id,
      eventType: appt.eventType,
      requestedLocation: appt.requestedLocation,
      cmoRemarks: appt.cmoRemarks,
      status: 'APPROVER_REVIEW',
      notifyApplicant: false,
      notifyDeo: false,
    }).pipe(finalize(() => this.saving = false))
      .subscribe({
        next: updated => {
          this.snackBar.open(`${updated.applicationId} forwarded to Approver.`, 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
          this.loadAppointments();
        },
        error: error => {
          this.snackBar.open(apiErrorMessage(error, 'Failed to forward appointment.'), 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
        },
      });
  }

  loadDocuments(appointmentId: number): void {
    this.documents = [];
    this.documentsError = '';
    this.documentsLoading = true;
    this.appointmentService.getAppointmentDocuments(appointmentId)
      .pipe(finalize(() => this.documentsLoading = false))
      .subscribe({
        next: documents => this.documents = documents,
        error: error => this.documentsError = apiErrorMessage(error, 'Unable to load attached documents.'),
      });
  }

  getSeverity(status: string): ChipSeverity {
    const map: Record<string, ChipSeverity> = {
      CREATED: 'info',
      SUBMITTED: 'info',
      DEO_PROCESSED: 'secondary',
      PENDING_APPROVER_REVIEW: 'warn',
      CMO_REVIEW: 'warn',
      APPROVER_REVIEW: 'success',
      REJECTED: 'danger',
    };
    return map[status] || 'secondary';
  }

  getStatusLabel(status: string): string {
    return status.replace(/_/g, ' ');
  }

  getEventTypeLabel(value?: EventType): string {
    return this.eventTypeOptions.find(option => option.value === value)?.label ?? (value || '-');
  }

  getLocationLabel(value?: Location): string {
    return this.locationOptions.find(option => option.value === value)?.label ?? (value || '-');
  }

  formatDocumentType(type: string): string {
    return (type || 'Document').replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, char => char.toUpperCase());
  }

  fileSizeLabel(size?: number): string {
    if (!size) return '-';
    if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  downloadDocument(doc: AppointmentDocument): void {
    if (!doc.id || this.downloadingDocumentId) {
      return;
    }

    this.downloadingDocumentId = doc.id;
    this.documentService.downloadDocument(doc.id)
      .pipe(finalize(() => this.downloadingDocumentId = null))
      .subscribe({
        next: blob => this.triggerBlobDownload(blob, doc.fileName || 'document'),
        error: error => {
          this.snackBar.open(apiErrorMessage(error, 'Unable to download document.'), 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
          });
        },
      });
  }

  trackByAppointmentId(_: number, appt: Appointment): number {
    return appt.id;
  }

  trackByDocumentId(index: number, doc: AppointmentDocument): number {
    return doc.id ?? index;
  }

  private toTime(value?: string): number {
    return value ? new Date(value).getTime() : 0;
  }

  private triggerBlobDownload(blob: Blob, fileName: string): void {
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = fileName;
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
  }
}
