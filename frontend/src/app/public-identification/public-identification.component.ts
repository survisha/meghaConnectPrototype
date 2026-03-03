import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonService } from '../services/person.service';
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
  searching = false;

  districts = ['East Khasi Hills','West Khasi Hills','Ri Bhoi','East Jaintia Hills','West Jaintia Hills','East Garo Hills','West Garo Hills','South Garo Hills','North Garo Hills'];

  schemeHistory: { scheme: string; year: string; amount: string; status: string }[] = [];
  meetingHistory: { date: string; agenda: string; outcome: string }[] = [];

  constructor(private personService: PersonService) {}

  search() {
    this.searched = true;
    this.searching = true;
    this.results = [];
    this.selected = null;

    const phone = this.searchPhone.trim();
    const epic = this.searchEpic.trim();
    const name = this.searchName.trim();
    const district = this.searchDistrict.trim();

    if (phone) {
      this.personService.searchByPhone(phone).subscribe(p => {
        if (p) this.results = [p];
        this.searching = false;
      });
    } else if (epic) {
      this.personService.searchByEpic(epic).subscribe(p => {
        if (p) this.results = [p];
        this.searching = false;
      });
    } else if (name) {
      this.personService.searchByName(name).subscribe(res => {
        this.results = res;
        this.searching = false;
      });
    } else if (district) {
      this.personService.searchByDistrict(district).subscribe(res => {
        this.results = res;
        this.searching = false;
      });
    } else {
      this.searching = false;
    }
  }

  select(p: Person) { this.selected = p; }

  clearSearch() { this.searchPhone=''; this.searchEpic=''; this.searchName=''; this.searchDistrict=''; this.results=[]; this.selected=null; this.searched=false; }
}
