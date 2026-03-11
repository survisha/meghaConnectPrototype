import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

interface EventCategory {
  code: string;
  description: string;
  color: string;
  maxPerDay: number;
  enabled: boolean;
}

@Component({
  selector: 'app-system-config',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    TableModule, Tag, Button, Dialog, Select, Toast,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatSlideToggleModule,
  ],
  providers: [MessageService],
  template: `
    <p-toast />
    <div class="config-page">
      <div class="page-header">
        <h2><i class="pi pi-cog"></i> System Configuration</h2>
        <p class="subtitle">Manage appointment limits, event categories, and notification settings</p>
      </div>

      <!-- Tabs -->
      <div class="tab-bar">
        <button class="tab-btn" [class.active]="activeTab==='limits'" (click)="activeTab='limits'">
          <i class="pi pi-sliders-h"></i> Appointment Limits
        </button>
        <button class="tab-btn" [class.active]="activeTab==='categories'" (click)="activeTab='categories'">
          <i class="pi pi-tags"></i> Event Categories
        </button>
        <button class="tab-btn" [class.active]="activeTab==='notifications'" (click)="activeTab='notifications'">
          <i class="pi pi-bell"></i> Notifications
        </button>
      </div>

      <!-- APPOINTMENT LIMITS -->
      <div *ngIf="activeTab==='limits'" class="section-card">
        <h3>Appointment Limits</h3>
        <form [formGroup]="limitsForm" class="config-form">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Max Daily Appointments</mat-label>
              <input matInput type="number" formControlName="maxDailyAppointments" min="1" />
              <mat-hint>Total appointments per day</mat-hint>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Max Walk-ins Per Day</mat-label>
              <input matInput type="number" formControlName="maxWalkInPerDay" min="0" />
              <mat-hint>B2 category walk-in limit</mat-hint>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Max Group Size</mat-label>
              <input matInput type="number" formControlName="maxGroupSize" min="1" />
              <mat-hint>Max members in group appointment</mat-hint>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Advance Booking Days</mat-label>
              <input matInput type="number" formControlName="advanceBookingDays" min="1" />
              <mat-hint>How far ahead can be booked</mat-hint>
            </mat-form-field>
          </div>
          <div class="form-actions">
            <button mat-raised-button color="primary" (click)="saveLimits()" [disabled]="limitsForm.invalid">
              <mat-icon>save</mat-icon> Save Limits
            </button>
            <button mat-stroked-button (click)="resetLimits()">
              <mat-icon>refresh</mat-icon> Reset
            </button>
          </div>
        </form>
      </div>

      <!-- EVENT CATEGORIES -->
      <div *ngIf="activeTab==='categories'" class="section-card">
        <div class="section-header">
          <h3>Event Categories</h3>
        </div>
        <p-table [value]="categories" [tableStyle]="{'min-width':'600px'}" styleClass="p-datatable-sm p-datatable-striped">
          <ng-template pTemplate="header">
            <tr>
              <th>Code</th>
              <th>Description</th>
              <th>Color</th>
              <th>Max Per Day</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-cat>
            <tr>
              <td><strong>{{ cat.code }}</strong></td>
              <td>{{ cat.description }}</td>
              <td>
                <span class="color-dot" [style.background]="cat.color"></span>
                {{ cat.color }}
              </td>
              <td>{{ cat.maxPerDay }}</td>
              <td>
                <p-tag [value]="cat.enabled ? 'Active' : 'Disabled'"
                       [severity]="cat.enabled ? 'success' : 'secondary'" />
              </td>
              <td>
                <button mat-icon-button color="primary" (click)="openCategoryEdit(cat)" title="Edit">
                  <mat-icon>edit</mat-icon>
                </button>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <!-- NOTIFICATIONS -->
      <div *ngIf="activeTab==='notifications'" class="section-card">
        <h3>Notification Settings</h3>
        <form [formGroup]="notifForm" class="config-form">
          <div class="toggle-list">
            <div class="toggle-row">
              <div class="toggle-info">
                <span class="toggle-label"><i class="pi pi-mobile"></i> SMS Provider</span>
                <span class="toggle-desc">Send SMS notifications to applicants</span>
              </div>
              <mat-slide-toggle formControlName="smsEnabled" color="primary" />
            </div>
            <div class="toggle-row">
              <div class="toggle-info">
                <span class="toggle-label"><i class="pi pi-whatsapp"></i> WhatsApp Provider</span>
                <span class="toggle-desc">Send WhatsApp messages to applicants</span>
              </div>
              <mat-slide-toggle formControlName="whatsappEnabled" color="primary" />
            </div>
            <div class="toggle-row">
              <div class="toggle-info">
                <span class="toggle-label"><i class="pi pi-envelope"></i> Email Notifications</span>
                <span class="toggle-desc">Send email confirmations and reminders</span>
              </div>
              <mat-slide-toggle formControlName="emailEnabled" color="primary" />
            </div>
          </div>
          <div class="form-actions">
            <button mat-raised-button color="primary" (click)="saveNotifications()">
              <mat-icon>save</mat-icon> Save Settings
            </button>
            <button mat-stroked-button (click)="resetNotifications()">
              <mat-icon>refresh</mat-icon> Reset
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Category Edit Dialog -->
    <p-dialog header="Edit Category" [(visible)]="showCategoryDialog" [modal]="true" [style]="{width:'420px'}">
      <div class="dialog-form" *ngIf="editCategory">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <input matInput [(ngModel)]="editCategory.description" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Color (hex)</mat-label>
          <input matInput [(ngModel)]="editCategory.color" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Max Per Day</mat-label>
          <input matInput type="number" [(ngModel)]="editCategory.maxPerDay" min="0" />
        </mat-form-field>
        <div class="toggle-row" style="margin-top:8px">
          <span class="toggle-label">Enabled</span>
          <mat-slide-toggle [(ngModel)]="editCategory.enabled" color="primary" />
        </div>
      </div>
      <ng-template pTemplate="footer">
        <button mat-raised-button color="primary" (click)="saveCategoryEdit()">
          <mat-icon>save</mat-icon> Save
        </button>
        <button mat-stroked-button (click)="showCategoryDialog=false">Cancel</button>
      </ng-template>
    </p-dialog>
  `,
  styles: [`
    .config-page { padding: 24px; max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 24px; }
    .page-header h2 { font-size: 1.5rem; font-weight: 700; color: #1a237e; margin: 0 0 4px; }
    .subtitle { color: #6b7280; margin: 0; }
    .tab-bar { display: flex; gap: 8px; margin-bottom: 24px; flex-wrap: wrap; }
    .tab-btn {
      padding: 8px 20px; border: 1.5px solid #d1d5db; border-radius: 8px;
      background: white; cursor: pointer; font-size: 0.9rem; font-weight: 500;
      color: #374151; display: flex; align-items: center; gap: 6px; transition: all .2s;
    }
    .tab-btn.active { background: #1a237e; color: white; border-color: #1a237e; }
    .tab-btn:hover:not(.active) { background: #f3f4f6; }
    .section-card { background: white; border-radius: 12px; padding: 24px; border: 1px solid #e5e7eb; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
    .section-card h3 { font-size: 1.1rem; font-weight: 600; color: #1f2937; margin: 0 0 20px; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .section-header h3 { margin: 0; }
    .config-form { display: flex; flex-direction: column; gap: 16px; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
    mat-form-field { background: white !important; }
    ::ng-deep .mat-mdc-text-field-wrapper { background: white !important; }
    ::ng-deep .mat-mdc-form-field-flex { background: white !important; }
    .form-actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 8px; }
    .toggle-list { display: flex; flex-direction: column; gap: 0; }
    .toggle-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 0; border-bottom: 1px solid #f3f4f6;
    }
    .toggle-row:last-child { border-bottom: none; }
    .toggle-info { display: flex; flex-direction: column; gap: 2px; }
    .toggle-label { font-weight: 600; color: #1f2937; display: flex; align-items: center; gap: 8px; }
    .toggle-desc { font-size: 0.85rem; color: #6b7280; }
    .color-dot { display: inline-block; width: 14px; height: 14px; border-radius: 50%; margin-right: 6px; vertical-align: middle; border: 1px solid #e5e7eb; }
    .dialog-form { display: flex; flex-direction: column; gap: 12px; padding: 8px 0; }
    .full-width { width: 100%; }
    .color-dot { display:inline-block; width:14px; height:14px; border-radius:50%; vertical-align:middle; margin-right:4px; border:1px solid #ccc; }
  `],
})
export class SystemConfigComponent {
  activeTab: 'limits' | 'categories' | 'notifications' = 'limits';

  limitsForm: FormGroup;
  notifForm: FormGroup;

  showCategoryDialog = false;
  editCategory: EventCategory | null = null;
  private editCategoryOriginal: EventCategory | null = null;

  categories: EventCategory[] = [
    { code: 'A1', description: 'Cabinet / Chief Minister Flight', color: '#1565c0', maxPerDay: 5, enabled: true },
    { code: 'A2', description: 'Government Events / Functions', color: '#2e7d32', maxPerDay: 8, enabled: true },
    { code: 'A3', description: 'Official Files / Meetings', color: '#f57f17', maxPerDay: 15, enabled: true },
    { code: 'A4', description: 'Individual / Public Meeting', color: '#c62828', maxPerDay: 30, enabled: true },
    { code: 'B1', description: 'Public Durbar / Mass Meeting', color: '#4527a0', maxPerDay: 3, enabled: true },
    { code: 'B2', description: 'Walk-in Counter', color: '#006064', maxPerDay: 50, enabled: true },
  ];

  constructor(private fb: FormBuilder, private messageService: MessageService) {
    this.limitsForm = this.fb.group({
      maxDailyAppointments: [50, [Validators.required, Validators.min(1)]],
      maxWalkInPerDay: [20, [Validators.required, Validators.min(0)]],
      maxGroupSize: [10, [Validators.required, Validators.min(1)]],
      advanceBookingDays: [30, [Validators.required, Validators.min(1)]],
    });

    this.notifForm = this.fb.group({
      smsEnabled: [true],
      whatsappEnabled: [false],
      emailEnabled: [true],
    });
  }

  saveLimits() {
    if (this.limitsForm.invalid) return;
    this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Appointment limits updated successfully.' });
  }

  resetLimits() {
    this.limitsForm.reset({ maxDailyAppointments: 50, maxWalkInPerDay: 20, maxGroupSize: 10, advanceBookingDays: 30 });
    this.messageService.add({ severity: 'info', summary: 'Reset', detail: 'Limits reset to defaults.' });
  }

  openCategoryEdit(cat: EventCategory) {
    this.editCategoryOriginal = cat;
    this.editCategory = { ...cat };
    this.showCategoryDialog = true;
  }

  saveCategoryEdit() {
    if (!this.editCategory || !this.editCategoryOriginal) return;
    const idx = this.categories.indexOf(this.editCategoryOriginal);
    if (idx !== -1) this.categories[idx] = { ...this.editCategory };
    this.showCategoryDialog = false;
    this.messageService.add({ severity: 'success', summary: 'Saved', detail: `Category ${this.editCategory.code} updated.` });
  }

  saveNotifications() {
    this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Notification settings saved successfully.' });
  }

  resetNotifications() {
    this.notifForm.reset({ smsEnabled: true, whatsappEnabled: false, emailEnabled: true });
    this.messageService.add({ severity: 'info', summary: 'Reset', detail: 'Notification settings reset to defaults.' });
  }
}
