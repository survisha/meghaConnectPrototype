import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { Steps } from 'primeng/steps';
import { FileUpload } from 'primeng/fileupload';
import { Checkbox } from 'primeng/checkbox';
import { RadioButton } from 'primeng/radiobutton';
import { Divider } from 'primeng/divider';
import { Message } from 'primeng/message';

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Button, InputText, Select, Textarea, Steps, FileUpload, Checkbox, RadioButton, Divider, Message],
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.scss'],
})
export class AppointmentFormComponent {
  step = 0;
  steps = [
    { label: 'Personal Info' }, { label: 'Agenda' },
    { label: 'Scheme Details' }, { label: 'Documents' }, { label: 'Review' }
  ];

  form = {
    fullName: '', phoneNumber: '', epicNumber: '', designation: '',
    district: '', constituency: '', booth: '', address: '',
    agendaType: '', requestedLocation: '', lastMeetingDate: '',
    agendaBrief: '', schemeType: '', projectName: '', projectCategory: '',
    beneficiaryType: '', beneficiaryCount: '', estimatedCost: '',
    communityContribution: '', justification: '', mlaMdcApproved: false,
    isNewApplication: true,
  };

  designations = ['Govt Servant','Retd Govt Servant','Teacher','Political Leader','Students','Religious Leader','Businessman','Media','General Public','Organisation – Village authority','Teachers body','Civil Society/NGO'];
  districts = ['East Khasi Hills','West Khasi Hills','South West Khasi Hills','Ri Bhoi','East Jaintia Hills','West Jaintia Hills','East Garo Hills','West Garo Hills','South Garo Hills','North Garo Hills','Eastern West Khasi Hills'];
  agendaTypes = ['Scheme availment (CM)','Governance','Trade & Commerce','Political Discussion','Public Grievance'];
  schemeTypes = ['CMSDF','CMSG','CM Care','CM Connect','CM Elevate','Focus+','Others'];
  locations = ['Shillong','Tura','Delhi','Others'];
  projectCategories = ['Electricity','Road','House','School','Community hall','Retaining wall','Office','Travel','Medical','Musical instrument','Sports Equipment','Buses','Pickup Van','Computer lab upgradation','Repair','Others'];
  beneficiaryTypes = ['Individual','Community/Society','School/Youth Organisation','All of the above','Others'];
  beneficiaryCounts = ['1 TO 100','101 TO 500','501 TO 1000','Above 1000'];

  get isScheme() { return this.form.agendaType === 'Scheme availment (CM)'; }

  nextStep() { if (this.step < this.steps.length - 1) this.step++; }
  prevStep() { if (this.step > 0) this.step--; }
  submit() { alert('✅ Appointment request submitted!\nApplication ID: MC-2024-000' + Math.floor(Math.random()*99+5)); }
}
