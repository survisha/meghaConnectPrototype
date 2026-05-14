import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppointmentService } from '../services/appointment.service';
import { ReferenceDataService } from '../services/reference-data.service';
import { Appointment, AppointmentStatus, EventType } from '../models';
import { environment } from '../../environments/environment';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, provideNativeDateAdapter } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { apiErrorMessage } from '../shared/api-error.util';

type ChipSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

@Component({
  selector: 'app-approver-workflow',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatSnackBarModule,
    MatBadgeModule,
    MatTooltipModule
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './approver-workflow.component.html',
  styleUrls: ['./approver-workflow.component.scss'],
})
export class ApproverWorkflowComponent implements OnInit {

  appointments: Appointment[] = [];
  filtered: Appointment[] = [];
  selected: Appointment | null = null;
  loading = false;
  errorMsg = '';
  search = '';
  filterStatus = '';
  filterEventType = '';
  filterFromDate: Date | null = null;
  filterToDate: Date | null = null;
  private readonly allowDummyFallback = !environment.production || environment.appName.includes('[UAT]');
  displayedColumns: string[] = ['applicationId', 'applicant', 'district', 'agenda', 'eventType', 'location', 'status', 'aiNotes', 'actions'];

  eventTypeOptions: Array<{ label: string; value: EventType | '' }> = [{ label: 'All Types', value: '' }];

  statusOptions: Array<{ label: string; value: AppointmentStatus | '' }> = [
    { label: 'All Statuses', value: '' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'CMO Review', value: 'CMO_REVIEW' },
    { label: 'Approver Review', value: 'APPROVER_REVIEW' },
    { label: 'Follow-up', value: 'FOLLOWUP' },
    { label: 'HCM Pending', value: 'HCM_PENDING' },
    { label: 'Approved with Date/Time', value: 'APPROVED_WITH_DATE_TIME' },
    { label: 'Scheduled', value: 'SCHEDULED' },
    { label: 'Completed', value: 'COMPLETED' },
  ];

  showRemarksDialog = false;
  showRescheduleDialog = false;
  remarksText = '';
  rescheduleDate = '';
  pendingAction: 'APPROVE' | 'REJECT' | null = null;
  followUpUpdatingId: number | null = null;
  private readonly followUpStatuses: AppointmentStatus[] = ['FOLLOWUP', 'SELECTED_FOR_PUBLIC_DARBAR'];
  private readonly followUpReviewableStatuses: AppointmentStatus[] = ['CREATED', 'SUBMITTED', 'PENDING_APPROVER_REVIEW', 'CMO_REVIEW', 'APPROVER_REVIEW'];

  constructor(
    private appointmentService: AppointmentService,
    private referenceDataService: ReferenceDataService,
    private snackBar: MatSnackBar
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
    this.appointmentService.getApproverAppointments(0, 100).subscribe({
      next: page => {
        this.errorMsg = '';
        this.appointments = page.content.filter(a =>
          ['SUBMITTED', 'PENDING_APPROVER_REVIEW', 'CMO_REVIEW', 'APPROVER_REVIEW', 'HCM_PENDING', 'FOLLOWUP'].includes(a.status)
        );
        this.applyFilter();
        this.loading = false;
      },
      error: error => {
        this.errorMsg = apiErrorMessage(error, 'Unable to load approver appointments from API. Please try again.');
        if (this.allowDummyFallback) {
          // TODO: Remove dummy fallback after API stabilization.
          this.initializeDummyData();
        } else {
          this.appointments = [];
          this.applyFilter();
        }
        this.loading = false;
      }
    });
  }

  private initializeDummyData() {
    this.appointments = [
      {
        id: 201,
        applicationId: 'MC-2024-00151',
        applicant: {
          id: 201,
          fullName: 'Manik Lyngdoh',
          phoneNumber: '+91-9856234789',
          epicNumber: 'MH/01/EKH/890123',
          district: 'East Khasi Hills',
          constituency: 'Shillong City',
          booth: 'EKH/890',
          designation: 'NGO Worker',
          village: 'Mawlai'
        },
        agendaType: 'Governance',
        agendaBrief: 'Request for CM intervention in illegal mining activities in nearby quarry. Environmental concerns raised by local community.',
        status: 'APPROVER_REVIEW',
        requestedLocation: 'SHILLONG',
        eventType: 'A4',
        mlaMdcApproved: true,
        meetingCountLast6Months: 1,
        cmoRemarks: 'Verified complaint. Local authorities notified. Environmental impact assessment submitted. Recommend CM review.',
        approverRemarks: '',
        shortNotes: 'AI Summary: Environmental protection case backed by 300+ signatures. Video evidence of violations submitted. Local MLA strongly supports. Requires urgent CM attention for enforcement action.',
        submittedAt: '2024-03-16T09:00:00',
        updatedAt: '2024-03-17T14:30:00'
      },
      {
        id: 202,
        applicationId: 'MC-2024-00152',
        applicant: {
          id: 202,
          fullName: 'Salsrang Marak',
          phoneNumber: '+91-9774123456',
          epicNumber: 'MH/02/WGH/901234',
          district: 'West Garo Hills',
          constituency: 'Dadenggre',
          booth: 'WGH/901',
          designation: 'Farmer Leader',
          village: 'Dadenggre'
        },
        agendaType: 'Scheme availment (CM)',
        agendaBrief: 'CMSDF application for farmers cooperative cold storage facility. 50 farmers. Project cost: ₹45 lakhs.',
        status: 'APPROVER_REVIEW',
        requestedLocation: 'TURA',
        eventType: 'B1',
        mlaMdcApproved: true,
        meetingCountLast6Months: 0,
        cmoRemarks: 'Farmers cooperative registered. DPR verified by Agriculture Dept. 50 member farmers. Land available. Recommend approval.',
        approverRemarks: '',
        shortNotes: 'AI Summary: Agricultural infrastructure project. 50 farmer families benefit. Post-harvest loss reduction estimated 40%. Market linkage confirmed with Tura wholesale market. Strong economic viability.',
        submittedAt: '2024-03-15T11:20:00',
        updatedAt: '2024-03-17T10:15:00'
      },
      {
        id: 203,
        applicationId: 'MC-2024-00153',
        applicant: {
          id: 203,
          fullName: 'Banrishisha Syiem',
          phoneNumber: '+91-9887665544',
          epicNumber: 'MH/03/RBH/012345',
          district: 'Ri Bhoi',
          constituency: 'Jirang',
          booth: 'RBH/012',
          designation: 'Youth Leader',
          village: 'Byrnihat'
        },
        agendaType: 'Trade & Commerce',
        agendaBrief: 'CM Elevate application - Youth skill training center for industrial belt workers. Expected to train 200 youth annually.',
        status: 'CMO_REVIEW',
        requestedLocation: 'SHILLONG',
        eventType: 'A4',
        mlaMdcApproved: false,
        meetingCountLast6Months: 0,
        cmoRemarks: 'New application under CM Elevate. Youth entrepreneur with detailed project plan. Located in industrial area. Awaiting skill development dept clearance.',
        approverRemarks: '',
        shortNotes: 'AI Summary: Skill development initiative targeting industrial belt youth. Partnerships with 12 local companies confirmed. Placement rate projection: 75%. First-of-kind in Byrnihat region.',
        submittedAt: '2024-03-17T08:30:00',
        updatedAt: '2024-03-17T16:00:00'
      },
      {
        id: 204,
        applicationId: 'MC-2024-00154',
        applicant: {
          id: 204,
          fullName: 'Kong Aidalin Kharkongor',
          phoneNumber: '+91-9856777888',
          epicNumber: 'MH/01/JH/123456',
          district: 'Jaintia Hills',
          constituency: 'Jowai',
          booth: 'JH/123',
          designation: 'Social Activist',
          village: 'Jowai Town'
        },
        agendaType: 'Public Grievance',
        agendaBrief: 'Request for improved healthcare infrastructure - renovation of Primary Health Centre serving 5000+ population.',
        status: 'HCM_PENDING',
        requestedLocation: 'SHILLONG',
        eventType: 'A4',
        mlaMdcApproved: true,
        meetingCountLast6Months: 1,
        cmoRemarks: 'PHC inspection completed. Critical infrastructure gaps identified. Health Dept estimate: ₹18L. Strong community need.',
        approverRemarks: 'Approved. Forwarding to CM for final decision. Project addresses critical healthcare gap in underserved area.',
        shortNotes: 'AI Summary: Healthcare infrastructure urgent need. PHC serves 5000+ residents with outdated equipment. Maternal mortality concerns raised. Health Dept strongly recommends. Already approved by Jt. Secretary.',
        submittedAt: '2024-03-14T10:00:00',
        updatedAt: '2024-03-17T11:45:00'
      }
    ];
    this.applyFilter();
  }

  applyFilter() {
    const searchValue = this.search.trim().toLowerCase();
    this.filtered = this.appointments.filter(appt => {
      const createdDate = this.parseAppointmentDate(appt);
      return this.matchesSearch(appt, searchValue) &&
        (!this.filterStatus || this.matchesStatusFilter(appt.status, this.filterStatus)) &&
        (!this.filterEventType || appt.eventType === this.filterEventType) &&
        (!this.filterFromDate || (createdDate && createdDate >= this.startOfDay(this.filterFromDate))) &&
        (!this.filterToDate || (createdDate && createdDate < this.nextDay(this.filterToDate)));
    });
  }

  private matchesSearch(appt: Appointment, searchValue: string) {
    if (!searchValue) return true;
    return [
      appt.applicationId,
      appt.applicant?.fullName,
      appt.applicantName,
      appt.applicant?.phoneNumber,
      appt.applicantPhone,
      appt.applicant?.district,
      appt.agendaType,
      appt.agendaBrief,
      appt.requestedLocation,
    ].some(value => String(value ?? '').toLowerCase().includes(searchValue));
  }

  private matchesStatusFilter(status: AppointmentStatus, filterStatus: string) {
    if (filterStatus === 'FOLLOWUP') {
      return this.followUpStatuses.includes(status);
    }
    return status === filterStatus;
  }

  private parseAppointmentDate(appt: Appointment): Date | null {
    const value = appt.submittedAt || appt.createdAt || appt.updatedAt || appt.scheduledDateTime;
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

  getSeverity(status: string): ChipSeverity {
    const map: Record<string, ChipSeverity> = {
      APPROVER_REVIEW: 'warn',
      CMO_REVIEW: 'info',
      HCM_PENDING: 'secondary',
      HCM_ACCEPTED: 'success',
      HCM_REJECTED: 'danger',
      FOLLOWUP: 'warn',
      SELECTED_FOR_PUBLIC_DARBAR: 'warn',
      SCHEDULED: 'success',
    };
    return map[status] ?? 'info';
  }

  getStatusLabel(status: string): string {
    if (this.followUpStatuses.includes(status as AppointmentStatus)) return 'FOLLOW-UP';
    return status.replace(/_/g, ' ');
  }

  canMarkFollowUp(appt: Appointment): boolean {
    return appt.eventType === 'B1' && this.followUpReviewableStatuses.includes(appt.status);
  }

  openApprove(appt: Appointment) {
    this.selected = appt;
    this.pendingAction = 'APPROVE';
    this.remarksText = '';
    this.showRemarksDialog = true;
  }

  openReject(appt: Appointment) {
    this.selected = appt;
    this.pendingAction = 'REJECT';
    this.remarksText = '';
    this.showRemarksDialog = true;
  }

  openReschedule(appt: Appointment) {
    this.selected = appt;
    this.rescheduleDate = '';
    this.showRescheduleDialog = true;
  }

  markFollowUp(appt: Appointment) {
    if (!this.canMarkFollowUp(appt) || this.followUpUpdatingId) return;
    this.followUpUpdatingId = appt.id;
    this.appointmentService.markFollowUp(appt.id, 'Follow-up').subscribe({
      next: () => {
        this.snackBar.open(`${appt.applicationId} marked as follow-up.`, 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        this.followUpUpdatingId = null;
        this.loadAppointments();
      },
      error: error => {
        this.followUpUpdatingId = null;
        this.snackBar.open(apiErrorMessage(error, 'Failed to mark appointment as follow-up.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
      }
    });
  }

  confirmAction() {
    if (!this.selected || !this.pendingAction) return;
    const newStatus = this.pendingAction === 'APPROVE' ? 'HCM_PENDING' : 'HCM_REJECTED';
    this.appointmentService.updateStatus(this.selected.id, newStatus, this.remarksText).subscribe({
      next: updated => {
        if (this.pendingAction === 'APPROVE') {
          this.snackBar.open(`${updated.applicationId} approved and pushed to HCM queue.`, 'Close', { duration: 5000, panelClass: ['success-snackbar'] });
        } else {
          this.snackBar.open(`${updated.applicationId} has been rejected.`, 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
        }
        this.showRemarksDialog = false;
        this.selected = null;
        this.pendingAction = null;
        this.loadAppointments();
      },
      error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to update appointment.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
    });
  }

  confirmReschedule() {
    if (!this.selected || !this.rescheduleDate) return;
    this.appointmentService.rescheduleAppointment(this.selected.id, {
      scheduledDateTime: new Date(this.rescheduleDate).toISOString().slice(0, 19),
      durationMinutes: 30
    }).subscribe({
      next: updated => {
        this.snackBar.open(`${updated.applicationId} rescheduled to ${this.rescheduleDate}.`, 'Close', { duration: 5000 });
      },
      error: error => this.snackBar.open(apiErrorMessage(error, 'Failed to reschedule.'), 'Close', { duration: 5000, panelClass: ['error-snackbar'] })
    });
    this.showRescheduleDialog = false;
    this.selected = null;
  }
}
