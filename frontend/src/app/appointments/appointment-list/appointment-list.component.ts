import { Component, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { AppointmentService } from '../../services/appointment.service';
import { AuthService } from '../../services/auth.service';
import { DocumentService } from '../../services/document.service';
import { ReferenceDataService } from '../../services/reference-data.service';
import { VisitorService } from '../../services/visitor.service';
import { Appointment, AppointmentDocument, AppointmentStatus, EventType } from '../../models';
import { finalize } from 'rxjs/operators';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, provideNativeDateAdapter } from '@angular/material/core';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CmoReviewModalComponent } from '../cmo-review-modal/cmo-review-modal.component';
import { apiErrorMessage } from '../../shared/api-error.util';

@Component({
  selector: 'app-appointment-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink, 
    FormsModule, 
    MatTableModule, 
    MatPaginatorModule,
    MatButtonModule, 
    MatIconModule, 
    MatFormFieldModule, 
    MatInputModule, 
    MatSelectModule,
    MatChipsModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDialogModule,
    MatCardModule,
    MatTooltipModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss'],
})
export class AppointmentListComponent implements OnInit, OnDestroy {
  @ViewChild('appointmentDetailsDialog') appointmentDetailsDialog!: TemplateRef<unknown>;
  @ViewChild('documentPreviewDialog') documentPreviewDialog!: TemplateRef<unknown>;

  appointments: Appointment[] = [];
  filtered: Appointment[] = [];
  selectedAppointment: Appointment | null = null;
  selectedDocument: AppointmentDocument | null = null;
  selectedDocumentPreviewUrl: SafeResourceUrl | null = null;
  selectedDocumentUrl = '';
  selectedVisitorPhotoUrl = '';
  selectedVisitorPhotoLoading = false;
  selectedVisitorPhotoError = '';
  documentPreviewLoading = false;
  documentDownloadLoading = false;
  documentPreviewError = '';
  documents: AppointmentDocument[] = [];
  documentsLoading = false;
  documentsError = '';
  search = '';
  filterStatus = '';
  filterEventType = '';
  filterFromDate: Date | null = null;
  filterToDate: Date | null = null;
  loading = false;
  bulkUpdating = false;
  errorMsg = '';
  selectedAppointmentIds = new Set<number>();
  eventTypeOptions: Array<{ label: string; value: EventType | '' }> = [{ label: 'All Types', value: '' }];
  displayedColumns: string[] = ['select', 'applicant', 'designation', 'constituency', 'agenda', 'eventType', 'location', 'status', 'actions'];

  statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'CMO Review', value: 'CMO_REVIEW' },
    { label: 'Approver Review', value: 'APPROVER_REVIEW' },
    { label: 'Follow-up', value: 'FOLLOWUP' },
    { label: 'Scheduled for Public Durbar', value: 'SCHEDULED_FOR_PUBLIC_DARBAR' },
    { label: 'HCM Pending', value: 'HCM_PENDING' },
    { label: 'HCM ACCEPTED', value: 'HCM_ACCEPTED' },
    { label: 'Scheduled', value: 'SCHEDULED' },
    { label: 'Completed', value: 'COMPLETED' },
  ];

  private documentPreviewDialogRef?: MatDialogRef<unknown>;
  private readonly followUpStatuses: AppointmentStatus[] = ['FOLLOWUP', 'SELECTED_FOR_PUBLIC_DARBAR'];
  private readonly followUpReviewableStatuses: AppointmentStatus[] = [
    'CREATED',
    'SUBMITTED',
    'PENDING_APPROVER_REVIEW',
    'CMO_REVIEW',
    'APPROVER_REVIEW'
  ];

  constructor(
    private appointmentService: AppointmentService,
    private documentService: DocumentService,
    private referenceDataService: ReferenceDataService,
    private visitorService: VisitorService,
    public auth: AuthService,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.loadAppointmentTypes();
    this.loadAppointments();
  }

  private loadAppointmentTypes() {
    this.referenceDataService.getByType('APPOINMENT_TYPES').subscribe({
      next: values => {
        this.eventTypeOptions = [
          { label: 'All Types', value: '' },
          ...(values ?? []).map(item => ({ label: item.value, value: item.code as EventType })),
        ];
      },
      error: error => {
        this.errorMsg = apiErrorMessage(error, 'Unable to load appointment types.');
      }
    });
  }

  private loadAppointments() {
    this.loading = true;
    const source = this.auth.hasRole('DATA_ENTRY_OPERATOR')
      ? this.appointmentService.getDeoAppointments(0, 100)
      : this.appointmentService.getAllAppointments(0, 100);
    source.subscribe({
      next: page => {
        this.errorMsg = '';
        this.appointments = page.content ?? [];
        this.applyFilter();
        this.loading = false;
      },
      error: error => {
        this.errorMsg = apiErrorMessage(error, 'Unable to load appointments from API. Please try again.');
        this.appointments = [];
        this.applyFilter();
        this.loading = false;
      }
    });
  }

  ngOnDestroy() {
    this.clearDocumentPreviewState();
  }

  applyFilter() {
    const searchValue = this.search.trim().toLowerCase();
    this.filtered = this.appointments.filter(a => {
      const createdDate = this.parseAppointmentDate(a);
      return (!searchValue ||
          a.applicant?.fullName?.toLowerCase().includes(searchValue) ||
          a.applicantName?.toLowerCase().includes(searchValue) ||
          a.applicationId?.toLowerCase().includes(searchValue) ||
          a.applicant?.phoneNumber?.includes(searchValue)) &&
        (!this.filterStatus || this.matchesStatusFilter(a.status, this.filterStatus)) &&
        (!this.filterEventType || a.eventType === this.filterEventType) &&
        (!this.filterFromDate || (createdDate && createdDate >= this.startOfDay(this.filterFromDate))) &&
        (!this.filterToDate || (createdDate && createdDate < this.nextDay(this.filterToDate)));
    });
    this.selectedAppointmentIds.forEach(id => {
      if (!this.filtered.some(appointment => appointment.id === id)) {
        this.selectedAppointmentIds.delete(id);
      }
    });
  }

  getStatusSeverity(s: AppointmentStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined {
    const m: Record<string,'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined> = {
      SUBMITTED: 'info', DEO_PROCESSED: 'info', CMO_REVIEW: 'warn',
      APPROVER_REVIEW: 'warn', HCM_PENDING: 'danger', HCM_ACCEPTED: 'success',
      SCHEDULED: 'success', COMPLETED: 'success', HCM_REJECTED: 'danger',
      FOLLOWUP: 'warn',
      HCM_SNOOZED: 'secondary', CANCELLED: 'secondary',
      SELECTED_FOR_PUBLIC_DARBAR: 'warn', SCHEDULED_FOR_PUBLIC_DARBAR: 'success',
      APPROVED_WITH_DATE_TIME: 'success', REJECTED: 'danger'
    };
    return m[s] ?? 'info';
  }

  getStatusLabel(s: AppointmentStatus) {
    if (this.isFollowUpStatus(s)) return 'FOLLOW-UP';
    if (s === 'SCHEDULED_FOR_PUBLIC_DARBAR') return 'SCHEDULED FOR PUBLIC DURBAR';
    return s.replace(/_/g, ' ');
  }

  isSelected(appointment: Appointment) {
    return this.selectedAppointmentIds.has(appointment.id);
  }

  toggleSelection(appointment: Appointment, checked: boolean) {
    if (checked) {
      this.selectedAppointmentIds.add(appointment.id);
    } else {
      this.selectedAppointmentIds.delete(appointment.id);
    }
  }

  areAllFilteredSelected() {
    return this.filtered.length > 0 && this.filtered.every(appointment => this.selectedAppointmentIds.has(appointment.id));
  }

  isSomeFilteredSelected() {
    return this.filtered.some(appointment => this.selectedAppointmentIds.has(appointment.id)) && !this.areAllFilteredSelected();
  }

  toggleAllFiltered(checked: boolean) {
    this.filtered.forEach(appointment => {
      if (checked) {
        this.selectedAppointmentIds.add(appointment.id);
      } else {
        this.selectedAppointmentIds.delete(appointment.id);
      }
    });
  }

  get selectedAppointments() {
    return this.appointments.filter(appointment => this.selectedAppointmentIds.has(appointment.id));
  }

  get canMarkSelectedFollowUp() {
    return this.auth.hasRole('APPROVER', 'CMO_OFFICER', 'OSD') &&
      this.selectedAppointments.length > 0 &&
      this.selectedAppointments.every(appointment => this.canAppointmentBeMarkedFollowUp(appointment));
  }

  markSelectedFollowUp() {
    if (!this.canMarkSelectedFollowUp || this.bulkUpdating) return;
    this.bulkUpdating = true;
    forkJoin(this.selectedAppointments.map(appointment =>
      this.appointmentService.markFollowUp(appointment.id, 'Follow-up')
    )).pipe(finalize(() => this.bulkUpdating = false))
      .subscribe({
        next: () => {
          this.selectedAppointmentIds.clear();
          this.loadAppointments();
        },
        error: error => this.errorMsg = apiErrorMessage(error, 'Unable to mark selected appointments for Public Durbar follow-up.')
      });
  }

  private canAppointmentBeMarkedFollowUp(appointment: Appointment) {
    return appointment.eventType === 'B1' && this.followUpReviewableStatuses.includes(appointment.status);
  }

  private isFollowUpStatus(status: AppointmentStatus) {
    return this.followUpStatuses.includes(status);
  }

  private matchesStatusFilter(status: AppointmentStatus, filterStatus: string) {
    if (filterStatus === 'FOLLOWUP') {
      return this.isFollowUpStatus(status);
    }
    return status === filterStatus;
  }

  exportFilteredToCsv() {
    const headers = [
      'Application ID',
      'Applicant',
      'Phone',
      'EPIC Number',
      'Designation',
      'Address',
      'District',
      'Constituency',
      'Agenda',
      'Agenda Brief',
      'Event Type',
      'Location',
      'Status',
      'Meeting Count Last 6 Months',
      'Submitted',
      'Created At',
      'Updated At',
    ];
    const rows = this.filtered.map(appointment => [
      appointment.applicationId,
      appointment.applicant?.fullName || appointment.applicantName || '',
      appointment.applicant?.phoneNumber || appointment.applicantPhone || '',
      appointment.applicant?.epicNumber || '',
      appointment.applicant?.designation || '',
      this.applicantAddress(appointment),
      appointment.applicant?.district || '',
      appointment.applicant?.constituency || '',
      appointment.agendaType || appointment.subject || '',
      appointment.agendaBrief || '',
      appointment.eventType,
      appointment.requestedLocation,
      this.getStatusLabel(appointment.status),
      appointment.meetingCountLast6Months ?? '',
      appointment.submittedAt || appointment.createdAt || '',
      appointment.createdAt || '',
      appointment.updatedAt || '',
    ]);
    const csv = [headers, ...rows].map(row => row.map(value => this.csvCell(value)).join(',')).join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    this.triggerBlobDownload(blob, `appointments-${new Date().toISOString().slice(0, 10)}.csv`);
  }

  private parseAppointmentDate(appointment: Appointment): Date | null {
    const value = appointment.submittedAt || appointment.createdAt || appointment.updatedAt;
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private startOfDay(date: Date) {
    const value = new Date(date);
    value.setHours(0, 0, 0, 0);
    return value;
  }

  private nextDay(date: Date) {
    const value = this.startOfDay(date);
    value.setDate(value.getDate() + 1);
    return value;
  }

  private csvCell(value: unknown) {
    const text = value === null || value === undefined ? '' : String(value);
    return `"${text.replace(/"/g, '""')}"`;
  }

  private applicantAddress(appointment: Appointment) {
    return appointment.applicant?.addressLine
      || appointment.applicant?.fullAddress
      || appointment.applicant?.address
      || '';
  }

  openViewDetails(appointment: Appointment) {
    this.selectedAppointment = appointment;
    this.loadVisitorPhoto(appointment);
    this.loadDocuments(appointment.id);
    this.dialog.open(this.appointmentDetailsDialog, {
      width: '940px',
      maxWidth: '96vw',
      autoFocus: false,
      panelClass: 'appointment-details-dialog-panel'
    });
  }

  closeViewDetails() {
    this.dialog.closeAll();
    this.selectedAppointment = null;
    this.clearVisitorPhotoState();
    this.documents = [];
    this.documentsError = '';
  }

  private loadVisitorPhoto(appointment: Appointment) {
    this.selectedVisitorPhotoUrl = this.resolveVisitorPhoto(appointment.applicant);
    this.selectedVisitorPhotoError = '';
    const visitorId = appointment.applicantId || appointment.applicant?.id;

    if (!visitorId) {
      this.selectedVisitorPhotoLoading = false;
      if (!this.selectedVisitorPhotoUrl) {
        this.selectedVisitorPhotoError = 'No photo captured.';
      }
      return;
    }

    this.selectedVisitorPhotoLoading = true;
    this.visitorService.getById(visitorId)
      .pipe(finalize(() => {
        if (this.selectedAppointment?.id === appointment.id) {
          this.selectedVisitorPhotoLoading = false;
        }
      }))
      .subscribe({
        next: visitor => {
          if (this.selectedAppointment?.id !== appointment.id) return;
          const photoUrl = this.resolveVisitorPhoto(visitor);
          if (photoUrl) {
            this.selectedVisitorPhotoUrl = photoUrl;
          } else if (!this.selectedVisitorPhotoUrl) {
            this.selectedVisitorPhotoError = 'No photo captured.';
          }
        },
        error: error => {
          if (this.selectedAppointment?.id !== appointment.id) return;
          if (!this.selectedVisitorPhotoUrl) {
            this.selectedVisitorPhotoError = apiErrorMessage(error, 'Photo unavailable.');
          }
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

  private loadDocuments(appointmentId: number) {
    this.documents = [];
    this.documentsError = '';
    this.documentsLoading = true;
    this.appointmentService.getAppointmentDocuments(appointmentId)
      .pipe(finalize(() => this.documentsLoading = false))
      .subscribe({
        next: documents => this.documents = documents,
        error: error => this.documentsError = apiErrorMessage(error, 'Unable to load attached documents.')
      });
  }

  formatDocumentType(type?: string) {
    return (type || 'Document').replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, char => char.toUpperCase());
  }

  fileSizeLabel(size?: number) {
    if (!size) return '—';
    if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  openDocumentPreview(doc: AppointmentDocument) {
    this.clearDocumentPreviewState();

    this.selectedDocument = doc;
    this.documentPreviewLoading = Boolean(doc.id && this.canInlinePreview(doc));
    this.documentPreviewError = doc.id ? '' : 'Document id is not available.';
    this.documentPreviewDialogRef = this.dialog.open(this.documentPreviewDialog, {
      width: '1040px',
      maxWidth: '96vw',
      height: '86vh',
      maxHeight: '92vh',
      autoFocus: false,
      panelClass: 'document-preview-dialog-panel'
    });
    this.documentPreviewDialogRef.afterClosed().subscribe(() => {
      this.documentPreviewDialogRef = undefined;
      this.clearDocumentPreviewState();
    });

    if (!doc.id || !this.canInlinePreview(doc)) return;

    this.documentService.getPreviewBlob(doc.id)
      .pipe(finalize(() => this.documentPreviewLoading = false))
      .subscribe({
        next: blob => {
          const objectUrl = URL.createObjectURL(blob);
          if (this.selectedDocument !== doc) {
            URL.revokeObjectURL(objectUrl);
            return;
          }
          this.selectedDocumentObjectUrl = objectUrl;
          this.selectedDocumentUrl = objectUrl;
          this.selectedDocumentPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
        },
        error: error => {
          this.showDocumentApiError(error, 'Unable to load this document. Please try again.');
        }
    });
  }

  closeDocumentPreview() {
    this.documentPreviewDialogRef?.close();
  }

  downloadDocument(doc: AppointmentDocument | null = this.selectedDocument) {
    if (!doc?.id || this.documentDownloadLoading) {
      return;
    }

    this.documentDownloadLoading = true;
    this.documentService.downloadDocument(doc.id)
      .pipe(finalize(() => this.documentDownloadLoading = false))
      .subscribe({
        next: blob => this.triggerBlobDownload(blob, doc.fileName || 'document'),
        error: error => {
          this.showDocumentApiError(error, 'Unable to download this document. Please try again.');
        }
      });
  }

  getDocumentExtension(doc: AppointmentDocument | null = this.selectedDocument) {
    const source = (doc?.fileName || '').toLowerCase();
    const match = source.match(/\.([a-z0-9]+)(?:\?.*)?$/);
    return match?.[1] ?? '';
  }

  getDocumentKindLabel(doc: AppointmentDocument | null = this.selectedDocument) {
    return this.getDocumentExtension(doc).toUpperCase() || 'DOCUMENT';
  }

  isImageDocument(doc: AppointmentDocument | null = this.selectedDocument) {
    const mimeType = this.getDocumentMimeType(doc);
    return mimeType.startsWith('image/')
      || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(this.getDocumentExtension(doc));
  }

  isPdfDocument(doc: AppointmentDocument | null = this.selectedDocument) {
    return this.getDocumentMimeType(doc) === 'application/pdf' || this.getDocumentExtension(doc) === 'pdf';
  }

  canInlinePreview(doc: AppointmentDocument | null = this.selectedDocument) {
    return this.isImageDocument(doc) || this.isPdfDocument(doc);
  }

  private selectedDocumentObjectUrl = '';

  private clearDocumentPreviewState() {
    if (this.selectedDocumentObjectUrl) {
      URL.revokeObjectURL(this.selectedDocumentObjectUrl);
    }
    this.selectedDocumentObjectUrl = '';
    this.selectedDocument = null;
    this.selectedDocumentPreviewUrl = null;
    this.selectedDocumentUrl = '';
    this.documentPreviewLoading = false;
    this.documentDownloadLoading = false;
    this.documentPreviewError = '';
  }

  private getDocumentMimeType(doc: AppointmentDocument | null = this.selectedDocument) {
    return (doc?.mimeType || '').toLowerCase();
  }

  private showDocumentApiError(error: unknown, fallbackMessage: string) {
    this.resolveDocumentApiErrorMessage(error, fallbackMessage)
      .then(message => this.documentPreviewError = message);
  }

  private async resolveDocumentApiErrorMessage(error: unknown, fallbackMessage: string): Promise<string> {
    const payload = await this.readApiErrorPayload(error);
    const errorCode = this.stringValue(payload?.['errorCode']);
    const message = this.stringValue(payload?.['message']) || this.stringValue(payload?.['error']);
    if (errorCode && message) {
      return `${errorCode}: ${message}`;
    }
    if (message) {
      return message;
    }
    if (error instanceof Error && error.message) {
      return error.message;
    }
    return fallbackMessage;
  }

  private async readApiErrorPayload(error: unknown): Promise<Record<string, unknown> | null> {
    const responseError = error instanceof HttpErrorResponse ? error.error : error;
    if (responseError instanceof Blob) {
      try {
        const text = await responseError.text();
        return text ? JSON.parse(text) as Record<string, unknown> : null;
      } catch {
        return null;
      }
    }
    if (typeof responseError === 'string') {
      try {
        return JSON.parse(responseError) as Record<string, unknown>;
      } catch {
        return { message: responseError };
      }
    }
    if (responseError && typeof responseError === 'object' && !(responseError instanceof Error)) {
      return responseError as Record<string, unknown>;
    }
    return null;
  }

  private stringValue(value: unknown) {
    return typeof value === 'string' && value.trim() ? value.trim() : '';
  }

  private triggerBlobDownload(blob: Blob, fileName: string) {
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

  trackByDocumentId(index: number, doc: AppointmentDocument) {
    return doc.id ?? index;
  }

  openCmoReviewFromDetails() {
    if (!this.selectedAppointment) return;
    const appointment = this.selectedAppointment;
    this.closeViewDetails();
    this.openCmoReview(appointment);
  }

  /**
   * Open CMO review modal to view applicant details and add remarks
   */
  openCmoReview(appointment: Appointment) {
    this.dialog.open(CmoReviewModalComponent, {
      width: '1200px',
      height: '90vh',
      maxHeight: '90vh',
      maxWidth: '95vw',
      data: { appointment },
      disableClose: false,
      panelClass: 'cmo-review-dialog'
    }).afterClosed().subscribe((result) => {
      if (result && result.submitted) {
        // Reload appointments after CMO submission
        this.ngOnInit();
      }
    });
  }
}
