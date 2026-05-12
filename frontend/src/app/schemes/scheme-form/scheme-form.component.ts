import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { SchemeService } from '../../services/scheme.service';
import { AuthService } from '../../services/auth.service';
import { apiErrorMessage } from '../../shared/api-error.util';

interface DocumentUpload {
  type: 'PLANS_ESTIMATES' | 'BANK_DETAILS' | 'MLA_APPROVAL_LETTER' | 'CM_CARE_HOSPITAL' | 'ORG_REGISTRATION_CERTIFICATE';
  label: string;
  isRequired: boolean;
  isVisible: boolean;
  accept: string;
  file?: File;
  fileName?: string;
}

@Component({
  selector: 'app-scheme-form',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterLink, 
    MatFormFieldModule, 
    MatInputModule, 
    MatSelectModule, 
    MatButtonModule, 
    MatRadioModule,
    MatCheckboxModule,
    MatDividerModule, 
    MatIconModule
  ],
  templateUrl: './scheme-form.component.html',
  styleUrls: ['./scheme-form.component.scss'],
})
export class SchemeFormComponent implements OnInit {
  step = 0;
  steps = [{ label: 'Scheme & Applicant' }, { label: 'Project Details' }, { label: 'Financial' }, { label: 'Documents' }, { label: 'Submit' }];

  form: any = {
    schemeType: '', projectName: '', projectCategory: '', beneficiaryType: '',
    beneficiaryCount: '', estimatedCost: 0, communityContribution: 0,
    justification: '', mlaMdcApproved: false, isReminder: false,
    items: [{ description: '', quantity: 1, unitCost: 0 }],
    schemeHistoryList: [] as string[],
  };

  schemeTypes: any[] = [];
  errorMsg = '';
  successMsg = '';
  loading = false;
  submitted = false;
  submittedApplicationId = '';
  projectCategories = ['Electricity','Road','House','School','Community hall','Retaining wall','Office','Travel','Medical','Musical instrument','Sports Equipment','Buses','Pickup Van','Computer lab upgradation','Repair','Others'];
  beneficiaryTypes = ['Individual','Community/Society','School/Youth Organisation','All of the above','Others'];
  beneficiaryCounts = ['1 TO 100','101 TO 500','501 TO 1000','Above 1000'];
  schemeHistoryOptions = ['CMSDF', 'CMSG', 'CM Care', 'CM Connect', 'CM Elevate', 'Focus+'];
  documents: DocumentUpload[] = [
    { type: 'PLANS_ESTIMATES', label: 'Plans & Estimates (3 nos)', isRequired: true, isVisible: true, accept: '.pdf' },
    { type: 'BANK_DETAILS', label: 'Bank Account Details', isRequired: true, isVisible: true, accept: '.pdf,.jpg,.jpeg,.png' },
    { type: 'MLA_APPROVAL_LETTER', label: 'MLA / MDC Letter', isRequired: false, isVisible: false, accept: '.pdf,.doc,.docx,.jpg,.jpeg,.png' },
    { type: 'CM_CARE_HOSPITAL', label: 'Hospital / Medical Docs (CM Care)', isRequired: false, isVisible: false, accept: '.pdf,.doc,.docx,.jpg,.jpeg,.png' },
    { type: 'ORG_REGISTRATION_CERTIFICATE', label: 'Organisation Registration Certificate', isRequired: false, isVisible: true, accept: '.pdf,.doc,.docx,.jpg,.jpeg,.png' },
  ];

  constructor(
    private schemeService: SchemeService,
    private auth: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.schemeService.getSchemeTypes().subscribe({
      next: (data) => {
        this.schemeTypes = data.map(d => ({ label: d.value, value: d.code }));
        this.errorMsg = '';
      },
      error: (err) => {
        console.error('Failed to load scheme types:', err);
        this.errorMsg = apiErrorMessage(err, 'Unable to load scheme types.');
      }
    });
  }

  addItem() { this.form.items.push({ description: '', quantity: 1, unitCost: 0 }); }
  removeItem(i: number) { if (this.form.items.length > 1) this.form.items.splice(i, 1); }
  get totalCost() {
    return this.form.items.reduce((sum: number, it: any) => {
      const quantity = Number(it.quantity) || 0;
      const unitCost = Number(it.unitCost) || 0;
      return sum + (quantity * unitCost);
    }, 0);
  }

  get selectedSchemeLabel(): string {
    const selected = this.schemeTypes.find(opt => opt.value === this.form.schemeType);
    return selected?.label || this.displayValue(this.form.schemeType);
  }

  get isCmCare(): boolean {
    return this.normalizeSchemeType(this.form.schemeType) === 'CM_CARE';
  }

  updateDocumentVisibility() {
    const mlaDoc = this.documents.find(d => d.type === 'MLA_APPROVAL_LETTER');
    if (mlaDoc) {
      mlaDoc.isVisible = !!this.form.mlaMdcApproved;
    }

    const cmCareDoc = this.documents.find(d => d.type === 'CM_CARE_HOSPITAL');
    if (cmCareDoc) {
      cmCareDoc.isVisible = this.isCmCare;
    }
  }

  onDocumentChange(event: Event, doc: DocumentUpload) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      doc.file = file;
      doc.fileName = file.name;
    }
  }

  displayValue(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return '–';
    }
    return String(value);
  }

  formatCurrency(value: unknown): string {
    const amount = Number(value) || 0;
    return `₹${amount.toLocaleString('en-IN')}`;
  }

  isSchemeInHistory(scheme: string): boolean {
    return this.form.schemeHistoryList.includes(scheme);
  }

  toggleSchemeHistory(scheme: string) {
    const idx = this.form.schemeHistoryList.indexOf(scheme);
    if (idx === -1) {
      this.form.schemeHistoryList = [...this.form.schemeHistoryList, scheme];
    } else {
      this.form.schemeHistoryList = this.form.schemeHistoryList.filter((s: string) => s !== scheme);
    }
  }

  nextStep() { if (this.step < this.steps.length - 1) this.step++; }
  prevStep() { if (this.step > 0) this.step--; }
  submit() {
    this.errorMsg = '';
    this.successMsg = '';

    const applicantId = this.resolveApplicantId();
    if (!applicantId) {
      this.errorMsg = 'Visitor context is missing. Please login as a visitor or open this form with a selected visitor.';
      return;
    }
    if (!this.form.schemeType) {
      this.errorMsg = 'Please select a scheme type.';
      return;
    }
    if (!this.form.projectName.trim()) {
      this.errorMsg = 'Please enter the project name.';
      return;
    }

    const items = this.form.items
      .filter((item: any) => (item.description || '').trim())
      .map((item: any) => ({
        description: item.description.trim(),
        quantity: Number(item.quantity) > 0 ? Number(item.quantity) : 1,
        unitCost: Number(item.unitCost) || 0,
      }));

    const missingDocs = this.documents.filter(doc => doc.isVisible && doc.isRequired && !doc.fileName);
    if (missingDocs.length > 0) {
      this.errorMsg = `Please upload all required documents: ${missingDocs.map(doc => doc.label).join(', ')}`;
      return;
    }

    const formData = new FormData();
    this.appendFormValue(formData, 'applicantId', applicantId);
    this.appendFormValue(formData, 'schemeType', this.form.schemeType);
    this.appendFormValue(formData, 'projectName', this.form.projectName.trim());
    this.appendFormValue(formData, 'projectCategory', this.form.projectCategory);
    this.appendFormValue(formData, 'beneficiaryType', this.form.beneficiaryType);
    this.appendFormValue(formData, 'beneficiaryCount', this.form.beneficiaryCount);
    this.appendFormValue(formData, 'estimatedCost', this.totalCost || Number(this.form.estimatedCost) || '');
    this.appendFormValue(formData, 'communityContribution', Number(this.form.communityContribution) || 0);
    this.appendFormValue(formData, 'justification', this.form.justification?.trim() || '');
    this.appendFormValue(formData, 'itemsJson', JSON.stringify(items));

    this.documents.forEach(doc => {
      if (doc.file) {
        formData.append(`documents_${doc.type}`, doc.file, doc.fileName || doc.file.name);
      }
    });

    this.loading = true;
    this.schemeService.createApplication(formData).subscribe({
      next: application => {
        this.loading = false;
        this.submitted = true;
        this.submittedApplicationId = application.id ? `SC-${application.id}` : '';
        this.successMsg = `Scheme application submitted successfully${this.submittedApplicationId ? ` (${this.submittedApplicationId})` : ''}.`;
      },
      error: err => {
        this.loading = false;
        this.errorMsg = apiErrorMessage(err, 'Scheme application submission failed. Please try again.');
      }
    });
  }

  private appendFormValue(formData: FormData, key: string, value: string | number | boolean | null | undefined) {
    if (value === null || value === undefined) {
      return;
    }
    formData.append(key, String(value));
  }

  private normalizeSchemeType(value: string): string {
    return (value || '')
      .trim()
      .toUpperCase()
      .replace(/&/g, 'AND')
      .replace(/\s+/g, '_')
      .replace(/-+/g, '_')
      .replace(/_+/g, '_')
      .replace(/^CMCARE$/, 'CM_CARE');
  }

  private resolveApplicantId(): number | null {
    const routeVisitorId = this.toPositiveNumber(this.route.snapshot.queryParamMap.get('visitorId'));
    const userVisitorId = this.auth.user()?.visitorId ?? null;
    const sessionVisitorId = this.toPositiveNumber(sessionStorage.getItem('megha_visitor_id'));
    return routeVisitorId || userVisitorId || sessionVisitorId;
  }

  private toPositiveNumber(value: string | number | null | undefined): number | null {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }
}
