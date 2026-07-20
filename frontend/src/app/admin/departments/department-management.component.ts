import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { environment } from '../../../environments/environment';
import { apiErrorMessage } from '../../shared/api-error.util';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface Department {
  id?: number;
  departmentCode: string;
  departmentName: string;
  description?: string;
  contactEmail?: string;
  contactMobile?: string;
  address?: string;
  status: 'ACTIVE' | 'INACTIVE';
}

@Component({
  selector: 'app-department-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './department-management.component.html',
  styleUrls: ['./department-management.component.scss'],
})
export class DepartmentManagementComponent implements OnInit {
  departments: Department[] = [];
  displayedColumns = ['departmentCode', 'departmentName', 'contact', 'status', 'actions'];
  showForm = false;
  isEdit = false;
  isLoading = false;
  isSaving = false;
  successMsg = '';
  errorMsg = '';
  form: Department = this.emptyForm();

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadDepartments();
  }

  openNew(): void {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.showForm = true;
    this.errorMsg = '';
  }

  openEdit(department: Department): void {
    this.form = { ...department };
    this.isEdit = true;
    this.showForm = true;
    this.errorMsg = '';
  }

  closeForm(): void {
    this.showForm = false;
    this.errorMsg = '';
  }

  save(): void {
    this.errorMsg = '';
    const payload: Department = {
      ...this.form,
      departmentCode: this.form.departmentCode.trim().toUpperCase(),
      departmentName: this.form.departmentName.trim(),
      contactEmail: (this.form.contactEmail ?? '').trim() || undefined,
      contactMobile: (this.form.contactMobile ?? '').trim() || undefined,
      description: (this.form.description ?? '').trim() || undefined,
      address: (this.form.address ?? '').trim() || undefined,
      status: this.form.status || 'ACTIVE',
    };
    if (!payload.departmentCode || !payload.departmentName) {
      this.errorMsg = 'Department code and name are required.';
      return;
    }
    if (!/^[A-Z0-9_]+$/.test(payload.departmentCode)) {
      this.errorMsg = 'Department code must use uppercase letters, numbers, and underscores only.';
      return;
    }
    if (payload.contactMobile && !/^[0-9]{10}$/.test(payload.contactMobile)) {
      this.errorMsg = 'Contact mobile must be exactly 10 digits.';
      return;
    }

    this.isSaving = true;
    const request = this.isEdit && this.form.id
      ? this.http.put<ApiResponse<Department>>(`${environment.apiUrl}/departments/${this.form.id}`, payload)
      : this.http.post<ApiResponse<Department>>(`${environment.apiUrl}/departments`, payload);

    request.subscribe({
      next: res => this.afterMutation(res.message || 'Department saved successfully.'),
      error: error => this.errorMsg = apiErrorMessage(error, 'Unable to save department.'),
      complete: () => this.isSaving = false,
    });
  }

  toggleStatus(department: Department): void {
    if (!department.id) return;
    const action = department.status === 'ACTIVE' ? 'deactivate' : 'activate';
    this.http.patch<ApiResponse<Department>>(`${environment.apiUrl}/departments/${department.id}/${action}`, {}).subscribe({
      next: res => this.afterMutation(res.message || 'Department status updated.'),
      error: error => this.errorMsg = apiErrorMessage(error, 'Unable to update department status.'),
    });
  }

  private loadDepartments(): void {
    this.isLoading = true;
    this.http.get<ApiResponse<Department[]>>(`${environment.apiUrl}/departments`).subscribe({
      next: res => this.departments = res.data ?? [],
      error: error => this.errorMsg = apiErrorMessage(error, 'Unable to load departments.'),
      complete: () => this.isLoading = false,
    });
  }

  private afterMutation(message: string): void {
    this.successMsg = message;
    this.errorMsg = '';
    this.showForm = false;
    this.loadDepartments();
    setTimeout(() => this.successMsg = '', 3000);
  }

  private emptyForm(): Department {
    return {
      departmentCode: '',
      departmentName: '',
      description: '',
      contactEmail: '',
      contactMobile: '',
      address: '',
      status: 'ACTIVE',
    };
  }
}
