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
  get totalCost() { return this.form.items.reduce((sum: number, it: any) => sum + (it.quantity * it.unitCost), 0); }

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

    this.loading = true;
    this.schemeService.createApplication({
      applicantId,
      schemeType: this.form.schemeType,
      projectName: this.form.projectName.trim(),
      projectCategory: this.form.projectCategory || undefined,
      beneficiaryType: this.form.beneficiaryType || undefined,
      beneficiaryCount: this.form.beneficiaryCount || undefined,
      estimatedCost: this.totalCost || Number(this.form.estimatedCost) || undefined,
      communityContribution: Number(this.form.communityContribution) || 0,
      justification: this.form.justification?.trim() || undefined,
      items,
    }).subscribe({
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
