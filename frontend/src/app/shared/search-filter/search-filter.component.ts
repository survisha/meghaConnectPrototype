import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface SearchFilterConfig {
  showPhone?: boolean;
  showEpic?: boolean;
  showName?: boolean;
  showDistrict?: boolean;
  showVillage?: boolean;
  showStatus?: boolean;
  statusOptions?: { label: string; value: string }[];
}

export interface SearchFilterValues {
  phoneNumber?: string;
  epicNumber?: string;
  name?: string;
  district?: string;
  village?: string;
  status?: string;
}

@Component({
  selector: 'app-search-filter',
  standalone: true,
  imports: [CommonModule, FormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  template: `
    <div class="search-filter-container">
      <div class="filter-row">
        <mat-form-field *ngIf="config.showPhone !== false" appearance="outline" class="filter-field">
          <mat-label>Phone Number</mat-label>
          <input matInput [(ngModel)]="values.phoneNumber" placeholder="Search by phone">
          <mat-icon matSuffix>phone</mat-icon>
        </mat-form-field>
        <mat-form-field *ngIf="config.showEpic" appearance="outline" class="filter-field">
          <mat-label>EPIC Number</mat-label>
          <input matInput [(ngModel)]="values.epicNumber" placeholder="Voter ID">
          <mat-icon matSuffix>credit_card</mat-icon>
        </mat-form-field>
        <mat-form-field *ngIf="config.showName !== false" appearance="outline" class="filter-field">
          <mat-label>Name</mat-label>
          <input matInput [(ngModel)]="values.name" placeholder="Full name">
          <mat-icon matSuffix>person</mat-icon>
        </mat-form-field>
        <mat-form-field *ngIf="config.showDistrict" appearance="outline" class="filter-field">
          <mat-label>District</mat-label>
          <mat-select [(ngModel)]="values.district">
            <mat-option value="">All Districts</mat-option>
            <mat-option *ngFor="let d of districts" [value]="d">{{ d }}</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field *ngIf="config.showVillage" appearance="outline" class="filter-field">
          <mat-label>Village / Booth</mat-label>
          <input matInput [(ngModel)]="values.village" placeholder="Village">
        </mat-form-field>
        <mat-form-field *ngIf="config.showStatus && config.statusOptions" appearance="outline" class="filter-field">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="values.status">
            <mat-option value="">All</mat-option>
            <mat-option *ngFor="let s of config.statusOptions" [value]="s.value">{{ s.label }}</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
      <div class="filter-actions">
        <button mat-raised-button color="primary" (click)="onSearch()">
          <mat-icon>search</mat-icon> Search
        </button>
        <button mat-stroked-button (click)="onReset()">
          <mat-icon>refresh</mat-icon> Reset
        </button>
      </div>
    </div>
  `,
  styles: [`
    .search-filter-container { background: white; border-radius: 8px; padding: 1rem; box-shadow: 0 1px 4px rgba(0,0,0,0.08); margin-bottom: 1rem; }
    .filter-row { display: flex; flex-wrap: wrap; gap: 0.75rem; align-items: center; }
    .filter-field { min-width: 180px; flex: 1; }
    .filter-actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
    mat-form-field { background: white !important; }
    mat-form-field input, mat-select { background: white !important; }
  `]
})
export class SearchFilterComponent {
  @Input() config: SearchFilterConfig = {};
  @Input() values: SearchFilterValues = {};
  @Output() search = new EventEmitter<SearchFilterValues>();
  @Output() reset = new EventEmitter<void>();

  districts = [
    'East Khasi Hills', 'West Khasi Hills', 'South West Khasi Hills',
    'Ri Bhoi', 'East Jaintia Hills', 'West Jaintia Hills',
    'East Garo Hills', 'West Garo Hills', 'South Garo Hills',
    'North Garo Hills', 'Eastern West Khasi Hills'
  ];

  onSearch() { this.search.emit({ ...this.values }); }
  onReset() {
    this.values = {};
    this.reset.emit();
  }
}
