import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReferenceDataDto, ReferenceDataService } from '../services/reference-data.service';
import { DepartmentAccessRequestService } from '../services/department-access-request.service';
import { ToastService } from '../shared/toast/toast.service';
import { apiErrorMessage } from '../shared/api-error.util';

@Component({
  selector: 'app-department-access-request-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './department-access-request-form.component.html',
  styleUrls: ['./department-access-request-form.component.scss']
})
export class DepartmentAccessRequestFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  departments: ReferenceDataDto[] = [];
  loadingDepartments = false;
  submitting = false;
  referenceLoadFailed = false;

  readonly form = this.fb.nonNullable.group({
    departmentCode: ['', Validators.required],
    nodalOfficerName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150), Validators.pattern(/^[A-Za-zÀ-ÖØ-öø-ÿ .'-]+$/)]],
    officialEmail: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    officialMobile: ['', [Validators.required, Validators.pattern(/^[6-9][0-9]{9}$/)]],
    requestPurpose: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
    expectedUserCount: [1, [Validators.required, Validators.min(1), Validators.max(10000), Validators.pattern(/^\d+$/)]],
    remarks: ['', Validators.maxLength(1000)]
  });

  constructor(
    private readonly references: ReferenceDataService,
    private readonly requests: DepartmentAccessRequestService,
    private readonly toast: ToastService,
    private readonly router: Router
  ) {}

  ngOnInit(): void { this.loadDepartments(); }

  loadDepartments(): void {
    if (this.loadingDepartments) return;
    this.loadingDepartments = true;
    this.referenceLoadFailed = false;
    this.references.getByType('DEPARTMENT').pipe(finalize(() => this.loadingDepartments = false)).subscribe({
      next: rows => this.departments = rows,
      error: error => {
        this.referenceLoadFailed = true;
        this.toast.error(apiErrorMessage(error, 'Unable to load departments. Please try again.'));
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.warning('Please correct the highlighted fields.');
      return;
    }
    if (this.submitting) return;
    this.submitting = true;
    const value = this.form.getRawValue();
    this.requests.submit({ ...value, remarks: value.remarks.trim() || undefined })
      .pipe(finalize(() => this.submitting = false)).subscribe({
        next: () => {
          this.toast.success('Your application access request has been submitted successfully.');
          this.router.navigate(['/']);
        },
        error: error => {
          if (error?.status === 409) {
            this.toast.warning('A request for the selected department is already pending.');
          } else {
            this.toast.error(apiErrorMessage(error, 'Unable to submit the request. Please try again.'));
          }
        }
      });
  }

  invalid(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }
}
