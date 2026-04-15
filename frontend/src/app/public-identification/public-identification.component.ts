import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VisitorSearchService } from '../services/visitor-search.service';
import { Visitor } from '../models';

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
export class PublicIdentificationComponent implements OnInit {
  searchPhone = '';
  searchEpic = '';
  searchName = '';
  searchDistrict = '';
  results: Visitor[] = [];
  selected: Visitor | null = null;
  searched = false;
  searching = false;

  districts = ['East Khasi Hills','West Khasi Hills','Ri Bhoi','East Jaintia Hills','West Jaintia Hills','East Garo Hills','West Garo Hills','South Garo Hills','North Garo Hills'];

  schemeHistoryColumns: string[] = ['scheme', 'year', 'amount', 'status'];
  meetingHistoryColumns: string[] = ['date', 'agenda', 'outcome'];
  schemeHistory: { scheme: string; year: string; amount: string; status: string }[] = [];
  meetingHistory: { date: string; agenda: string; outcome: string }[] = [];

  constructor(private visitorSearchService: VisitorSearchService) {}

  ngOnInit() {
    this.initializeDummyData();
  }

  initializeDummyData() {
    // Create dummy person results
    this.results = [
      {
        id: 1001,
        fullName: 'Dr. Aidashisha Lyngdoh',
        phoneNumber: '+91-9876543210',
        epicNumber: 'EKH1234567',
        designation: 'Medical Officer',
        district: 'East Khasi Hills',
        constituency: 'South Shillong',
        booth: 'EKH/234',
        village: 'Mawprem',
        briefProfile: 'Community health advocate with 15+ years of experience. Active in maternal health programs and village health initiatives.',
      },
      {
        id: 1002,
        fullName: 'Shri Balajied Syiemlieh',
        phoneNumber: '+91-8765432109',
        epicNumber: 'RBH2345678',
        designation: 'Farmer & Cooperative Leader',
        district: 'Ri Bhoi',
        constituency: 'Nongkrem',
        booth: 'RBH/567',
        village: 'Umsning',
        briefProfile: 'President of local agricultural cooperative. Pioneer in organic farming and cold storage initiatives.',
      },
      {
        id: 1003,
        fullName: 'Kong Evaristarisha Warjri',
        phoneNumber: '+91-7654321098',
        epicNumber: 'WKH3456789',
        designation: 'Self-Help Group Leader',
        district: 'West Khasi Hills',
        constituency: 'Mawshynrut',
        booth: 'WKH/890',
        village: 'Nongstoin',
        briefProfile: 'Women empowerment activist. Runs skill training programs in weaving and handicrafts for 200+ women.',
      },
      {
        id: 1004,
        fullName: 'Shri Tengrik M. Sangma',
        phoneNumber: '+91-6543210987',
        epicNumber: 'EGH4567890',
        designation: 'Village Headman (Nokma)',
        district: 'East Garo Hills',
        constituency: 'Williamnagar',
        booth: 'EGH/345',
        village: 'Samanda',
        briefProfile: 'Traditional leader advocating for road connectivity and infrastructure development in remote villages.',
      },
      {
        id: 1005,
        fullName: 'Dr. Wallambok Nongkhlaw',
        phoneNumber: '+91-5432109876',
        epicNumber: 'WJH5678901',
        designation: 'College Principal',
        district: 'West Jaintia Hills',
        constituency: 'Jowai',
        booth: 'WJH/678',
        village: 'Jowai Town',
        briefProfile: 'Educator focused on digital literacy and modern education infrastructure. Serving for 25+ years in government institutions.',
      },
    ];

    // Select first person by default and populate history
    this.selected = this.results[0];
    this.searched = true;
    this.populateHistory();
  }

  populateHistory() {
    if (!this.selected) {
      this.schemeHistory = [];
      this.meetingHistory = [];
      return;
    }

    // Populate scheme history based on selected person
    this.schemeHistory = [
      { scheme: 'CM Special Development Fund', year: '2023', amount: '₹8.5L', status: 'Approved' },
      { scheme: 'Chief Minister\'s Special Grant', year: '2022', amount: '₹5.0L', status: 'Completed' },
      { scheme: 'CM Care Fund', year: '2021', amount: '₹3.2L', status: 'Completed' },
    ];

    this.meetingHistory = [
      { date: '2024-02-15', agenda: 'Healthcare infrastructure improvement', outcome: 'Approved for mobile medical unit' },
      { date: '2023-11-08', agenda: 'Community health awareness program', outcome: 'Sanctioned ₹2.5L for awareness campaigns' },
      { date: '2023-06-22', agenda: 'Village road connectivity', outcome: 'Forwarded to PWD for survey' },
    ];
  }

  search() {
    this.searched = true;
    this.searching = true;
    this.selected = null;

    const phone = this.searchPhone.trim();
    const epic = this.searchEpic.trim();
    const name = this.searchName.trim();
    const district = this.searchDistrict.trim();

    // Try API first
    if (phone) {
      this.visitorSearchService.searchByPhone(phone).subscribe({
        next: p => {
          if (p) this.results = [p];
          this.searching = false;
        },
        error: () => {
          // Fallback to dummy data
          this.results = this.results.filter(r => r.phoneNumber.includes(phone));
          this.searching = false;
        }
      });
    } else if (epic) {
      this.visitorSearchService.searchByEpic(epic).subscribe({
        next: p => {
          if (p) this.results = [p];
          this.searching = false;
        },
        error: () => {
          // Fallback to dummy data
          this.results = this.results.filter(r => r.epicNumber.toUpperCase().includes(epic.toUpperCase()));
          this.searching = false;
        }
      });
    } else if (name) {
      this.visitorSearchService.searchByName(name).subscribe({
        next: res => {
          this.results = res;
          this.searching = false;
        },
        error: () => {
          // Fallback to dummy data
          this.results = this.results.filter(r => r.fullName.toLowerCase().includes(name.toLowerCase()));
          this.searching = false;
        }
      });
    } else if (district) {
      this.visitorSearchService.searchByDistrict(district).subscribe({
        next: res => {
          this.results = res;
          this.searching = false;
        },
        error: () => {
          // Fallback to dummy data
          this.results = this.results.filter(r => r.district === district);
          this.searching = false;
        }
      });
    } else {
      // Show all dummy data if no criteria
      this.initializeDummyData();
      this.searching = false;
    }
  }

  select(p: Visitor) {
    this.selected = p;
    this.populateHistory();
  }

  clearSearch() {
    this.searchPhone = '';
    this.searchEpic = '';
    this.searchName = '';
    this.searchDistrict = '';
    this.searched = false;
    this.initializeDummyData();
  }
}
