import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';

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
    MatDividerModule, 
    MatIconModule
  ],
  templateUrl: './scheme-form.component.html',
  styleUrls: ['./scheme-form.component.scss'],
})
export class SchemeFormComponent {
  step = 0;
  steps = [{ label: 'Scheme & Applicant' }, { label: 'Project Details' }, { label: 'Financial' }, { label: 'Documents' }, { label: 'Submit' }];

  form: any = {
    schemeType: '', projectName: '', projectCategory: '', beneficiaryType: '',
    beneficiaryCount: '', estimatedCost: 0, communityContribution: 0,
    justification: '', mlaMdcApproved: false, isReminder: false,
    items: [{ description: '', quantity: 1, unitCost: 0 }],
  };

  schemeTypes = [
    { label: 'CMSDF – CM Special Development Fund', value: 'CMSDF' },
    { label: 'CMSG – CM Special Grant', value: 'CMSG' },
    { label: 'CM Care – Medical Assistance', value: 'CM_CARE' },
    { label: 'CM Connect – Connectivity', value: 'CM_CONNECT' },
    { label: 'CM Elevate – Youth Employment', value: 'CM_ELEVATE' },
    { label: 'Focus+ – Focused Development', value: 'FOCUS_PLUS' },
    { label: 'Others', value: 'OTHERS' },
  ];
  projectCategories = ['Electricity','Road','House','School','Community hall','Retaining wall','Office','Travel','Medical','Musical instrument','Sports Equipment','Buses','Pickup Van','Computer lab upgradation','Repair','Others'];
  beneficiaryTypes = ['Individual','Community/Society','School/Youth Organisation','All of the above','Others'];
  beneficiaryCounts = ['1 TO 100','101 TO 500','501 TO 1000','Above 1000'];

  addItem() { this.form.items.push({ description: '', quantity: 1, unitCost: 0 }); }
  removeItem(i: number) { if (this.form.items.length > 1) this.form.items.splice(i, 1); }
  get totalCost() { return this.form.items.reduce((sum: number, it: any) => sum + (it.quantity * it.unitCost), 0); }
  nextStep() { if (this.step < this.steps.length - 1) this.step++; }
  prevStep() { if (this.step > 0) this.step--; }
  submit() { alert('✅ Scheme application submitted!\nApplication ID: SC-2024-' + Math.floor(Math.random()*9000+1000)); }
}
