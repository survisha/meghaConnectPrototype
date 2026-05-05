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
  otp = '';
  errorMsg = '';
  successMsg = '';
  loading = false;

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
    if (!this.phoneNumber || this.phoneNumber.length !== 10 || !/^\d{10}$/.test(this.phoneNumber)) {
      this.errorMsg = this.translate.instant('ERROR_VALID_10_DIGIT_MOBILE');
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
          this.errorMsg = this.translate.instant('ACCOUNT_NOT_FOUND_REGISTER');
        }
      },
      error: () => {
        this.loading = false;
        this.errorMsg = this.translate.instant('ERROR_UNABLE_CHECK_MOBILE');
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
          this.successMsg = this.mockOtp
            ? this.translate.instant('OTP_SENT_TO_DEMO', { phone: this.phoneNumber, otp: this.mockOtp })
            : this.translate.instant('OTP_SENT_TO', { phone: this.phoneNumber });
          this.step = 'enter-otp';
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
          this.auth.setVisitorSession(this.phoneNumber, res.fullName, res.token, res.visitorId);
          this.router.navigate(['/visitor']);
        } else {
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
    this.successMsg = '';
    this.mockOtp = '';
  }

  goToRegister() {
    this.router.navigate(['/register-visitor']);
  }
}
