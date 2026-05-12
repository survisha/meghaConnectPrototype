import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from '../../services/appointment.service';
import { Appointment } from '../../models';
import { Tag } from 'primeng/tag';

import { Timeline } from 'primeng/timeline';
import { Steps } from 'primeng/steps';
import { Divider } from 'primeng/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { apiErrorMessage } from '../../shared/api-error.util';

@Component({
  selector: 'app-appointment-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    Tag,
    Timeline,
    Steps,
    Divider,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule
  ],
  templateUrl: './appointment-detail.component.html',
  styleUrls: ['./appointment-detail.component.scss'],
})
export class AppointmentDetailComponent implements OnInit {
  appointment: Appointment | undefined;
  showDirectionDialog = false;
  directionColor = 'GREEN';
  directionText = '';
  loading = false;
  errorMsg = '';

  workflowSteps = [
    { label: 'Submitted' }, { label: 'CMO Review' }, { label: 'Approver Review' },
    { label: 'HCM Decision' }, { label: 'Scheduled' }
  ];
  currentStep = 3;

  timeline = [
    { status: 'SUBMITTED', date: '14 Mar 09:00', icon: 'send', color: '#4b5563', text: 'Application submitted by applicant' },
    { status: 'CMO_REVIEW', date: '14 Mar 09:15', icon: 'visibility', color: '#b45309', text: 'CMO Officer reviewed - documents verified' },
    { status: 'APPROVER_REVIEW', date: '14 Mar 11:30', icon: 'check', color: '#1a237e', text: 'Forwarded to Jt Secretary for approval' },
    { status: 'HCM_PENDING', date: '14 Mar 15:00', icon: 'star', color: '#dc2626', text: 'Approver approved - awaiting HCM decision' },
  ];

  constructor(private route: ActivatedRoute, private appointmentService: AppointmentService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading = true;
    this.appointmentService.getAppointmentById(id).subscribe({
      next: appt => {
        this.appointment = appt;
        this.errorMsg = '';
        this.loading = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load appointment details.');
        this.loading = false;
      }
    });
  }

  getStatusSeverity(s: string): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined {
    const m: Record<string,'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined> = { HCM_PENDING:'danger', SCHEDULED:'success', CMO_REVIEW:'warn', SUBMITTED:'info', COMPLETED:'success' };
    return m[s] ?? 'info';
  }

  issueDirection(color: string) {
    this.directionColor = color;
    this.showDirectionDialog = true;
  }

  saveDirection() {
    if (this.appointment && this.directionText) {
      this.appointment.directions = this.appointment.directions ?? [];
      this.appointment.directions.push({
        id: Date.now(), appointmentId: this.appointment.id,
        color: this.directionColor as any, directionText: this.directionText,
        isCompleted: false,
      });
      this.showDirectionDialog = false;
      this.directionText = '';
    }
  }

  getDirClass(color: string) {
    return { GREEN: 'dir-green', YELLOW: 'dir-yellow', BLUE: 'dir-blue' }[color] ?? '';
  }
}
