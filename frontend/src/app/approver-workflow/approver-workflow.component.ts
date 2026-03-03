import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../services/mock-data.service';
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

  showRemarksDialog = false;
  showRescheduleDialog = false;
  remarksText = '';
  rescheduleDate = '';
  pendingAction: 'APPROVE' | 'REJECT' | null = null;

  constructor(private mock: MockDataService, private msg: MessageService) {}

  ngOnInit() {
    // Show all appointments eligible for approver review (APPROVER_REVIEW) plus a few others for demo
    this.appointments = this.mock.appointments.filter(a =>
      ['CMO_REVIEW', 'APPROVER_REVIEW', 'HCM_PENDING'].includes(a.status)
    );
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
    const appt = this.mock.appointments.find(a => a.id === this.selected!.id);
    if (appt) {
      appt.approverRemarks = this.remarksText;
      if (this.pendingAction === 'APPROVE') {
        appt.status = 'HCM_PENDING';
        this.msg.add({ severity: 'success', summary: 'Forwarded to HCM', detail: `${appt.applicationId} approved and pushed to HCM queue.` });
      } else {
        appt.status = 'HCM_REJECTED';
        this.msg.add({ severity: 'error', summary: 'Appointment Rejected', detail: `${appt.applicationId} has been rejected.` });
      }
    }
    this.ngOnInit();
    this.showRemarksDialog = false;
    this.selected = null;
    this.pendingAction = null;
  }

  confirmReschedule() {
    if (!this.selected) return;
    const appt = this.mock.appointments.find(a => a.id === this.selected!.id);
    if (appt && this.rescheduleDate) {
      appt.scheduledDateTime = new Date(this.rescheduleDate).toISOString();
      this.msg.add({ severity: 'info', summary: 'Rescheduled', detail: `${appt.applicationId} rescheduled to ${this.rescheduleDate}.` });
    }
    this.showRescheduleDialog = false;
    this.selected = null;
  }
}
