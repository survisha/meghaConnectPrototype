import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models';

import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';

interface ManagedUser {
  username: string;
  fullName: string;
  role: UserRole;
  password: string;
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
export class UserManagementComponent {
  users: ManagedUser[];
  showDialog = false;
  isEdit = false;
  editTarget = '';
  successMsg = '';
  errorMsg = '';
  showPassword = false;

  form: ManagedUser = { username: '', fullName: '', role: 'DATA_ENTRY_OPERATOR', password: '' };
  displayedColumns: string[] = ['fullName', 'username', 'role', 'actions'];
  pageSize = 10;
  pageSizeOptions = [5, 10, 20];

  roleOptions: { label: string; value: UserRole }[] = [
    { label: 'HCM', value: 'HCM' },
    { label: 'Admin', value: 'ADMIN' },
    { label: 'OSD', value: 'OSD' },
    { label: 'Approver', value: 'APPROVER' },
    { label: 'CMO Officer', value: 'CMO_OFFICER' },
    { label: 'Data Entry Operator', value: 'DATA_ENTRY_OPERATOR' },
  ];

  constructor(public auth: AuthService, public dialog: MatDialog) {
    this.users = this.auth.DEMO_USERS
      .filter((u: any) => u.role !== 'PUBLIC')
      .map((u: any) => ({ username: u.username, fullName: u.fullName, role: u.role, password: u.password }));
  }

  roleBadge(role: UserRole): { [klass: string]: any } {
    const map: Record<string, { [klass: string]: any }> = {
      HCM: { 'background': '#fee2e2', 'color': '#991b1b' },
      ADMIN: { 'background': '#fef3c7', 'color': '#92400e' },
      OSD: { 'background': '#fef3c7', 'color': '#92400e' },
      APPROVER: { 'background': '#dbeafe', 'color': '#1e40af' },
      CMO_OFFICER: { 'background': '#dbeafe', 'color': '#1e40af' },
      DATA_ENTRY_OPERATOR: { 'background': '#f3f4f6', 'color': '#374151' },
    };
    return map[role] ?? { 'background': '#f3f4f6', 'color': '#374151' };
  }

  roleLabel(role: UserRole): string {
    const found = this.roleOptions.find(r => r.value === role);
    return found ? found.label : role;
  }

  openNew() {
    this.form = { username: '', fullName: '', role: 'DATA_ENTRY_OPERATOR', password: '' };
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
    if (!this.form.username || !this.form.fullName || !this.form.password) {
      this.errorMsg = 'All fields are required.';
      return;
    }
    if (!this.isEdit && this.users.some(u => u.username === this.form.username)) {
      this.errorMsg = 'Username already exists.';
      return;
    }
    if (this.isEdit) {
      const idx = this.users.findIndex(u => u.username === this.editTarget);
      if (idx !== -1) this.users[idx] = { ...this.form };
      const demoIdx = this.auth.DEMO_USERS.findIndex((u: any) => u.username === this.editTarget);
      if (demoIdx !== -1) {
        this.auth.DEMO_USERS[demoIdx].password = this.form.password;
        this.auth.DEMO_USERS[demoIdx].fullName = this.form.fullName;
        this.auth.DEMO_USERS[demoIdx].role = this.form.role;
      }
    } else {
      this.users.push({ ...this.form });
      this.auth.DEMO_USERS.push({ username: this.form.username, password: this.form.password, fullName: this.form.fullName, role: this.form.role });
    }
    this.showDialog = false;
    this.successMsg = this.isEdit ? 'User updated successfully.' : 'User created successfully.';
    setTimeout(() => this.successMsg = '', 3000);
  }

  deleteUser(u: ManagedUser) {
    if (u.username === this.auth.user()?.username) { this.successMsg = ''; this.errorMsg = 'Cannot delete yourself.'; setTimeout(() => this.errorMsg = '', 3000); return; }
    this.users = this.users.filter(x => x.username !== u.username);
    const idx = this.auth.DEMO_USERS.findIndex((d: any) => d.username === u.username);
    if (idx !== -1) this.auth.DEMO_USERS.splice(idx, 1);
    this.successMsg = 'User deleted.';
    setTimeout(() => this.successMsg = '', 3000);
  }
}
