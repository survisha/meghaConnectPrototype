import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from '../../services/appointment.service';
import { Appointment, AppointmentStatus } from '../../models';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { Tooltip } from 'primeng/tooltip';

@Component({
  selector: 'app-appointment-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, TableModule, Tag, Select, InputText, Tooltip],
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss'],
})
export class AppointmentListComponent implements OnInit {
  appointments: Appointment[] = [];
  filtered: Appointment[] = [];
  search = '';
  filterStatus = '';
  loading = false;

  statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'CMO Review', value: 'CMO_REVIEW' },
    { label: 'Approver Review', value: 'APPROVER_REVIEW' },
    { label: 'HCM Pending', value: 'HCM_PENDING' },
    { label: 'Scheduled', value: 'SCHEDULED' },
    { label: 'Completed', value: 'COMPLETED' },
  ];

  constructor(private appointmentService: AppointmentService) {}

  ngOnInit() {
    this.loading = true;
    this.appointmentService.getAllAppointments(0, 100).subscribe({
      next: page => { this.appointments = page.content; this.applyFilter(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  applyFilter() {
    this.filtered = this.appointments.filter(a =>
      (!this.search || a.applicant?.fullName?.toLowerCase().includes(this.search.toLowerCase()) || a.applicationId?.includes(this.search)) &&
      (!this.filterStatus || a.status === this.filterStatus)
    );
  }

  getStatusSeverity(s: AppointmentStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | null | undefined {
    const m: Record<string,'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | null | undefined> = {
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
}
