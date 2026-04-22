import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { Appointment } from '../../models';
import { AppointmentService } from '../../services/appointment.service';

@Component({
  selector: 'app-cmo-review-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatTabsModule,
    MatDividerModule
  ],
  templateUrl: './cmo-review-modal.component.html',
  styleUrls: ['./cmo-review-modal.component.scss']
})
export class CmoReviewModalComponent implements OnInit {
  appointment: Appointment;
  cmoRemarks: string = '';
  pendingInformation: string = '';
  isSubmitting = false;

  constructor(
    public dialogRef: MatDialogRef<CmoReviewModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { appointment: Appointment },
    private appointmentService: AppointmentService
  ) {
    this.appointment = data.appointment;
  }

  ngOnInit() {
    // Load existing CMO remarks
    if (this.appointment.cmoRemarks) {
      this.cmoRemarks = this.appointment.cmoRemarks;
    }
  }

  /**
   * Submit CMO review with remarks about pending information
   */
  submitCmoReview() {
    if (!this.pendingInformation.trim()) {
      alert('Please add remarks about pending information or mark as complete');
      return;
    }

    this.isSubmitting = true;

    const payload = {
      appointmentId: this.appointment.id,
      cmoRemarks: this.cmoRemarks,
      pendingInformation: this.pendingInformation,
      status: this.pendingInformation.toLowerCase().includes('complete') ? 'APPROVER_REVIEW' : 'CMO_REVIEW',
      notifyApplicant: true,
      notifyDeo: true
    };

    this.appointmentService.submitCmoReview(payload).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        alert('CMO review submitted successfully. Applicant and DEO have been notified.');
        this.dialogRef.close({ submitted: true, data: response });
      },
      error: (err) => {
        this.isSubmitting = false;
        alert('Error submitting CMO review: ' + (err.error?.message || err.message));
        console.error('Error:', err);
      }
    });
  }

  /**
   * Close modal without submitting
   */
  closeModal() {
    this.dialogRef.close();
  }

  /**
   * Add missing information template
   */
  addMissingInfoTemplate(info: string) {
    this.pendingInformation += (this.pendingInformation ? '\n' : '') + `• ${info}`;
  }
}
