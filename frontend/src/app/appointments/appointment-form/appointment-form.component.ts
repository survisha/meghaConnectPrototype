import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { VisitorRegisterComponent } from '../../visitor-register/visitor-register.component';
import { AiDocumentService, AiExtractedFields, AiDocumentAnalysisResponse, DuplicateCheckResponse } from '../../services/ai-document.service';
import { SchemeService } from '../../services/scheme.service';
import { VisitorService } from '../../services/visitor.service';
import { AssociateCitizen, VisitorSearchService } from '../../services/visitor-search.service';
import { AuthService } from '../../services/auth.service';
import { apiErrorMessage } from '../../shared/api-error.util';
import { ReferenceDataService } from '../../services/reference-data.service';
import { CameraCaptureService } from '../../shared/camera-capture.service';
import { jsPDF } from 'jspdf';

interface DocumentUpload {
  type: 'APPLICATION_LETTER' | 'PLANS_ESTIMATES' | 'BANK_DETAILS' | 'MLA_APPROVAL_LETTER' | 'ORG_REGISTRATION_CERTIFICATE' | 'CM_CARE_ELIGIBILITY' | 'CM_CARE_HOSPITAL' | 'CM_CARE_SUPPORTING';
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
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule
  ],
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.scss'],
})
export class AppointmentFormComponent implements OnInit, OnDestroy {
  readonly maxScanPages = 10;
  scanningDocument: DocumentUpload | null = null;
  scannedPages: string[] = [];
  scanProcessing = false;
  scanMessage = '';
  private documentCameraStream: MediaStream | null = null;
  step = 0;
  submitted = false;
  loading = false;
  errorMsg = '';
  submittedAppId = '';
  submittedAppointmentId: number | null = null;
  submittedTokenNumber = '';
  visitorId = '';
  visitorProfile: any | null = null;
  isWalkInFlow = false;

  steps = [
    { label: 'Personal Info' }, { label: 'Agenda' },
    { label: 'Scheme Details' }, { label: 'Associates' }, { label: 'Documents' }, { label: 'Review' }
  ];

  form = {
    fullName: '', phoneNumber: '', epicNumber: '', designation: '',
    district: '', constituency: '', booth: '', partNumber: '', address: '',
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
  associates: AssociateCitizen[] = [];
  associateSearchQuery = '';
  associateSearchResults: AssociateCitizen[] = [];
  associateSearching = false;
  associateSearchError = '';
  associateSearchAttempted = false;
  associateValidationError = '';
  readonly maxAssociates = 10;

  // AI state – R004/R005/R006/R007
  aiAnalysisLoading = false;
  aiExtracted: AiExtractedFields | null = null;
  aiSummary = '';
  aiPriorityLevel: 'HIGH' | 'MEDIUM' | 'LOW' | '' = '';
  aiPriorityReason = '';
  aiPriorityOverridden = false;
  overriddenPriority: 'HIGH' | 'MEDIUM' | 'LOW' | '' = '';
  duplicateWarning: { previousApplicationId: string; schemeName: string; dateSubmitted: string } | null = null;

  // Document tracking
  visitorKycStatus: 'PENDING' | 'VERIFIED' | 'REJECTED' | null = null;
  documents: DocumentUpload[] = [
    { type: 'APPLICATION_LETTER', label: 'Application Letter / Project Proposal', isRequired: true, isVisible: true },
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
  agendaTypes: string[] = [];
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

  get isScheme() { return this.isSchemeAgenda(this.form.agendaType); }
  get isCmCare() { return this.form.schemeType === 'CM Care'; }
  get isPublicUser() { return this.auth.hasRole('PUBLIC'); }
  get hasVisitorContext() { return this.isPublicUser || !!this.visitorId; }
  get applicantSectionTitle() {
    if (this.isPublicUser) return 'Logged-in Citizen';
    if (this.isWalkInFlow) return 'Selected Walk-in Visitor';
    if (this.visitorId) return 'Selected Visitor';
    return 'Personal Information';
  }
  get applicantSummaryText() {
    return this.isPublicUser
      ? 'MeghaConnect profile details will be used for this appointment.'
      : 'Selected visitor profile details will be used for this appointment.';
  }
  get applicantInfoText() {
    return this.isPublicUser
      ? 'Personal information is pulled from your logged-in visitor profile and is not collected again on this form.'
      : 'Personal information is pulled from the visitor profile selected by the DEO and is not collected again on this form.';
  }
  get backRoute() {
    if (this.isWalkInFlow) return '/appointments/walkin';
    if (this.isPublicUser) return '/visitor';
    return '/dashboard';
  }
  get backLabel() {
    if (this.isWalkInFlow) return 'Back to Walk-in Counter';
    if (this.isPublicUser) return 'Back to Dashboard';
    return 'Back to Dashboard';
  }

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

  searchAssociates() {
    const query = this.associateSearchQuery.trim();
    this.associateSearchError = '';
    this.associateValidationError = '';
    this.associateSearchResults = [];
    this.associateSearchAttempted = true;
    if (query.length < 2) {
      this.associateSearchError = 'Enter mobile number, EPIC, or at least 2 letters of the citizen name.';
      return;
    }
    this.associateSearching = true;
    this.visitorSearchService.searchAssociateCitizens(query).subscribe({
      next: results => {
        this.associateSearching = false;
        this.associateSearchResults = results || [];
        if (this.associateSearchResults.length === 0) {
          this.associateSearchError = 'Visitor not found.';
        }
      },
      error: err => {
        this.associateSearching = false;
        this.associateSearchError = apiErrorMessage(err, 'Unable to search registered citizens.');
      }
    });
  }

  registerAssociateInline() {
    const ref = this.dialog.open(VisitorRegisterComponent, {
      width: 'min(960px, 96vw)', maxWidth: '96vw', maxHeight: '94vh', disableClose: true
    });
    ref.afterClosed().subscribe((result?: { visitorId?: number }) => {
      if (!result?.visitorId) return;
      this.visitorSearchService.getById(result.visitorId).subscribe({
        next: visitor => {
          if (!visitor?.id) return;
          this.addAssociate({
            id: visitor.id, citizenId: visitor.id, fullName: visitor.fullName || '',
            mobileNumber: visitor.phoneNumber, epicReference: visitor.epicNumber,
            addressSummary: visitor.fullAddress || visitor.address,
            photoUrl: visitor.photoUrl || visitor.photoStoragePath,
            kycStatus: visitor.kycStatus, status: 'ACTIVE'
          });
          this.associateSearchQuery = '';
          this.associateSearchAttempted = false;
          this.associateSearchError = '';
        },
        error: err => this.associateValidationError = apiErrorMessage(err, 'Visitor registered but could not be loaded.')
      });
    });
  }

  addAssociate(candidate: AssociateCitizen) {
    this.associateValidationError = '';
    const citizenId = candidate?.citizenId;
    if (!citizenId) {
      this.associateValidationError = 'Citizen must register in the portal before being added as an associate visitor.';
      return;
    }
    if (this.associates.length >= this.maxAssociates) {
      this.associateValidationError = `Maximum ${this.maxAssociates} associate visitors can be added.`;
      return;
    }
    const primaryId = Number(this.visitorId || 0);
    if (primaryId && citizenId === primaryId) {
      this.associateValidationError = 'Primary citizen cannot be added again as an associate visitor.';
      return;
    }
    if (this.associates.some(a => a.citizenId === citizenId)) {
      this.associateValidationError = 'Duplicate associate visitors are not allowed.';
      return;
    }
    if ((candidate.status || 'ACTIVE').toUpperCase() !== 'ACTIVE') {
      this.associateValidationError = 'This citizen is inactive or blocked and cannot be added as an associate visitor.';
      return;
    }
    this.associates = [...this.associates, candidate];
    this.associateSearchResults = this.associateSearchResults.filter(item => item.citizenId !== citizenId);
  }

  removeAssociate(index: number) {
    this.associates = this.associates.filter((_, i) => i !== index);
  }

  associatePhotoUrl(candidate: AssociateCitizen): string {
    return this.normalizePhotoSource(candidate.photoUrl || '');
  }

  isKycPending(status?: string): boolean {
    return (status || '').toUpperCase() === 'KYC_PENDING' || (status || '').toUpperCase() === 'PENDING';
  }

  statusClass(status?: string): string {
    const normalized = (status || '').toUpperCase();
    if (['PHOTO_MATCHED', 'DEMOGRAPHIC_MATCHED', 'VERIFIED', 'APPROVED', 'ACTIVE'].includes(normalized)) return 'status-success';
    if (['KYC_PENDING', 'PENDING', 'MANUAL_VERIFICATION_REQUIRED'].includes(normalized)) return 'status-warn';
    if (['FAILED', 'REJECTED', 'BLOCKED', 'INACTIVE'].includes(normalized)) return 'status-danger';
    return 'status-info';
  }

  displayStatus(status?: string): string {
    return (status || 'PENDING').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }

  getOrganizationTypeLabel(): string {
    if (!this.form.organizationSubType) return '–';
    const orgType = this.organizationTypes.find(o => o.value === this.form.organizationSubType);
    return orgType ? orgType.label : '–';
  }

  updateDocumentVisibility() {
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
      // On moving to Review step, trigger AI priority + duplicate check
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

  /** R007: AI priority recommendation + R006 duplicate check */
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
  }

  async startDocumentScan(docType: DocumentUpload): Promise<void> {
    if (this.scanProcessing) return;
    this.cancelDocumentScan();
    this.errorMsg = '';
    this.scanMessage = '';
    try {
      this.documentCameraStream = await this.cameraCapture.open('environment');
      this.scanningDocument = docType;
      setTimeout(() => {
        const video = document.getElementById('appointmentDocumentScanVideo') as HTMLVideoElement | null;
        if (video && this.documentCameraStream) this.cameraCapture.attach(video, this.documentCameraStream);
      });
    } catch {
      this.errorMsg = 'Camera permission is required to scan a document. You can enable camera permission or upload an existing PDF.';
      this.stopDocumentCamera();
    }
  }

  captureDocumentPage(): void {
    if (!this.scanningDocument || this.scanProcessing || this.scannedPages.length >= this.maxScanPages) return;
    const video = document.getElementById('appointmentDocumentScanVideo') as HTMLVideoElement | null;
    if (!video?.videoWidth || !video.videoHeight) {
      this.errorMsg = 'Camera is not ready. Please wait and try again.';
      return;
    }
    const maxDimension = 1800;
    const scale = Math.min(1, maxDimension / Math.max(video.videoWidth, video.videoHeight));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(video.videoWidth * scale);
    canvas.height = Math.round(video.videoHeight * scale);
    const context = canvas.getContext('2d');
    if (!context) {
      this.errorMsg = 'Unable to capture the document page.';
      return;
    }
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    this.scannedPages.push(canvas.toDataURL('image/jpeg', 0.82));
    this.scanMessage = `Page ${this.scannedPages.length} captured.`;
  }

  removeScannedPage(index: number): void {
    if (!this.scanProcessing) this.scannedPages.splice(index, 1);
  }

  async finishDocumentScan(): Promise<void> {
    const target = this.scanningDocument;
    if (!target || !this.scannedPages.length || this.scanProcessing) return;
    this.scanProcessing = true;
    this.scanMessage = 'Processing document...';
    try {
      const pdf = new jsPDF({ unit: 'pt', format: 'a4', compress: true });
      for (let index = 0; index < this.scannedPages.length; index++) {
        if (index > 0) pdf.addPage();
        const properties = pdf.getImageProperties(this.scannedPages[index]);
        const pageWidth = pdf.internal.pageSize.getWidth();
        const pageHeight = pdf.internal.pageSize.getHeight();
        const margin = 24;
        const scale = Math.min((pageWidth - margin * 2) / properties.width, (pageHeight - margin * 2) / properties.height);
        const width = properties.width * scale;
        const height = properties.height * scale;
        pdf.addImage(this.scannedPages[index], 'JPEG', (pageWidth - width) / 2, (pageHeight - height) / 2, width, height, undefined, 'FAST');
      }
      const blob = pdf.output('blob');
      if (!blob.size) throw new Error('Empty PDF');
      const fileName = `appointment-document-${Date.now()}.pdf`;
      target.file = new File([blob], fileName, { type: 'application/pdf' });
      target.fileName = fileName;
      this.scanMessage = 'Document ready and attached. It will upload automatically when the appointment is submitted.';
      this.stopDocumentCamera();
      this.scanningDocument = null;
      this.scannedPages = [];
    } catch {
      this.errorMsg = 'PDF generation failed. Your captured pages are still available; please retry or cancel.';
    } finally {
      this.scanProcessing = false;
    }
  }

  cancelDocumentScan(): void {
    this.stopDocumentCamera();
    this.scanningDocument = null;
    this.scannedPages = [];
    this.scanProcessing = false;
    this.scanMessage = '';
  }

  private stopDocumentCamera(): void {
    this.cameraCapture.stop(this.documentCameraStream);
    this.documentCameraStream = null;
  }

  onAgendaTypeChange() {
    this.updateDocumentVisibility();
    if (this.isScheme) {
      this.step = Math.max(this.step, 1);
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

  constructor(
    private http: HttpClient,
    private aiDocumentService: AiDocumentService,
    private schemeService: SchemeService,
    private visitorService: VisitorService,
    private visitorSearchService: VisitorSearchService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private referenceDataService: ReferenceDataService,
    private cameraCapture: CameraCaptureService,
    private dialog: MatDialog
  ) {}

  ngOnDestroy(): void {
    this.cancelDocumentScan();
  }

  ngOnInit() {
    this.isWalkInFlow = this.route.snapshot.queryParamMap.get('source') === 'walkin'
      || this.route.snapshot.queryParamMap.get('walkin') === 'true';
    this.steps[0] = { label: this.isPublicUser ? 'Citizen Details' : this.isWalkInFlow ? 'Visitor Details' : 'Personal Info' };
    this.loadAgendaTypes();
    this.loadOrganizationTypes();
    this.loadVisitorProfileIfAvailable();
    // Initial visibility update based on default form values
    this.updateDocumentVisibility();
  }

  private loadVisitorProfileIfAvailable() {
    const routeVisitorId = this.route.snapshot.queryParamMap.get('visitorId');
    const visitorId = this.isPublicUser ? sessionStorage.getItem('megha_visitor_id') : routeVisitorId;
    if (!visitorId) {
      if (this.isPublicUser) {
        this.errorMsg = 'Visitor session is missing. Please login again before creating an appointment.';
      }
      return;
    }
    this.visitorId = visitorId;

    this.visitorService.getById(parseInt(visitorId, 10)).subscribe({
      next: (visitor) => {
        this.visitorProfile = visitor;
        // Auto-populate form fields from visitor data
        this.form.fullName = visitor.fullName || '';
        this.form.phoneNumber = visitor.phoneNumber || '';
        this.form.epicNumber = visitor.epicNumber || '';
        this.form.designation = visitor.designation || '';
        this.form.district = visitor.district || '';
        this.form.constituency = visitor.constituency || '';
        this.form.partNumber = visitor.partNumber || visitor.pollingPartNo || '';
        this.form.booth = visitor.boothVillage || visitor.booth || this.form.partNumber || '';
        this.form.address = visitor.fullAddress || visitor.address || visitor.addressLine || visitor.address1 || '';
        this.applyRegistrationAgenda(visitor);
        if (this.isWalkInFlow) {
          this.form.requestedLocation = 'Shillong';
          this.applyWalkInAgendaDefault();
        }
        
        // Set KYC status and update document visibility
        this.visitorKycStatus = visitor.kycStatus as any || 'PENDING';
        this.updateDocumentVisibility();
      },
      error: (err) => {
        console.error('Failed to load visitor data:', err);
        this.errorMsg = apiErrorMessage(err, 'Could not load your visitor profile. Please refresh or login again.');
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

    const visitorId = this.visitorId || (this.isPublicUser ? sessionStorage.getItem('megha_visitor_id') : '') || '';
    if ((this.isPublicUser || this.isWalkInFlow) && !visitorId) {
      this.errorMsg = this.isWalkInFlow
        ? 'Visitor context is missing. Please return to Walk-in Counter and search the visitor again.'
        : 'Visitor context is missing. Please login again before submitting an appointment.';
      return;
    }
    if (!this.form.agendaType || !this.form.requestedLocation || !this.form.agendaBrief.trim()) {
      this.errorMsg = 'Please complete the appointment agenda, location, and purpose before submitting.';
      return;
    }
    if (this.includeAssociates) {
      const invalidAssociate = this.associates.some(a => !a.citizenId);
      if (invalidAssociate) {
        this.errorMsg = 'Every associate visitor must be selected from registered citizens.';
        return;
      }
      const primaryId = Number(visitorId || 0);
      if (primaryId && this.associates.some(a => a.citizenId === primaryId)) {
        this.errorMsg = 'Primary citizen cannot be added again as an associate visitor.';
        return;
      }
      const uniqueIds = new Set(this.associates.map(a => a.citizenId));
      if (uniqueIds.size !== this.associates.length) {
        this.errorMsg = 'Duplicate associate visitors are not allowed.';
        return;
      }
    }

    this.loading = true;

    // Create FormData to handle file uploads
    const formData = new FormData();

    // Add form fields
    if (visitorId) {
      formData.append('applicantId', visitorId);
    }
    if (!this.hasVisitorContext) {
      formData.append('applicantName', this.form.fullName);
      formData.append('applicantPhone', this.form.phoneNumber);
      formData.append('epicNumber', this.form.epicNumber);
    }
    formData.append('agendaType', this.form.agendaType);
    formData.append('agendaBrief', this.form.agendaBrief);
    formData.append('registrationAgendaType', this.visitorProfile?.agendaType || this.form.agendaType || '');
    formData.append('registrationBriefDescription', this.visitorProfile?.briefDescription || this.form.agendaBrief || '');
    formData.append('requestedLocation', this.form.requestedLocation?.toUpperCase() || 'OTHERS');
    formData.append('eventType', this.isWalkInFlow ? 'B2' : 'A1');
    formData.append('isWalkIn', this.isWalkInFlow ? 'true' : 'false');
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
      formData.append('associates', JSON.stringify(this.associates.map(a => ({
        citizenId: a.citizenId,
        remarks: a.remarks || a.relationship || ''
      }))));
    }

    // Add document files
    this.documents.forEach(doc => {
      if (doc.file) {
        formData.append(`documents_${doc.type}`, doc.file, doc.fileName);
      }
    });

    this.http.post<{ success: boolean; applicationId?: string; message?: string; id?: number; tokenNumber?: string; walkInTokenNumber?: string }>(
      `${environment.apiUrl}/appointments`, formData
    ).subscribe({
      next: res => {
        this.loading = false;
        if (res.success !== false) {
          this.submitted = true;
          this.submittedAppId = res.applicationId || 'MC-' + Date.now().toString().slice(-6);
          this.submittedAppointmentId = res.id ?? null;
          this.submittedTokenNumber = res.tokenNumber || res.walkInTokenNumber || '';
        } else {
          this.errorMsg = res.message || 'Submission failed. Please try again.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = apiErrorMessage(err, 'Submission failed. Please try again.');
      },
    });
  }

  private applyRegistrationAgenda(visitor: { agendaType?: string; briefDescription?: string }) {
    const storedAgenda = (visitor.agendaType || '').trim();
    if (storedAgenda) {
      this.form.agendaType = this.matchAgendaOption(storedAgenda);
    }
    const storedBrief = (visitor.briefDescription || '').trim();
    if (storedBrief && !this.form.agendaBrief) {
      this.form.agendaBrief = storedBrief;
    }
    this.updateDocumentVisibility();
  }

  private applyWalkInAgendaDefault() {
    if (!this.form.agendaType && this.agendaTypes.includes('Invitation')) {
      this.form.agendaType = 'Invitation';
    }
    this.updateDocumentVisibility();
  }

  private matchAgendaOption(value: string) {
    const normalized = this.normalizeAgenda(value);
    return this.agendaTypes.find(option => this.normalizeAgenda(option) === normalized) || value;
  }

  private isSchemeAgenda(value: string) {
    const normalized = this.normalizeAgenda(value);
    return normalized.includes('scheme') && normalized.includes('availment');
  }

  private normalizeAgenda(value: string) {
    return (value || '')
      .trim()
      .toLowerCase()
      .replace(/[\s_-]+/g, ' ')
      .replace(/[()]/g, '')
      .trim();
  }

  private loadAgendaTypes() {
    this.referenceDataService.getByType('CM_AGENDA_MEETING').subscribe({
      next: data => {
        this.agendaTypes = (data || []).map(item => item.value).filter(Boolean);
        if (this.form.agendaType) {
          this.form.agendaType = this.matchAgendaOption(this.form.agendaType);
        }
        if (this.isWalkInFlow) {
          this.applyWalkInAgendaDefault();
        }
      },
      error: err => {
        this.agendaTypes = [];
        this.errorMsg = apiErrorMessage(err, 'Unable to load agenda types.');
      }
    });
  }

  private normalizePhotoSource(value: string): string {
    const source = value.trim();
    if (!source) return '';
    if (source.startsWith('data:image/') || source.startsWith('blob:') || /^https?:\/\//i.test(source)) {
      return source;
    }
    const origin = environment.apiUrl.replace(/\/api\/v1\/?$/i, '');
    const cleanPath = source.replace(/^\/+/, '');
    return `${origin}/${cleanPath.startsWith('uploads/') ? cleanPath : `uploads/${cleanPath}`}`;
  }

  downloadSubmittedPass() {
    if (!this.submittedAppointmentId) {
      return;
    }
    this.http.get(`${environment.apiUrl}/appointments/${this.submittedAppointmentId}/visitor-pass/download`, {
      responseType: 'blob',
    }).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `${this.submittedTokenNumber || this.submittedAppId || 'walkin-pass'}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
    });
  }
}

