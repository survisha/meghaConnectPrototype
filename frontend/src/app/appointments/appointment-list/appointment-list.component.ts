import { Component, Input, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { AppointmentDocumentAiNotes, AiNotesStatus, AppointmentRemark, AppointmentService } from '../../services/appointment.service';
import { AuthService } from '../../services/auth.service';
import { DocumentService } from '../../services/document.service';
import { ReferenceDataService } from '../../services/reference-data.service';
import { ScheduleEventService } from '../../services/schedule-event.service';
import { VisitorService } from '../../services/visitor.service';
import { Appointment, AppointmentDocument, AppointmentStatus, EventType, Location, ScheduleEvent } from '../../models';
import { canRejectScheduled, canReturnToPending, canScheduleOrReschedule, isLiveWalkIn as isPendingWalkIn } from '../appointment-action-policy';
import { catchError, finalize } from 'rxjs/operators';
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
import { ToastService } from '../../shared/toast/toast.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { apiErrorMessage } from '../../shared/api-error.util';
import { CameraCaptureService, CameraDeviceOption, CameraFacingMode } from '../../shared/camera-capture.service';
import { appointmentRemarkExportFields } from '../appointment-pdf-export.util';
import { AuthenticatedPhotoComponent } from '../../shared/authenticated-photo.component';
import { resolvePhotoUrl } from '../../shared/photo-url.util';

type SortDirection = 'asc' | 'desc';
type AppointmentSortColumn =
  | 'applicant'
  | 'designation'
  | 'constituency'
  | 'agenda'
  | 'appointmentSource'
  | 'location'
  | 'status'
  | 'createdAt'
  | 'aiNotes';

interface AppointmentExportOptions {
  basic: boolean;
  citizen: boolean;
  guest: boolean;
  schedule: boolean;
  workflow: boolean;
  hcmActions: boolean;
  associates: boolean;
}

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
    MatProgressSpinnerModule,
    AuthenticatedPhotoComponent,
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss'],
})
export class AppointmentListComponent implements OnInit, OnDestroy {
  @Input() listMode: 'appointments' | 'walkin' = 'appointments';
  @ViewChild('appointmentDetailsDialog') appointmentDetailsDialog!: TemplateRef<unknown>;
  @ViewChild('documentPreviewDialog') documentPreviewDialog!: TemplateRef<unknown>;
  @ViewChild('appointmentRemarksDialog') appointmentRemarksDialog!: TemplateRef<unknown>;
  @ViewChild('appointmentRescheduleDialog') appointmentRescheduleDialog!: TemplateRef<unknown>;
  @ViewChild('appointmentExportDialog') appointmentExportDialog!: TemplateRef<unknown>;
  @ViewChild('aiNotesDialog') aiNotesDialog!: TemplateRef<unknown>;
  @ViewChild('cmoModifyDialog') cmoModifyDialog!: TemplateRef<unknown>;
  @ViewChild('cmoMissingInfoDialog') cmoMissingInfoDialog!: TemplateRef<unknown>;

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
  supportingDocumentUploading = false;
  selectedSupportingDocument: File | null = null;
  proofCameraStream: MediaStream | null = null;
  proofCameraActive = false;
  proofCameraFacingMode: CameraFacingMode = 'environment';
  proofCameraOptions: CameraDeviceOption[] = [];
  selectedProofCameraDeviceId = '';
  proofCaptureUrl = '';
  proofCaptureFile: File | null = null;
  proofCaptureError = '';
  documentPreviewError = '';
  documents: AppointmentDocument[] = [];
  selectedAppointmentRemarks: AppointmentRemark[] = [];
  selectedAppointmentRemarksLoading = false;
  selectedAppointmentRemarksError = '';
  documentsLoading = false;
  documentsError = '';
  search = '';
  filterStatus = '';
  filterSource = '';
  filterEventType = '';
  filterFromDate: Date | null = null;
  filterToDate: Date | null = null;
  loading = false;
  bulkUpdating = false;
  eventAssigning = false;
  exportPreparing = false;
  eventsLoading = false;
  actionUpdating = false;
  cmoActionUpdating = false;
  errorMsg = '';
  remarksText = '';
  approverRemarksText = '';
  approverDepartmentCode = '';
  hcmRemarksText = '';
  rescheduleDate: Date | null = null;
  rescheduleTime = '10:00';
  pendingAction: 'APPROVE' | 'REJECT' | 'RETURN_PENDING' | null = null;
  cmoModifyEventType: EventType = 'A4';
  cmoModifyLocation: Location = 'SHILLONG';
  cmoModifyRemarks = '';
  cmoMissingInfoNote = '';
  aiNotesByAppointmentId = new Map<number, AppointmentDocumentAiNotes[]>();
  aiNotesLoadingAppointmentIds = new Set<number>();
  aiNotesFailedAppointmentIds = new Set<number>();
  aiNotesRegeneratingDocumentIds = new Set<number>();
  selectedAiNotesAppointment: Appointment | null = null;
  selectedAiNotes: AppointmentDocumentAiNotes[] = [];
  selectedAppointmentIds = new Set<number>();
  availableEvents: ScheduleEvent[] = [];
  departments: Array<{ label: string; value: string }> = [];
  selectedEventId: number | null = null;
  eventTypeOptions: Array<{ label: string; value: EventType | '' }> = [{ label: 'All Types', value: '' }];
  displayedColumns: string[] = ['select', 'applicant', 'designation', 'constituency', 'agenda', 'appointmentSource', 'location', 'status', 'createdAt', 'aiNotes', 'actions'];
  pageSizeOptions = [10, 25, 50];
  pageSize = 10;
  pageIndex = 0;
  serverPageIndex = 0;
  serverPageSize = 100;
  serverTotalElements = 0;
  loadingMoreAppointments = false;
  hasMoreServerAppointments = false;
  sortColumn: AppointmentSortColumn = 'createdAt';
  sortDirection: SortDirection = 'desc';

  statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Scheduled', value: 'SCHEDULED' },
    { label: 'Reschedule', value: 'SCHEDULED' },
  ];

  sourceOptions = [
    { label: 'All Sources', value: '' },
    { label: 'Citizen', value: 'CITIZEN' },
    { label: 'Guest', value: 'GUEST' },
  ];

  readonly cmoEventTypeOptions: { label: string; value: EventType }[] = [
    { label: 'A1 - Cabinet / Minister / Media / Flight', value: 'A1' },
    { label: 'A2 - Event / Programme', value: 'A2' },
    { label: 'A3 - File Clearing / Birthday', value: 'A3' },
    { label: 'A4 - Individual Appointment', value: 'A4' },
    { label: 'B1 - Public Durbar', value: 'B1' },
    { label: 'B2 - Public Walk-in', value: 'B2' },
  ];

  readonly cmoLocationOptions: { label: string; value: Location }[] = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura', value: 'TURA' },
    { label: 'Delhi', value: 'DELHI' },
    { label: 'Others', value: 'OTHERS' },
  ];

  private documentPreviewDialogRef?: MatDialogRef<unknown>;
  private appointmentRemarksDialogRef?: MatDialogRef<unknown>;
  private appointmentRescheduleDialogRef?: MatDialogRef<unknown>;
  private appointmentExportDialogRef?: MatDialogRef<unknown>;
  private aiNotesDialogRef?: MatDialogRef<unknown>;
  private cmoModifyDialogRef?: MatDialogRef<unknown>;
  private cmoMissingInfoDialogRef?: MatDialogRef<unknown>;
  private aiNotesPollTimers = new Map<number, number>();
  private readonly handleProofCameraDeviceChange = () => {
    void this.loadProofCameraDevices(true);
  };
  private readonly cmoQueueStatuses = new Set<AppointmentStatus>([
    'SUBMITTED',
    'CMO_REVIEW',
  ]);
  private readonly followUpStatuses: AppointmentStatus[] = ['FOLLOWUP'];
  exportOptions: AppointmentExportOptions = {
    basic: true,
    citizen: true,
    guest: true,
    schedule: true,
    workflow: true,
    hcmActions: true,
    associates: true,
  };

  constructor(
    private appointmentService: AppointmentService,
    private documentService: DocumentService,
    private referenceDataService: ReferenceDataService,
    private scheduleEventService: ScheduleEventService,
    private visitorService: VisitorService,
    public auth: AuthService,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer,
    private snackBar: ToastService,
    private cameraCapture: CameraCaptureService
  ) {}

  ngOnInit() {
    this.configureRoleDefaults();
    this.loadAppointmentTypes();
    this.loadDepartments();
    this.loadScheduleEvents();
    this.loadAppointments();
    this.loadProofCameraDevices();
    navigator.mediaDevices?.addEventListener?.('devicechange', this.handleProofCameraDeviceChange);
  }

  private configureRoleDefaults() {
    if (!this.canSelectAppointments()) {
      this.displayedColumns = this.displayedColumns.filter(column => column !== 'select');
    }
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

  private loadDepartments() {
    this.referenceDataService.getByType('DEPARTMENT').subscribe({
      next: values => {
        this.departments = (values ?? []).map(item => ({ label: item.value, value: item.code }));
      },
      error: error => {
        this.snackBar.open(apiErrorMessage(error, 'Unable to load departments.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
      }
    });
  }

  private loadAppointments() {
    this.loading = true;
    this.serverPageIndex = 0;
    this.loadAppointmentsPage(0, false);
  }

  loadMoreAppointments() {
    if (!this.hasMoreServerAppointments || this.loadingMoreAppointments) return;
    this.loadingMoreAppointments = true;
    this.loadAppointmentsPage(this.serverPageIndex + 1, true);
  }

  private loadAppointmentsPage(serverPage: number, append: boolean) {
    const source = this.appointmentPageSource(serverPage, this.serverPageSize);
    source.subscribe({
      next: page => {
        this.errorMsg = '';
        const rows = page.content ?? [];
        this.appointments = append ? this.mergeAppointments(this.appointments, rows) : rows;
        this.serverPageIndex = page.number ?? serverPage;
        this.serverTotalElements = page.totalElements ?? this.appointments.length;
        this.hasMoreServerAppointments = this.appointments.length < this.serverTotalElements
          && this.serverPageIndex + 1 < (page.totalPages ?? 1);
        this.applyFilter();
        this.loadAiNotesForAppointments(rows);
        this.loading = false;
        this.loadingMoreAppointments = false;
      },
      error: error => {
        this.errorMsg = apiErrorMessage(error, 'Unable to load appointments from API. Please try again.');
        if (!append) {
          this.appointments = [];
          this.applyFilter();
        }
        this.loading = false;
        this.loadingMoreAppointments = false;
      }
    });
  }

  private appointmentPageSource(serverPage: number, size: number) {
    if (this.auth.hasRole('DEO')) {
      return this.appointmentService.getDeoAppointments(serverPage, size);
    }
    if (this.auth.hasRole('APPROVER', 'HCM') && !this.auth.hasRole('ADMIN')) {
      return this.appointmentService.getApproverAppointments(serverPage, size);
    }
    return this.appointmentService.getAllAppointments(serverPage, size, undefined, {
      sort: 'createdAt,desc',
    });
  }

  private mergeAppointments(existing: Appointment[], incoming: Appointment[]) {
    const byId = new Map<number, Appointment>();
    existing.forEach(appointment => byId.set(appointment.id, appointment));
    incoming.forEach(appointment => byId.set(appointment.id, appointment));
    return Array.from(byId.values());
  }

  private loadScheduleEvents() {
    this.eventsLoading = true;
    this.scheduleEventService.getAll()
      .pipe(finalize(() => this.eventsLoading = false))
      .subscribe({
        next: events => {
          this.availableEvents = (events ?? []).filter(event => event.sourceType !== 'APPOINTMENT' && event.id > 0);
        },
        error: error => {
          this.snackBar.open(apiErrorMessage(error, 'Unable to load schedule events.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
        }
      });
  }

  ngOnDestroy() {
    navigator.mediaDevices?.removeEventListener?.('devicechange', this.handleProofCameraDeviceChange);
    this.clearDocumentPreviewState();
    this.stopProofCamera();
    this.clearProofCapture();
    this.aiNotesPollTimers.forEach(timerId => window.clearTimeout(timerId));
    this.aiNotesPollTimers.clear();
  }

  applyFilter() {
    const searchValue = this.search.trim().toLowerCase();
    this.pageIndex = 0;
    this.filtered = this.appointments.filter(a => {
      const createdDate = this.parseAppointmentDate(a);
      return (!searchValue ||
          a.applicant?.fullName?.toLowerCase().includes(searchValue) ||
          a.applicantName?.toLowerCase().includes(searchValue) ||
          a.applicationId?.toLowerCase().includes(searchValue) ||
          a.applicant?.phoneNumber?.includes(searchValue)) &&
        (this.listMode === 'walkin' ? a.eventType === 'B2' : a.eventType !== 'B2') &&
        (!this.filterStatus || this.matchesStatusFilter(a.status, this.filterStatus)) &&
        (!this.filterSource || (a.appointmentSource || 'CITIZEN') === this.filterSource) &&
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

  get sortedAppointments() {
    return [...this.filtered].sort((left, right) => this.compareValues(
      this.appointmentSortValue(left, this.sortColumn),
      this.appointmentSortValue(right, this.sortColumn)
    ) * (this.sortDirection === 'asc' ? 1 : -1));
  }

  get pagedAppointments() {
    const start = this.pageIndex * this.pageSize;
    return this.sortedAppointments.slice(start, start + this.pageSize);
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

  setSort(column: AppointmentSortColumn) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = column === 'createdAt' ? 'desc' : 'asc';
    }
    this.pageIndex = 0;
  }

  sortIcon(column: AppointmentSortColumn) {
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

  private appointmentSortValue(appointment: Appointment, column: AppointmentSortColumn): string | number {
    switch (column) {
      case 'applicant':
        return appointment.applicant?.fullName || appointment.applicantName || '';
      case 'designation':
        return appointment.appointmentSource === 'GUEST'
          ? `${appointment.organizationName || ''} ${appointment.guestDesignation || ''}`.trim()
          : appointment.applicant?.designation || '';
      case 'constituency':
        return appointment.applicant?.constituency || '';
      case 'agenda':
        return appointment.agendaType || appointment.subject || '';
      case 'appointmentSource':
        return appointment.appointmentSource || '';
      case 'location':
        return appointment.requestedLocation || '';
      case 'status':
        return this.getStatusLabel(appointment.status);
      case 'createdAt':
        return this.dateSortValue(appointment.createdAt);
      case 'aiNotes':
        return this.getAiNotesStatusLabel(appointment);
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

  getStatusFilterLabel() {
    return this.filterStatus ? this.getStatusLabel(this.filterStatus as AppointmentStatus) : 'All';
  }

  getDisplayName(appointment: Appointment) {
    return appointment.guestName || appointment.applicant?.fullName || appointment.applicantName || '-';
  }

  getDisplayPhone(appointment: Appointment) {
    return appointment.guestMobile || appointment.applicant?.phoneNumber || appointment.applicantPhone || '-';
  }

  getDisplayDesignation(appointment: Appointment) {
    if (appointment.appointmentSource === 'GUEST') {
      return [appointment.organizationName, appointment.guestDesignation].filter(Boolean).join(' / ') || '-';
    }
    return appointment.applicant?.designation || '-';
  }

  getDisplayConstituency(appointment: Appointment) {
    if (appointment.appointmentSource === 'GUEST') {
      return [appointment.referredOffice, appointment.visitorCategory].filter(Boolean).join(' / ') || '-';
    }
    return appointment.applicant?.constituency || '-';
  }

  getDisplayAddress(appointment: Appointment) {
    if (appointment.appointmentSource === 'GUEST') {
      return appointment.guestAddress || '-';
    }

    const addressParts = [
      appointment.applicant?.address1,
      appointment.applicant?.address,
    ].map(value => value?.trim()).filter(Boolean) as string[];

    const combinedAddress = Array.from(new Set(addressParts)).join(', ');
    return combinedAddress
      || appointment.applicant?.addressLine
      || appointment.applicant?.fullAddress
      || '-';
  }

  getDisplayAgenda(appointment: Appointment) {
    return appointment.reasonForAppointment || appointment.agendaType || appointment.subject || '-';
  }

  isSelected(appointment: Appointment) {
    return this.selectedAppointmentIds.has(appointment.id);
  }

  canSelectAppointments() {
    return this.auth.hasRole('APPROVER', 'APPROVER_JT_SECY', 'ADMIN', 'HCM');
  }

  canSelectAppointment(appointment: Appointment) {
    return this.canSelectAppointments() && !!appointment;
  }

  toggleSelection(appointment: Appointment, checked: boolean) {
    if (!this.canSelectAppointment(appointment)) return;
    if (checked) {
      this.selectedAppointmentIds.add(appointment.id);
    } else {
      this.selectedAppointmentIds.delete(appointment.id);
    }
  }

  areAllFilteredSelected() {
    const selectable = this.filtered.filter(appointment => this.canSelectAppointment(appointment));
    return selectable.length > 0 && selectable.every(appointment => this.selectedAppointmentIds.has(appointment.id));
  }

  isSomeFilteredSelected() {
    return this.filtered.some(appointment => this.canSelectAppointment(appointment) && this.selectedAppointmentIds.has(appointment.id))
      && !this.areAllFilteredSelected();
  }

  toggleAllFiltered(checked: boolean) {
    this.filtered.forEach(appointment => {
      if (!this.canSelectAppointment(appointment)) return;
      if (checked) {
        this.selectedAppointmentIds.add(appointment.id);
      } else {
        this.selectedAppointmentIds.delete(appointment.id);
      }
    });
  }

  get selectedAppointments() {
    return this.filtered.filter(appointment => this.selectedAppointmentIds.has(appointment.id));
  }

  get canExportPdf() {
    return this.auth.hasRole('APPROVER', 'APPROVER_JT_SECY', 'HCM');
  }

  get canAssignSelectedToEvent() {
    return this.auth.hasRole('APPROVER', 'ADMIN', 'HCM') &&
      this.selectedAppointments.length > 0 &&
      this.selectedAppointments.every(appointment => appointment.status === 'APPROVED' || this.isFollowUpStatus(appointment.status));
  }

  assignSelectedToEvent() {
    if (!this.canAssignSelectedToEvent || !this.selectedEventId || this.eventAssigning) return;
    this.eventAssigning = true;
    this.appointmentService.assignAppointmentsToEvent(
      this.selectedEventId,
      this.selectedAppointments.map(appointment => appointment.id),
      'Scheduled'
    ).pipe(finalize(() => this.eventAssigning = false))
      .subscribe({
        next: () => {
          this.selectedAppointmentIds.clear();
          this.selectedEventId = null;
          this.snackBar.open('Selected follow-up applications assigned to event.', 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
          this.loadAppointments();
        },
        error: error => this.errorMsg = apiErrorMessage(error, 'Unable to assign selected applications to event.')
      });
  }

  canViewAiNotes() {
    return this.auth.hasRole('HCM', 'ADMIN', 'APPROVER');
  }

  canManageAiNotes() {
    return this.canViewAiNotes();
  }

  getAiNotes(appointment: Appointment) {
    return this.aiNotesByAppointmentId.get(appointment.id) ?? [];
  }

  isAiNotesLoading(appointment: Appointment) {
    return this.aiNotesLoadingAppointmentIds.has(appointment.id);
  }

  hasAiNotesLoadFailed(appointment: Appointment) {
    return this.aiNotesFailedAppointmentIds.has(appointment.id);
  }

  getAiNotesStatus(appointment: Appointment): AiNotesStatus | 'LOADING' | 'NONE' | 'UNAVAILABLE' {
    if (!this.canViewAiNotes()) return 'UNAVAILABLE';
    if (this.isAiNotesLoading(appointment)) return 'LOADING';
    const notes = this.getAiNotes(appointment);
    if (notes.some(note => note.status === 'PROCESSING')) return 'PROCESSING';
    if (notes.some(note => note.status === 'PENDING')) return 'PENDING';
    if (notes.some(note => note.status === 'COMPLETED' || note.status === 'PARTIAL_SUCCESS')) return 'COMPLETED';
    if (notes.some(note => note.status === 'FAILED')) return 'FAILED';
    return 'NONE';
  }

  getAiNotesStatusLabel(appointment: Appointment) {
    switch (this.getAiNotesStatus(appointment)) {
      case 'LOADING': return 'Loading AI notes...';
      case 'PENDING': return 'AI notes pending';
      case 'PROCESSING': return 'Generating notes...';
      case 'COMPLETED': return 'AI notes ready';
      case 'FAILED': return 'AI notes failed';
      case 'UNAVAILABLE': return 'AI notes unavailable';
      default: return 'No AI notes';
    }
  }

  getAiNotesStatusClass(appointment: Appointment) {
    return `ai-status-${this.getAiNotesStatus(appointment).toString().toLowerCase()}`;
  }

  getAiNotesPreview(appointment: Appointment) {
    const completed = this.getAiNotes(appointment).find(note => (note.status === 'COMPLETED' || note.status === 'PARTIAL_SUCCESS') && note.aiSummary);
    if (!completed) return '';
    return this.compactText(completed.aiSummary, 90);
  }

  hasAiNotes(appointment: Appointment) {
    return this.getAiNotes(appointment).length > 0;
  }

  getRegeneratableAiNote(appointment: Appointment) {
    return this.getAiNotes(appointment).find(note => note.documentId && note.status === 'FAILED')
      ?? this.getAiNotes(appointment).find(note => note.documentId);
  }

  openAiNotesDialog(appointment: Appointment) {
    this.selectedAiNotesAppointment = appointment;
    this.selectedAiNotes = this.getAiNotes(appointment);
    this.aiNotesDialogRef = this.dialog.open(this.aiNotesDialog, {
      width: '760px',
      maxWidth: '96vw',
      autoFocus: false,
      panelClass: 'ai-notes-dialog-panel'
    });
    this.aiNotesDialogRef.afterClosed().subscribe(() => {
      this.aiNotesDialogRef = undefined;
      this.selectedAiNotesAppointment = null;
      this.selectedAiNotes = [];
    });
  }

  closeAiNotesDialog() {
    this.aiNotesDialogRef?.close();
  }

  regenerateAiNotes(note: AppointmentDocumentAiNotes, appointment?: Appointment, event?: Event) {
    event?.stopPropagation();
    if (!this.canManageAiNotes() || !note.documentId || this.aiNotesRegeneratingDocumentIds.has(note.documentId)) {
      return;
    }

    this.aiNotesRegeneratingDocumentIds.add(note.documentId);
    this.appointmentService.regenerateAiNotes(note.documentId)
      .pipe(finalize(() => this.aiNotesRegeneratingDocumentIds.delete(note.documentId)))
      .subscribe({
        next: updated => {
          this.replaceAiNote(updated);
          if (appointment) {
            this.loadAiNotesForAppointments([appointment], true);
          }
          this.snackBar.open('AI notes regeneration queued.', 'Close', { duration: 4000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Unable to regenerate AI notes.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  isAiNoteRegenerating(note: AppointmentDocumentAiNotes) {
    return !!note.documentId && this.aiNotesRegeneratingDocumentIds.has(note.documentId);
  }

  private loadAiNotesForAppointments(appointments: Appointment[], force = false) {
    if (!this.canViewAiNotes()) return;
    const uniqueAppointments = appointments.filter((appointment, index, rows) =>
      appointment.id && rows.findIndex(row => row.id === appointment.id) === index
    );

    uniqueAppointments.forEach(appointment => {
      if (!force && (this.aiNotesByAppointmentId.has(appointment.id) || this.aiNotesLoadingAppointmentIds.has(appointment.id))) {
        return;
      }
      this.aiNotesLoadingAppointmentIds.add(appointment.id);
      this.aiNotesFailedAppointmentIds.delete(appointment.id);
      this.appointmentService.getAiNotesByAppointment(appointment.id)
        .pipe(
          catchError(() => {
            this.aiNotesFailedAppointmentIds.add(appointment.id);
            return of([]);
          }),
          finalize(() => this.aiNotesLoadingAppointmentIds.delete(appointment.id))
        )
        .subscribe(notes => {
          this.aiNotesByAppointmentId.set(appointment.id, notes);
          if (this.selectedAiNotesAppointment?.id === appointment.id) {
            this.selectedAiNotes = notes;
          }
          this.scheduleAiNotesRefreshIfNeeded(appointment, notes);
        });
    });
  }

  private scheduleAiNotesRefreshIfNeeded(appointment: Appointment, notes: AppointmentDocumentAiNotes[]) {
    const shouldRefresh = notes.some(note => note.status === 'PENDING' || note.status === 'PROCESSING');
    const existingTimer = this.aiNotesPollTimers.get(appointment.id);
    if (existingTimer) {
      window.clearTimeout(existingTimer);
      this.aiNotesPollTimers.delete(appointment.id);
    }
    if (!shouldRefresh) return;
    const timerId = window.setTimeout(() => {
      this.aiNotesPollTimers.delete(appointment.id);
      this.loadAiNotesForAppointments([appointment], true);
    }, 7000);
    this.aiNotesPollTimers.set(appointment.id, timerId);
  }

  private replaceAiNote(updated: AppointmentDocumentAiNotes) {
    const notes = this.aiNotesByAppointmentId.get(updated.appointmentId) ?? [];
    const index = notes.findIndex(note => note.id === updated.id || note.documentId === updated.documentId);
    const nextNotes = [...notes];
    if (index >= 0) {
      nextNotes[index] = updated;
    } else {
      nextNotes.push(updated);
    }
    this.aiNotesByAppointmentId.set(updated.appointmentId, nextNotes);
    if (this.selectedAiNotesAppointment?.id === updated.appointmentId) {
      this.selectedAiNotes = nextNotes;
    }
  }

  private compactText(value: string, maxLength: number) {
    const normalized = value.replace(/\s+/g, ' ').trim();
    return normalized.length > maxLength ? `${normalized.substring(0, maxLength)}...` : normalized;
  }

  canUseApproverActions(appointment: Appointment | null) {
    return !!appointment && this.auth.hasRole('SUPER_ADMIN', 'APPROVER', 'HCM');
  }

  isLiveWalkIn(appointment: Appointment | null) {
    return isPendingWalkIn(appointment);
  }

  canEditApproverRemarks(appointment: Appointment | null) {
    return !!appointment && this.auth.hasRole('APPROVER', 'APPROVER_JT_SECY', 'ADMIN', 'SUPER_ADMIN');
  }

  canEditHcmRemarks(appointment: Appointment | null) {
    return !!appointment && this.auth.hasRole('APPROVER', 'APPROVER_JT_SECY', 'HCM', 'ADMIN', 'SUPER_ADMIN');
  }

  saveApproverRemarks() {
    this.saveSeparatedRemark('APPROVER_REMARK', this.approverRemarksText, this.approverDepartmentCode);
  }

  saveHcmRemarks() {
    this.saveSeparatedRemark('HCM_REMARK', this.hcmRemarksText);
  }

  remarkTypeLabel(value?: string) {
    if (value === 'APPROVER_REMARK') return 'Approver Remark';
    if (value === 'HCM_REMARK') return 'HCM Remark';
    return value || 'Remark';
  }

  canApproveOrReject(appointment: Appointment | null) {
    return this.canUseApproverActions(appointment) && canRejectScheduled(appointment);
  }

  canRescheduleAppointment(appointment: Appointment | null) {
    return this.canUseApproverActions(appointment) && canScheduleOrReschedule(appointment);
  }

  canReturnAppointmentToPending(appointment: Appointment | null) {
    return this.canUseApproverActions(appointment) && canReturnToPending(appointment);
  }

  canUseCmoActions(appointment: Appointment | null) {
    return !!appointment &&
      this.auth.hasRole('HCM', 'ADMIN', 'APPROVER') &&
      this.cmoQueueStatuses.has(appointment.status);
  }

  openCmoModify(appointment: Appointment) {
    if (!this.canUseCmoActions(appointment)) return;
    this.selectedAppointment = appointment;
    this.cmoModifyEventType = appointment.eventType;
    this.cmoModifyLocation = appointment.requestedLocation;
    this.cmoModifyRemarks = appointment.cmoRemarks ?? '';
    this.cmoModifyDialogRef = this.dialog.open(this.cmoModifyDialog, {
      width: '620px',
      maxWidth: '94vw',
      autoFocus: false,
      panelClass: 'appointment-action-dialog-panel'
    });
    this.cmoModifyDialogRef.afterClosed().subscribe(() => {
      this.cmoModifyDialogRef = undefined;
    });
  }

  openCmoMissingInfo(appointment: Appointment) {
    if (!this.canUseCmoActions(appointment)) return;
    this.selectedAppointment = appointment;
    this.cmoMissingInfoNote = appointment.cmoRemarks ?? '';
    this.cmoMissingInfoDialogRef = this.dialog.open(this.cmoMissingInfoDialog, {
      width: '620px',
      maxWidth: '94vw',
      autoFocus: false,
      panelClass: 'appointment-action-dialog-panel'
    });
    this.cmoMissingInfoDialogRef.afterClosed().subscribe(() => {
      this.cmoMissingInfoDialogRef = undefined;
      this.cmoMissingInfoNote = '';
    });
  }

  closeCmoActionDialog() {
    this.cmoModifyDialogRef?.close();
    this.cmoMissingInfoDialogRef?.close();
  }

  saveCmoModify() {
    if (!this.selectedAppointment || !this.canUseCmoActions(this.selectedAppointment) || this.cmoActionUpdating) return;
    const appointment = this.selectedAppointment;
    this.cmoActionUpdating = true;
    this.appointmentService.submitCmoReview({
      appointmentId: appointment.id,
      eventType: this.cmoModifyEventType,
      requestedLocation: this.cmoModifyLocation,
      cmoRemarks: this.cmoModifyRemarks,
      status: 'APPROVER_REVIEW',
      notifyApplicant: false,
      notifyDeo: false,
    }).pipe(finalize(() => this.cmoActionUpdating = false))
      .subscribe({
        next: updated => {
          this.selectedAppointment = updated;
          this.replaceAppointment(updated);
          this.cmoModifyDialogRef?.close();
          this.snackBar.open(`${updated.applicationId} forwarded to Approver.`, 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to update appointment.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  sendCmoMissingInfoNote() {
    const note = this.cmoMissingInfoNote.trim();
    if (!this.selectedAppointment || !this.canUseCmoActions(this.selectedAppointment)) return;
    if (!note) {
      this.snackBar.open('Please enter the missing information note.', 'Close', { duration: 3000, panelClass: ['error-snackbar'] });
      return;
    }

    const appointment = this.selectedAppointment;
    this.cmoActionUpdating = true;
    this.appointmentService.submitCmoReview({
      appointmentId: appointment.id,
      cmoRemarks: note,
      pendingInformation: note,
      status: 'CMO_REVIEW',
      notifyApplicant: true,
      notifyDeo: true,
    }).pipe(finalize(() => this.cmoActionUpdating = false))
      .subscribe({
        next: updated => {
          this.selectedAppointment = updated;
          this.replaceAppointment(updated);
          this.cmoMissingInfoDialogRef?.close();
          this.snackBar.open(`Missing information note sent for ${updated.applicationId}.`, 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to send missing information note.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  forwardToApprover(appointment: Appointment) {
    if (!this.canUseCmoActions(appointment) || this.cmoActionUpdating) return;
    this.cmoActionUpdating = true;
    this.appointmentService.submitCmoReview({
      appointmentId: appointment.id,
      eventType: appointment.eventType,
      requestedLocation: appointment.requestedLocation,
      cmoRemarks: appointment.cmoRemarks,
      status: 'APPROVER_REVIEW',
      notifyApplicant: false,
      notifyDeo: false,
    }).pipe(finalize(() => this.cmoActionUpdating = false))
      .subscribe({
        next: updated => {
          this.selectedAppointment = updated;
          this.replaceAppointment(updated);
          this.snackBar.open(`${updated.applicationId} forwarded to Approver.`, 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to forward appointment.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  openApprove(appointment: Appointment) {
    this.selectedAppointment = appointment;
    this.pendingAction = 'APPROVE';
    this.remarksText = '';
    this.appointmentRemarksDialogRef = this.dialog.open(this.appointmentRemarksDialog, {
      width: '520px',
      maxWidth: '94vw',
      autoFocus: false,
      panelClass: 'appointment-action-dialog-panel'
    });
    this.appointmentRemarksDialogRef.afterClosed().subscribe(() => {
      this.appointmentRemarksDialogRef = undefined;
      this.pendingAction = null;
      this.remarksText = '';
    });
  }

  openReject(appointment: Appointment) {
    this.selectedAppointment = appointment;
    this.pendingAction = 'REJECT';
    this.remarksText = '';
    this.appointmentRemarksDialogRef = this.dialog.open(this.appointmentRemarksDialog, {
      width: '520px',
      maxWidth: '94vw',
      autoFocus: false,
      panelClass: 'appointment-action-dialog-panel'
    });
    this.appointmentRemarksDialogRef.afterClosed().subscribe(() => {
      this.appointmentRemarksDialogRef = undefined;
      this.pendingAction = null;
      this.remarksText = '';
    });
  }

  openReturnToPending(appointment: Appointment) {
    this.selectedAppointment = appointment;
    this.pendingAction = 'RETURN_PENDING';
    this.remarksText = '';
    this.appointmentRemarksDialogRef = this.dialog.open(this.appointmentRemarksDialog, {
      width: '520px', maxWidth: '94vw', autoFocus: false,
      panelClass: 'appointment-action-dialog-panel'
    });
    this.appointmentRemarksDialogRef.afterClosed().subscribe(() => {
      this.appointmentRemarksDialogRef = undefined;
      this.pendingAction = null;
      this.remarksText = '';
    });
  }

  openReschedule(appointment: Appointment) {
    this.selectedAppointment = appointment;
    this.rescheduleDate = appointment.scheduledDateTime ? new Date(appointment.scheduledDateTime) : null;
    this.rescheduleTime = appointment.scheduledDateTime
      ? this.toTimeInputValue(new Date(appointment.scheduledDateTime))
      : '10:00';
    this.appointmentRescheduleDialogRef = this.dialog.open(this.appointmentRescheduleDialog, {
      width: '460px',
      maxWidth: '94vw',
      autoFocus: false,
      panelClass: 'appointment-action-dialog-panel'
    });
    this.appointmentRescheduleDialogRef.afterClosed().subscribe(() => {
      this.appointmentRescheduleDialogRef = undefined;
      this.rescheduleDate = null;
      this.rescheduleTime = '10:00';
    });
  }

  closeAppointmentRemarksDialog() {
    this.appointmentRemarksDialogRef?.close();
  }

  closeAppointmentRescheduleDialog() {
    this.appointmentRescheduleDialogRef?.close();
  }

  confirmAction() {
    if (!this.selectedAppointment || !this.pendingAction || this.actionUpdating) return;
    const remarks = this.remarksText.trim();
    if (!remarks) {
      this.snackBar.open('Remarks are required.', 'Close', { duration: 3000, panelClass: ['error-snackbar'] });
      return;
    }
    const appointment = this.selectedAppointment;
    const action = this.pendingAction;
    const newStatus: AppointmentStatus = action === 'APPROVE' ? 'APPROVED' : action === 'REJECT' ? 'REJECTED' : 'PENDING';

    this.actionUpdating = true;
    this.appointmentService.updateStatus(appointment.id, newStatus, remarks)
      .pipe(finalize(() => this.actionUpdating = false))
      .subscribe({
        next: updated => {
          this.selectedAppointment = updated;
          this.replaceAppointment(updated);
          this.appointmentRemarksDialogRef?.close();
          const message = action === 'APPROVE'
            ? `${updated.applicationId} approved by Approver. It can now be scheduled.`
            : action === 'REJECT'
              ? (this.isLiveWalkIn(appointment) ? 'Walk-in rejected successfully.' : `${updated.applicationId} has been rejected.`)
              : 'Appointment returned to Pending successfully.';
          this.snackBar.open(message, 'Close', {
            duration: 5000,
            panelClass: [action === 'REJECT' ? 'error-snackbar' : 'success-snackbar']
          });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to update appointment.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  confirmReschedule() {
    if (!this.selectedAppointment || !this.rescheduleDate || !this.rescheduleTime || this.actionUpdating) return;
    const appointment = this.selectedAppointment;
    const scheduledDateTime = this.combineDateAndTime(this.rescheduleDate, this.rescheduleTime);
    if (!scheduledDateTime) return;
    this.actionUpdating = true;
    this.appointmentService.rescheduleAppointment(appointment.id, {
      scheduledDateTime,
      durationMinutes: 30
    }).pipe(finalize(() => this.actionUpdating = false))
      .subscribe({
        next: updated => {
          this.selectedAppointment = updated;
          this.replaceAppointment(updated);
          this.appointmentRescheduleDialogRef?.close();
          this.loadScheduleEvents();
          this.snackBar.open(`Appointment scheduled successfully.`, 'Close', { duration: 5000 });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to reschedule.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  private saveSeparatedRemark(type: 'APPROVER_REMARK' | 'HCM_REMARK', value: string, departmentCode = '') {
    const remarks = value.trim();
    const canEdit = type === 'APPROVER_REMARK'
      ? this.canEditApproverRemarks(this.selectedAppointment)
      : this.canEditHcmRemarks(this.selectedAppointment);
    if (!this.selectedAppointment || !canEdit || this.actionUpdating) return;
    if (!remarks) {
      this.snackBar.open('Please enter remarks before saving.', 'Close', { duration: 3000, panelClass: ['error-snackbar'] });
      return;
    }

    const appointment = this.selectedAppointment;
    const payload = {
      hcmRemarks: remarks,
      decision: type,
      departmentCode: type === 'APPROVER_REMARK' ? departmentCode || undefined : undefined,
    };

    this.actionUpdating = true;
    this.appointmentService.addRemark(appointment.id, payload).pipe(finalize(() => this.actionUpdating = false))
      .subscribe({
        next: _note => {
          this.loadAppointmentRemarks(appointment.id);
          this.loadAppointmentById(appointment.id);
          this.snackBar.open(type === 'APPROVER_REMARK' ? 'Approver remarks saved.' : 'HCM remarks saved.', 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to save remarks.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  onSupportingDocumentSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedSupportingDocument = input.files?.[0] ?? null;
  }

  uploadSupportingDocument() {
    if (!this.selectedAppointment || !this.selectedSupportingDocument || this.supportingDocumentUploading) return;
    this.supportingDocumentUploading = true;
    this.appointmentService.uploadSupportingDocument(this.selectedAppointment.id, this.selectedSupportingDocument)
      .pipe(finalize(() => this.supportingDocumentUploading = false))
      .subscribe({
        next: document => {
          this.documents = [document, ...this.documents];
          this.selectedSupportingDocument = null;
          this.snackBar.open('Supporting document uploaded.', 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Unable to upload supporting document.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  async openProofCamera() {
    try {
      this.proofCaptureError = '';
      this.stopProofCamera();
      this.proofCameraStream = await this.cameraCapture.open(this.proofCameraFacingMode, this.selectedProofCameraDeviceId || undefined);
      this.proofCameraActive = true;
      this.watchProofCameraDisconnect(this.proofCameraStream);
      this.loadProofCameraDevices();
      setTimeout(() => {
        const video = document.getElementById('appointment-proof-camera-preview') as HTMLVideoElement;
        if (video && this.proofCameraStream) {
          this.cameraCapture.attach(video, this.proofCameraStream);
        }
      }, 100);
    } catch (error) {
      this.stopProofCamera();
      this.proofCaptureError = this.proofCameraErrorMessage(error);
    }
  }

  captureMeetingProof() {
    const video = document.getElementById('appointment-proof-camera-preview') as HTMLVideoElement;
    if (!video) {
      this.proofCaptureError = 'Camera is not ready yet.';
      return;
    }

    try {
      const dataUrl = this.cameraCapture.capture(video);
      this.setProofCapture(dataUrl);
    } catch {
      this.proofCaptureError = 'Unable to capture proof photo.';
      return;
    }

    this.stopProofCamera();
  }

  retakeMeetingProof() {
    this.clearProofCapture();
    this.openProofCamera();
  }

  switchProofCamera() {
    this.selectedProofCameraDeviceId = '';
    this.proofCameraFacingMode = this.cameraCapture.toggle(this.proofCameraFacingMode);
    if (this.proofCameraActive) {
      this.openProofCamera();
    }
  }

  onProofCameraDeviceChange(deviceId: string) {
    this.selectedProofCameraDeviceId = deviceId;
    if (this.proofCameraActive) {
      this.openProofCamera();
    }
  }

  proofCameraDeviceLabel(device: CameraDeviceOption, index: number): string {
    return this.cameraCapture.deviceLabel(device, index);
  }

  get proofCameraFacingLabel(): string {
    return this.cameraCapture.label(this.proofCameraFacingMode);
  }

  uploadMeetingProof() {
    if (!this.selectedAppointment || !this.proofCaptureFile || this.supportingDocumentUploading) return;
    this.supportingDocumentUploading = true;
    this.appointmentService.uploadSupportingDocument(this.selectedAppointment.id, this.proofCaptureFile)
      .pipe(finalize(() => this.supportingDocumentUploading = false))
      .subscribe({
        next: document => {
          this.documents = [document, ...this.documents];
          this.clearProofCapture();
          this.snackBar.open('Meeting proof photo uploaded.', 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        },
        error: error => this.snackBar.open(apiErrorMessage(error, 'Unable to upload meeting proof photo.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
      });
  }

  stopProofCamera() {
    this.cameraCapture.stop(this.proofCameraStream);
    this.proofCameraStream = null;
    this.proofCameraActive = false;
  }

  private async loadProofCameraDevices(fromDeviceChange = false) {
    try {
      this.proofCameraOptions = await this.cameraCapture.listVideoDevices();
      if (!this.proofCameraOptions.length) {
        this.selectedProofCameraDeviceId = '';
        if (fromDeviceChange && this.proofCameraActive) {
          this.proofCaptureError = 'No camera found';
          this.stopProofCamera();
        }
        return;
      }

      const selectedStillAvailable = this.proofCameraOptions.some(device => device.deviceId === this.selectedProofCameraDeviceId);
      if (!this.selectedProofCameraDeviceId || !selectedStillAvailable) {
        this.selectedProofCameraDeviceId = this.proofCameraOptions[0].deviceId;
        if (fromDeviceChange && this.proofCameraActive && !selectedStillAvailable) {
          this.proofCaptureError = 'External camera disconnected';
          this.openProofCamera();
        }
      }
    } catch {
      this.proofCameraOptions = [];
      this.selectedProofCameraDeviceId = '';
    }
  }

  private watchProofCameraDisconnect(stream: MediaStream) {
    stream.getVideoTracks().forEach(track => {
      track.onended = () => {
        if (!this.proofCameraStream) return;
        this.proofCaptureError = 'External camera disconnected';
        this.stopProofCamera();
      };
    });
  }

  private proofCameraErrorMessage(error: unknown) {
    const name = error instanceof DOMException ? error.name : '';
    if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
      return 'Camera permission denied. Please allow camera access and try again.';
    }
    if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
      return 'No camera found';
    }
    if (name === 'NotReadableError' || name === 'TrackStartError') {
      return 'Unable to access selected camera. It may be busy in another application.';
    }
    if (name === 'OverconstrainedError' || name === 'ConstraintNotSatisfiedError') {
      return 'Unable to access selected camera. Please select another camera.';
    }
    return error instanceof Error && error.message ? error.message : 'Camera access was blocked. Please allow camera permission and try again.';
  }

  canUploadSupportingDocument() {
    return this.auth.hasRole('APPROVER', 'ADMIN', 'HCM', 'DEO');
  }

  private replaceAppointment(updated: Appointment) {
    const index = this.appointments.findIndex(appointment => appointment.id === updated.id);
    if (index >= 0) {
      this.appointments[index] = updated;
    } else {
      this.appointments = [updated, ...this.appointments];
    }
    this.applyFilter();
  }

  private loadAppointmentById(appointmentId: number) {
    this.appointmentService.getAppointmentById(appointmentId)
      .pipe(catchError(() => of(null)))
      .subscribe(updated => {
        if (!updated) return;
        this.selectedAppointment = updated;
        this.approverRemarksText = updated.approverRemarks ?? '';
        this.hcmRemarksText = updated.hcmRemarks ?? '';
        this.replaceAppointment(updated);
      });
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

  openExportDialog() {
    this.exportSelectedToPdf();
  }

  closeExportDialog() {
    this.appointmentExportDialogRef?.close();
  }

  get canExportWithCurrentOptions() {
    return this.canExportPdf && this.selectedAppointments.length > 0;
  }

  exportSelectedToPdf() {
    if (!this.canExportPdf || this.exportPreparing) return;
    const appointments = [...this.selectedAppointments];
    if (appointments.length === 0) {
      this.snackBar.open('Please select at least one appointment to export.', 'Close', { duration: 4000, panelClass: ['error-snackbar'] });
      return;
    }

    this.exportPreparing = true;
    const appointmentSource = forkJoin(appointments.map(appointment =>
      this.appointmentService.getAppointmentById(appointment.id).pipe(catchError(() => of(appointment)))
    ));

    appointmentSource.subscribe({
        next: refreshedAppointments => {
          Promise.all(refreshedAppointments.map(appointment => this.loadExportPhoto(appointment).catch(() => null)))
            .then(photoRows => {
              this.generateAppointmentPdf(refreshedAppointments, photoRows);
              this.auditPdfExport(refreshedAppointments);
              this.closeExportDialog();
              this.exportPreparing = false;
            })
            .catch(error => {
              this.exportPreparing = false;
              this.snackBar.open(apiErrorMessage(error, 'Unable to export appointments.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
            });
        },
        error: error => {
          this.exportPreparing = false;
          this.snackBar.open(apiErrorMessage(error, 'Unable to export appointments.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
        }
      });
  }

  private generateAppointmentPdf(
    appointments: Appointment[],
    photoRows: Array<string | null>
  ) {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const generatedAt = new Date();
    const photoByRow = new Map<number, string>();
    photoRows.forEach((photo, index) => {
      if (photo) photoByRow.set(index, photo);
    });

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(16);
    doc.text('Appointment Agenda List', 14, 14);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.text(`Generated: ${generatedAt.toLocaleString()}`, 14, 21);
    doc.text(`From Date: ${this.filterFromDate ? this.filterFromDate.toLocaleDateString() : 'All'}`, 14, 27);
    doc.text(`To Date: ${this.filterToDate ? this.filterToDate.toLocaleDateString() : 'All'}`, 70, 27);
    doc.text(`Status: ${this.filterStatus ? this.getStatusLabel(this.filterStatus as AppointmentStatus) : 'All'}`, 126, 27);

    autoTable(doc, {
      startY: 34,
      head: [['S.No', 'Token', 'Applicant Name', 'Agenda', 'Brief Agenda / Brief Description', 'Mobile Number', 'Address', 'Visitor Photo', 'Approver Remarks', 'Forwarded Department', 'HCM Remarks / Directions']],
      body: appointments.map((appointment, index) => {
        const remarkFields = appointmentRemarkExportFields(appointment);
        return [
          index + 1,
          this.exportTokenLastFour(appointment),
          this.getDisplayName(appointment),
          appointment.agendaType || appointment.subject || '-',
          appointment.agendaBrief || appointment.reasonForAppointment || appointment.applicant?.briefDescription || '-',
          this.getDisplayPhone(appointment),
          this.applicantAddress(appointment) || appointment.guestAddress || '-',
          photoByRow.has(index) ? '' : 'No Photo',
          remarkFields.approverRemarks,
          remarkFields.forwardedDepartment,
          remarkFields.hcmRemarks,
        ];
      }),
      theme: 'grid',
      styles: { font: 'helvetica', fontSize: 8, cellPadding: 2, overflow: 'linebreak', valign: 'middle' },
      headStyles: { fillColor: [31, 41, 55], textColor: 255, fontStyle: 'bold' },
      columnStyles: {
        0: { cellWidth: 12, halign: 'center' },
        1: { cellWidth: 14, halign: 'center' },
        2: { cellWidth: 24 },
        3: { cellWidth: 20 },
        4: { cellWidth: 32 },
        5: { cellWidth: 20 },
        6: { cellWidth: 28 },
        7: { cellWidth: 18, minCellHeight: 24, halign: 'center' },
        8: { cellWidth: 30 },
        9: { cellWidth: 23 },
        10: { cellWidth: 32 },
      },
      margin: { left: 14, right: 14 },
      didParseCell: data => {
        if (data.section === 'body' && data.column.index === 7) {
          data.cell.styles.minCellHeight = 24;
        }
      },
      didDrawCell: data => {
        if (data.section !== 'body' || data.column.index !== 7) return;
        const photo = photoByRow.get(data.row.index);
        if (!photo) return;
        try {
          doc.addImage(photo, this.imageFormat(photo), data.cell.x + 5, data.cell.y + 2, 18, 18);
        } catch {
          doc.setFontSize(8);
          doc.text('No Photo', data.cell.x + 5, data.cell.y + 12);
        }
      },
    });

    doc.save(`appointment-agenda-list-${generatedAt.toISOString().slice(0, 10)}.pdf`);
  }

  private exportTokenLastFour(appointment: Appointment) {
    const token = (appointment.walkInTokenNumber || '').trim();
    if (!token) return '-';
    const digits = token.replace(/\D/g, '');
    if (digits.length >= 4) {
      return digits.slice(-4);
    }
    return token.length > 4 ? token.slice(-4) : token;
  }

  private async loadExportPhoto(appointment: Appointment): Promise<string | null> {
    const directPhoto = await this.toPdfImageData(this.resolveVisitorPhoto(appointment.applicant));
    if (directPhoto) return directPhoto;

    const visitorId = appointment.applicantId || appointment.applicant?.id;
    if (!visitorId) return null;

    const visitor = await new Promise<any | null>(resolve => {
      this.visitorService.getById(visitorId)
        .pipe(catchError(() => of(null)))
        .subscribe(value => resolve(value));
    });
    return this.toPdfImageData(this.resolveVisitorPhoto(visitor));
  }

  private async toPdfImageData(value?: string | null): Promise<string | null> {
    const photo = (value || '').trim();
    if (!photo) return null;
    if (photo.startsWith('data:image/')) return photo;
    if (/^[A-Za-z0-9+/=]+$/.test(photo) && photo.length > 100) {
      return `data:image/jpeg;base64,${photo}`;
    }
    if (/^https?:\/\//i.test(photo) || photo.startsWith('/')) {
      return this.imageUrlToDataUri(photo).catch(() => null);
    }
    return null;
  }

  private imageUrlToDataUri(url: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.crossOrigin = 'anonymous';
      image.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        const context = canvas.getContext('2d');
        if (!context) {
          reject(new Error('Canvas is unavailable.'));
          return;
        }
        context.drawImage(image, 0, 0);
        resolve(canvas.toDataURL('image/jpeg', 0.82));
      };
      image.onerror = () => reject(new Error('Image load failed.'));
      image.src = url;
    });
  }

  private imageFormat(dataUri: string): 'JPEG' | 'PNG' {
    return dataUri.toLowerCase().startsWith('data:image/png') ? 'PNG' : 'JPEG';
  }

  private auditPdfExport(appointments: Appointment[]) {
    this.appointmentService.auditPdfExport({
      selectedCount: appointments.length,
      appointmentIds: appointments.map(appointment => appointment.id),
      filters: {
        fromDate: this.filterFromDate ? this.toLocalDate(this.filterFromDate) : undefined,
        toDate: this.filterToDate ? this.toLocalDate(this.filterToDate) : undefined,
        status: this.filterStatus || undefined,
      },
    }).pipe(catchError(() => of(undefined))).subscribe();
  }

  private buildAppointmentExport(appointments: Appointment[], remarksByAppointmentId: Map<number, AppointmentRemark[]>) {
    const columns: Array<{ header: string; value: (appointment: Appointment, remarks: AppointmentRemark[]) => unknown }> = [];
    if (this.exportOptions.basic) {
      columns.push(
        { header: 'Application ID', value: appointment => appointment.applicationId },
        { header: 'Appointment ID', value: appointment => appointment.id },
        { header: 'Source', value: appointment => appointment.appointmentSource || 'CITIZEN' },
        { header: 'Status', value: appointment => this.getStatusLabel(appointment.status) },
        { header: 'Event Type', value: appointment => appointment.eventType },
        { header: 'Location', value: appointment => appointment.requestedLocation },
        { header: 'Agenda', value: appointment => appointment.agendaType || appointment.subject || '' },
        { header: 'Purpose / Agenda Brief', value: appointment => appointment.agendaBrief || appointment.reasonForAppointment || '' },
        { header: 'Short Notes', value: appointment => appointment.shortNotes || '' },
        { header: 'Created At', value: appointment => appointment.createdAt || '' },
        { header: 'Submitted At', value: appointment => appointment.submittedAt || appointment.createdAt || '' },
        { header: 'Updated At', value: appointment => appointment.updatedAt || '' },
      );
    }
    if (this.exportOptions.citizen) {
      columns.push(
        { header: 'Applicant Name', value: appointment => appointment.applicant?.fullName || appointment.applicantName || appointment.guestName || '' },
        { header: 'Applicant Phone', value: appointment => appointment.applicant?.phoneNumber || appointment.applicantPhone || appointment.guestMobile || '' },
        { header: 'EPIC Number', value: appointment => appointment.applicant?.epicNumber || '' },
        { header: 'KYC Status', value: appointment => appointment.applicant?.kycStatus || '' },
        { header: 'Designation', value: appointment => this.getDisplayDesignation(appointment) },
        { header: 'Address', value: appointment => this.applicantAddress(appointment) || appointment.guestAddress || '' },
        { header: 'District', value: appointment => appointment.applicant?.district || '' },
        { header: 'Constituency', value: appointment => appointment.applicant?.constituency || '' },
        { header: 'Meeting Count Last 6 Months', value: appointment => appointment.meetingCountLast6Months ?? '' },
      );
    }
    if (this.exportOptions.guest) {
      columns.push(
        { header: 'Guest Reference ID', value: appointment => appointment.guestReferenceId || '' },
        { header: 'Guest Name', value: appointment => appointment.guestName || '' },
        { header: 'Guest Mobile', value: appointment => appointment.guestMobile || '' },
        { header: 'Guest Email', value: appointment => appointment.guestEmail || '' },
        { header: 'Organization', value: appointment => appointment.organizationName || '' },
        { header: 'Guest Designation', value: appointment => appointment.guestDesignation || '' },
        { header: 'Visitor Category', value: appointment => appointment.visitorCategory || '' },
        { header: 'Referred Office', value: appointment => appointment.referredOffice || '' },
        { header: 'Referred By', value: appointment => appointment.referredByName || '' },
        { header: 'Preferred Date', value: appointment => appointment.preferredDate || '' },
      );
    }
    if (this.exportOptions.schedule) {
      columns.push(
        { header: 'Scheduled Date Time', value: appointment => appointment.scheduledDateTime || '' },
        { header: 'Scheduled Duration Minutes', value: appointment => appointment.scheduledDurationMinutes ?? '' },
      );
    }
    if (this.exportOptions.workflow) {
      columns.push(
        { header: 'CMO Remarks', value: appointment => appointment.cmoRemarks || '' },
        { header: 'Approver Remarks', value: appointment => appointment.approverRemarks || '' },
        { header: 'HCM Remarks / Directions', value: appointment => appointment.hcmRemarks || 'Not Available' },
        { header: 'Allocated / Forwarded Department', value: appointment => appointment.department || '' },
      );
    }
    if (this.exportOptions.hcmActions) {
      columns.push(
        { header: 'HCM / APPROVER Action Count', value: (_appointment, remarks) => remarks.length },
        { header: 'HCM / APPROVER Decisions', value: (_appointment, remarks) => this.joinRemarks(remarks, 'decision') },
        { header: 'Departments Forwarded', value: (appointment, remarks) => this.joinDepartments(appointment, remarks) },
        { header: 'HCM / APPROVER Remarks History', value: (_appointment, remarks) => this.joinRemarks(remarks, 'hcmRemarks') },
        { header: 'HCM / APPROVER Actioned By', value: (_appointment, remarks) => remarks.map(note => [note.createdByRole, note.createdBy].filter(Boolean).join('/')).filter(Boolean).join(' | ') },
        { header: 'HCM / APPROVER Actioned At', value: (_appointment, remarks) => remarks.map(note => note.createdAt).filter(Boolean).join(' | ') },
      );
    }
    if (this.exportOptions.associates) {
      columns.push(
        { header: 'Associate Count', value: appointment => appointment.associates?.length || 0 },
        { header: 'Associate Names', value: appointment => (appointment.associates || []).map(item => item.fullName).filter(Boolean).join(' | ') },
        { header: 'Associate Mobiles', value: appointment => (appointment.associates || []).map(item => item.mobileNumber).filter(Boolean).join(' | ') },
        { header: 'Associate KYC Status', value: appointment => (appointment.associates || []).map(item => item.kycStatus).filter(Boolean).join(' | ') },
        { header: 'Associate Remarks', value: appointment => (appointment.associates || []).map(item => item.remarks || item.relationship).filter(Boolean).join(' | ') },
      );
    }

    return {
      headers: columns.map(column => column.header),
      rows: appointments.map(appointment => {
        const remarks = remarksByAppointmentId.get(appointment.id) ?? [];
        return columns.map(column => column.value(appointment, remarks));
      }),
    };
  }

  private joinRemarks(remarks: AppointmentRemark[], key: 'decision' | 'hcmRemarks') {
    return remarks.map(note => note[key]).filter(Boolean).join(' | ');
  }

  private joinDepartments(appointment: Appointment, remarks: AppointmentRemark[]) {
    const values = [
      appointment.department,
      ...remarks.map(note => note.departmentName || note.departmentCode),
    ].filter(Boolean) as string[];
    return Array.from(new Set(values)).join(' | ');
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

  private combineDateAndTime(date: Date | null, time: string | null | undefined): string | null {
    if (!date || !time) return null;
    const [hours, minutes] = time.split(':').map(value => Number(value));
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null;
    const value = new Date(date);
    value.setHours(hours, minutes, 0, 0);
    return this.toLocalDateTime(value);
  }

  private toLocalDateTime(date: Date): string {
    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  private toLocalDate(date: Date): string {
    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  private toTimeInputValue(date: Date): string {
    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
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
    this.resetRemarkForms();
    this.loadVisitorPhoto(appointment);
    this.loadDocuments(appointment.id);
    this.loadAppointmentRemarks(appointment.id);
    this.dialog.open(this.appointmentDetailsDialog, {
      width: '940px',
      maxWidth: '96vw',
      autoFocus: false,
      panelClass: 'appointment-details-dialog-panel'
    });
  }

  associateCount(appointment: Appointment | null = this.selectedAppointment) {
    return appointment?.associates?.length || 0;
  }

  associateKycLabel(status?: string) {
    return (status || 'PENDING')
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, char => char.toUpperCase());
  }

  associateKycClass(status?: string) {
    const normalized = (status || '').toUpperCase();
    if (['PHOTO_MATCHED', 'DEMOGRAPHIC_MATCHED', 'VERIFIED'].includes(normalized)) {
      return 'status-success';
    }
    if (['KYC_PENDING', 'PENDING', 'MANUAL_VERIFICATION_REQUIRED'].includes(normalized)) {
      return 'status-warn';
    }
    if (['FAILED', 'REJECTED', 'BLOCKED', 'INACTIVE'].includes(normalized)) {
      return 'status-danger';
    }
    return 'status-info';
  }

  closeViewDetails() {
    this.dialog.closeAll();
    this.selectedAppointment = null;
    this.clearVisitorPhotoState();
    this.documents = [];
    this.documentsError = '';
    this.selectedAppointmentRemarks = [];
    this.selectedAppointmentRemarksError = '';
    this.selectedAppointmentRemarksLoading = false;
    this.resetRemarkForms();
    this.selectedSupportingDocument = null;
    this.stopProofCamera();
    this.clearProofCapture();
  }

  private loadAppointmentRemarks(appointmentId: number) {
    this.selectedAppointmentRemarks = [];
    this.selectedAppointmentRemarksError = '';
    this.selectedAppointmentRemarksLoading = true;
    this.appointmentService.getRemarks(appointmentId)
      .pipe(finalize(() => this.selectedAppointmentRemarksLoading = false))
      .subscribe({
        next: remarks => {
          this.selectedAppointmentRemarks = remarks;
          const approverNote = remarks.find(note => note.decision === 'APPROVER_REMARK');
          this.approverDepartmentCode = approverNote?.departmentCode ?? '';
        },
        error: error => this.selectedAppointmentRemarksError = apiErrorMessage(error, 'Unable to load HCM/APPROVER remarks.')
      });
  }

  private resetRemarkForms() {
    this.approverRemarksText = this.selectedAppointment?.approverRemarks ?? '';
    this.hcmRemarksText = this.selectedAppointment?.hcmRemarks ?? '';
    this.approverDepartmentCode = '';
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
    const source = visitor?.photoUrl || visitor?.livePhotoBase64 || visitor?.photoBase64 || '';
    return resolvePhotoUrl(source) || '';
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

  isWordDocument(doc: AppointmentDocument | null = this.selectedDocument) {
    const mimeType = this.getDocumentMimeType(doc);
    const extension = this.getDocumentExtension(doc);
    return mimeType === 'application/msword'
      || mimeType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      || ['doc', 'docx'].includes(extension);
  }

  shouldRenderPdfPreview(doc: AppointmentDocument | null = this.selectedDocument) {
    return this.isPdfDocument(doc) || this.isWordDocument(doc);
  }

  canInlinePreview(doc: AppointmentDocument | null = this.selectedDocument) {
    return this.isImageDocument(doc) || this.isPdfDocument(doc) || this.isWordDocument(doc);
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

  private setProofCapture(dataUrl: string) {
    this.proofCaptureUrl = dataUrl;
    this.proofCaptureFile = this.dataUrlToFile(dataUrl, this.meetingProofFileName());
    this.proofCaptureError = '';
  }

  private clearProofCapture() {
    this.proofCaptureUrl = '';
    this.proofCaptureFile = null;
    this.proofCaptureError = '';
  }

  private dataUrlToFile(dataUrl: string, fileName: string) {
    const [header, base64Data] = dataUrl.split(',');
    const mimeType = header.match(/data:(.*?);base64/)?.[1] || 'image/jpeg';
    const binary = atob(base64Data || '');
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index++) {
      bytes[index] = binary.charCodeAt(index);
    }
    return new File([bytes], fileName, { type: mimeType });
  }

  private meetingProofFileName() {
    const applicationId = this.selectedAppointment?.applicationId || 'appointment';
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    return `${applicationId}-meeting-proof-${timestamp}.jpg`;
  }

  trackByDocumentId(index: number, doc: AppointmentDocument) {
    return doc.id ?? index;
  }

}
