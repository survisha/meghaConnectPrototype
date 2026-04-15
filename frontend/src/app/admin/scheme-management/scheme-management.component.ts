import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Scheme {
  id: number;
  schemeCode: string;
  schemeName: string;
  description?: string;
  isActive: boolean;
  requiredDocuments?: SchemeDocument[];
}

interface SchemeDocument {
  id?: number;
  documentType: string;
  documentLabel: string;
  isRequired: boolean;
  description?: string;
  fileFormatAllowed?: string;
  displayOrder?: number;
}

@Component({
  selector: 'app-scheme-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTooltipModule
  ],
  templateUrl: './scheme-management.component.html',
  styleUrls: ['./scheme-management.component.scss']
})
export class SchemeManagementComponent implements OnInit {
  schemes: Scheme[] = [];
  displayedColumns: string[] = ['schemeCode', 'schemeName', 'isActive', 'actions'];
  loading = false;
  showNewSchemeForm = false;
  selectedScheme: Scheme | null = null;
  showDocumentConfig = false;

  newSchemeForm: any = {
    schemeCode: '',
    schemeName: '',
    description: ''
  };

  documentForm: SchemeDocument[] = [];

  documentTypeOptions = [
    'PLANS_ESTIMATES',
    'BANK_DETAILS',
    'MLA_APPROVAL',
    'MEDICAL_DOCS',
    'FINANCIAL_PROOF',
    'ORG_REGISTRATION',
    'KYC_DOCUMENTS',
    'OTHER'
  ];

  fileFormatOptions = [
    { value: 'pdf', label: 'PDF' },
    { value: 'jpg,jpeg', label: 'JPG/JPEG' },
    { value: 'png', label: 'PNG' },
    { value: 'pdf,jpg', label: 'PDF or Image' },
    { value: 'all', label: 'All formats' }
  ];

  constructor(private http: HttpClient) { }

  ngOnInit() {
    this.loadSchemes();
  }

  loadSchemes() {
    this.loading = true;
    this.http.get<Scheme[]>(`${environment.apiUrl}/admin/schemes`)
      .subscribe({
        next: (data) => {
          this.schemes = data;
          this.loading = false;
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error loading schemes:', err);
          alert('Error loading schemes: ' + (err.error?.message || err.message || 'Unknown error'));
          this.loading = false;
        }
      });
  }

  toggleNewSchemeForm() {
    this.showNewSchemeForm = !this.showNewSchemeForm;
    if (!this.showNewSchemeForm) {
      this.resetNewSchemeForm();
    }
  }

  resetNewSchemeForm() {
    this.newSchemeForm = {
      schemeCode: '',
      schemeName: '',
      description: ''
    };
  }

  createScheme() {
    if (!this.newSchemeForm.schemeCode || !this.newSchemeForm.schemeName) {
      alert('Please enter Scheme Code and Scheme Name');
      return;
    }

    this.loading = true;
    this.http.post<Scheme>(`${environment.apiUrl}/admin/schemes`, this.newSchemeForm)
      .subscribe({
        next: (newScheme) => {
          alert('Scheme created successfully');
          this.schemes.push(newScheme);
          this.resetNewSchemeForm();
          this.showNewSchemeForm = false;
          this.loading = false;
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error creating scheme:', err);
          alert('Error creating scheme: ' + (err.error?.message || err.message || 'Unknown error'));
          this.loading = false;
        }
      });
  }

  toggleSchemeActive(scheme: Scheme, event: any) {
    const newStatus = event.checked;
    const updateData = {
      schemeCode: scheme.schemeCode,
      schemeName: scheme.schemeName,
      description: scheme.description || '',
      isActive: newStatus
    };

    this.loading = true;
    this.http.put<Scheme>(`${environment.apiUrl}/admin/schemes/${scheme.schemeCode}`, updateData)
      .subscribe({
        next: (updated) => {
          scheme.isActive = updated.isActive;
          alert(scheme.isActive ? 'Scheme activated' : 'Scheme marked as inactive');
          this.loading = false;
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error updating scheme:', err);
          alert('Error updating scheme: ' + (err.error?.message || err.message || 'Unknown error'));
          this.loading = false;
          // Revert the toggle
          event.source.checked = !newStatus;
        }
      });
  }

  editScheme(scheme: Scheme) {
    this.selectedScheme = { ...scheme };
    this.loadSchemeDocuments(scheme);
  }

  loadSchemeDocuments(scheme: Scheme) {
    this.loading = true;
    this.http.get<Scheme>(`${environment.apiUrl}/admin/schemes/${scheme.schemeCode}`)
      .subscribe({
        next: (data) => {
          this.selectedScheme = data;
          this.documentForm = data.requiredDocuments || [];
          this.showDocumentConfig = true;
          this.loading = false;
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error loading scheme documents:', err);
          alert('Error loading scheme documents: ' + (err.error?.message || err.message || 'Unknown error'));
          this.loading = false;
        }
      });
  }

  addDocumentRow() {
    this.documentForm.push({
      documentType: '',
      documentLabel: '',
      isRequired: true,
      description: '',
      fileFormatAllowed: 'pdf',
      displayOrder: this.documentForm.length
    });
  }

  removeDocumentRow(index: number) {
    this.documentForm.splice(index, 1);
  }

  saveSchemeDocuments() {
    if (!this.selectedScheme) {
      alert('No scheme selected');
      return;
    }

    // Validate documents
    for (let doc of this.documentForm) {
      if (!doc.documentType || !doc.documentLabel) {
        alert('Please fill in all required document fields');
        return;
      }
    }

    this.loading = true;
    this.http.put<Scheme>(
      `${environment.apiUrl}/admin/schemes/${this.selectedScheme.schemeCode}/documents`,
      this.documentForm
    ).subscribe({
      next: (updated) => {
        alert('Documents configured successfully');
        this.selectedScheme = updated;
        this.loadSchemes();
        this.closeDocumentConfig();
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error saving documents:', err);
        alert('Error saving documents: ' + (err.error?.message || err.message || 'Unknown error'));
        this.loading = false;
      }
    });
  }

  closeDocumentConfig() {
    this.showDocumentConfig = false;
    this.selectedScheme = null;
    this.documentForm = [];
  }

  getFormatLabel(format?: string): string {
    if (!format) return '';
    const option = this.fileFormatOptions.find(o => o.value === format);
    return option?.label || format;
  }
}
