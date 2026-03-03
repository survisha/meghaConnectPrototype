import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppointmentService } from '../services/appointment.service';
import { Appointment } from '../models';

// PrimeNG
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ButtonDirective } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Textarea } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined;

@Component({
  selector: 'app-approver-workflow',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    TableModule, Tag, ButtonDirective, Dialog,
    Textarea, Toast,
  ],
  providers: [MessageService],
  templateUrl: './approver-workflow.component.html',
  styleUrls: ['./approver-workflow.component.scss'],
})
export class ApproverWorkflowComponent implements OnInit {

  appointments: Appointment[] = [];
  selected: Appointment | null = null;
  loading = false;

  showRemarksDialog = false;
  showRescheduleDialog = false;
  remarksText = '';
  rescheduleDate = '';
  pendingAction: 'APPROVE' | 'REJECT' | null = null;

  constructor(private appointmentService: AppointmentService, private msg: MessageService) {}

  ngOnInit() {
    this.loading = true;
    this.appointmentService.getAllAppointments(0, 100).subscribe({
      next: page => {
        this.appointments = page.content.filter(a =>
          ['CMO_REVIEW', 'APPROVER_REVIEW', 'HCM_PENDING'].includes(a.status)
        );
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getSeverity(status: string): TagSeverity {
    const map: Record<string, TagSeverity> = {
      APPROVER_REVIEW: 'warn',
      CMO_REVIEW: 'info',
      HCM_PENDING: 'secondary',
      HCM_ACCEPTED: 'success',
      HCM_REJECTED: 'danger',
      SCHEDULED: 'success',
    };
    return map[status] ?? undefined;
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

  confirmAction() {
    if (!this.selected || !this.pendingAction) return;
    const newStatus = this.pendingAction === 'APPROVE' ? 'HCM_PENDING' : 'HCM_REJECTED';
    this.appointmentService.updateStatus(this.selected.id, newStatus, this.remarksText).subscribe({
      next: updated => {
        if (this.pendingAction === 'APPROVE') {
          this.msg.add({ severity: 'success', summary: 'Forwarded to HCM', detail: `${updated.applicationId} approved and pushed to HCM queue.` });
        } else {
          this.msg.add({ severity: 'error', summary: 'Appointment Rejected', detail: `${updated.applicationId} has been rejected.` });
        }
        this.showRemarksDialog = false;
        this.selected = null;
        this.pendingAction = null;
        this.ngOnInit();
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Error', detail: 'Failed to update appointment.' })
    });
  }

  confirmReschedule() {
    if (!this.selected || !this.rescheduleDate) return;
    this.appointmentService.rescheduleAppointment(this.selected.id, {
      scheduledDateTime: new Date(this.rescheduleDate).toISOString().slice(0, 19),
      durationMinutes: 30
    }).subscribe({
      next: updated => {
        this.msg.add({ severity: 'info', summary: 'Rescheduled', detail: `${updated.applicationId} rescheduled to ${this.rescheduleDate}.` });
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Error', detail: 'Failed to reschedule.' })
    });
    this.showRescheduleDialog = false;
    this.selected = null;
  }
}
