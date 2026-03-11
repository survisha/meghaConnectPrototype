import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { PersonService } from '../services/person.service';
import { AppointmentService, CreateAppointmentRequest, CreateGroupAppointmentRequest } from '../services/appointment.service';
import { Person } from '../models';

// Angular Material
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';

// PrimeNG
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Dialog } from 'primeng/dialog';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

// Shared
import { SearchFilterComponent, SearchFilterValues } from '../shared/search-filter/search-filter.component';
import { ProfileCardComponent } from '../shared/profile-card/profile-card.component';
import { DocumentUploadComponent } from '../shared/document-upload/document-upload.component';
import { StatusBadgeComponent } from '../shared/status-badge/status-badge.component';

interface AssociateMember {
  person: Person | null;
  searchQuery: string;
  searchResults: Person[];
  searching: boolean;
}

@Component({
  selector: 'app-deo-dashboard',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatCheckboxModule,
    TableModule, Tag, Dialog, Toast,
    SearchFilterComponent, ProfileCardComponent,
    DocumentUploadComponent, StatusBadgeComponent,
  ],
  providers: [MessageService],
  templateUrl: './deo-dashboard.component.html',
  styleUrls: ['./deo-dashboard.component.scss'],
})
export class DeoDashboardComponent implements OnInit {
  searchConfig = { showPhone: true, showEpic: true, showName: true, showDistrict: true, showVillage: true };
  searchResults: Person[] = [];
  searching = false;
  selectedCitizen: Person | null = null;

  showWalkinDialog = false;
  showGroupDialog = false;
  appointmentForm!: FormGroup;
  groupForm!: FormGroup;

  associates: AssociateMember[] = [];
  maxAssociates = 5;

  stats = { todayWalkin: 0, todayGroup: 0, registered: 0 };

  readonly eventTypes = [
    { label: 'A4 – Individual Appointment', value: 'A4' },
    { label: 'B1 – Public Durbar',           value: 'B1' },
    { label: 'B2 – Public Walk-in',           value: 'B2' },
  ];
  readonly agendaTypes = [
    'Development Work', 'Scheme Application', 'Job/Employment',
    'Medical Assistance', 'Legal Matter', 'General Petition', 'Other',
  ];
  readonly locations = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura',     value: 'TURA' },
    { label: 'Delhi',    value: 'DELHI' },
    { label: 'Others',   value: 'OTHERS' },
  ];

  constructor(
    private personService: PersonService,
    private appointmentService: AppointmentService,
    private fb: FormBuilder,
    private msg: MessageService,
  ) {}

  ngOnInit() {
    this.appointmentForm = this.fb.group({
      eventType:      ['A4', Validators.required],
      agendaType:     ['', Validators.required],
      agendaBrief:    ['', Validators.required],
      requestedLocation: ['SHILLONG', Validators.required],
      mlaMdcApproved: [false],
      isWalkIn:       [true],
    });
    this.groupForm = this.fb.group({
      eventType:      ['B1', Validators.required],
      agendaType:     ['General Petition', Validators.required],
      agendaBrief:    ['', Validators.required],
      requestedLocation: ['SHILLONG', Validators.required],
    });
    // Mock stats — replace with API call when endpoint is ready
    this.stats = { todayWalkin: 12, todayGroup: 3, registered: 45 };
  }

  /** Dispatch to the appropriate PersonService method(s) based on populated filter fields. */
  onSearch(filters: SearchFilterValues) {
    this.searching = true;
    this.searchResults = [];

    const searches: Observable<Person[]>[] = [];

    if (filters.phoneNumber?.trim()) {
      searches.push(
        this.personService.searchByPhone(filters.phoneNumber.trim()).pipe(
          map(p => (p ? [p] : []))
        )
      );
    }
    if (filters.epicNumber?.trim()) {
      searches.push(
        this.personService.searchByEpic(filters.epicNumber.trim()).pipe(
          map(p => (p ? [p] : []))
        )
      );
    }
    if (filters.name?.trim()) {
      searches.push(this.personService.searchByName(filters.name.trim()));
    }
    if (filters.district?.trim()) {
      searches.push(this.personService.searchByDistrict(filters.district.trim()));
    }

    if (searches.length === 0) {
      this.searching = false;
      this.msg.add({ severity: 'warn', summary: 'Enter a search term', detail: 'Please fill at least one search field' });
      return;
    }

    forkJoin(searches).subscribe({
      next: (arrays) => {
        // Merge results; deduplicate by id
        const seen = new Set<number>();
        const merged: Person[] = [];
        for (const arr of arrays) {
          for (const p of arr) {
            if (!seen.has(p.id)) { seen.add(p.id); merged.push(p); }
          }
        }
        this.searchResults = merged;
        this.searching = false;
      },
      error: () => {
        this.searching = false;
        this.msg.add({ severity: 'error', summary: 'Search failed', detail: 'Could not retrieve citizens' });
      },
    });
  }

  onSearchReset() {
    this.searchResults = [];
    this.selectedCitizen = null;
  }

  selectCitizen(person: Person) {
    this.selectedCitizen = person;
  }

  openWalkin() {
    if (!this.selectedCitizen) {
      this.msg.add({ severity: 'warn', summary: 'Select a citizen', detail: 'Please select a citizen first' });
      return;
    }
    this.appointmentForm.patchValue({ isWalkIn: true });
    this.showWalkinDialog = true;
  }

  openGroupAppointment() {
    if (!this.selectedCitizen) {
      this.msg.add({ severity: 'warn', summary: 'Select a citizen', detail: 'Please select a citizen first' });
      return;
    }
    this.associates = [];
    this.showGroupDialog = true;
  }

  addAssociate() {
    if (this.associates.length >= this.maxAssociates) {
      this.msg.add({ severity: 'warn', summary: 'Limit reached', detail: `Maximum ${this.maxAssociates} associates allowed` });
      return;
    }
    this.associates.push({ person: null, searchQuery: '', searchResults: [], searching: false });
  }

  searchAssociate(index: number) {
    const assoc = this.associates[index];
    if (!assoc.searchQuery.trim()) return;
    assoc.searching = true;

    // Route to phone or name search based on content
    const query = assoc.searchQuery.trim();
    const isPhone = /^\d{7,}$/.test(query);
    const obs$: Observable<Person[]> = isPhone
      ? this.personService.searchByPhone(query).pipe(map(p => (p ? [p] : [])))
      : this.personService.searchByName(query);

    obs$.subscribe({
      next: results => { assoc.searchResults = results; assoc.searching = false; },
      error: () => { assoc.searching = false; },
    });
  }

  selectAssociate(index: number, person: Person) {
    this.associates[index].person = person;
    this.associates[index].searchResults = [];
  }

  removeAssociate(index: number) {
    this.associates.splice(index, 1);
  }

  submitWalkin() {
    if (!this.selectedCitizen || this.appointmentForm.invalid) return;
    const fv = this.appointmentForm.value;
    const req: CreateAppointmentRequest = {
      applicantId:       this.selectedCitizen.id,
      eventType:         fv.eventType,
      agendaType:        fv.agendaType,
      agendaBrief:       fv.agendaBrief,
      requestedLocation: fv.requestedLocation,
      mlaMdcApproved:    fv.mlaMdcApproved,
      isWalkIn:          true,
    };
    this.appointmentService.createAppointment(req).subscribe({
      next: () => {
        this.msg.add({ severity: 'success', summary: 'Walk-in Created', detail: 'Appointment registered successfully' });
        this.showWalkinDialog = false;
        this.stats.todayWalkin++;
        this.appointmentForm.reset({ eventType: 'A4', requestedLocation: 'SHILLONG', isWalkIn: true, mlaMdcApproved: false });
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Error', detail: 'Failed to create appointment' }),
    });
  }

  submitGroupAppointment() {
    if (!this.selectedCitizen || this.groupForm.invalid) return;
    const fv = this.groupForm.value;
    const associateIds = this.associates.filter(a => a.person).map(a => a.person!.id);

    const req: CreateGroupAppointmentRequest = {
      applicantId:        this.selectedCitizen.id,
      eventType:          fv.eventType,
      agendaType:         fv.agendaType,
      agendaBrief:        fv.agendaBrief,
      requestedLocation:  fv.requestedLocation,
      isWalkIn:           false,
      isGroupAppointment: true,
      associateIds,
    };

    this.appointmentService.createAppointment(req).subscribe({
      next: () => {
        this.msg.add({ severity: 'success', summary: 'Group Appointment Created', detail: `Primary + ${associateIds.length} associates registered` });
        this.showGroupDialog = false;
        this.stats.todayGroup++;
        this.groupForm.reset({ eventType: 'B1', agendaType: 'General Petition', requestedLocation: 'SHILLONG' });
        this.associates = [];
      },
      error: () => this.msg.add({ severity: 'error', summary: 'Error', detail: 'Failed to create group appointment' }),
    });
  }
}
