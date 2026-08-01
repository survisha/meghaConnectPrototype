import { Component, ElementRef, Input, OnInit, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { apiErrorMessage } from '../../shared/api-error.util';
import { BrandLogoComponent } from '../../shared/brand-logo/brand-logo.component';
import { CaptchaService } from '../../services/captcha.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    TranslateModule,
    BrandLogoComponent
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  @Input() embedded = false;
  @ViewChild('captchaInput') captchaInput?: ElementRef<HTMLInputElement>;

  readonly loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: Validators.required }),
    password: new FormControl('', { nonNullable: true, validators: Validators.required }),
    captchaId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    captchaValue: new FormControl('', { nonNullable: true, validators: Validators.required }),
  });
  readonly captchaImageSrc = signal('');
  readonly captchaDisplayText = signal('');
  readonly captchaLoading = signal(false);
  errorMsg = '';
  loading = false;
  isPublicMode = true;
  otpSent = false;
  otp = '';

  demoCredentials = [
    { label: 'HCM (hcm / hcm123)', value: { u: 'hcm', p: 'hcm123' } },
    { label: 'Admin (admin / admin123)', value: { u: 'admin', p: 'admin123' } },
    { label: 'OSD (saidul / osd123)', value: { u: 'saidul', p: 'osd123' } },
    { label: 'Jt Secretary (jtsecy / jts123)', value: { u: 'jtsecy', p: 'jts123' } },
    { label: 'CMO Officer (cmo / cmo123)', value: { u: 'cmo', p: 'cmo123' } },
    { label: 'DEO (deo1 / deo123)', value: { u: 'deo1', p: 'deo123' } },
  ];

  constructor(
    private auth: AuthService,
    private captchaService: CaptchaService,
    private router: Router,
    private translate: TranslateService
  ) {
    if (auth.isLoggedIn()) router.navigate([auth.user()?.passwordChangeRequired ? '/change-password' : '/dashboard']);
  }

  ngOnInit(): void {
    this.refreshCaptcha(false);
  }

  selectDemo(cred: { u: string; p: string }) {
    this.loginForm.patchValue({ username: cred.u, password: cred.p });
  }

  login() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      if (this.loginForm.controls.captchaValue.hasError('required')) this.errorMsg = 'Captcha is required';
      return;
    }
    this.loading = true; this.errorMsg = '';
    const value = this.loginForm.getRawValue();
    this.auth.login(value.username, value.password, value.captchaId, value.captchaValue).subscribe({
      next: success => {
        if (success) {
          this.router.navigate([this.auth.user()?.passwordChangeRequired ? '/change-password' : '/dashboard']);
        } else {
          this.errorMsg = this.translate.instant('ERROR_INVALID_USERNAME_PASSWORD');
        }
        this.loading = false;
      },
      error: err => {
        const code = err?.error?.errorCode;
        this.errorMsg = code === 'CAPTCHA_EXPIRED' ? 'Captcha expired'
          : code === 'INVALID_CAPTCHA' ? 'Invalid captcha'
          : apiErrorMessage(err, this.translate.instant('ERROR_INVALID_USERNAME_PASSWORD'));
        this.loading = false;
        if (code === 'CAPTCHA_EXPIRED' || code === 'INVALID_CAPTCHA') this.refreshCaptcha();
      }
    });
  }

  refreshCaptcha(focus = true): void {
    if (this.captchaLoading()) return;
    this.captchaLoading.set(true);
    this.loginForm.patchValue({ captchaId: '', captchaValue: '' });
    this.captchaService.generate().subscribe({
      next: captcha => {
        this.loginForm.controls.captchaId.setValue(captcha.captchaId);
        this.captchaImageSrc.set(captcha.captchaImage ? `data:image/png;base64,${captcha.captchaImage}` : '');
        this.captchaDisplayText.set(captcha.captchaText ?? '');
        this.captchaLoading.set(false);
        if (focus) setTimeout(() => this.captchaInput?.nativeElement.focus());
      },
      error: () => {
        this.captchaLoading.set(false);
        this.errorMsg = 'Unable to load captcha';
      },
    });
  }

  publicLogin() {
    this.loading = true;
    this.auth.login('public1', 'public123').subscribe({
      next: success => {
        if (success) {
          this.router.navigate(['/visitor']);
        } else {
          this.errorMsg = this.translate.instant('ERROR_OTP_VERIFICATION_FAILED');
        }
        this.loading = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, this.translate.instant('ERROR_OTP_VERIFICATION_FAILED'));
        this.loading = false;
      }
    });
  }
}
