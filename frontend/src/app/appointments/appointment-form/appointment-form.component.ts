import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatStepperModule } from '@angular/material/stepper';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AiDocumentService, AiExtractedFields, AiDocumentAnalysisResponse, DuplicateCheckResponse } from '../../services/ai-document.service';

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
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatStepperModule,
    MatRadioModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
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

  // Associate visitors – R013
  includeAssociates = false;
  associates: Associate[] = [];
  newAssociate: Associate = { name: '', phoneNumber: '', epicNumber: '', designation: '', address: '' };

  // AI state – R004/R005/R006/R007/R015
  aiAnalysisLoading = false;
  aiExtracted: AiExtractedFields | null = null;
  aiSummary = '';
  aiPriorityLevel: 'HIGH' | 'MEDIUM' | 'LOW' | '' = '';
  aiPriorityReason = '';
  aiPriorityOverridden = false;
  overriddenPriority: 'HIGH' | 'MEDIUM' | 'LOW' | '' = '';
  duplicateWarning: { previousApplicationId: string; schemeName: string; dateSubmitted: string } | null = null;
  suggestedSlots: string[] = [];

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
  priorityOptions: Array<'HIGH' | 'MEDIUM' | 'LOW'> = ['HIGH', 'MEDIUM', 'LOW'];

  get isScheme() { return this.form.agendaType === 'Scheme availment (CM)'; }
  get isCmCare() { return this.form.schemeType === 'CM Care'; }

  get effectivePriority(): 'HIGH' | 'MEDIUM' | 'LOW' | '' {
    return this.aiPriorityOverridden ? this.overriddenPriority : this.aiPriorityLevel;
  }

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

  nextStep() {
    if (this.step < this.steps.length - 1) {
      this.step++;
      // On moving to Review step, trigger AI priority + duplicate check + slot suggestions
      if (this.step === 5) {
        this.runAiPreSubmitChecks();
      }
    }
  }
  prevStep() { if (this.step > 0) this.step--; }

  /** R004/R005: Analyse uploaded document with AI */
  onDocumentUpload(event: any) {
    const file: File | null = event?.files?.[0] ?? null;
    if (!file) return;
    this.aiAnalysisLoading = true;
    this.aiExtracted = null;
    this.aiSummary = '';
    this.aiDocumentService.analyzeDocument(file).subscribe((res: AiDocumentAnalysisResponse) => {
      this.aiAnalysisLoading = false;
      if (res.success) {
        this.aiExtracted = res.extractedFields;
        this.aiSummary = res.summary;
        this.autoFillFromExtracted(res.extractedFields);
      }
    });
  }

  /** Auto-fills form fields from AI-extracted document data (only if field is currently empty) */
  private autoFillFromExtracted(fields: AiExtractedFields) {
    if (fields.projectName && !this.form.projectName) {
      this.form.projectName = fields.projectName;
    }
    if (fields.projectCategory && !this.form.projectCategory) {
      this.form.projectCategory = fields.projectCategory;
    }
    if (fields.estimatedCost && !this.form.estimatedCost) {
      this.form.estimatedCost = fields.estimatedCost;
    }
    if (fields.justification && !this.form.justification) {
      this.form.justification = fields.justification;
    }
  }

  /** R007: AI priority recommendation + R006 duplicate check + R015 slot suggestions */
  private runAiPreSubmitChecks() {
    // Priority
    if (!this.aiPriorityLevel) {
      this.aiDocumentService.suggestPriority(this.form.agendaType, this.form.agendaBrief).subscribe((res: { level: 'HIGH' | 'MEDIUM' | 'LOW'; reason: string }) => {
        this.aiPriorityLevel = res.level;
        this.aiPriorityReason = res.reason;
      });
    }
    // Duplicate check
    if (this.form.epicNumber || this.form.phoneNumber) {
      this.aiDocumentService.checkDuplicate({
        epicNumber: this.form.epicNumber,
        phoneNumber: this.form.phoneNumber,
        agendaType: this.form.agendaType,
        schemeType: this.isScheme ? this.form.schemeType : undefined,
        projectName: this.form.projectName,
      }).subscribe((res: DuplicateCheckResponse) => {
        if (res.isDuplicate) {
          this.duplicateWarning = {
            previousApplicationId: res.previousApplicationId ?? '',
            schemeName: res.schemeName ?? '',
            dateSubmitted: res.dateSubmitted ?? '',
          };
        }
      });
    }
    // Slot suggestions
    if (this.form.requestedLocation) {
      this.aiDocumentService.suggestTimeSlots(this.form.requestedLocation, this.form.agendaType).subscribe((slots: string[]) => {
        this.suggestedSlots = slots;
      });
    }
  }

  overridePriority(level: 'HIGH' | 'MEDIUM' | 'LOW') {
    this.aiPriorityOverridden = true;
    this.overriddenPriority = level;
  }

  resetPriorityOverride() {
    this.aiPriorityOverridden = false;
    this.overriddenPriority = '';
  }

  constructor(private http: HttpClient, private aiDocumentService: AiDocumentService) {}

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
      associates: this.includeAssociates ? this.associates : [],
      schemeHistoryList: this.form.schemeHistoryList,
      aiPriorityLevel: this.effectivePriority || null,
      aiSummary: this.aiSummary || null,
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

