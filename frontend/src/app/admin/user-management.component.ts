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
import { MatMenuModule } from '@angular/material/menu';
import { MatSortModule, Sort } from '@angular/material/sort';

interface ManagedUser {
  id?: number;
  username: string;
  fullName: string;
  role: UserRole;
  password?: string;
  phoneNumber?: string;
  email?: string;
  department?: string;
  designation?: string;
  active?: boolean;
  locked?: boolean;
  offlineAccess?: boolean;
  lastLogin?: string;
  createdAt?: string;
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
    MatSelectModule, MatIconModule, MatPaginatorModule, MatTooltipModule, MatMenuModule, MatSortModule,
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
})
export class UserManagementComponent implements OnInit {
  users: ManagedUser[] = [];
  filteredUsers: ManagedUser[] = [];
  pagedUsers: ManagedUser[] = [];
  showDialog = false;
  isEdit = false;
  editTarget = '';
  successMsg = '';
  errorMsg = '';
  showPassword = false;

  roleOptions: { label: string; value: UserRole }[] = [];
  form: ManagedUser = this.emptyForm();
  displayedColumns: string[] = ['sno', 'fullName', 'username', 'phoneNumber', 'role', 'active', 'locked', 'createdAt', 'actions'];
  sortActive: keyof ManagedUser = 'fullName';
  sortDirection: 'asc' | 'desc' = 'asc';
  pageIndex = 0;
  pageSize = 10;
  pageSizeOptions = [5, 10, 20, 50];
  totalRecords = 0;
  isLoading = false;
  isSaving = false;

  filters = {
    search: '',
    role: '',
    active: '',
    locked: '',
    department: '',
  };

  constructor(public auth: AuthService, private http: HttpClient) {}

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
    return found ? found.label : this.toRoleLabel(role);
  }

  departments(): string[] {
    return Array.from(new Set(this.users.map(u => (u.department || u.designation || '').trim()).filter(Boolean))).sort();
  }

  openNew() {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.editTarget = '';
    this.errorMsg = '';
    this.showPassword = false;
    this.showDialog = true;
  }

  openEdit(u: ManagedUser) {
    this.form = { ...u, password: '' };
    this.isEdit = true;
    this.editTarget = u.username;
    this.errorMsg = '';
    this.showPassword = false;
    this.showDialog = true;
  }

  closeDialog() {
    this.showDialog = false;
    this.errorMsg = '';
    this.isSaving = false;
  }

  save() {
    this.successMsg = '';
    this.errorMsg = '';

    const username = this.form.username.trim();
    const fullName = this.form.fullName.trim();
    const password = (this.form.password ?? '').trim();
    const phoneNumber = (this.form.phoneNumber ?? '').trim();

    if (!fullName || !this.form.role || (!this.isEdit && !username)) {
      this.errorMsg = 'Full name, username, and role are required.';
      return;
    }
    if (!this.isEdit && !password) {
      this.errorMsg = 'Password is required for a new user.';
      return;
    }
    if (password && password.length < 6) {
      this.errorMsg = 'Password must be at least 6 characters.';
      return;
    }
    if (phoneNumber && !/^[0-9]{10}$/.test(phoneNumber)) {
      this.errorMsg = 'Mobile number must be exactly 10 digits.';
      return;
    }

    this.isSaving = true;
    if (this.isEdit && this.form.id) {
      const payload = {
        fullName,
        role: this.form.role,
        phoneNumber: phoneNumber || null,
        active: this.form.active !== false,
        locked: this.form.locked === true,
        offlineAccess: this.form.offlineAccess === true,
      };
      this.http.put<ApiResponse<ManagedUser>>(`${environment.apiUrl}/users/${this.form.id}`, payload).subscribe({
        next: res => this.afterMutation(res.message || 'User updated successfully.'),
        error: error => this.failMutation(this.extractApiErrorMessage(error, 'Failed to update user.')),
        complete: () => this.isSaving = false,
      });
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
    this.http.post<ApiResponse<ManagedUser>>(`${environment.apiUrl}/users`, payload).subscribe({
      next: res => this.afterMutation(res.message || 'User created successfully.'),
      error: error => this.failMutation(this.extractApiErrorMessage(error, 'Unable to create user.')),
      complete: () => this.isSaving = false,
    });
  }

  deleteUser(u: ManagedUser) {
    if (u.username === this.auth.user()?.username) {
      this.flashError('Cannot delete yourself.');
      return;
    }
    if (!u.id || !confirm('Are you sure you want to delete this user?')) return;
    this.http.delete(`${environment.apiUrl}/users/${u.id}`).subscribe({
      next: () => this.afterMutation('User deleted successfully.'),
      error: error => this.flashError(this.extractApiErrorMessage(error, 'Failed to delete user.')),
    });
  }

  toggleActive(u: ManagedUser) {
    if (u.username === this.auth.user()?.username && u.active) {
      this.flashError('Cannot deactivate yourself.');
      return;
    }
    const action = u.active ? 'deactivate' : 'activate';
    const success = u.active ? 'User deactivated successfully.' : 'User activated successfully.';
    const failure = u.active ? 'Failed to deactivate user.' : 'Failed to activate user.';
    if (u.active && !confirm('Are you sure you want to deactivate this user?')) return;
    this.http.patch<ApiResponse<ManagedUser>>(`${environment.apiUrl}/users/${u.id}/${action}`, {}).subscribe({
      next: () => this.afterMutation(success),
      error: error => this.flashError(this.extractApiErrorMessage(error, failure)),
    });
  }

  unlockUser(u: ManagedUser) {
    if (!u.id || !u.locked) return;
    this.http.patch<ApiResponse<ManagedUser>>(`${environment.apiUrl}/users/${u.id}/unlock`, {}).subscribe({
      next: () => this.afterMutation('User unlocked successfully.'),
      error: error => this.flashError(this.extractApiErrorMessage(error, 'Failed to unlock user.')),
    });
  }

  applyFilters(resetPage = true) {
    if (resetPage) this.pageIndex = 0;
    const search = this.filters.search.trim().toLowerCase();
    this.filteredUsers = this.users.filter(user => {
      const haystack = [
        user.fullName, user.username, user.phoneNumber, user.email,
      ].join(' ').toLowerCase();
      const active = user.active !== false;
      const locked = user.locked === true;
      const dept = (user.department || user.designation || '').trim();
      return (!search || haystack.includes(search))
        && (!this.filters.role || user.role === this.filters.role)
        && (!this.filters.active || String(active) === this.filters.active)
        && (!this.filters.locked || String(locked) === this.filters.locked)
        && (!this.filters.department || dept === this.filters.department);
    });
    this.totalRecords = this.filteredUsers.length;
    this.sortFilteredUsers();
    this.updatePage();
  }

  onSort(sort: Sort) {
    this.sortActive = (sort.active as keyof ManagedUser) || 'fullName';
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.pageIndex = 0;
    this.sortFilteredUsers();
    this.updatePage();
  }

  resetFilters() {
    this.filters = { search: '', role: '', active: '', locked: '', department: '' };
    this.applyFilters();
  }

  changePage(delta: number) {
    const maxPage = Math.max(0, Math.ceil(this.totalRecords / this.pageSize) - 1);
    this.pageIndex = Math.min(maxPage, Math.max(0, this.pageIndex + delta));
    this.updatePage();
  }

  changePageSize(size: number | string) {
    this.pageSize = Number(size);
    this.pageIndex = 0;
    this.updatePage();
  }

  pageNumber(): number {
    return this.totalRecords === 0 ? 0 : this.pageIndex + 1;
  }

  pageCount(): number {
    return Math.ceil(this.totalRecords / this.pageSize) || 0;
  }

  private loadRoles() {
    this.http.get<string[]>(`${environment.apiUrl}/roles`).subscribe({
      next: roles => {
        this.roleOptions = (roles ?? [])
          .map(role => this.normalizeRoleName(role))
          .filter(role => role && role !== 'PUBLIC' && role !== 'CITIZEN')
          .map(role => ({ label: this.toRoleLabel(role), value: role as UserRole }));
        if (!this.roleOptions.some(option => option.value === this.form.role)) {
          this.form.role = this.defaultRole();
        }
      },
      error: error => {
        this.errorMsg = this.extractApiErrorMessage(error, 'Unable to load roles.');
        this.roleOptions = [];
      },
    });
  }

  private loadUsers() {
    this.isLoading = true;
    this.http.get<ManagedUser[]>(`${environment.apiUrl}/users`).subscribe({
      next: users => {
        this.users = (users ?? [])
          .filter(user => user.role !== 'PUBLIC' && user.role !== 'CITIZEN')
          .map(user => ({
            ...user,
            role: this.normalizeRoleName(user.role) as UserRole,
            active: user.active !== false,
            locked: user.locked === true,
            password: '',
          }));
        this.applyFilters(false);
      },
      error: error => this.errorMsg = this.extractApiErrorMessage(error, 'Failed to load users.'),
      complete: () => this.isLoading = false,
    });
  }

  private sortFilteredUsers() {
    const direction = this.sortDirection === 'desc' ? -1 : 1;
    const active = this.sortActive;
    this.filteredUsers = [...this.filteredUsers].sort((left, right) => {
      const comparison = this.sortValue(left, active).localeCompare(
        this.sortValue(right, active),
        undefined,
        { numeric: true, sensitivity: 'base' },
      );
      return comparison * direction;
    });
  }

  private sortValue(user: ManagedUser, column: keyof ManagedUser): string {
    switch (column) {
      case 'fullName':
        return user.fullName || '';
      case 'username':
        return user.username || '';
      case 'phoneNumber':
        return user.phoneNumber || '';
      case 'role':
        return this.roleLabel(user.role);
      case 'active':
        return user.active !== false ? '1' : '0';
      case 'locked':
        return user.locked ? '1' : '0';
      case 'createdAt':
        return user.createdAt ? String(new Date(user.createdAt).getTime()) : '0';
      default:
        return String(user[column] ?? '');
    }
  }

  private updatePage() {
    const start = this.pageIndex * this.pageSize;
    this.pagedUsers = this.filteredUsers.slice(start, start + this.pageSize);
  }

  private afterMutation(message: string) {
    this.showDialog = false;
    this.successMsg = message;
    this.errorMsg = '';
    this.loadUsers();
    setTimeout(() => this.successMsg = '', 3000);
  }

  private failMutation(message: string) {
    this.errorMsg = message;
    this.isSaving = false;
  }

  private flashError(message: string) {
    this.successMsg = '';
    this.errorMsg = message;
    setTimeout(() => this.errorMsg = '', 3000);
  }

  private extractApiErrorMessage(error: any, fallbackMessage: string): string {
    return error?.error?.message
      || error?.message
      || error?.error?.errorMessage
      || error?.error?.details
      || fallbackMessage;
  }

  private emptyForm(): ManagedUser {
    return {
      username: '',
      fullName: '',
      role: this.defaultRole(),
      password: '',
      phoneNumber: '',
      active: true,
      locked: false,
      offlineAccess: false,
    };
  }

  private defaultRole(): UserRole {
    const options = this.roleOptions ?? [];
    return options.find(option => option.value === 'DATA_ENTRY_OPERATOR')?.value
      ?? options[0]?.value
      ?? 'DATA_ENTRY_OPERATOR';
  }

  private toRoleLabel(role: string): string {
    return role
      .toLowerCase()
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private normalizeRoleName(role: string): string {
    const normalized = (role ?? '').trim().toUpperCase();
    if (normalized === 'SAIDUL_OSD') return 'OSD';
    if (normalized === 'APPROVER_JT_SECY') return 'APPROVER';
    if (normalized === 'SECURITY_POLICE') return 'SECURITY';
    return normalized;
  }
}
