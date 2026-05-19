import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models';
import { environment } from '../../environments/environment';

import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';

interface ManagedUser {
  id?: number;
  username: string;
  fullName: string;
  role: UserRole;
  password?: string;
  phoneNumber?: string;
  active?: boolean;
  offlineAccess?: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatButtonModule, MatTableModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatIconModule, MatPaginatorModule, MatTooltipModule,
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
})
export class UserManagementComponent implements OnInit {
  users: ManagedUser[];
  showDialog = false;
  isEdit = false;
  editTarget = '';
  successMsg = '';
  errorMsg = '';
  showPassword = false;

  form: ManagedUser = { username: '', fullName: '', role: 'DATA_ENTRY_OPERATOR', password: '', phoneNumber: '' };
  displayedColumns: string[] = ['fullName', 'username', 'phoneNumber', 'role', 'actions'];
  pageSize = 10;
  pageSizeOptions = [5, 10, 20];
  isLoading = false;
  isSaving = false;

  roleOptions: { label: string; value: UserRole }[] = [];

  constructor(public auth: AuthService, private http: HttpClient) {
    this.users = [];
  }

  ngOnInit(): void {
    this.loadRoles();
    this.loadUsers();
  }

  roleBadge(role: UserRole): { [klass: string]: any } {
    const map: Record<string, { [klass: string]: any }> = {
      HCM: { 'background': '#fee2e2', 'color': '#991b1b' },
      ADMIN: { 'background': '#fef3c7', 'color': '#92400e' },
      OSD: { 'background': '#fef3c7', 'color': '#92400e' },
      APPROVER: { 'background': '#dbeafe', 'color': '#1e40af' },
      CMO_OFFICER: { 'background': '#dbeafe', 'color': '#1e40af' },
      CMO: { 'background': '#dbeafe', 'color': '#1e40af' },
      DATA_ENTRY_OPERATOR: { 'background': '#f3f4f6', 'color': '#374151' },
      SECURITY: { 'background': '#ecfdf5', 'color': '#047857' },
      CITIZEN: { 'background': '#eef2ff', 'color': '#3730a3' },
      PUBLIC: { 'background': '#eef2ff', 'color': '#3730a3' },
    };
    return map[role] ?? { 'background': '#f3f4f6', 'color': '#374151' };
  }

  roleLabel(role: UserRole): string {
    const found = this.roleOptions.find(r => r.value === role);
    return found ? found.label : role;
  }

  openNew() {
    this.form = { username: '', fullName: '', role: this.defaultRole(), password: '', phoneNumber: '' };
    this.isEdit = false;
    this.editTarget = '';
    this.errorMsg = '';
    this.showDialog = true;
  }

  openEdit(u: ManagedUser) {
    this.form = { ...u };
    this.isEdit = true;
    this.editTarget = u.username;
    this.errorMsg = '';
    this.showDialog = true;
  }

  save() {
    this.successMsg = '';
    this.errorMsg = '';

    if (this.isEdit) {
      this.errorMsg = 'User update is not available yet.';
      return;
    }

    const username = this.form.username.trim();
    const fullName = this.form.fullName.trim();
    const password = (this.form.password ?? '').trim();
    const phoneNumber = (this.form.phoneNumber ?? '').trim();

    if (!username || !fullName || !this.form.role || !password) {
      this.errorMsg = 'Full name, username, role, and password are required.';
      return;
    }

    if (password.length < 6) {
      this.errorMsg = 'Password must be at least 6 characters.';
      return;
    }

    if (phoneNumber && !/^[0-9]{10}$/.test(phoneNumber)) {
      this.errorMsg = 'Mobile number must be exactly 10 digits.';
      return;
    }

    const payload = {
      username,
      fullName,
      role: this.form.role,
      password,
      phoneNumber: phoneNumber || null,
      active: true,
      offlineAccess: false,
    };

    this.isSaving = true;
    this.http.post<ApiResponse<ManagedUser>>(`${environment.apiUrl}/users`, payload).subscribe({
      next: res => {
        this.showDialog = false;
        this.successMsg = res.message || 'User created successfully.';
        this.loadUsers();
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: err => {
        this.errorMsg = err?.message || 'Unable to create user.';
        this.isSaving = false;
      },
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  deleteUser(u: ManagedUser) {
    if (u.username === this.auth.user()?.username) { this.successMsg = ''; this.errorMsg = 'Cannot delete yourself.'; setTimeout(() => this.errorMsg = '', 3000); return; }
    this.successMsg = '';
    this.errorMsg = 'User delete is not available yet.';
    setTimeout(() => this.errorMsg = '', 3000);
  }

  private loadRoles() {
    this.http.get<string[]>(`${environment.apiUrl}/roles`).subscribe({
      next: roles => {
        this.roleOptions = (roles ?? [])
          .filter(role => role && role !== 'PUBLIC' && role !== 'CITIZEN')
          .map(role => ({ label: this.toRoleLabel(role), value: role as UserRole }));
        if (!this.roleOptions.some(option => option.value === this.form.role)) {
          this.form.role = this.defaultRole();
        }
      },
      error: err => {
        this.errorMsg = err?.message || 'Unable to load roles.';
      },
    });
  }

  private loadUsers() {
    this.isLoading = true;
    this.http.get<ManagedUser[]>(`${environment.apiUrl}/users`).subscribe({
      next: users => {
        this.users = (users ?? [])
          .filter(user => user.role !== 'PUBLIC' && user.role !== 'CITIZEN')
          .map(user => ({ ...user, password: '' }));
      },
      error: err => {
        this.errorMsg = err?.message || 'Unable to load users.';
      },
      complete: () => {
        this.isLoading = false;
      },
    });
  }

  private defaultRole(): UserRole {
    return this.roleOptions.find(option => option.value === 'DATA_ENTRY_OPERATOR')?.value
      ?? this.roleOptions[0]?.value
      ?? 'DATA_ENTRY_OPERATOR';
  }

  private toRoleLabel(role: string): string {
    return role
      .toLowerCase()
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}
