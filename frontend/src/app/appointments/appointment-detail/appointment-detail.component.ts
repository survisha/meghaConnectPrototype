import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MockDataService } from '../../services/mock-data.service';
import { Appointment } from '../../models';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import { Timeline } from 'primeng/timeline';
import { Steps } from 'primeng/steps';
import { Dialog } from 'primeng/dialog';
import { Textarea } from 'primeng/textarea';
import { Divider } from 'primeng/divider';

@Component({
  selector: 'app-appointment-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, Button, Tag,  Timeline, Steps, Dialog, Textarea, Divider],
  templateUrl: './appointment-detail.component.html',
  styleUrls: ['./appointment-detail.component.scss'],
})
export class AppointmentDetailComponent implements OnInit {
  appointment: Appointment | undefined;
  showDirectionDialog = false;
  directionColor = 'GREEN';
  directionText = '';

  workflowSteps = [
    { label: 'Submitted' }, { label: 'CMO Review' }, { label: 'Approver Review' },
    { label: 'HCM Decision' }, { label: 'Scheduled' }
  ];
  currentStep = 3;

  timeline = [
    { status: 'SUBMITTED', date: '14 Mar 09:00', icon: 'pi pi-send', color: '#4b5563', text: 'Application submitted by applicant' },
    { status: 'CMO_REVIEW', date: '14 Mar 09:15', icon: 'pi pi-eye', color: '#b45309', text: 'CMO Officer reviewed – documents verified' },
    { status: 'APPROVER_REVIEW', date: '14 Mar 11:30', icon: 'pi pi-check', color: '#1a237e', text: 'Forwarded to Jt Secretary for approval' },
    { status: 'HCM_PENDING', date: '14 Mar 15:00', icon: 'pi pi-star', color: '#dc2626', text: 'Approver approved – awaiting HCM decision' },
  ];

  constructor(private route: ActivatedRoute, public mock: MockDataService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.appointment = this.mock.appointments.find(a => a.id === id);
  }

  getStatusSeverity(s: string): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | null | undefined {
    const m: Record<string,'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | null | undefined> = { HCM_PENDING:'danger', SCHEDULED:'success', CMO_REVIEW:'warn', SUBMITTED:'info', COMPLETED:'success' };
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
