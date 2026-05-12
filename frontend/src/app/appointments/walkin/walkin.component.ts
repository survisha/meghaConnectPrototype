import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { VisitorSearchService } from '../../services/visitor-search.service';
import { Visitor } from '../../models';
import { apiErrorMessage } from '../../shared/api-error.util';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-walkin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatChipsModule, MatIconModule, MatCardModule, MatTooltipModule],
  templateUrl: './walkin.component.html',
  styleUrls: ['./walkin.component.scss'],
})
export class WalkinComponent {
  phoneNumber = '';
  epicNumber = '';
  referenceId = '';
  foundPerson: Visitor | null = null;
  notFound = false;
  checkedIn = false;
  ticketId = '';
  agendaType = '';
  requestedLocation = 'SHILLONG';
  agendaBrief = '';
  errorMsg = '';
  associates: Visitor[] = [];
  searching = false;
  creating = false;

  agendaTypes = [
    { label: 'Scheme availment (CM)', value: 'Scheme availment (CM)' },
    { label: 'Governance', value: 'Governance' },
    { label: 'Trade & Commerce', value: 'Trade & Commerce' },
    { label: 'Political Discussion', value: 'Political Discussion' },
    { label: 'Public Grievance', value: 'Public Grievance' }
  ];
  locations = ['SHILLONG', 'TURA', 'DELHI', 'OTHERS'];

  constructor(private visitorSearchService: VisitorSearchService, private router: Router) {}

  search() {
    this.errorMsg = '';
    this.notFound = false; this.foundPerson = null; this.searching = true;
    const phone = this.phoneNumber.trim();
    const epic = this.epicNumber.trim();
    const referenceId = this.referenceId.trim();

    if (!phone && !epic && !referenceId) {
      this.notFound = true;
      this.searching = false;
      this.errorMsg = 'Enter mobile, EPIC, or visitor reference ID to search.';
      return;
    }

    this.visitorSearchService.search({ mobile: phone, epic, referenceId }).subscribe({
      next: results => {
        this.foundPerson = results[0] ?? null;
        this.notFound = !this.foundPerson;
        this.searching = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to search visitor records.');
        this.notFound = false;
        this.searching = false;
      }
    });
  }

  continueToAppointmentForm() {
    this.errorMsg = '';
    if (!this.foundPerson?.id) {
      this.errorMsg = 'Select an existing visitor before opening the appointment form.';
      return;
    }
    this.router.navigate(['/appointments/new'], {
      queryParams: {
        visitorId: this.foundPerson.id,
        source: 'walkin',
        walkin: true,
      }
    });
  }

  addAssociate() {
    // No-op without a real search; user must search separately
  }
}
