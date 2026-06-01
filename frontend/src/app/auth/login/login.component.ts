import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { apiErrorMessage } from '../../shared/api-error.util';
import { BrandLogoComponent } from '../../shared/brand-logo/brand-logo.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
    BrandLogoComponent
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  @Input() embedded = false;

  username = '';
  password = '';
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
    private router: Router,
    private translate: TranslateService
  ) {
    if (auth.isLoggedIn()) router.navigate(['/dashboard']);
  }

  selectDemo(cred: { u: string; p: string }) {
    this.username = cred.u;
    this.password = cred.p;
  }

  login() {
    this.loading = true; this.errorMsg = '';
    this.auth.login(this.username, this.password).subscribe({
      next: success => {
        if (success) {
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMsg = this.translate.instant('ERROR_INVALID_USERNAME_PASSWORD');
        }
        this.loading = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, this.translate.instant('ERROR_INVALID_USERNAME_PASSWORD'));
        this.loading = false;
      }
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
