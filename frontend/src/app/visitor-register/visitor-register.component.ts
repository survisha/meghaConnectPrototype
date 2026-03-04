import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface VisitorRegistrationForm {
  fullName: string;
  phoneNumber: string;
  email: string;
  address: string;
  idType: 'EPIC' | 'AADHAAR' | '';
  epicNumber: string;
  aadhaarNumber: string;
  photoStoragePath: string;
}

@Component({
  selector: 'app-visitor-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './visitor-register.component.html',
  styleUrls: ['./visitor-register.component.scss'],
})
export class VisitorRegisterComponent {
  form: VisitorRegistrationForm = {
    fullName: '',
    phoneNumber: '',
    email: '',
    address: '',
    idType: '',
    epicNumber: '',
    aadhaarNumber: '',
    photoStoragePath: '',
  };

  errorMsg = '';
  successMsg = '';
  loading = false;
  submitted = false;

  constructor(private http: HttpClient, private router: Router) {}

  // ── Validate & Submit ───────────────────────────────────────────────────

  register() {
    this.errorMsg = '';
    if (!this.validate()) return;

    this.loading = true;
    const payload: Record<string, string> = {
      fullName:    this.form.fullName.trim(),
      phoneNumber: this.form.phoneNumber.trim(),
      email:       this.form.email.trim(),
      address:     this.form.address.trim(),
    };

    if (this.form.idType === 'EPIC') {
      payload['epicNumber'] = this.form.epicNumber.trim().toUpperCase();
    } else if (this.form.idType === 'AADHAAR') {
      payload['aadhaarNumber'] = this.form.aadhaarNumber.trim();
    }

    if (this.form.photoStoragePath) {
      payload['photoStoragePath'] = this.form.photoStoragePath;
    }

    this.http.post<{ success: boolean; message: string }>('/api/v1/visitor/auth/register', payload).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.submitted = true;
          this.successMsg = res.message || 'Registration successful!';
        } else {
          this.errorMsg = res.message || 'Registration failed. Please try again.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Registration failed. Please check your details and try again.';
      },
    });
  }

  private validate(): boolean {
    if (!this.form.fullName.trim()) {
      this.errorMsg = 'Full name is required.'; return false;
    }
    if (!this.form.phoneNumber || !/^\d{10}$/.test(this.form.phoneNumber)) {
      this.errorMsg = 'A valid 10-digit mobile number is required.'; return false;
    }
    if (this.form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) {
      this.errorMsg = 'Please enter a valid email address.'; return false;
    }
    if (this.form.idType === 'EPIC') {
      if (!this.form.epicNumber || !/^[A-Za-z]{3}[0-9]{7}$/.test(this.form.epicNumber)) {
        this.errorMsg = 'EPIC number must be 3 letters followed by 7 digits (e.g. ABC1234567).'; return false;
      }
    }
    if (this.form.idType === 'AADHAAR') {
      if (!this.form.aadhaarNumber || !/^\d{12}$/.test(this.form.aadhaarNumber)) {
        this.errorMsg = 'Aadhaar number must be exactly 12 digits.'; return false;
      }
    }
    return true;
  }

  goToLogin() {
    this.router.navigate(['/public-login']);
  }
}
