import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppointmentService } from '../services/appointment.service';
import { Appointment, EventType, Location, Visitor } from '../models';

// Angular Material
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';

type ChipSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

@Component({
  selector: 'app-cmo-moderation',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatTableModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatBadgeModule, MatTooltipModule,
  ],
  templateUrl: './cmo-moderation.component.html',
  styleUrls: ['./cmo-moderation.component.scss'],
})
export class CmoModerationComponent implements OnInit {

  appointments: Appointment[] = [];
  selected: Appointment | null = null;
  showModifyDialog = false;
  loading = false;

  modifyEventType: EventType = 'A4';
  modifyLocation: Location = 'SHILLONG';
  modifyRemarks = '';

  displayedColumns: string[] = [
    'applicationId', 'applicant', 'district', 'agenda', 'category', 
    'location', 'mlaApproved', 'status', 'actions'
  ];

  readonly eventTypeOptions: { label: string; value: EventType }[] = [
    { label: 'A1 – Cabinet/Minister/Media/Flight', value: 'A1' },
    { label: 'A2 – Event / Programme',              value: 'A2' },
    { label: 'A3 – File Clearing / Birthday',       value: 'A3' },
    { label: 'A4 – Individual Appointment',         value: 'A4' },
    { label: 'B1 – Public Durbar',                  value: 'B1' },
    { label: 'B2 – Public Walk-in',                 value: 'B2' },
  ];

  readonly locationOptions: { label: string; value: Location }[] = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura',     value: 'TURA' },
    { label: 'Delhi',    value: 'DELHI' },
    { label: 'Others',   value: 'OTHERS' },
  ];

  constructor(private appointmentService: AppointmentService, private snackBar: MatSnackBar) {}

  ngOnInit() {
    this.initializeDummyData();
    
    this.loading = true;
    this.appointmentService.getAllAppointments(0, 100).subscribe({
      next: page => {
        const filtered = page.content.filter(a =>
          ['SUBMITTED', 'DEO_PROCESSED', 'CMO_REVIEW'].includes(a.status)
        );
        if (filtered.length > 0) {
          this.appointments = filtered;
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  initializeDummyData() {
    const dummyApplicants: Visitor[] = [
      {
        id: 901,
        fullName: 'Dr. Carness Lyngdoh',
        designation: 'Medical Officer',
        constituency: 'South Shillong',
        district: 'East Khasi Hills',
        booth: 'EKH/234',
        phoneNumber: '+91-9876543210',
        epicNumber: 'EKH1234567',
      },
      {
        id: 902,
        fullName: 'Shri Balajied Syiemlieh',
        designation: 'Farmer & Cooperative Member',
        constituency: 'Nongkrem',
        district: 'Ri Bhoi',
        booth: 'RBH/567',
        phoneNumber: '+91-8765432109',
        epicNumber: 'RBH2345678',
      },
      {
        id: 903,
        fullName: 'Kong Aidashisha Kharkongor',
        designation: 'Social Worker',
        constituency: 'Mawsynram',
        district: 'East Khasi Hills',
        booth: 'EKH/890',
        phoneNumber: '+91-7654321098',
        epicNumber: 'EKH3456789',
      },
      {
        id: 904,
        fullName: 'Shri Tengrik M. Sangma',
        designation: 'Village Headman',
        constituency: 'Williamnagar',
        district: 'East Garo Hills',
        booth: 'EGH/345',
        phoneNumber: '+91-6543210987',
        epicNumber: 'EGH4567890',
      },
      {
        id: 905,
        fullName: 'Dr. Wallambok Nongkhlaw',
        designation: 'Principal, Govt. College',
        constituency: 'Jowai',
        district: 'West Jaintia Hills',
        booth: 'WJH/678',
        phoneNumber: '+91-5432109876',
        epicNumber: 'WJH5678901',
      },
    ];

    this.appointments = [
      {
        id: 1051,
        applicationId: 'MC-2024-01051',
        applicant: dummyApplicants[0],
        agendaType: 'Healthcare Infrastructure',
        agendaBrief: 'Request for mobile medical unit for remote villages in East Khasi Hills. Current health facilities are 30km away making emergency care difficult.',
        eventType: 'A4',
        requestedLocation: 'SHILLONG',
        scheduledDateTime: '2024-04-15T10:00:00',
        status: 'CMO_REVIEW',
        mlaMdcApproved: true,
        submittedAt: '2024-03-20T09:15:00',
        updatedAt: '2024-03-21T14:30:00',
      },
      {
        id: 1052,
        applicationId: 'MC-2024-01052',
        applicant: dummyApplicants[1],
        agendaType: 'Agricultural Support',
        agendaBrief: 'Cooperative society seeking ₹25 lakh grant for cold storage facility to reduce post-harvest losses of vegetables and fruits.',
        eventType: 'A4',
        requestedLocation: 'SHILLONG',
        scheduledDateTime: '2024-04-16T11:30:00',
        status: 'DEO_PROCESSED',
        mlaMdcApproved: true,
        submittedAt: '2024-03-22T10:00:00',
        updatedAt: '2024-03-23T16:45:00',
      },
      {
        id: 1053,
        applicationId: 'MC-2024-01053',
        applicant: dummyApplicants[2],
        agendaType: 'Women Empowerment',
        agendaBrief: 'Proposal for skill training center for women in weaving, bamboo crafts, and food processing to boost rural employment.',
        eventType: 'A2',
        requestedLocation: 'SHILLONG',
        scheduledDateTime: '2024-04-17T14:00:00',
        status: 'CMO_REVIEW',
        mlaMdcApproved: true,
        submittedAt: '2024-03-25T11:20:00',
        updatedAt: '2024-03-26T09:10:00',
      },
      {
        id: 1054,
        applicationId: 'MC-2024-01054',
        applicant: dummyApplicants[3],
        agendaType: 'Road Connectivity',
        agendaBrief: 'Urgent repair needed for 12km village road. Current condition makes it impassable during monsoon affecting 800+ families.',
        eventType: 'A4',
        requestedLocation: 'TURA',
        scheduledDateTime: '2024-04-18T10:30:00',
        status: 'SUBMITTED',
        mlaMdcApproved: false,
        submittedAt: '2024-03-28T08:45:00',
      },
      {
        id: 1055,
        applicationId: 'MC-2024-01055',
        applicant: dummyApplicants[4],
        agendaType: 'Education Infrastructure',
        agendaBrief: 'Request for library expansion and computer lab setup for 500+ students. Current infrastructure inadequate for modern curriculum.',
        eventType: 'A4',
        requestedLocation: 'SHILLONG',
        scheduledDateTime: '2024-04-19T15:00:00',
        status: 'DEO_PROCESSED',
        mlaMdcApproved: true,
        submittedAt: '2024-03-29T13:30:00',
        updatedAt: '2024-03-30T10:20:00',
      },
    ];
  }

  getSeverity(status: string): ChipSeverity {
    const map: Record<string, ChipSeverity> = {
      SUBMITTED: 'info',
      DEO_PROCESSED: 'secondary',
      CMO_REVIEW: 'warn',
    };
    return map[status] || 'secondary';
  }

  getStatusLabel(status: string): string {
    return status.replace(/_/g, ' ');
  }

  openModify(appt: Appointment) {
    this.selected = appt;
    this.modifyEventType = appt.eventType;
    this.modifyLocation = appt.requestedLocation;
    this.modifyRemarks = appt.cmoRemarks ?? '';
    this.showModifyDialog = true;
  }

  saveModify() {
    if (!this.selected) return;
    this.appointmentService.updateStatus(this.selected.id, 'APPROVER_REVIEW', this.modifyRemarks).subscribe({
      next: updated => {
        this.snackBar.open(`${updated.applicationId} modified and forwarded to Approver.`, 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
        this.showModifyDialog = false;
        this.selected = null;
        this.ngOnInit();
      },
      error: () => {
        this.snackBar.open('Failed to update appointment.', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
      }
    });
  }

  forwardToApprover(appt: Appointment) {
    this.appointmentService.updateStatus(appt.id, 'APPROVER_REVIEW').subscribe({
      next: updated => {
        this.snackBar.open(`${updated.applicationId} forwarded to Jt. Secretary.`, 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
        this.ngOnInit();
      },
      error: () => {
        this.snackBar.open('Failed to forward appointment.', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
      }
    });
  }
}
