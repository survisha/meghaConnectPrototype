import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppointmentService } from '../services/appointment.service';
import { AuthService } from '../services/auth.service';
import { Appointment, Direction, DirectionColor } from '../models';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { ButtonDirective } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Textarea } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { UIChart } from 'primeng/chart';
import { MessageService } from 'primeng/api';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined;

@Component({
  selector: 'app-hcm-dashboard',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatButtonModule, MatIconModule,
    TableModule, Tag, ButtonDirective, Dialog, Textarea, Toast, UIChart,
  ],
  providers: [MessageService],
  templateUrl: './hcm-dashboard.component.html',
  styleUrls: ['./hcm-dashboard.component.scss'],
})
export class HcmDashboardComponent implements OnInit {
  appointments: Appointment[] = [];
  selected: Appointment | null = null;
  loading = false;
  showRemarksDialog = false;
  remarksText = '';
  pendingAction: 'APPROVE' | 'REJECT' | 'SNOOZE' | null = null;
  showDirectionDialog = false;
  directionText = '';
  pendingDirection: DirectionColor | null = null;
  scheduleDate = '';

  kpis = [
    { label: "Today's Appointments", value: 8,  icon: 'pi-calendar',    color: '#1a237e' },
    { label: 'Pending Approvals',     value: 14, icon: 'pi-clock',       color: '#c62828' },
    { label: 'Active Schemes',         value: 32, icon: 'pi-briefcase',   color: '#2e7d32' },
    { label: 'Pending Follow-ups',     value: 7,  icon: 'pi-exclamation-triangle', color: '#f57f17' },
  ];

  appointmentTypeData: any;
  districtData: any;
  chartOptions: any;

  constructor(
    public auth: AuthService,
    private appointmentService: AppointmentService,
    private msg: MessageService,
  ) {}

  ngOnInit() {
    this.loadPendingAppointments();
    this.appointmentTypeData = {
      labels: ['A1: Cabinet', 'A2: Events', 'A3: Files', 'A4: Individual', 'B1: Public Durbar', 'B2: Walk-in'],
      datasets: [{ data: [2, 5, 3, 18, 8, 6], backgroundColor: ['#1565c0','#2e7d32','#f57f17','#c62828','#4527a0','#006064'] }]
    };
    this.districtData = {
      labels: ['EKH', 'WKH', 'RiBhoi', 'EGH', 'WGH', 'Jaintia'],
      datasets: [{ label: 'Schemes', data: [45, 32, 18, 28, 22, 15], backgroundColor: '#1a237e' }]
    };
    this.chartOptions = { plugins: { legend: { position: 'bottom' } }, responsive: true };
  }

  loadPendingAppointments() {
    this.loading = true;
    this.appointmentService.getAllAppointments(0, 100).subscribe({
      next: page => {
        this.appointments = page.content.filter(a =>
          ['HCM_PENDING', 'APPROVER_REVIEW', 'SCHEDULED'].includes(a.status)
        );
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getSeverity(status: string): TagSeverity {
    const m: Record<string, TagSeverity> = {
      HCM_PENDING: 'warn', APPROVER_REVIEW: 'info',
      HCM_ACCEPTED: 'success', HCM_REJECTED: 'danger',
      HCM_SNOOZED: 'secondary', SCHEDULED: 'success',
    };
    return m[status] ?? undefined;
  }

  openAction(appt: Appointment, action: 'APPROVE' | 'REJECT' | 'SNOOZE') {
    this.selected = appt;
    this.pendingAction = action;
    this.remarksText = '';
    this.scheduleDate = '';
    this.showRemarksDialog = true;
  }

  openDirection(appt: Appointment, color: DirectionColor) {
    this.selected = appt;
    this.pendingDirection = color;
    this.directionText = '';
    this.showDirectionDialog = true;
  }

  getDirectionLabel(color: DirectionColor): string {
    const m: Record<DirectionColor, string> = {
      GREEN: 'Urgent Follow-up',
      YELLOW: 'Forward',
      BLUE: 'Ignore / Note',
    };
    return m[color];
  }

  confirmAction() {
    if (!this.selected || !this.pendingAction) return;
    const action = this.pendingAction;
    const id = this.selected.id;
    const obs = action === 'APPROVE'
      ? this.appointmentService.approveAppointment(id, this.remarksText)
      : action === 'REJECT'
        ? this.appointmentService.rejectAppointment(id, this.remarksText)
        : this.appointmentService.updateStatus(id, 'HCM_SNOOZED', this.remarksText);
    obs.subscribe({
      next: () => {
        const label = action === 'APPROVE' ? 'Approved' : action === 'REJECT' ? 'Rejected' : 'Snoozed';
        this.msg.add({ severity: 'success', summary: label, detail: `Application ${this.selected!.applicationId} ${label.toLowerCase()}` });
        this.appointments = this.appointments.filter(a => a.id !== id);
        this.showRemarksDialog = false;
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Error', detail: 'Action failed. Please try again.' })
    });
  }

  confirmDirection() {
    if (!this.selected || !this.pendingDirection) return;
    this.msg.add({ severity: 'success', summary: 'Direction Set', detail: `${this.getDirectionLabel(this.pendingDirection)} direction added` });
    this.showDirectionDialog = false;
  }

  reschedule(appt: Appointment) {
    this.selected = appt;
    this.pendingAction = null;
    this.scheduleDate = '';
    this.remarksText = '';
    this.showRemarksDialog = true;
  }
}
