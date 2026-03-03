import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../services/mock-data.service';
import { Appointment, EventType, Location } from '../models';

// PrimeNG
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { Divider } from 'primeng/divider';
import { MessageService } from 'primeng/api';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined;

@Component({
  selector: 'app-cmo-moderation',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    TableModule, Tag, Button, Dialog,
    Select, Textarea, Toast, Divider,
  ],
  providers: [MessageService],
  templateUrl: './cmo-moderation.component.html',
  styleUrls: ['./cmo-moderation.component.scss'],
})
export class CmoModerationComponent implements OnInit {

  appointments: Appointment[] = [];
  selected: Appointment | null = null;
  showModifyDialog = false;

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

  constructor(private mock: MockDataService, private msg: MessageService) {}

  ngOnInit() {
    this.appointments = this.mock.appointments.filter(a =>
      ['SUBMITTED', 'DEO_PROCESSED', 'CMO_REVIEW'].includes(a.status)
    );
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
    const appt = this.mock.appointments.find(a => a.id === this.selected!.id);
    if (appt) {
      appt.eventType = this.modifyEventType;
      appt.requestedLocation = this.modifyLocation;
      appt.cmoRemarks = this.modifyRemarks;
      appt.status = 'APPROVER_REVIEW';
      this.msg.add({ severity: 'success', summary: 'Updated & Forwarded', detail: `${appt.applicationId} modified and forwarded to Approver.` });
    }
    this.ngOnInit();
    this.showModifyDialog = false;
    this.selected = null;
  }

  forwardToApprover(appt: Appointment) {
    const a = this.mock.appointments.find(x => x.id === appt.id);
    if (a) {
      a.status = 'APPROVER_REVIEW';
      this.msg.add({ severity: 'info', summary: 'Forwarded', detail: `${a.applicationId} forwarded to Jt. Secretary.` });
      this.ngOnInit();
    }
  }
}
