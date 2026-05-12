import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { MatBadgeModule } from '@angular/material/badge';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { AppointmentApprovalService, AppointmentApproval } from '../services/appointment-approval.service';
import { apiErrorMessage } from '../shared/api-error.util';

@Component({
  selector: 'app-approver-inbox',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCardModule,
    MatBadgeModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './approver-inbox.component.html',
  styleUrls: ['./approver-inbox.component.scss']
})
export class ApproverInboxComponent implements OnInit {
  displayedColumns: string[] = ['applicantName', 'agendaType', 'location', 'submittedDate', 'status', 'actions'];
  appointments: AppointmentApproval[] = [];
  filteredAppointments: AppointmentApproval[] = [];
  
  filterForm: FormGroup;
  pageSize = 10;
  pageIndex = 0;
  totalCount = 0;
  
  loading = false;
  errorMsg = '';
  userRole = 'CMO_OFFICER'; // Default role, can be overridden from auth service

  statusBadgeColor: { [key: string]: string } = {
    'SUBMITTED': '#1e40af',
    'CMO_REVIEW': '#ea580c',
    'APPROVER_REVIEW': '#7c3aed',
    'HCM_PENDING': '#0891b2',
    'SCHEDULED': '#059669'
  };

  constructor(
    private appointmentService: AppointmentApprovalService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.filterForm = this.fb.group({
      searchText: [''],
      statusFilter: [''],
      agendaFilter: [''],
      dateFromFilter: [''],
      dateToFilter: ['']
    });
  }

  ngOnInit(): void {
    this.loadPendingAppointments();
    this.setupFilterListener();
  }

  loadPendingAppointments(): void {
    this.loading = true;
    this.appointmentService.getPendingAppointments(this.userRole).subscribe({
      next: (data) => {
        this.errorMsg = '';
        this.appointments = data;
        this.totalCount = data.length;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading pending appointments:', err);
        this.errorMsg = apiErrorMessage(err, 'Unable to load appointment inbox from API. Please try again.');
        this.appointments = [];
        this.filteredAppointments = [];
        this.totalCount = 0;
        this.loading = false;
      }
    });
  }

  setupFilterListener(): void {
    this.filterForm.valueChanges.subscribe(() => {
      this.pageIndex = 0; // Reset to first page when filtering
      this.applyFilters();
    });
  }

  applyFilters(): void {
    const { searchText, statusFilter, agendaFilter, dateFromFilter, dateToFilter } = this.filterForm.value;

    this.filteredAppointments = this.appointments.filter(appointment => {
      // Search by applicant name or phone
      const searchMatch = !searchText || 
        (appointment.applicantName || '').toLowerCase().includes(searchText.toLowerCase()) ||
        (appointment.applicantPhone || '').includes(searchText);

      // Filter by status
      const statusMatch = !statusFilter || appointment.status === statusFilter;

      // Filter by agenda type
      const agendaMatch = !agendaFilter || appointment.agendaType === agendaFilter;

      // Filter by date range
      const appointmentDate = new Date(appointment.submittedDate);
      const fromMatch = !dateFromFilter || appointmentDate >= new Date(dateFromFilter);
      const toMatch = !dateToFilter || appointmentDate <= new Date(dateToFilter);

      return searchMatch && statusMatch && agendaMatch && fromMatch && toMatch;
    });

    this.totalCount = this.filteredAppointments.length;
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  getPaginatedData(): AppointmentApproval[] {
    const start = this.pageIndex * this.pageSize;
    return this.filteredAppointments.slice(start, start + this.pageSize);
  }

  reviewAppointment(appointmentId: number): void {
    this.router.navigate(['/appointments/approval-details', appointmentId]);
  }

  scheduleAppointment(appointmentId: number): void {
    this.router.navigate(['/scheduling'], { queryParams: { appointmentId: appointmentId } });
  }

  getStatusBadgeColor(status: string): string {
    return this.statusBadgeColor[status] || '#6b7280';
  }

  getStatusDisplayText(status: string): string {
    const statusText: { [key: string]: string } = {
      'SUBMITTED': 'Awaiting Review',
      'CMO_REVIEW': 'CMO Review',
      'APPROVER_REVIEW': 'Approver Review',
      'HCM_PENDING': 'Pending HCM Action',
      'SCHEDULED': 'Scheduled'
    };
    return statusText[status] || status;
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.pageIndex = 0;
  }

  getAgendaTypeOptions(): string[] {
    const agendas = new Set(this.appointments.map(a => a.agendaType));
    return Array.from(agendas).sort();
  }

  getUniqueStatuses(): string[] {
    const statuses = new Set(this.appointments.map(a => a.status));
    return Array.from(statuses).sort();
  }

  getPendingCount(): number {
    return this.appointments.filter(a => 
      a.status === 'SUBMITTED' || a.status === 'CMO_REVIEW'
    ).length;
  }
}
