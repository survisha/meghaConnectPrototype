import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../services/auth.service';
import { apiErrorMessage } from '../../shared/api-error.util';
import { ToastService } from '../../shared/toast/toast.service';

@Component({
  selector: 'app-change-password', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss'],
})
export class ChangePasswordComponent {
  readonly form = new FormGroup({
    currentPassword: new FormControl('', { nonNullable: true, validators: Validators.required }),
    newPassword: new FormControl('', { nonNullable: true, validators: [Validators.required,
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{10,72}$/)] }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: Validators.required }),
  });
  loading = false;
  errorMsg = '';
  constructor(private readonly auth: AuthService, private readonly router: Router, private readonly toast: ToastService) {}
  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    if (value.newPassword !== value.confirmPassword) { this.errorMsg = 'Passwords do not match'; return; }
    this.loading = true; this.errorMsg = '';
    this.auth.changeTemporaryPassword(value.currentPassword, value.newPassword).subscribe({
      next: () => {
        this.toast.success('Password changed successfully. Please log in again.');
        this.router.navigate(['/login'], { queryParams: { passwordChanged: true } });
      },
      error: error => { this.loading = false; this.toast.error(apiErrorMessage(error, 'Unable to change password')); },
    });
  }
  logout(): void { this.auth.logout(); }
}
