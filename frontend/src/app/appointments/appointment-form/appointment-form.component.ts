import { Component, OnInit } from '@angular/core';
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
import { environment } from '../../../environments/environment';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AiDocumentService, AiExtractedFields, AiDocumentAnalysisResponse, DuplicateCheckResponse } from '../../services/ai-document.service';
import { SchemeService } from '../../services/scheme.service';
import { VisitorService } from '../../services/visitor.service';
import { AuthService } from '../../services/auth.service';

interface Associate {
  name: string;
  phoneNumber: string;
  epicNumber: string;
  designation: string;
  address: string;
}

interface DocumentUpload {
  type: 'EPIC_SCAN' | 'APPLICATION_LETTER' | 'PLANS_ESTIMATES' | 'BANK_DETAILS' | 'MLA_APPROVAL_LETTER' | 'ORG_REGISTRATION_CERTIFICATE' | 'CM_CARE_ELIGIBILITY' | 'CM_CARE_HOSPITAL' | 'CM_CARE_SUPPORTING';
  label: string;
  isRequired: boolean;
  isVisible: boolean;
  file?: File;
  fileName?: string;
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
export class AppointmentFormComponent implements OnInit {
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
    organizationSubType: '',
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

  // Document tracking
  visitorKycStatus: 'PENDING' | 'VERIFIED' | 'REJECTED' | null = null;
  documents: DocumentUpload[] = [
    { type: 'EPIC_SCAN', label: 'EPIC / Voter ID Scan', isRequired: true, isVisible: false },
    { type: 'APPLICATION_LETTER', label: 'Application Letter / Project Proposal', isRequired: true, isVisible: true },
    { type: 'PLANS_ESTIMATES', label: 'Plans & Estimates (up to 3)', isRequired: false, isVisible: false },
    { type: 'BANK_DETAILS', label: 'Bank Account Details', isRequired: true, isVisible: true },
    { type: 'MLA_APPROVAL_LETTER', label: 'MLA/MDC/Community Leader Approval Letter', isRequired: false, isVisible: false },
    { type: 'ORG_REGISTRATION_CERTIFICATE', label: 'Organisation Registration Certificate', isRequired: false, isVisible: false },
    { type: 'CM_CARE_ELIGIBILITY', label: 'CM Care – Eligibility Proof', isRequired: false, isVisible: false },
    { type: 'CM_CARE_HOSPITAL', label: 'CM Care – Hospital Documents', isRequired: false, isVisible: false },
    { type: 'CM_CARE_SUPPORTING', label: 'CM Care – Supporting Documents', isRequired: false, isVisible: false },
  ];


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
  organizationTypes: Array<{ value: string; label: string }> = [];
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

  getOrganizationTypeLabel(): string {
    if (!this.form.organizationSubType) return '–';
    const orgType = this.organizationTypes.find(o => o.value === this.form.organizationSubType);
    return orgType ? orgType.label : '–';
  }

  updateDocumentVisibility() {
    // EPIC scan: visible if KYC is pending
    const epicDoc = this.documents.find(d => d.type === 'EPIC_SCAN');
    if (epicDoc) epicDoc.isVisible = this.visitorKycStatus === 'PENDING';

    // Plans & Estimates: visible for scheme applications
    const plansDoc = this.documents.find(d => d.type === 'PLANS_ESTIMATES');
    if (plansDoc) plansDoc.isVisible = this.isScheme;

    // MLA Approval Letter: visible if MLA approval selected
    const mlaDoc = this.documents.find(d => d.type === 'MLA_APPROVAL_LETTER');
    if (mlaDoc) mlaDoc.isVisible = this.form.mlaMdcApproved;

    // Organisation Registration Certificate: visible if organisation selected
    const orgDoc = this.documents.find(d => d.type === 'ORG_REGISTRATION_CERTIFICATE');
    if (orgDoc) orgDoc.isVisible = this.form.isOrganisation;

    // CM Care documents: visible if CM Care scheme selected
    const cmCareEligibility = this.documents.find(d => d.type === 'CM_CARE_ELIGIBILITY');
    const cmCareHospital = this.documents.find(d => d.type === 'CM_CARE_HOSPITAL');
    const cmCareSupporting = this.documents.find(d => d.type === 'CM_CARE_SUPPORTING');
    if (cmCareEligibility) cmCareEligibility.isVisible = this.isCmCare;
    if (cmCareHospital) cmCareHospital.isVisible = this.isCmCare;
    if (cmCareSupporting) cmCareSupporting.isVisible = this.isCmCare;
  }

  onDocumentChange(event: any, docType: DocumentUpload) {
    const file = event.target.files?.[0];
    if (file) {
      docType.file = file;
      docType.fileName = file.name;
    }
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

  constructor(private http: HttpClient, private aiDocumentService: AiDocumentService, private schemeService: SchemeService, private visitorService: VisitorService, private auth: AuthService) {}

  ngOnInit() {
    this.loadOrganizationTypes();
    this.loadVisitorDataIfPublic();
    // Initial visibility update based on default form values
    this.updateDocumentVisibility();
  }

  private loadVisitorDataIfPublic() {
    // Only auto-populate for PUBLIC users
    if (!this.auth.hasRole('PUBLIC')) return;

    const visitorId = sessionStorage.getItem('megha_visitor_id');
    if (!visitorId) return;

    this.visitorService.getById(parseInt(visitorId, 10)).subscribe({
      next: (visitor) => {
        // Auto-populate form fields from visitor data
        this.form.fullName = visitor.fullName || '';
        this.form.phoneNumber = visitor.phoneNumber || '';
        this.form.epicNumber = visitor.epicNumber || '';
        this.form.designation = visitor.designation || '';
        this.form.district = visitor.district || '';
        this.form.constituency = visitor.constituency || '';
        this.form.booth = visitor.booth || '';
        
        // Set KYC status and update document visibility
        this.visitorKycStatus = visitor.kycStatus as any || 'PENDING';
        this.updateDocumentVisibility();
      },
      error: (err) => {
        console.error('Failed to load visitor data:', err);
        // Default to PENDING status if API fails
        this.visitorKycStatus = 'PENDING';
        this.updateDocumentVisibility();
      }
    });
  }

  private loadOrganizationTypes() {
    this.schemeService.getOrganizationTypes().subscribe({
      next: (data) => {
        this.organizationTypes = data.map(d => ({
          value: d.code,
          label: d.value
        }));
      },
      error: (err) => {
        console.error('Failed to load organization types:', err);
        // Fallback to hardcoded values if API fails
        this.organizationTypes = [
          { value: 'VILLAGE_AUTHORITY', label: 'Village Authority' },
          { value: 'TEACHERS_BODY', label: 'Teachers Body' },
          { value: 'NGO', label: 'Civil Society / NGO' },
          { value: 'INSTITUTE', label: 'Institute' },
          { value: 'OTHER_ORG', label: 'Other' }
        ];
      }
    });
  }

  submit() {
    this.errorMsg = '';

    // Validate required documents are uploaded
    const requiredDocs = this.documents.filter(d => d.isVisible && d.isRequired);
    const missingDocs = requiredDocs.filter(d => !d.fileName);
    if (missingDocs.length > 0) {
      this.errorMsg = `Please upload all required documents: ${missingDocs.map(d => d.label).join(', ')}`;
      return;
    }

    this.loading = true;

    // Create FormData to handle file uploads
    const formData = new FormData();
    const visitorId = sessionStorage.getItem('megha_visitor_id');

    // Add form fields
    formData.append('applicantId', visitorId ? visitorId : '');
    formData.append('applicantName', this.form.fullName);
    formData.append('applicantPhone', this.form.phoneNumber);
    formData.append('epicNumber', this.form.epicNumber);
    formData.append('agendaType', this.form.agendaType);
    formData.append('agendaBrief', this.form.agendaBrief);
    formData.append('requestedLocation', this.form.requestedLocation?.toUpperCase() || 'OTHERS');
    formData.append('eventType', 'A1');
    formData.append('mlaMdcApproved', this.form.mlaMdcApproved ? 'true' : 'false');
    formData.append('schemeType', this.isScheme ? this.form.schemeType : '');
    formData.append('projectName', this.isScheme ? this.form.projectName : '');
    formData.append('projectCategory', this.isScheme ? this.form.projectCategory : '');
    formData.append('beneficiaryType', this.isScheme ? this.form.beneficiaryType : '');
    formData.append('beneficiaryCount', this.isScheme ? this.form.beneficiaryCount : '');
    formData.append('estimatedCost', this.isScheme && this.form.estimatedCost ? this.form.estimatedCost : '');
    formData.append('communityContribution', this.isScheme && this.form.communityContribution ? this.form.communityContribution : '');
    formData.append('justification', this.form.justification);
    formData.append('applicationType', this.form.applicationType);
    formData.append('organizationSubType', this.form.isOrganisation ? this.form.organizationSubType : '');
    formData.append('schemeHistoryList', JSON.stringify(this.form.schemeHistoryList));
    formData.append('aiPriorityLevel', this.effectivePriority || '');
    formData.append('aiSummary', this.aiSummary || '');

    // Add associates
    if (this.includeAssociates) {
      formData.append('associates', JSON.stringify(this.associates));
    }

    // Add document files
    this.documents.forEach(doc => {
      if (doc.file) {
        formData.append(`documents_${doc.type}`, doc.file, doc.fileName);
      }
    });

    this.http.post<{ success: boolean; applicationId?: string; message?: string; id?: number }>(
      `${environment.apiUrl}/visitor/appointments`, formData
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

