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
import { MatRadioModule } from '@angular/material/radio';
import { AiChatbotComponent } from '../ai-chatbot/ai-chatbot.component';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';
import { apiErrorMessage } from '../shared/api-error.util';

type LoginStep = 'enter-mobile' | 'enter-otp';

interface GenerateOtpResponse {
  success: boolean;
  code?: string;
  otp?: string;
  message: string;
  requiresEpic?: boolean;
}

interface LoginRegistrationOption {
  visitorId: number;
  fullName: string;
  epicNumber: string;
  maskedEpicNumber: string;
  kycStatus?: string;
  district?: string;
  constituency?: string;
}

interface RegistrationSearchResponse {
  success: boolean;
  registered: boolean;
  registrationCount: number;
  requiresEpic: boolean;
  registrations: LoginRegistrationOption[];
  message: string;
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
    MatRadioModule,
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
  registrationOptions: LoginRegistrationOption[] = [];
  selectedVisitorId: number | null = null;

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

    if (this.requiresEpic && !this.selectedRegistration) {
      this.warningMsg = 'Please select your registration to continue.';
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
    const selectedEpic = this.selectedRegistration?.epicNumber?.trim().toUpperCase() || normalizedEpic;
    if (selectedEpic) {
      payload.epicNumber = selectedEpic;
      this.epicNumber = selectedEpic;
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
          this.loadRegistrationOptions();
        } else {
          this.errorMsg = res.message || this.translate.instant('ERROR_FAILED_SEND_OTP');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = apiErrorMessage(err, this.translate.instant('ERROR_FAILED_SEND_OTP_TRY'));
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
    const selectedEpic = this.selectedRegistration?.epicNumber?.trim().toUpperCase() || this.epicNumber.trim().toUpperCase();
    this.auth.validateOtp({
      phoneNumber: this.phoneNumber,
      epicNumber: selectedEpic || undefined,
      otp: this.otp,
    }).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          // Store visitor session using AuthService helper
          this.auth.setVisitorSession(this.phoneNumber, res.fullName || 'Visitor', res.token || '', res.visitorId);
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
        this.errorMsg = apiErrorMessage(err, this.translate.instant('ERROR_OTP_VERIFICATION_FAILED_TRY'));
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
    this.registrationOptions = [];
    this.selectedVisitorId = null;
    this.otp = '';
  }

  onEpicInput() {
    this.epicNumber = this.epicNumber.toUpperCase().replace(/[^A-Z0-9]/g, '');
    this.errorMsg = '';
    this.warningMsg = '';
    this.successMsg = '';
  }

  onRegistrationSelect(visitorId: number) {
    this.selectedVisitorId = visitorId;
    const selected = this.selectedRegistration;
    this.epicNumber = selected?.epicNumber?.trim().toUpperCase() || '';
    this.errorMsg = '';
    this.warningMsg = '';
    this.successMsg = '';
  }

  get selectedRegistration(): LoginRegistrationOption | undefined {
    return this.registrationOptions.find(option => option.visitorId === this.selectedVisitorId);
  }

  private loadRegistrationOptions() {
    if (!this.phoneNumber || this.phoneNumber.length !== 10) {
      return;
    }

    this.loading = true;
    this.http.post<RegistrationSearchResponse>(`${environment.apiUrl}/visitor/auth/search-registrations`, {
      phoneNumber: this.phoneNumber,
    }).subscribe({
      next: res => {
        this.loading = false;
        this.registrationOptions = res.registrations || [];
        if (this.registrationOptions.length === 1) {
          this.onRegistrationSelect(this.registrationOptions[0].visitorId);
        }
        if (!this.registrationOptions.length) {
          this.errorMsg = res.message || this.translate.instant('ACCOUNT_NOT_FOUND_REGISTER');
        }
      },
      error: err => {
        this.loading = false;
        this.errorMsg = apiErrorMessage(err, 'Unable to load registrations for this mobile number.');
      },
    });
  }

  goToRegister() {
    this.router.navigate(['/register-visitor']);
  }
}
