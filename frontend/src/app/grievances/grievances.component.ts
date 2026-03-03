import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GrievanceService } from '../services/grievance.service';
import { AuthService } from '../services/auth.service';
import { Grievance, GrievanceCategory, GrievanceStatus } from '../models';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Dialog } from 'primeng/dialog';
import { Divider } from 'primeng/divider';
import { Steps } from 'primeng/steps';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-grievances',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, Tag, Select, InputText, Textarea, Dialog, Divider, Steps, Toast],
  providers: [MessageService],
  templateUrl: './grievances.component.html',
  styleUrls: ['./grievances.component.scss'],
})
export class GrievancesComponent implements OnInit {
  grievances: Grievance[] = [];
  filtered: Grievance[] = [];
  search = '';
  filterStatus = '';
  filterCategory = '';
  loading = false;

  showForm = false;
  showDetail = false;
  selectedGrievance: Grievance | null = null;

  step = 0;
  formSteps = [{ label: 'Personal Info' }, { label: 'Grievance Details' }, { label: 'Review & Submit' }];

  form = {
    applicantName: '', phoneNumber: '', district: '', constituency: '',
    category: '' as GrievanceCategory | '',
    subject: '', description: '',
  };

  statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'Acknowledged', value: 'ACKNOWLEDGED' },
    { label: 'Under Review', value: 'UNDER_REVIEW' },
    { label: 'Forwarded', value: 'FORWARDED' },
    { label: 'Resolved', value: 'RESOLVED' },
    { label: 'Closed', value: 'CLOSED' },
  ];

  categoryOptions = [
    { label: 'All Categories', value: '' },
    { label: 'Public Services', value: 'PUBLIC_SERVICES' },
    { label: 'Infrastructure', value: 'INFRASTRUCTURE' },
    { label: 'Health', value: 'HEALTH' },
    { label: 'Education', value: 'EDUCATION' },
    { label: 'Employment', value: 'EMPLOYMENT' },
    { label: 'Welfare Scheme', value: 'WELFARE_SCHEME' },
    { label: 'Law & Order', value: 'LAW_ORDER' },
    { label: 'Others', value: 'OTHERS' },
  ];

  districts = ['East Khasi Hills', 'West Khasi Hills', 'South West Khasi Hills', 'Ri Bhoi',
    'East Jaintia Hills', 'West Jaintia Hills', 'East Garo Hills', 'West Garo Hills',
    'South Garo Hills', 'North Garo Hills', 'Eastern West Khasi Hills'];

  constructor(
    private grievanceService: GrievanceService,
    public auth: AuthService,
    private messageService: MessageService
  ) {}

  ngOnInit() {
    this.loading = true;
    this.grievanceService.getAll(0, 100).subscribe({
      next: page => { this.grievances = page.content; this.applyFilter(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  applyFilter() {
    this.filtered = this.grievances.filter(g =>
      (!this.search || g.applicantName.toLowerCase().includes(this.search.toLowerCase()) ||
        g.ticketId.includes(this.search) || g.subject.toLowerCase().includes(this.search.toLowerCase())) &&
      (!this.filterStatus || g.status === this.filterStatus) &&
      (!this.filterCategory || g.category === this.filterCategory)
    );
  }

  openDetail(g: Grievance) { this.selectedGrievance = g; this.showDetail = true; }

  nextStep() {
    if (this.step === 0 && (!this.form.applicantName.trim() || !this.form.phoneNumber.trim() || !this.form.district)) {
      return;
    }
    if (this.step === 1 && (!this.form.category || !this.form.subject.trim() || !this.form.description.trim())) {
      return;
    }
    if (this.step < this.formSteps.length - 1) this.step++;
  }
  prevStep() { if (this.step > 0) this.step--; }

  submitGrievance() {
    this.grievanceService.create({
      applicantName: this.form.applicantName,
      phoneNumber: this.form.phoneNumber,
      district: this.form.district,
      constituency: this.form.constituency,
      category: this.form.category as GrievanceCategory,
      subject: this.form.subject,
      description: this.form.description,
    }).subscribe({
      next: newGrievance => {
        this.grievances = [newGrievance, ...this.grievances];
        this.applyFilter();
        this.showForm = false;
        this.step = 0;
        this.form = { applicantName: '', phoneNumber: '', district: '', constituency: '', category: '', subject: '', description: '' };
        this.messageService.add({ severity: 'success', summary: 'Grievance Submitted', detail: `Ticket ID: ${newGrievance.ticketId}`, life: 5000 });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to submit grievance.' })
    });
  }

  updateStatus(g: Grievance, status: GrievanceStatus) {
    this.grievanceService.updateStatus(g.id, status).subscribe({
      next: updated => {
        const idx = this.grievances.findIndex(x => x.id === updated.id);
        if (idx >= 0) this.grievances[idx] = updated;
        this.applyFilter();
        this.showDetail = false;
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update status.' })
    });
  }

  getStatusSeverity(s: GrievanceStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | null | undefined {
    const m: Record<GrievanceStatus, 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast'> = {
      SUBMITTED: 'info', ACKNOWLEDGED: 'info', UNDER_REVIEW: 'warn',
      FORWARDED: 'warn', RESOLVED: 'success', CLOSED: 'secondary',
    };
    return m[s] ?? 'info';
  }

  getCategoryLabel(c: GrievanceCategory): string {
    const m: Record<GrievanceCategory, string> = {
      PUBLIC_SERVICES: 'Public Services', INFRASTRUCTURE: 'Infrastructure',
      HEALTH: 'Health', EDUCATION: 'Education', EMPLOYMENT: 'Employment',
      WELFARE_SCHEME: 'Welfare Scheme', LAW_ORDER: 'Law & Order', OTHERS: 'Others',
    };
    return m[c] ?? c;
  }

  get isStaff() { return this.auth.hasRole('HCM', 'ADMIN', 'SAIDUL_OSD', 'APPROVER_JT_SECY', 'CMO_OFFICER', 'DATA_ENTRY_OPERATOR'); }
  get isPublic() { return this.auth.hasRole('PUBLIC'); }
}
