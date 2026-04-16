import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { VisitorSearchService } from '../../services/visitor-search.service';
import { Visitor } from '../../models';
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
  foundPerson: Visitor | null = null;
  notFound = false;
  checkedIn = false;
  ticketId = '';
  agendaType = '';
  associates: Visitor[] = [];
  searching = false;

  agendaTypes = [
    { label: 'Scheme availment (CM)', value: 'SCHEME_CM' },
    { label: 'Governance', value: 'GOVERNANCE' },
    { label: 'Trade & Commerce', value: 'TRADE_COMMERCE' },
    { label: 'Political Discussion', value: 'POLITICAL' },
    { label: 'Public Grievance', value: 'GRIEVANCE' }
  ];

  constructor(private visitorSearchService: VisitorSearchService) {}

  search() {
    this.notFound = false; this.foundPerson = null; this.searching = true;
    const phone = this.phoneNumber.trim();
    const epic = this.epicNumber.trim();

    if (phone) {
      this.visitorSearchService.searchByPhone(phone).subscribe(p => {
        this.foundPerson = p;
        if (!p) this.notFound = true;
        this.searching = false;
      });
    } else if (epic) {
      this.visitorSearchService.searchByEpic(epic).subscribe(p => {
        this.foundPerson = p;
        if (!p) this.notFound = true;
        this.searching = false;
      });
    } else {
      this.notFound = true;
      this.searching = false;
    }
  }

  checkIn() {
    this.checkedIn = true;
    this.ticketId = 'WI-' + new Date().getFullYear() + '-' + String(Math.floor(Math.random()*9000+1000));
  }

  addAssociate() {
    // No-op without a real search; user must search separately
  }
}
