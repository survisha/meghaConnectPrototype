import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PersonService } from '../../services/person.service';
import { Person } from '../../models';
import { InputText } from 'primeng/inputtext';
import { Tag } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { Select } from 'primeng/select';

@Component({
  selector: 'app-walkin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InputText, Tag, TableModule, Select],
  templateUrl: './walkin.component.html',
  styleUrls: ['./walkin.component.scss'],
})
export class WalkinComponent {
  phoneNumber = '';
  epicNumber = '';
  foundPerson: Person | null = null;
  notFound = false;
  checkedIn = false;
  ticketId = '';
  agendaType = '';
  associates: Person[] = [];
  searching = false;

  agendaTypes = ['Scheme availment (CM)','Governance','Trade & Commerce','Political Discussion','Public Grievance'];

  constructor(private personService: PersonService) {}

  search() {
    this.notFound = false; this.foundPerson = null; this.searching = true;
    const phone = this.phoneNumber.trim();
    const epic = this.epicNumber.trim();

    if (phone) {
      this.personService.searchByPhone(phone).subscribe(p => {
        this.foundPerson = p;
        if (!p) this.notFound = true;
        this.searching = false;
      });
    } else if (epic) {
      this.personService.searchByEpic(epic).subscribe(p => {
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
