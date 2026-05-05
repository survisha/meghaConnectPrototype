import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from '../../services/appointment.service';
import { AuthService } from '../../services/auth.service';
import { Appointment, AppointmentStatus } from '../../models';
import { environment } from '../../../environments/environment';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CmoReviewModalComponent } from '../cmo-review-modal/cmo-review-modal.component';

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
    MatTooltipModule
  ],
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss'],
})
export class AppointmentListComponent implements OnInit {
  appointments: Appointment[] = [];
  filtered: Appointment[] = [];
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

  constructor(private appointmentService: AppointmentService, public auth: AuthService, private dialog: MatDialog) {}

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
      error: () => {
        this.errorMsg = 'Unable to load appointments from API. Please try again.';
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
