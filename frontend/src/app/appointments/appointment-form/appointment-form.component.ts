import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { Steps } from 'primeng/steps';
import { FileUpload } from 'primeng/fileupload';
import { RadioButton } from 'primeng/radiobutton';

interface Associate {
  name: string;
  phoneNumber: string;
  epicNumber: string;
  designation: string;
  address: string;
}

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InputText, Select, Textarea, Steps, FileUpload, RadioButton],
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.scss'],
})
export class AppointmentFormComponent {
  step = 0;
  submitted = false;
  loading = false;
  errorMsg = '';
  submittedAppId = '';

  steps = [
    { label: 'Personal Info' }, { label: 'Agenda' },
    { label: 'Scheme Details' }, { label: 'Associates' }, { label: 'Documents' }, { label: 'Review' }
  ];

  form = {
    fullName: '', phoneNumber: '', epicNumber: '', designation: '',
    district: '', constituency: '', booth: '', address: '',
    agendaType: '', requestedLocation: '', lastMeetingDate: '',
    agendaBrief: '', schemeType: '', projectName: '', projectCategory: '',
    beneficiaryType: '', beneficiaryCount: '', estimatedCost: '',
    communityContribution: '', justification: '',
    mlaMdcApproved: false,
    applicationType: 'NEW_APPLICATION',
    isOrganisation: false,
    schemeHistoryList: [] as string[],
  };

  // Associate visitors
  associates: Associate[] = [];
  newAssociate: Associate = { name: '', phoneNumber: '', epicNumber: '', designation: '', address: '' };

  designations = [
    'Govt Servant', 'Retd Govt Servant', 'Teacher', 'Political Leader',
    'Students', 'Religious Leader', 'Businessman', 'Media', 'General Public',
    'Organisation – Village Authority', 'Teachers Body', 'Civil Society / NGO',
    'Institute', 'Others'
  ];
  districts = [
    'East Khasi Hills', 'West Khasi Hills', 'South West Khasi Hills', 'Ri Bhoi',
    'East Jaintia Hills', 'West Jaintia Hills', 'East Garo Hills', 'West Garo Hills',
    'South Garo Hills', 'North Garo Hills', 'Eastern West Khasi Hills'
  ];
  agendaTypes = ['Scheme availment (CM)', 'Governance', 'Trade & Commerce', 'Political Discussion', 'Public Grievance'];
  schemeTypes = ['CMSDF', 'CMSG', 'CM Care', 'CM Connect', 'CM Elevate', 'Others'];
  schemeHistoryOptions = ['CMSDF', 'CMSG', 'CM Care', 'CM Connect', 'CM Elevate', 'Focus+'];
  applicationTypes = [
    { label: 'New Application', value: 'NEW_APPLICATION' },
    { label: 'Reminder for Old Application', value: 'REMINDER' },
  ];
  locations = ['Shillong', 'Tura', 'Delhi', 'Others'];
  projectCategories = [
    'Electricity', 'Road', 'House', 'School', 'Community Hall',
    'Retaining Wall', 'Office', 'Travel', 'Medical', 'Musical Instrument',
    'Sports Equipment', 'Buses', 'Pickup Van', 'Computer Lab Upgradation', 'Repair', 'Others'
  ];
  beneficiaryTypes = ['Individual', 'Community/Society', 'School/Youth Organisation', 'All of the Above', 'Others'];
  beneficiaryCounts = ['1 to 100', '101 to 500', '501 to 1000', 'Above 1000'];

  get isScheme() { return this.form.agendaType === 'Scheme availment (CM)'; }
  get isCmCare() { return this.form.schemeType === 'CM Care'; }

  isSchemeInHistory(scheme: string): boolean {
    return this.form.schemeHistoryList.includes(scheme);
  }

  toggleSchemeHistory(scheme: string) {
    const idx = this.form.schemeHistoryList.indexOf(scheme);
    if (idx === -1) {
      this.form.schemeHistoryList = [...this.form.schemeHistoryList, scheme];
    } else {
      this.form.schemeHistoryList = this.form.schemeHistoryList.filter(s => s !== scheme);
    }
  }

  addAssociate() {
    if (!this.newAssociate.name.trim()) return;
    this.associates = [...this.associates, { ...this.newAssociate }];
    this.newAssociate = { name: '', phoneNumber: '', epicNumber: '', designation: '', address: '' };
  }

  removeAssociate(index: number) {
    this.associates = this.associates.filter((_, i) => i !== index);
  }

  nextStep() { if (this.step < this.steps.length - 1) this.step++; }
  prevStep() { if (this.step > 0) this.step--; }

  constructor(private http: HttpClient) {}

  submit() {
    this.errorMsg = '';
    this.loading = true;

    const visitorId = sessionStorage.getItem('megha_visitor_id');
    const payload = {
      applicantId: visitorId ? parseInt(visitorId, 10) : null,
      applicantName: this.form.fullName,
      applicantPhone: this.form.phoneNumber,
      epicNumber: this.form.epicNumber,
      agendaType: this.form.agendaType,
      agendaBrief: this.form.agendaBrief,
      requestedLocation: this.form.requestedLocation?.toUpperCase() || 'OTHERS',
      eventType: 'A1',
      mlaMdcApproved: this.form.mlaMdcApproved,
      schemeType: this.isScheme ? this.form.schemeType : null,
      projectName: this.isScheme ? this.form.projectName : null,
      projectCategory: this.isScheme ? this.form.projectCategory : null,
      beneficiaryType: this.isScheme ? this.form.beneficiaryType : null,
      beneficiaryCount: this.isScheme ? this.form.beneficiaryCount : null,
      estimatedCost: this.isScheme && this.form.estimatedCost ? parseFloat(this.form.estimatedCost) : null,
      communityContribution: this.isScheme && this.form.communityContribution ? parseFloat(this.form.communityContribution) : null,
      justification: this.form.justification,
      applicationType: this.form.applicationType,
      associates: this.associates,
      schemeHistoryList: this.form.schemeHistoryList,
    };

    this.http.post<{ success: boolean; applicationId?: string; message?: string; id?: number }>(
      '/api/v1/visitor/appointments', payload
    ).subscribe({
      next: res => {
        this.loading = false;
        if (res.success !== false) {
          this.submitted = true;
          this.submittedAppId = res.applicationId || 'MC-' + Date.now().toString().slice(-6);
        } else {
          this.errorMsg = res.message || 'Submission failed. Please try again.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Submission failed. Please try again.';
      },
    });
  }
}
