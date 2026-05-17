import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CitizenAppointmentHistory,
  CitizenSchemeHistory,
  PublicIdentificationHistory,
  VisitorSearchService
} from '../services/visitor-search.service';
import { Visitor } from '../models';
import { environment } from '../../environments/environment';
import { apiErrorMessage } from '../shared/api-error.util';

// Angular Material
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-public-identification',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatInputModule, MatSelectModule, MatChipsModule, MatDividerModule,
    MatTableModule, MatButtonModule, MatIconModule, MatFormFieldModule,
  ],
  templateUrl: './public-identification.component.html',
  styleUrls: ['./public-identification.component.scss'],
})
export class PublicIdentificationComponent {
  searchPhone = '';
  searchEpic = '';
  searchName = '';
  searchDistrict = '';
  results: Visitor[] = [];
  selected: Visitor | null = null;
  searched = false;
  searching = false;
  errorMessage = '';
  selectedPhotoLoadFailed = false;
  selectedPhotoPreviewOpen = false;
  historyLoading = false;
  historyError = '';
  fullHistoryOpen = false;
  citizenHistory: PublicIdentificationHistory | null = null;

  districts = ['East Khasi Hills','West Khasi Hills','Ri Bhoi','East Jaintia Hills','West Jaintia Hills','East Garo Hills','West Garo Hills','South Garo Hills','North Garo Hills'];

  schemeHistoryColumns: string[] = ['scheme', 'appliedDate', 'amount', 'status'];
  meetingHistoryColumns: string[] = ['date', 'department', 'purpose', 'status'];
  schemeHistory: CitizenSchemeHistory[] = [];
  meetingHistory: CitizenAppointmentHistory[] = [];

  constructor(private visitorSearchService: VisitorSearchService) {}

  populateHistory() {
    this.schemeHistory = [];
    this.meetingHistory = [];
    this.citizenHistory = null;
    this.historyError = '';
    this.historyLoading = false;
    this.fullHistoryOpen = false;
  }

  search() {
    const phone = this.searchPhone.trim();
    const epic = this.searchEpic.trim();
    const name = this.searchName.trim();
    const district = this.searchDistrict.trim();

    this.searched = true;
    this.searching = false;
    this.errorMessage = '';
    this.results = [];
    this.selected = null;
    this.populateHistory();

    if (!phone && !epic && !name && !district) {
      this.errorMessage = 'Enter at least one search criteria.';
      return;
    }

    this.searching = true;

    if (phone) {
      this.visitorSearchService.searchByPhone(phone).subscribe({
        next: visitors => this.setResults(visitors, { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    } else if (epic) {
      this.visitorSearchService.searchByEpic(epic).subscribe({
        next: visitor => this.setResults(visitor ? [visitor] : [], { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    } else if (name) {
      this.visitorSearchService.searchByName(name).subscribe({
        next: visitors => this.setResults(visitors, { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    } else if (district) {
      this.visitorSearchService.searchByDistrict(district).subscribe({
        next: visitors => this.setResults(visitors, { phone, epic, name, district }),
        error: error => this.handleSearchError(error),
      });
    }
  }

  select(p: Visitor) {
    this.selected = p;
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.populateHistory();
    this.loadCitizenHistory(p.id);
  }

  clearSearch() {
    this.searchPhone = '';
    this.searchEpic = '';
    this.searchName = '';
    this.searchDistrict = '';
    this.results = [];
    this.selected = null;
    this.searched = false;
    this.searching = false;
    this.errorMessage = '';
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.populateHistory();
  }

  initial(name?: string | null): string {
    return (name?.trim().charAt(0) || '?').toUpperCase();
  }

  displayValue(value?: string | number | null): string {
    const formatted = value?.toString().trim();
    return formatted || '-';
  }

  formatDateTime(value?: string | null): string {
    if (!value) {
      return '-';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
  }

  formatCurrency(value?: number | null): string {
    if (value === null || value === undefined) {
      return '-';
    }
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  statusLabel(status?: string | null): string {
    return this.displayValue(status?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()));
  }

  statusClass(status?: string | null): string {
    const normalized = (status || '').toUpperCase();
    if (['APPROVED', 'COMPLETED', 'RESOLVED', 'HCM_ACCEPTED', 'APPROVED_WITH_DATE_TIME'].includes(normalized)) {
      return 'status-success';
    }
    if (['REJECTED', 'CANCELLED', 'HCM_REJECTED'].includes(normalized)) {
      return 'status-danger';
    }
    if (['PENDING', 'SUBMITTED', 'CMO_REVIEW', 'APPROVER_REVIEW', 'HCM_PENDING', 'SCHEDULED'].includes(normalized)) {
      return 'status-warn';
    }
    return 'status-info';
  }

  get latestSchemes(): CitizenSchemeHistory[] {
    return this.schemeHistory.slice(0, 3);
  }

  get latestMeetings(): CitizenAppointmentHistory[] {
    return this.meetingHistory.slice(0, 3);
  }

  get hasAnyHistory(): boolean {
    return this.schemeHistory.length > 0 || this.meetingHistory.length > 0;
  }

  get groupVisitHistory(): CitizenAppointmentHistory[] {
    return this.meetingHistory.filter(item => item.role === 'ASSOCIATE' || (item.groupMembers?.length || 0) > 0);
  }

  toggleFullHistory(): void {
    this.fullHistoryOpen = !this.fullHistoryOpen;
  }

  get selectedPhotoUrl(): string {
    return this.getVisitorPhotoUrl(this.selected);
  }

  toggleSelectedPhotoPreview(): void {
    if (!this.selectedPhotoUrl || this.selectedPhotoLoadFailed) {
      return;
    }
    this.selectedPhotoPreviewOpen = !this.selectedPhotoPreviewOpen;
  }

  closeSelectedPhotoPreview(): void {
    this.selectedPhotoPreviewOpen = false;
  }

  onSelectedPhotoError(): void {
    this.selectedPhotoLoadFailed = true;
    this.selectedPhotoPreviewOpen = false;
  }

  private setResults(visitors: Visitor[], criteria: SearchCriteria): void {
    this.results = (visitors || []).filter(visitor => this.matchesCriteria(visitor, criteria));
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.searching = false;
    if (this.results[0]) {
      this.select(this.results[0]);
    } else {
      this.selected = null;
      this.populateHistory();
    }
  }

  private handleSearchError(error: unknown): void {
    this.results = [];
    this.selected = null;
    this.searching = false;
    this.errorMessage = apiErrorMessage(error, 'Unable to search visitor records right now. Please try again.');
    this.selectedPhotoLoadFailed = false;
    this.selectedPhotoPreviewOpen = false;
    this.populateHistory();
  }

  private matchesCriteria(visitor: Visitor, criteria: SearchCriteria): boolean {
    const phoneDigits = this.onlyDigits(criteria.phone);
    const visitorPhoneDigits = this.onlyDigits(visitor.phoneNumber);
    if (phoneDigits && !visitorPhoneDigits.includes(phoneDigits)) {
      return false;
    }

    const epic = criteria.epic.toUpperCase();
    if (epic && !this.text(visitor.epicNumber).toUpperCase().includes(epic)) {
      return false;
    }

    const name = criteria.name.toLowerCase();
    if (name && !this.text(visitor.fullName).toLowerCase().includes(name)) {
      return false;
    }

    const district = criteria.district.toLowerCase();
    if (district && this.text(visitor.district).toLowerCase() !== district) {
      return false;
    }

    return true;
  }

  private text(value?: string | null): string {
    return (value || '').trim();
  }

  private onlyDigits(value?: string | null): string {
    return (value || '').replace(/\D/g, '');
  }

  private loadCitizenHistory(citizenId: number): void {
    this.historyLoading = true;
    this.historyError = '';
    this.visitorSearchService.getPublicIdentificationHistory(citizenId).subscribe({
      next: history => {
        this.historyLoading = false;
        this.citizenHistory = history;
        this.schemeHistory = history.schemes || [];
        this.meetingHistory = history.appointments || [];
        if (this.selected && this.selected.id === citizenId && history.photoUrl) {
          this.selected = { ...this.selected, photoUrl: history.photoUrl };
          this.selectedPhotoLoadFailed = false;
        }
      },
      error: error => {
        this.historyLoading = false;
        this.historyError = apiErrorMessage(error, 'Unable to load citizen history. Please try again.');
        this.schemeHistory = [];
        this.meetingHistory = [];
        this.citizenHistory = null;
      },
    });
  }

  private getVisitorPhotoUrl(visitor?: Visitor | null): string {
    if (!visitor) {
      return '';
    }

    const inlinePhoto = this.firstNonBlank(visitor.livePhotoBase64, visitor.photoBase64, visitor.photoUrl);
    if (inlinePhoto) {
      return this.normalizePhotoSource(inlinePhoto);
    }

    const storedPath = this.firstNonBlank(visitor.livePhotoPath, visitor.photoStoragePath, visitor.photoPath);
    return storedPath ? this.normalizePhotoSource(storedPath) : '';
  }

  private normalizePhotoSource(value: string): string {
    const source = value.trim();
    if (!source) {
      return '';
    }
    if (source.startsWith('data:image/') || source.startsWith('blob:') || /^https?:\/\//i.test(source)) {
      return source;
    }

    const origin = environment.apiUrl.replace(/\/api\/v1\/?$/i, '');
    const path = source.replace(/^\/+/, '');
    return `${origin}/${path.startsWith('uploads/') ? path : `uploads/${path}`}`;
  }

  private firstNonBlank(...values: Array<string | null | undefined>): string {
    return values.map(value => value?.trim() || '').find(Boolean) || '';
  }
}

interface SearchCriteria {
  phone: string;
  epic: string;
  name: string;
  district: string;
}
