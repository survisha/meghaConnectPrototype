import { Component, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AppointmentService } from '../../services/appointment.service';
import { AuthService } from '../../services/auth.service';
import { DocumentService } from '../../services/document.service';
import { Appointment, AppointmentDocument, AppointmentStatus } from '../../models';
import { environment } from '../../../environments/environment';
import { finalize } from 'rxjs/operators';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
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
    MatDialogModule,
    MatCardModule,
    MatTooltipModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
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
  documentPreviewLoading = false;
  documentDownloadLoading = false;
  documentPreviewError = '';
  documents: AppointmentDocument[] = [];
  documentsLoading = false;
  documentsError = '';
  search = '';
  filterStatus = '';
  loading = false;
  errorMsg = '';
  private readonly allowDummyFallback = !environment.production || environment.appName.includes('[UAT]');
  displayedColumns: string[] = ['applicant', 'designation', 'constituency', 'agenda', 'eventType', 'location', 'status', 'actions'];

  statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'CMO Review', value: 'CMO_REVIEW' },
    { label: 'Approver Review', value: 'APPROVER_REVIEW' },
    { label: 'HCM Pending', value: 'HCM_PENDING' },
    { label: 'Scheduled', value: 'SCHEDULED' },
    { label: 'Completed', value: 'COMPLETED' },
  ];

  private documentPreviewDialogRef?: MatDialogRef<unknown>;

  constructor(
    private appointmentService: AppointmentService,
    private documentService: DocumentService,
    public auth: AuthService,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
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
        if (this.allowDummyFallback) {
          // TODO: Remove dummy fallback after API stabilization.
          this.initializeDummyData();
        } else {
          this.appointments = [];
        }
        this.applyFilter();
        this.loading = false;
      }
    });
  }

  ngOnDestroy() {
    this.clearDocumentPreviewState();
  }

  private initializeDummyData() {
    // Demo appointments with varied statuses for review/approval workflow
    this.appointments = [
      {
        id: 1,
        applicationId: 'MC-2024-00145',
        applicant: {
          id: 101,
          fullName: 'Ramsing Marak',
          phoneNumber: '+91-9876543210',
          epicNumber: 'MH/01/WGH/234567',
          district: 'West Garo Hills',
          constituency: 'Ampati',
          booth: 'WGH/234',
          designation: 'Political Leader',
          village: 'Dalu'
        },
        agendaType: 'Scheme availment (CM)',
        agendaBrief: 'CMSDF application for community hall construction at Dalu village. Project cost: ₹25 lakhs. Benefits ~500 villagers. MLA letter attached.',
        status: 'HCM_PENDING',
        requestedLocation: 'TURA',
        eventType: 'A4',
        mlaMdcApproved: true,
        meetingCountLast6Months: 2,
        cmoRemarks: 'Verified application. Community contribution 20% (₹5L). Site inspection completed. MLA letter attached. Recommend approval.',
        shortNotes: 'AI Summary: Community infrastructure project for Dalu village. Strong MLA endorsement. 2 prior meetings with CM. Community has committed 20% co-funding. Estimated beneficiaries: 500+.',
        submittedAt: '2024-03-10T09:30:00',
        updatedAt: '2024-03-14T16:45:00'
      },
      {
        id: 2,
        applicationId: 'MC-2024-00146',
        applicant: {
          id: 102,
          fullName: 'Sunita Sangma',
          phoneNumber: '+91-9876500001',
          epicNumber: 'MH/01/EKH/345678',
          district: 'East Khasi Hills',
          constituency: 'Shillong East',
          booth: 'EKH/345',
          designation: 'Teacher',
          village: 'Laitumkhrah'
        },
        agendaType: 'Public Grievance',
        agendaBrief: 'Request for school infrastructure improvement - repair of classrooms and addition of computer lab at Govt. Upper Primary School, Laitumkhrah.',
        status: 'CMO_REVIEW',
        requestedLocation: 'SHILLONG',
        eventType: 'A4',
        mlaMdcApproved: false,
        meetingCountLast6Months: 0,
        cmoRemarks: 'Documents verified. School inspection report pending. Awaiting Education Dept comments.',
        shortNotes: 'AI Summary: Teacher seeking infrastructure support for Govt school serving 250+ students. No prior scheme receipts. MLA endorsement pending. Requires Education Dept clearance.',
        submittedAt: '2024-03-12T11:15:00',
        updatedAt: '2024-03-14T10:30:00'
      },
      {
        id: 3,
        applicationId: 'MC-2024-00147',
        applicant: {
          id: 103,
          fullName: 'Bijoy Momin',
          phoneNumber: '+91-9812345678',
          epicNumber: 'MH/02/SGH/456789',
          district: 'South Garo Hills',
          constituency: 'Baghmara',
          booth: 'SGH/456',
          designation: 'General Public',
          village: 'Baghmara Town'
        },
        agendaType: 'Scheme availment (CM)',
        agendaBrief: 'CM Care application for medical treatment - cardiac surgery required. Estimated cost: ₹3.5 lakhs. BPL family.',
        status: 'SCHEDULED',
        requestedLocation: 'TURA',
        scheduledDateTime: '2024-03-16T14:30:00',
        scheduledDurationMinutes: 20,
        eventType: 'A4',
        mlaMdcApproved: true,
        meetingCountLast6Months: 1,
        cmoRemarks: 'Medical documents verified. Hospital estimate: ₹3.5L. Patient requires urgent cardiac bypass surgery. Recommend approval under CM Care.',
        shortNotes: 'AI Summary: Urgent medical assistance request. BPL family. Cardiac bypass surgery at NEIGRIHMS. Hospital estimates verified. MLA recommended. Previous CM Care recipient (2022 - minor surgery, ₹50k).',
        submittedAt: '2024-03-08T14:20:00',
        updatedAt: '2024-03-15T09:00:00'
      },
      {
        id: 4,
        applicationId: 'MC-2024-00148',
        applicant: {
          id: 104,
          fullName: 'Deibok Lyngdoh',
          phoneNumber: '+91-9887654321',
          epicNumber: 'MH/03/RBH/567890',
          district: 'Ri Bhoi',
          constituency: 'Umsning',
          booth: 'RBH/567',
          designation: 'Businessman',
          village: 'Nongpoh'
        },
        agendaType: 'Trade & Commerce',
        agendaBrief: 'CM Elevate scheme application - Request for transport vehicle permit. Plans to start passenger transport service between Nongpoh and Guwahati.',
        status: 'APPROVER_REVIEW',
        requestedLocation: 'SHILLONG',
        eventType: 'A4',
        mlaMdcApproved: false,
        meetingCountLast6Months: 0,
        isWalkIn: true,
        cmoRemarks: 'Walk-in visitor. Documents submitted: Vehicle registration, driving license, business plan. CM Elevate category - youth entrepreneur. Recommend forwarding to Transport Dept.',
        shortNotes: 'AI Summary: Youth entrepreneur (28 years) seeking transport permit under CM Elevate. Business plan shows potential for 5 direct jobs. No prior meetings or scheme benefits. Walkabouts-in applicant with complete documentation.',
        submittedAt: '2024-03-14T10:45:00',
        updatedAt: '2024-03-14T15:30:00'
      },
      {
        id: 5,
        applicationId: 'MC-2024-00149',
        applicant: {
          id: 105,
          fullName: 'Larsing Nongkhlaw',
          phoneNumber: '+91-9856123456',
          epicNumber: 'MH/01/EKH/678901',
          district: 'East Khasi Hills',
          constituency: 'Mawsynram',
          booth: 'EKH/678',
          designation: 'Village Headman',
          village: 'Mawsynram'
        },
        agendaType: 'Governance',
        agendaBrief: 'Request for bridge construction over seasonal stream. Village connectivity severely affected during monsoon. Proposal includes cost estimate and village council resolution.',
        status: 'SUBMITTED',
        requestedLocation: 'SHILLONG',
        eventType: 'A4',
        mlaMdcApproved: true,
        meetingCountLast6Months: 0,
        cmoRemarks: '',
        shortNotes: 'AI Summary: Infrastructure request from wettest place on Earth. Bridge construction critical for monsoon connectivity. 800+ villagers affected. Village council resolution passed unanimously. PWD cost estimate: ₹45 lakhs.',
        submittedAt: '2024-03-15T16:00:00',
        updatedAt: '2024-03-15T16:00:00'
      },
      {
        id: 6,
        applicationId: 'MC-2024-00150',
        applicant: {
          id: 106,
          fullName: 'Christina Marak',
          phoneNumber: '+91-9774455667',
          epicNumber: 'MH/01/WGH/789012',
          district: 'West Garo Hills',
          constituency: 'Tura',
          booth: 'WGH/789',
          designation: 'Social Worker',
          village: 'Tura Town'
        },
        agendaType: 'Scheme availment (CM)',
        agendaBrief: 'CMSG application for women self-help group - Weaving cooperative setup. 25 women members. Equipment cost: ₹12 lakhs.',
        status: 'HCM_PENDING',
        requestedLocation: 'TURA',
        eventType: 'B1',
        mlaMdcApproved: true,
        meetingCountLast6Months: 1,
        cmoRemarks: 'SHG registered with MSRLM. Members trained in weaving. Market linkage established with state emporium. Strong project viability. Recommend approval.',
        shortNotes: 'AI Summary: Women empowerment project. 25-member SHG with MSRLM certification. Traditional Garo weaving revival. Market linkage confirmed. MLA strongly endorsed. Expected revenue: ₹8L/year.',
        submittedAt: '2024-03-11T09:00:00',
        updatedAt: '2024-03-14T14:20:00'
      }
    ];
  }

  applyFilter() {
    this.filtered = this.appointments.filter(a =>
      (!this.search || a.applicant?.fullName?.toLowerCase().includes(this.search.toLowerCase()) || a.applicationId?.includes(this.search)) &&
      (!this.filterStatus || a.status === this.filterStatus)
    );
  }

  getStatusSeverity(s: AppointmentStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined {
    const m: Record<string,'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined> = {
      SUBMITTED: 'info', DEO_PROCESSED: 'info', CMO_REVIEW: 'warn',
      APPROVER_REVIEW: 'warn', HCM_PENDING: 'danger', HCM_ACCEPTED: 'success',
      SCHEDULED: 'success', COMPLETED: 'success', HCM_REJECTED: 'danger',
      HCM_SNOOZED: 'secondary', CANCELLED: 'secondary'
    };
    return m[s] ?? 'info';
  }

  getStatusLabel(s: AppointmentStatus) {
    return s.replace(/_/g, ' ');
  }

  openViewDetails(appointment: Appointment) {
    this.selectedAppointment = appointment;
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
    this.documents = [];
    this.documentsError = '';
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
