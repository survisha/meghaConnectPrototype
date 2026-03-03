import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppointmentService } from '../services/appointment.service';
import { Appointment, EventType, Location } from '../models';

// PrimeNG
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ButtonDirective } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined;

@Component({
  selector: 'app-cmo-moderation',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    TableModule, Tag, ButtonDirective, Dialog,
    Select, Textarea, Toast,
  ],
  providers: [MessageService],
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

  constructor(private appointmentService: AppointmentService, private msg: MessageService) {}

  ngOnInit() {
    this.loading = true;
    this.appointmentService.getAllAppointments(0, 100).subscribe({
      next: page => {
        this.appointments = page.content.filter(a =>
          ['SUBMITTED', 'DEO_PROCESSED', 'CMO_REVIEW'].includes(a.status)
        );
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getSeverity(status: string): TagSeverity {
    const map: Record<string, TagSeverity> = {
      SUBMITTED: 'info',
      DEO_PROCESSED: 'secondary',
      CMO_REVIEW: 'warn',
    };
    return map[status] ?? undefined;
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
        this.msg.add({ severity: 'success', summary: 'Updated & Forwarded', detail: `${updated.applicationId} modified and forwarded to Approver.` });
        this.showModifyDialog = false;
        this.selected = null;
        this.ngOnInit();
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Error', detail: 'Failed to update appointment.' })
    });
  }

  forwardToApprover(appt: Appointment) {
    this.appointmentService.updateStatus(appt.id, 'APPROVER_REVIEW').subscribe({
      next: updated => {
        this.msg.add({ severity: 'info', summary: 'Forwarded', detail: `${updated.applicationId} forwarded to Jt. Secretary.` });
        this.ngOnInit();
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Error', detail: 'Failed to forward appointment.' })
    });
  }
}
