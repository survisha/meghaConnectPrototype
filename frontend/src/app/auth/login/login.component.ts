import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, Button, InputText, Password],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  username = '';
  password = '';
  errorMsg = '';
  loading = false;
  isPublicMode = false;
  otpSent = false;
  otp = '';

  demoCredentials = [
    { label: 'HCM (hcm / hcm123)', value: { u: 'hcm', p: 'hcm123' } },
    { label: 'Admin (admin / admin123)', value: { u: 'admin', p: 'admin123' } },
    { label: 'Saidul OSD (saidul / osd123)', value: { u: 'saidul', p: 'osd123' } },
    { label: 'Jt Secretary (jtsecy / jts123)', value: { u: 'jtsecy', p: 'jts123' } },
    { label: 'CMO Officer (cmo / cmo123)', value: { u: 'cmo', p: 'cmo123' } },
    { label: 'DEO (deo1 / deo123)', value: { u: 'deo1', p: 'deo123' } },
  ];

  constructor(private auth: AuthService, private router: Router) {
    if (auth.isLoggedIn()) router.navigate(['/dashboard']);
  }

  selectDemo(cred: { u: string; p: string }) {
    this.username = cred.u;
    this.password = cred.p;
  }

  login() {
    this.loading = true; this.errorMsg = '';
    this.auth.login(this.username, this.password).subscribe(success => {
      if (success) {
        this.router.navigate(['/dashboard']);
      } else {
        this.errorMsg = 'Invalid username or password.';
      }
      this.loading = false;
    });
  }

  publicLogin() {
    this.loading = true;
    this.auth.login('public1', 'public123').subscribe(success => {
      if (success) {
        this.router.navigate(['/visitor']);
      } else {
        this.errorMsg = 'OTP verification failed.';
      }
      this.loading = false;
    });
  }
}
