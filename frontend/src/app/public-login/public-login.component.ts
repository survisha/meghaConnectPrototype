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
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';

type LoginStep = 'enter-mobile' | 'enter-otp';

interface GenerateOtpResponse {
  success: boolean;
  code?: string;
  otp?: string;
  message: string;
  requiresEpic?: boolean;
}

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
    TranslateModule,
    LanguageSelectorComponent,
    AiChatbotComponent
  ],
  templateUrl: './public-login.component.html',
  styleUrls: ['./public-login.component.scss'],
})
export class PublicLoginComponent {
  step: LoginStep = 'enter-mobile';

  phoneNumber = '';
  epicNumber = '';
  otp = '';
  errorMsg = '';
  warningMsg = '';
  successMsg = '';
  loading = false;
  requiresEpic = false;

  /** OTP returned in mock response (for demo only – remove when SMS gateway is live) */
  mockOtp = '';

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private router: Router,
    private translate: TranslateService
  ) {}

  // ── Step 1: Check mobile ────────────────────────────────────────────────

  checkMobile() {
    this.errorMsg = '';
    this.successMsg = '';
    this.warningMsg = '';
    if (!this.phoneNumber || this.phoneNumber.length !== 10 || !/^\d{10}$/.test(this.phoneNumber)) {
      this.errorMsg = this.translate.instant('ERROR_VALID_10_DIGIT_MOBILE');
      return;
    }

    if (this.requiresEpic && !this.epicNumber.trim()) {
      this.warningMsg = this.translate.instant('MULTIPLE_REGISTRATIONS_EPIC_REQUIRED');
      return;
    }

    this.sendOtp();
  }

  // ── Step 1b: Generate OTP ───────────────────────────────────────────────

  sendOtp() {
    this.errorMsg = '';
    this.warningMsg = '';
    this.successMsg = '';
    this.loading = true;
    const payload: { phoneNumber: string; epicNumber?: string } = {
      phoneNumber: this.phoneNumber,
    };

    const normalizedEpic = this.epicNumber.trim().toUpperCase();
    if (normalizedEpic) {
      payload.epicNumber = normalizedEpic;
      this.epicNumber = normalizedEpic;
    }

    this.http.post<GenerateOtpResponse>(`${environment.apiUrl}/visitor/auth/generate-otp`, payload).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.mockOtp = res.otp ?? '';  // demo only
          this.requiresEpic = false;
          this.successMsg = this.mockOtp
            ? this.translate.instant('OTP_SENT_TO_DEMO', { phone: this.phoneNumber, otp: this.mockOtp })
            : this.translate.instant('OTP_SENT_TO', { phone: this.phoneNumber });
          this.step = 'enter-otp';
        } else if (res.requiresEpic || res.code === 'MULTIPLE_REGISTRATIONS_FOUND') {
          this.requiresEpic = true;
          this.warningMsg = res.message || this.translate.instant('MULTIPLE_REGISTRATIONS_EPIC_REQUIRED');
        } else {
          this.errorMsg = res.message || this.translate.instant('ERROR_FAILED_SEND_OTP');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || this.translate.instant('ERROR_FAILED_SEND_OTP_TRY');
      },
    });
  }

  // ── Step 2: Validate OTP ────────────────────────────────────────────────

  validateOtp() {
    this.errorMsg = '';
    if (!this.otp || this.otp.length !== 6) {
      this.errorMsg = this.translate.instant('ERROR_VALID_6_DIGIT_OTP');
      return;
    }
    this.loading = true;
    this.http.post<{
      success: boolean;
      code?: string;
      token: string;
      fullName: string;
      visitorId: number;
      role: string;
      message: string;
      requiresEpic?: boolean;
    }>(`${environment.apiUrl}/visitor/auth/validate-otp`, {
      phoneNumber: this.phoneNumber,
      epicNumber: this.epicNumber.trim().toUpperCase() || undefined,
      otp: this.otp,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          // Store visitor session using AuthService helper
          this.auth.setVisitorSession(this.phoneNumber, res.fullName, res.token, res.visitorId);
          this.router.navigate(['/visitor']);
        } else {
          if (res.requiresEpic) {
            this.requiresEpic = true;
          }
          this.errorMsg = res.message || this.translate.instant('ERROR_OTP_VERIFICATION_FAILED');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = err?.error?.message || this.translate.instant('ERROR_OTP_VERIFICATION_FAILED_TRY');
      },
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────

  goBack() {
    this.step = 'enter-mobile';
    this.otp = '';
    this.errorMsg = '';
    this.warningMsg = '';
    this.successMsg = '';
    this.mockOtp = '';
  }

  onMobileInput() {
    this.phoneNumber = this.phoneNumber.replace(/\D/g, '');
    this.errorMsg = '';
    this.warningMsg = '';
    this.successMsg = '';
    this.requiresEpic = false;
    this.epicNumber = '';
    this.otp = '';
  }

  onEpicInput() {
    this.epicNumber = this.epicNumber.toUpperCase().replace(/[^A-Z0-9]/g, '');
    this.errorMsg = '';
    this.warningMsg = '';
    this.successMsg = '';
  }

  goToRegister() {
    this.router.navigate(['/register-visitor']);
  }
}
