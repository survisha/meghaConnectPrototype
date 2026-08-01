import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Appointment, AppointmentStatus } from '../../models';
import { AppointmentRemark, AppointmentService } from '../../services/appointment.service';
import { ReferenceDataDto, ReferenceDataService } from '../../services/reference-data.service';
import {
  CitizenAppointmentHistory,
  PublicIdentificationHistory,
  VisitorSearchService
} from '../../services/visitor-search.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { apiErrorMessage } from '../../shared/api-error.util';
import { ToastService } from '../../shared/toast/toast.service';

interface HcmAppointmentCard {
  id: number;
  subject: string;
  applicantName: string;
  dateTime?: string;
  location: string;
  type: string;
  category: string;
  description?: string;
  appointment: Appointment;
}

/**
 * HCM Dashboard Component
 * Displays appointments/meetings with gesture-based actions:
 * Right Swipe: Accept/Modify
 * Left Swipe: Reject/Delay (Snooze)
 */
@Component({
  selector: 'app-hcm-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatTabsModule
  ],
  templateUrl: './hcm-dashboard.component.html',
  styleUrls: ['./hcm-dashboard.component.scss']
})
export class HcmDashboardComponent implements OnInit {
  
  appointments: HcmAppointmentCard[] = [];
  selectedDate: Date = new Date();
  departments: ReferenceDataDto[] = [];
  remarksHistory: AppointmentRemark[] = [];
  citizenHistory: PublicIdentificationHistory | null = null;
  citizenAppointmentHistory: CitizenAppointmentHistory[] = [];
  loadingRemarks = false;
  loadingCitizenHistory = false;
  citizenHistoryError = '';
  loadingAppointments = false;
  submittingAction = false;
  errorMsg = '';
  private apiErrors = new Map<string, string>();
  
  // Swipe tracking
  touchStartX = 0;
  touchStartY = 0;
  touchEndX = 0;
  touchEndY = 0;
  swipedAppointmentId: number | null = null;
  lastSwipeDirection: 'LEFT' | 'RIGHT' | null = null;
  
  // Action modals
  showActionMenu = false;
  selectedAppointment: any = null;
  selectedDetailAppointment: Appointment | null = null;
  showDetailsDialog = false;
  selectedActionType: string = '';
  
  // Form data for actions
  actionFormData = {
    remarks: '',
    decision: '',
    departmentCode: '',
    modifiedDateTime: null as Date | null,
    snoozeType: 'DAYS_7',
    clarification: '',
    markImportant: false
  };
  
  constructor(
    private http: HttpClient,
    private appointmentService: AppointmentService,
    private referenceDataService: ReferenceDataService,
    private visitorSearchService: VisitorSearchService,
    private toast: ToastService,
  ) {}
  
  ngOnInit() {
    this.loadDepartments();
    this.loadAppointments();
  }
  
  /**
   * Load appointments for HCM
   */
  loadAppointments() {
    this.loadingAppointments = true;
    this.appointmentService.getHcmActionAppointments(this.toDateParam(this.selectedDate))
      .subscribe({
        next: page => {
          this.appointments = (page ?? [])
            .filter(appointment => this.isPendingHcmActionAppointment(appointment))
            .map(appointment => this.mapAppointmentCard(appointment));
          this.clearApiError('appointments');
          this.loadingAppointments = false;
        },
        error: err => {
          this.appointments = [];
          this.setApiError('appointments', err, 'Unable to load HCM appointments.');
          this.loadingAppointments = false;
        },
      });
  }

  onDateSelected(date: Date | null) {
    if (!date) return;
    this.selectedDate = date;
    this.loadAppointments();
  }

  private loadDepartments() {
    this.referenceDataService.getByType('DEPARTMENT').subscribe({
      next: departments => this.departments = departments ?? [],
      error: err => this.setApiError('departments', err, 'Unable to load departments.'),
    });
  }
  
  /**
   * Handle card swipe start
   */
  onTouchStart(event: TouchEvent, appointmentId: number) {
    const touch = event.touches[0];
    this.touchStartX = touch.clientX;
    this.touchStartY = touch.clientY;
    this.swipedAppointmentId = appointmentId;
  }

  /**
   * Handle mouse down (for desktop testing)
   */
  onMouseDown(event: MouseEvent, appointmentId: number) {
    this.touchStartX = event.clientX;
    this.touchStartY = event.clientY;
    this.swipedAppointmentId = appointmentId;
  }

  /**
   * Handle card swipe end
   */
  onTouchEnd(event: TouchEvent, appointmentId: number) {
    const touch = event.changedTouches[0];
    this.touchEndX = touch.clientX;
    this.touchEndY = touch.clientY;
    
    this.handleSwipe(appointmentId);
  }

  /**
   * Handle mouse up (for desktop testing)
   */
  onMouseUp(event: MouseEvent, appointmentId: number) {
    this.touchEndX = event.clientX;
    this.touchEndY = event.clientY;
    
    this.handleSwipe(appointmentId);
  }
  
  /**
   * Detect swipe direction
   */
  handleSwipe(appointmentId: number) {
    const diff = this.touchEndX - this.touchStartX;
    const diffY = Math.abs(this.touchEndY - this.touchStartY);
    
    // Only process horizontal swipes (vertical movement < 50px)
    if (diffY > 50) return;
    
    // Swipe threshold: 50px
    const threshold = 50;
    
    if (Math.abs(diff) > threshold) {
      if (diff > 0) {
        // Right Swipe: Accept/Modify
        this.lastSwipeDirection = 'RIGHT';
        this.showRightSwipeOptions(appointmentId);
      } else {
        // Left Swipe: Reject/Delay
        this.lastSwipeDirection = 'LEFT';
        this.showLeftSwipeOptions(appointmentId);
      }
    }
  }
  
  /**
   * Show right swipe options (Accept/Modify)
   */
  showRightSwipeOptions(appointmentId: number) {
    const appointment = this.appointments.find(a => a.id === appointmentId);
    if (!appointment) return;
    
    this.selectedAppointment = appointment;
    this.selectedActionType = 'RIGHT_SWIPE';
    this.showActionMenu = true;
    this.loadRemarks(appointmentId);
  }
  
  /**
   * Show left swipe options (Reject/Delay)
   */
  showLeftSwipeOptions(appointmentId: number) {
    const appointment = this.appointments.find(a => a.id === appointmentId);
    if (!appointment) return;
    
    this.selectedAppointment = appointment;
    this.selectedActionType = 'LEFT_SWIPE';
    this.showActionMenu = true;
    this.loadRemarks(appointmentId);
  }

  loadRemarks(appointmentId: number) {
    this.loadingRemarks = true;
    this.appointmentService.getRemarks(appointmentId)
      .subscribe({
        next: remarks => {
          this.remarksHistory = remarks;
          this.loadingRemarks = false;
        },
        error: err => {
          this.remarksHistory = [];
          this.setApiError('remarks', err, 'Unable to load notes history.');
          this.loadingRemarks = false;
        }
      });
  }

  saveMeetingRemark() {
    if (!this.selectedAppointment || !this.actionFormData.remarks.trim()) {
      this.toast.warning('Please enter remarks before saving.');
      return;
    }
    this.submittingAction = true;
    this.appointmentService.addRemark(this.selectedAppointment.id, {
      hcmRemarks: this.actionFormData.remarks,
      decision: this.actionFormData.decision,
      departmentCode: this.actionFormData.departmentCode,
    }).subscribe({
      next: () => {
        this.toast.success('Remarks saved successfully.');
        this.loadRemarks(this.selectedAppointment.id);
        this.loadAppointments();
        this.resetActionForm();
        this.submittingAction = false;
      },
      error: err => {
        this.setApiError('saveRemark', err, 'Unable to save remarks.');
        this.submittingAction = false;
      }
    });
  }

  openAppointmentDetails(card: HcmAppointmentCard, event?: Event) {
    event?.stopPropagation();
    this.selectedDetailAppointment = card.appointment;
    this.showDetailsDialog = true;
    this.loadRemarks(card.id);
    this.loadCitizenHistory(card.appointment);
  }

  closeAppointmentDetails() {
    this.showDetailsDialog = false;
    this.selectedDetailAppointment = null;
    this.citizenHistory = null;
    this.citizenAppointmentHistory = [];
    this.citizenHistoryError = '';
    this.loadingCitizenHistory = false;
  }
  
  /**
   * Accept appointment with suggested date/time (Right Swipe - Option 1)
   */
  acceptAppointment() {
    if (!this.selectedAppointment) return;
    
    const payload = {
      acceptedDateTime: this.toApiDateTime(this.selectedAppointment.dateTime),
      originalDateTime: this.toApiDateTime(this.selectedAppointment.dateTime),
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks,
      decision: this.actionFormData.decision,
      departmentCode: this.actionFormData.departmentCode
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/accept`, payload);
  }
  
  /**
   * Mark as important and reschedule earlier (Right Swipe - Option 2)
   */
  markImportantAndReschedule() {
    if (!this.selectedAppointment) return;
    
    const payload = {
      requestedEarlierDateTime: this.toApiDateTime(this.actionFormData.modifiedDateTime),
      originalDateTime: this.toApiDateTime(this.selectedAppointment.dateTime),
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks,
      decision: this.actionFormData.decision,
      departmentCode: this.actionFormData.departmentCode
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/mark-important`, payload);
  }
  
  /**
   * Modify date/time with comments (Right Swipe - Option 3)
   */
  modifyAppointmentDateTime() {
    if (!this.selectedAppointment || !this.actionFormData.modifiedDateTime) {
      this.toast.warning('Please select a new date and time');
      return;
    }
    
    const payload = {
      acceptedDateTime: this.toApiDateTime(this.actionFormData.modifiedDateTime),
      originalDateTime: this.toApiDateTime(this.selectedAppointment.dateTime),
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks,
      decision: this.actionFormData.decision,
      departmentCode: this.actionFormData.departmentCode
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/modify`, payload);
  }
  
  /**
   * Snooze appointment (Left Swipe - Option 1)
   */
  snoozeAppointment() {
    if (!this.selectedAppointment) return;
    
    const payload = {
      snoozeType: this.actionFormData.snoozeType,
      originalDateTime: this.toApiDateTime(this.selectedAppointment.dateTime),
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks,
      decision: this.actionFormData.decision,
      departmentCode: this.actionFormData.departmentCode
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/snooze`, payload);
  }
  
  /**
   * Reject and request clarification (Left Swipe - Option 2)
   */
  rejectAndRequestClarification() {
    if (!this.selectedAppointment) return;
    
    const payload = {
      clarificationRequested: this.actionFormData.clarification,
      originalDateTime: this.toApiDateTime(this.selectedAppointment.dateTime),
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks,
      decision: this.actionFormData.decision,
      departmentCode: this.actionFormData.departmentCode
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/reject`, payload);
  }
  
  /**
   * Submit action to backend
   */
  submitAction(endpoint: string, payload: any) {
    this.submittingAction = true;
    const handledAppointmentId = endpoint.includes('/accept') ? this.selectedAppointment?.id : null;
    this.http.post(`${environment.apiUrl}/hcm/actions${endpoint}`, payload)
      .subscribe({
        next: () => {
          this.clearApiError('submitAction');
          this.toast.success('Action submitted successfully');
          if (handledAppointmentId) {
            this.appointments = this.appointments.filter(item => item.id !== handledAppointmentId);
          }
          this.resetActionForm();
          this.showActionMenu = false;
          this.selectedAppointment = null;
          this.loadAppointments();
          this.submittingAction = false;
        },
        error: err => {
          this.setApiError('submitAction', err, 'Error submitting action.');
          this.submittingAction = false;
        },
      });
  }
  
  /**
   * Reset action form
   */
  resetActionForm() {
    this.actionFormData = {
      remarks: '',
      decision: '',
      departmentCode: '',
      modifiedDateTime: null,
      snoozeType: 'DAYS_7',
      clarification: '',
      markImportant: false
    };
  }
  
  /**
   * Close action menu
   */
  closeActionMenu() {
    this.showActionMenu = false;
    this.selectedAppointment = null;
    this.remarksHistory = [];
    this.resetActionForm();
  }
  
  /**
   * Format date/time for display
   */
  formatDateTime(date: any): string {
    if (!date) return '-';
    try {
      return new Date(date).toLocaleString();
    } catch {
      return date;
    }
  }

  private mapAppointmentCard(appointment: Appointment): HcmAppointmentCard {
    return {
      id: appointment.id,
      subject: appointment.subject || appointment.agendaType || appointment.applicationId || `Appointment #${appointment.id}`,
      applicantName: appointment.applicant?.fullName || appointment.applicantName || 'Not specified',
      dateTime: appointment.scheduledDateTime || appointment.createdAt || appointment.submittedAt,
      location: appointment.requestedLocation || 'Not specified',
      type: appointment.appointmentType || appointment.eventType || 'Appointment',
      category: appointment.department || appointment.agendaType || 'General',
      description: appointment.agendaBrief || appointment.shortNotes || appointment.cmoRemarks || appointment.approverRemarks,
      appointment,
    };
  }

  private setApiError(key: string, error: unknown, fallbackMessage: string): void {
    this.apiErrors.set(key, apiErrorMessage(error, fallbackMessage));
    this.syncApiErrors();
  }

  private clearApiError(key: string): void {
    if (this.apiErrors.delete(key)) {
      this.syncApiErrors();
    }
  }

  private syncApiErrors(): void {
    this.errorMsg = Array.from(this.apiErrors.values()).join(' ');
  }

  private loadCitizenHistory(appointment: Appointment): void {
    const citizenId = appointment.applicantId || appointment.applicant?.id;
    this.citizenHistory = null;
    this.citizenAppointmentHistory = [];
    this.citizenHistoryError = '';

    if (!citizenId) {
      this.citizenHistoryError = 'Visitor history is not available for this appointment.';
      return;
    }

    this.loadingCitizenHistory = true;
    this.visitorSearchService.getPublicIdentificationHistory(citizenId).subscribe({
      next: history => {
        if (this.selectedDetailAppointment?.id !== appointment.id) {
          return;
        }
        this.citizenHistory = history;
        this.citizenAppointmentHistory = history.appointments || [];
        this.loadingCitizenHistory = false;
      },
      error: error => {
        if (this.selectedDetailAppointment?.id !== appointment.id) {
          return;
        }
        this.citizenHistoryError = apiErrorMessage(error, 'Unable to load visitor history.');
        this.loadingCitizenHistory = false;
      },
    });
  }

  private toApiDateTime(value: string | Date | null | undefined): string | null {
    if (!value) {
      return null;
    }

    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }

    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  private toDateParam(date: Date): string {
    const pad = (part: number) => part.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  private isPendingHcmActionAppointment(appointment: Appointment): boolean {
    const handledStatuses: AppointmentStatus[] = [
      'HCM_ACCEPTED',
      'HCM_REJECTED',
      'FORWARDED_TO_DEPARTMENT',
      'COMPLETED',
      'CANCELLED',
      'REJECTED',
    ];
    return !handledStatuses.includes(appointment.status);
  }
}
