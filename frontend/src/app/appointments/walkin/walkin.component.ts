import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../services/mock-data.service';
import { Person } from '../../models';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { TableModule } from 'primeng/table';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';

@Component({
  selector: 'app-walkin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Button, InputText, Tag, Divider, TableModule, Select, Textarea],
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

  agendaTypes = ['Scheme availment (CM)','Governance','Trade & Commerce','Political Discussion','Public Grievance'];

  constructor(public mock: MockDataService) {}

  search() {
    this.notFound = false; this.foundPerson = null;
    this.foundPerson = this.mock.persons.find(
      p => p.phoneNumber === this.phoneNumber || p.epicNumber === this.epicNumber
    ) ?? null;
    if (!this.foundPerson) this.notFound = true;
  }

  checkIn() {
    this.checkedIn = true;
    this.ticketId = 'WI-' + new Date().getFullYear() + '-' + String(Math.floor(Math.random()*9000+1000));
  }

  addAssociate() {
    const p = this.mock.persons[Math.floor(Math.random()*this.mock.persons.length)];
    if (!this.associates.find(a => a.id === p.id)) this.associates.push(p);
  }
}
