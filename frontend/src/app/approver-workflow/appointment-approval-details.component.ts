import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentApprovalService, AppointmentApproval } from '../services/appointment-approval.service';
import { DocumentService } from '../services/document.service';
import { MatDialog } from '@angular/material/dialog';
import { apiErrorMessage } from '../shared/api-error.util';

@Component({
  selector: 'app-appointment-approval-details',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDividerModule,
    MatChipsModule,
    MatListModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './appointment-approval-details.component.html',
  styleUrls: ['./appointment-approval-details.component.scss']
})
export class AppointmentApprovalDetailsComponent implements OnInit {
  appointment: AppointmentApproval | null = null;
  loading = false;
  submitting = false;
  appointmentId: number = 0;
  downloadingDocumentId: number | null = null;
  
  approvalForm: FormGroup;
  rejectForm: FormGroup;

  activeTab = 0;

  constructor(
    private appointmentService: AppointmentApprovalService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private documentService: DocumentService
  ) {
    this.approvalForm = this.fb.group({
      remarks: ['', Validators.required]
    });

    this.rejectForm = this.fb.group({
      rejectReason: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.appointmentId = params['id'];
      this.loadAppointmentDetails();
    });
  }

  loadAppointmentDetails(): void {
    this.loading = true;
    this.appointmentService.getAppointmentDetails(this.appointmentId).subscribe({
      next: (data) => {
        this.appointment = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading appointment details:', err);
        alert(apiErrorMessage(err, 'Error loading appointment details. Please try again.'));
        this.loading = false;
      }
    });
  }

  approveAndForward(): void {
    if (this.approvalForm.invalid) {
      alert('Please enter remarks before approving');
      return;
    }

    this.submitting = true;
    const remarks = this.approvalForm.get('remarks')?.value;

    this.appointmentService.approveAndForward(this.appointmentId, remarks).subscribe({
      next: () => {
        alert('Appointment approved and forwarded to next approver');
        this.router.navigate(['/appointments/pending-approvals']);
        this.submitting = false;
      },
      error: (err) => {
        console.error('Error approving appointment:', err);
        alert(apiErrorMessage(err, 'Error approving appointment. Please try again.'));
        this.submitting = false;
      }
    });
  }

  rejectAppointment(): void {
    if (this.rejectForm.invalid) {
      alert('Please enter rejection reason');
      return;
    }

    const confirmed = confirm('Are you sure you want to reject this appointment?');
    if (!confirmed) return;

    this.submitting = true;
    const rejectReason = this.rejectForm.get('rejectReason')?.value;

    this.appointmentService.rejectAppointment(this.appointmentId, rejectReason).subscribe({
      next: () => {
        alert('Appointment rejected');
        this.router.navigate(['/appointments/pending-approvals']);
        this.submitting = false;
      },
      error: (err) => {
        console.error('Error rejecting appointment:', err);
        alert(apiErrorMessage(err, 'Error rejecting appointment. Please try again.'));
        this.submitting = false;
      }
    });
  }

  scheduleAppointment(): void {
    this.router.navigate(['/scheduling'], { queryParams: { appointmentId: this.appointmentId } });
  }

  editAppointmentDetails(): void {
    // Open details edit dialog (to be implemented)
    alert('Edit details feature coming soon');
  }

  goBack(): void {
    this.router.navigate(['/appointments/pending-approvals']);
  }

  downloadDocument(docId: string | number, fileName = 'document'): void {
    const documentId = Number(docId);
    if (!documentId || this.downloadingDocumentId) {
      return;
    }

    this.downloadingDocumentId = documentId;
    this.documentService.downloadDocument(documentId).subscribe({
      next: blob => {
        this.triggerBlobDownload(blob, fileName);
        this.downloadingDocumentId = null;
      },
      error: error => {
        this.downloadingDocumentId = null;
        alert(apiErrorMessage(error, 'Unable to download document. Please try again.'));
      },
    });
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'SUBMITTED': '#1e40af',
      'CMO_REVIEW': '#ea580c',
      'APPROVER_REVIEW': '#7c3aed',
      'HCM_PENDING': '#0891b2',
      'SCHEDULED': '#059669'
    };
    return colors[status] || '#6b7280';
  }

  private triggerBlobDownload(blob: Blob, fileName: string): void {
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
}
