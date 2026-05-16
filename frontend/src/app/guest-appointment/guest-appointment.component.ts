import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule, provideNativeDateAdapter } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { AppointmentService, GuestAppointmentRequest } from '../services/appointment.service';
import { ReferenceDataDto, ReferenceDataService } from '../services/reference-data.service';
import { apiErrorMessage } from '../shared/api-error.util';

@Component({
  selector: 'app-guest-appointment',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './guest-appointment.component.html',
  styleUrls: ['./guest-appointment.component.scss'],
})
export class GuestAppointmentComponent implements OnInit {
  referredOffices: ReferenceDataDto[] = [];
  visitorCategories: ReferenceDataDto[] = [];
  submitting = false;
  errorMsg = '';
  referenceId = '';
  supportingDocument: File | null = null;
  preferredDate: Date | null = null;

  form: GuestAppointmentRequest = {
    fullName: '',
    mobileNumber: '',
    address: '',
    referredOffice: '',
    reasonForAppointment: '',
  };

  constructor(
    private appointmentService: AppointmentService,
    private referenceDataService: ReferenceDataService
  ) {}

  ngOnInit(): void {
    this.referenceDataService.getByType('GUEST_REFERRED_OFFICE').subscribe({
      next: values => this.referredOffices = values ?? [],
      error: error => this.errorMsg = apiErrorMessage(error, 'Unable to load referred offices.'),
    });
    this.referenceDataService.getByType('GUEST_VISITOR_CATEGORY').subscribe({
      next: values => this.visitorCategories = values ?? [],
      error: error => this.errorMsg = apiErrorMessage(error, 'Unable to load visitor categories.'),
    });
  }

  submit(): void {
    this.errorMsg = '';
    if (!this.isValid()) return;
    this.submitting = true;
    this.appointmentService.createGuestAppointment({
      ...this.form,
      preferredDate: this.preferredDate ? this.toDateParam(this.preferredDate) : undefined,
      supportingDocument: this.supportingDocument,
    }).subscribe({
      next: response => {
        this.referenceId = response.referenceId;
        this.submitting = false;
      },
      error: error => {
        this.errorMsg = apiErrorMessage(error, 'Unable to submit guest appointment.');
        this.submitting = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.supportingDocument = input.files?.[0] ?? null;
  }

  reset(): void {
    this.referenceId = '';
    this.supportingDocument = null;
    this.preferredDate = null;
    this.form = {
      fullName: '',
      mobileNumber: '',
      address: '',
      referredOffice: '',
      reasonForAppointment: '',
    };
  }

  private isValid(): boolean {
    if (!this.form.fullName.trim() || !this.form.mobileNumber.trim() || !this.form.address.trim()
      || !this.form.referredOffice || !this.form.reasonForAppointment.trim()) {
      this.errorMsg = 'Please fill all required fields.';
      return false;
    }
    if (!/^[6-9]\d{9}$/.test(this.form.mobileNumber.trim())) {
      this.errorMsg = 'Please enter a valid 10-digit mobile number.';
      return false;
    }
    if (this.form.reasonForAppointment.trim().length < 10) {
      this.errorMsg = 'Reason for appointment must be at least 10 characters.';
      return false;
    }
    return true;
  }

  private toDateParam(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }
}
