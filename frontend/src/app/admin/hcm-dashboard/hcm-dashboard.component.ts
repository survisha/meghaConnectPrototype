import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';

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
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatBadgeModule
  ],
  templateUrl: './hcm-dashboard.component.html',
  styleUrls: ['./hcm-dashboard.component.scss']
})
export class HcmDashboardComponent implements OnInit {
  
  appointments: any[] = [];
  pendingWorkItems: any[] = [];
  loading = false;
  pendingWorkCount = 0;
  
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
  selectedActionType: string = '';
  
  // Form data for actions
  actionFormData = {
    remarks: '',
    modifiedDateTime: null as Date | null,
    snoozeType: 'DAYS_7',
    clarification: '',
    markImportant: false
  };
  
  constructor(
    private http: HttpClient,
    private dialog: MatDialog
  ) {}
  
  ngOnInit() {
    // Initialize with dummy data first
    this.appointments = this.getDummyAppointments();
    this.pendingWorkItems = this.getDummyPendingWorkItems();
    this.pendingWorkCount = this.getDummyPendingWorkItems().length;
    
    // Try to fetch from API (optional)
    this.loadAppointments();
    this.loadPendingWorkItems();
    this.getPendingWorkCount();
  }
  
  /**
   * Load appointments for HCM
   */
  loadAppointments() {
    this.loading = true;
    this.http.get<any[]>(`${environment.apiUrl}/hcm/actions/pending-work`)
      .subscribe({
        next: (data) => {
          // Only use API data if it has items, otherwise keep dummy data
          if (data && data.length > 0) {
            this.appointments = data;
          }
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading appointments', err);
          // Keep the dummy data already loaded
          this.loading = false;
        }
      });
  }
  
  /**
   * Load pending work items
   */
  loadPendingWorkItems() {
    this.http.get<any[]>(`${environment.apiUrl}/hcm/actions/pending-work`)
      .subscribe({
        next: (data) => {
          // Only use API data if it has items, otherwise keep dummy data
          if (data && data.length > 0) {
            this.pendingWorkItems = data;
            this.pendingWorkCount = data.length;
          }
        },
        error: (err) => {
          console.error('Error loading pending work items', err);
          // Keep the dummy data already loaded
        }
      });
  }
  
  /**
   * Get pending work count for badge
   */
  getPendingWorkCount() {
    this.http.get<number>(`${environment.apiUrl}/hcm/actions/pending-work/count`)
      .subscribe({
        next: (count) => {
          this.pendingWorkCount = count || this.getDummyPendingWorkItems().length;
        },
        error: (err) => {
          console.error('Error getting pending work count', err);
          this.pendingWorkCount = this.getDummyPendingWorkItems().length;
        }
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
  }
  
  /**
   * Accept appointment with suggested date/time (Right Swipe - Option 1)
   */
  acceptAppointment() {
    if (!this.selectedAppointment) return;
    
    const payload = {
      originalDateTime: this.selectedAppointment.dateTime,
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/accept`, payload);
  }
  
  /**
   * Mark as important and reschedule earlier (Right Swipe - Option 2)
   */
  markImportantAndReschedule() {
    if (!this.selectedAppointment) return;
    
    const payload = {
      requestedEarlierDateTime: this.actionFormData.modifiedDateTime,
      originalDateTime: this.selectedAppointment.dateTime,
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/mark-important`, payload);
  }
  
  /**
   * Modify date/time with comments (Right Swipe - Option 3)
   */
  modifyAppointmentDateTime() {
    if (!this.selectedAppointment || !this.actionFormData.modifiedDateTime) {
      alert('Please select a new date and time');
      return;
    }
    
    const payload = {
      acceptedDateTime: this.actionFormData.modifiedDateTime,
      originalDateTime: this.selectedAppointment.dateTime,
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks
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
      originalDateTime: this.selectedAppointment.dateTime,
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks
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
      originalDateTime: this.selectedAppointment.dateTime,
      originalLocation: this.selectedAppointment.location,
      appointmentSubject: this.selectedAppointment.subject,
      hcmRemarks: this.actionFormData.remarks
    };
    
    this.submitAction(`/appointment/${this.selectedAppointment.id}/reject`, payload);
  }
  
  /**
   * Submit action to backend
   */
  submitAction(endpoint: string, payload: any) {
    this.loading = true;
    this.http.post(`${environment.apiUrl}/hcm/actions${endpoint}`, payload)
      .subscribe({
        next: (response) => {
          alert('Action submitted successfully');
          this.resetActionForm();
          this.showActionMenu = false;
          this.selectedAppointment = null;
          this.loadAppointments();
          this.loadPendingWorkItems();
          this.getPendingWorkCount();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error submitting action', err);
          alert('Error submitting action: ' + (err.error?.message || err.message));
          this.loading = false;
        }
      });
  }
  
  /**
   * Reset action form
   */
  resetActionForm() {
    this.actionFormData = {
      remarks: '',
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
    this.resetActionForm();
  }
  
  /**
   * Get status badge color
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING': return '#ff9800';
      case 'CONFIRMED': return '#4caf50';
      case 'COMPLETED': return '#2196f3';
      case 'REJECTED': return '#f44336';
      default: return '#9e9e9e';
    }
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

  /**
   * Get dummy appointments for demo
   */
  getDummyAppointments(): any[] {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const nextWeek = new Date(today);
    nextWeek.setDate(nextWeek.getDate() + 7);

    return [
      {
        id: 1,
        subject: 'Budget Review Meeting',
        dateTime: tomorrow.toISOString(),
        location: 'Conference Room A, State Secretariat',
        type: 'Meeting',
        category: 'Finance',
        description: 'Quarterly budget review and allocation discussion'
      },
      {
        id: 2,
        subject: 'Cabinet Meeting',
        dateTime: new Date(tomorrow.getTime() + 2 * 24 * 60 * 60 * 1000).toISOString(),
        location: 'Main Assembly Hall',
        type: 'Meeting',
        category: 'Administrative',
        description: 'Weekly cabinet meeting for policy decisions'
      },
      {
        id: 3,
        subject: 'Public Health Initiative Discussion',
        dateTime: nextWeek.toISOString(),
        location: 'Health Ministry Office',
        type: 'Discussion',
        category: 'Health',
        description: 'Discussion on new public health initiatives for the state'
      },
      {
        id: 4,
        subject: 'Education Sector Review',
        dateTime: new Date(nextWeek.getTime() + 2 * 24 * 60 * 60 * 1000).toISOString(),
        location: 'Education Ministry',
        type: 'Meeting',
        category: 'Education',
        description: 'Review of education sector performance and improvements'
      },
      {
        id: 5,
        subject: 'Tourism Development Committee',
        dateTime: new Date(nextWeek.getTime() + 4 * 24 * 60 * 60 * 1000).toISOString(),
        location: 'Tourism Board Office',
        type: 'Committee',
        category: 'Development',
        description: 'Discussion on tourism development projects in Meghalaya'
      }
    ];
  }

  /**
   * Get dummy pending work items for demo
   */
  getDummyPendingWorkItems(): any[] {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const nextWeek = new Date(today);
    nextWeek.setDate(nextWeek.getDate() + 7);
    const twoWeeks = new Date(today);
    twoWeeks.setDate(twoWeeks.getDate() + 14);

    return [
      {
        appointmentSubject: 'Budget Review Meeting',
        actionType: 'MARK_IMPORTANT',
        actionStatus: 'PENDING',
        originalDateTime: tomorrow.toISOString(),
        originalLocation: 'Conference Room A, State Secretariat',
        requestedEarlierDateTime: new Date(today.getTime() + 6 * 60 * 60 * 1000).toISOString(),
        hcmRemarks: 'Marked as important - requires earlier scheduling'
      },
      {
        appointmentSubject: 'Cabinet Meeting',
        actionType: 'ACCEPT_WITH_CHANGES',
        actionStatus: 'CONFIRMED',
        originalDateTime: new Date(tomorrow.getTime() + 2 * 24 * 60 * 60 * 1000).toISOString(),
        originalLocation: 'Main Assembly Hall',
        acceptedDateTime: new Date(tomorrow.getTime() + 3 * 24 * 60 * 60 * 1000).toISOString(),
        hcmRemarks: 'Accepted with date modification as per convenience'
      },
      {
        appointmentSubject: 'Public Health Initiative Discussion',
        actionType: 'SNOOZE',
        actionStatus: 'PENDING',
        originalDateTime: nextWeek.toISOString(),
        originalLocation: 'Health Ministry Office',
        snoozedUntil: new Date(nextWeek.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        hcmRemarks: 'Snoozed for 7 days - to be revisited later'
      },
      {
        appointmentSubject: 'Education Sector Review',
        actionType: 'REJECT',
        actionStatus: 'PENDING',
        originalDateTime: new Date(nextWeek.getTime() + 2 * 24 * 60 * 60 * 1000).toISOString(),
        originalLocation: 'Education Ministry',
        clarificationRequested: 'Please provide more details on the expected outcomes and participants. Also, need confirmation on time slot.',
        hcmRemarks: 'Clarification requested - awaiting response from CMO'
      }
    ];
  }
}
