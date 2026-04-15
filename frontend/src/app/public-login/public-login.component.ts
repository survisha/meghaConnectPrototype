import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AiChatbotComponent } from '../ai-chatbot/ai-chatbot.component';

type LoginStep = 'enter-mobile' | 'enter-otp';

@Component({
  selector: 'app-public-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    AiChatbotComponent
  ],
  templateUrl: './public-login.component.html',
  styleUrls: ['./public-login.component.scss'],
})
export class PublicLoginComponent {
  step: LoginStep = 'enter-mobile';

  phoneNumber = '';
  otp = '';
  errorMsg = '';
  successMsg = '';
  loading = false;

  /** OTP returned in mock response (for demo only – remove when SMS gateway is live) */
  mockOtp = '';

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private router: Router
  ) {}

  // ── Step 1: Check mobile ────────────────────────────────────────────────

  checkMobile() {
    this.errorMsg = '';
    if (!this.phoneNumber || this.phoneNumber.length !== 10 || !/^\d{10}$/.test(this.phoneNumber)) {
      this.errorMsg = 'Please enter a valid 10-digit mobile number.';
      return;
    }
    this.loading = true;
    this.http.post<{ registered: boolean; message: string }>(`${environment.apiUrl}/visitor/auth/check-mobile`, {
      phoneNumber: this.phoneNumber,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.registered) {
          this.sendOtp();
        } else {
          this.errorMsg = 'Account not found. Please register as a new visitor.';
        }
      },
      error: () => {
        this.loading = false;
        this.errorMsg = 'Unable to check mobile. Please try again.';
      },
    });
  }

  // ── Step 1b: Generate OTP ───────────────────────────────────────────────

  sendOtp() {
    this.errorMsg = '';
    this.loading = true;
    this.http.post<{ success: boolean; otp?: string; message: string }>(`${environment.apiUrl}/visitor/auth/generate-otp`, {
      phoneNumber: this.phoneNumber,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.mockOtp = res.otp ?? '';  // demo only
          this.successMsg = `OTP sent to ${this.phoneNumber}` + (this.mockOtp ? ` (demo OTP: ${this.mockOtp})` : '');
          this.step = 'enter-otp';
        } else {
          this.errorMsg = res.message || 'Failed to send OTP.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Failed to send OTP. Please try again.';
      },
    });
  }

  // ── Step 2: Validate OTP ────────────────────────────────────────────────

  validateOtp() {
    this.errorMsg = '';
    if (!this.otp || this.otp.length !== 6) {
      this.errorMsg = 'Please enter the 6-digit OTP.';
      return;
    }
    this.loading = true;
    this.http.post<{
      success: boolean;
      token: string;
      fullName: string;
      visitorId: number;
      role: string;
      message: string;
    }>(`${environment.apiUrl}/visitor/auth/validate-otp`, {
      phoneNumber: this.phoneNumber,
      otp: this.otp,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          // Store visitor session using AuthService helper
          this.auth.setVisitorSession(this.phoneNumber, res.fullName, res.token);
          sessionStorage.setItem('megha_visitor_id', String(res.visitorId));
          this.router.navigate(['/visitor']);
        } else {
          this.errorMsg = res.message || 'OTP verification failed.';
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'OTP verification failed. Please try again.';
      },
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────

  goBack() {
    this.step = 'enter-mobile';
    this.otp = '';
    this.errorMsg = '';
    this.successMsg = '';
    this.mockOtp = '';
  }

  goToRegister() {
    this.router.navigate(['/register-visitor']);
  }
}
