import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { apiErrorMessage } from '../../shared/api-error.util';

interface AppointmentType {
  id?: number;
  typeCode: string;
  typeName: string;
  description?: string;
  typeCategory: string;
  requiresTravel?: boolean;
  travelTimeBefore?: number;
  travelTimeAfter?: number;
  blockTimeIncludes?: boolean;
  hasAppointmentLimit?: boolean;
  maxAppointmentLimit?: number;
  limitIsSacrosanct?: boolean;
  generateAlerts?: boolean;
  noTravelTime?: boolean;
  isActive?: boolean;
  displayOrder?: number;
}

@Component({
  selector: 'app-appointment-type-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatCheckboxModule,
    MatDividerModule,
    MatTooltipModule,
    MatCardModule,
    MatExpansionModule
  ],
  templateUrl: './appointment-type-management.component.html',
  styleUrls: ['./appointment-type-management.component.scss']
})
export class AppointmentTypeManagementComponent implements OnInit {

  appointmentTypes: AppointmentType[] = [];
  displayedColumns: string[] = ['typeCode', 'typeName', 'typeCategory', 'requiresTravel', 'hasLimit', 'isActive', 'actions'];
  loading = false;
  selectedType: AppointmentType | null = null;
  showEditForm = false;
  editFormData: AppointmentType | null = null;
  errorMsg = '';

  categoryOptions = ['INDIVIDUAL', 'BATCH'];

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadAppointmentTypes();
  }

  loadAppointmentTypes() {
    this.loading = true;
    this.http.get<AppointmentType[]>(`${environment.apiUrl}/admin/appointment-types`)
      .subscribe({
        next: (data) => {
          this.appointmentTypes = data;
          this.errorMsg = '';
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading appointment types', err);
          this.errorMsg = apiErrorMessage(err, 'Unable to load appointment types.');
          this.loading = false;
        }
      });
  }

  editType(type: AppointmentType) {
    this.selectedType = type;
    this.editFormData = JSON.parse(JSON.stringify(type)); // Deep copy
    this.showEditForm = true;
  }

  saveChanges() {
    if (!this.editFormData || !this.selectedType) return;

    this.loading = true;
    this.http.put<AppointmentType>(
      `${environment.apiUrl}/admin/appointment-types/${this.selectedType.typeCode}`,
      this.editFormData
    ).subscribe({
      next: (updated) => {
        const index = this.appointmentTypes.findIndex(t => t.typeCode === updated.typeCode);
        if (index > -1) {
          this.appointmentTypes[index] = updated;
        }
        this.errorMsg = '';
        this.showEditForm = false;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error updating appointment type', err);
        this.errorMsg = apiErrorMessage(err, 'Unable to update appointment type.');
        this.loading = false;
      }
    });
  }

  toggleStatus(type: AppointmentType) {
    this.loading = true;
    this.http.patch<AppointmentType>(
      `${environment.apiUrl}/admin/appointment-types/${type.typeCode}/toggle`,
      {}
    ).subscribe({
      next: (updated) => {
        const index = this.appointmentTypes.findIndex(t => t.typeCode === updated.typeCode);
        if (index > -1) {
          this.appointmentTypes[index] = updated;
        }
        this.errorMsg = '';
        this.loading = false;
      },
      error: (err) => {
        console.error('Error toggling status', err);
        this.errorMsg = apiErrorMessage(err, 'Unable to update appointment type status.');
        this.loading = false;
      }
    });
  }

  cancelEdit() {
    this.showEditForm = false;
    this.selectedType = null;
    this.editFormData = null;
  }

  getTravelTimeDisplay(type: AppointmentType): string {
    if (!type.requiresTravel) return 'N/A';
    return `${type.travelTimeBefore}m before, ${type.travelTimeAfter}m after`;
  }

  getLimitDisplay(type: AppointmentType): string {
    if (!type.hasAppointmentLimit) return 'No Limit';
    return `Max: ${type.maxAppointmentLimit} (${type.limitIsSacrosanct ? 'Strict' : 'Flexible'})`;
  }
}
