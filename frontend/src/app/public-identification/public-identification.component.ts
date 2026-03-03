import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MockDataService } from '../services/mock-data.service';
import { Person } from '../models';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-public-identification',
  standalone: true,
  imports: [CommonModule, FormsModule, InputText, Select, Tag, Divider, TableModule],
  templateUrl: './public-identification.component.html',
  styleUrls: ['./public-identification.component.scss'],
})
export class PublicIdentificationComponent {
  searchPhone = '';
  searchEpic = '';
  searchName = '';
  searchDistrict = '';
  results: Person[] = [];
  selected: Person | null = null;
  searched = false;

  districts = ['East Khasi Hills','West Khasi Hills','Ri Bhoi','East Jaintia Hills','West Jaintia Hills','East Garo Hills','West Garo Hills','South Garo Hills','North Garo Hills'];

  schemeHistory = [
    { scheme: 'CMSDF', year: '2022', amount: '₹2.5L', status: 'Completed' },
    { scheme: 'CM Care', year: '2023', amount: '₹50K', status: 'Completed' },
  ];

  meetingHistory = [
    { date: '15 Jan 2024', agenda: 'CMSDF Application', outcome: 'Approved' },
    { date: '10 Nov 2023', agenda: 'Governance Issue', outcome: 'Forwarded to Dept' },
    { date: '05 Aug 2023', agenda: 'CMSG Application', outcome: 'Under Process' },
  ];

  constructor(public mock: MockDataService) {}

  search() {
    this.searched = true;
    this.results = this.mock.persons.filter(p => {
      if (this.searchPhone && p.phoneNumber.includes(this.searchPhone)) return true;
      if (this.searchEpic && p.epicNumber.toLowerCase().includes(this.searchEpic.toLowerCase())) return true;
      if (this.searchName && p.fullName.toLowerCase().includes(this.searchName.toLowerCase())) return true;
      if (this.searchDistrict && p.district === this.searchDistrict) return true;
      return false;
    });
    if (this.results.length === 0 && !this.searchPhone && !this.searchEpic && !this.searchName && !this.searchDistrict) {
      this.results = this.mock.persons;
    }
  }

  select(p: Person) { this.selected = p; }

  clearSearch() { this.searchPhone=''; this.searchEpic=''; this.searchName=''; this.searchDistrict=''; this.results=[]; this.selected=null; this.searched=false; }
}
