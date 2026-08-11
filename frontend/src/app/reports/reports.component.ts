import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, RouterLink, UIChart, TableModule, MatButtonModule, MatIconModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss'],
})
export class ReportsComponent implements OnInit {
  meetingsPerDay: any;
  approvalRatio: any;
  schemeWise: any;
  chartOpts: any;
  barChartOpts: any;

  topConstituencies = [
    { name: 'Ampati', total: 12, approved: 8, rejected: 2 },
    { name: 'Shillong East', total: 9, approved: 6, rejected: 1 },
    { name: 'Baghmara', total: 8, approved: 5, rejected: 2 },
    { name: 'Umsning', total: 7, approved: 4, rejected: 1 },
    { name: 'Tura', total: 11, approved: 7, rejected: 2 },
  ];

  constructor(public readonly auth: AuthService) {}

  get showAnalytics(): boolean {
    return !this.auth.hasRole('DEPARTMENT_ADMIN');
  }

  ngOnInit() {
    this.chartOpts = { plugins: { legend: { position: 'bottom' } }, responsive: true };
    this.barChartOpts = { ...this.chartOpts, scales: { x: { stacked: false }, y: { beginAtZero: true } } };

    this.meetingsPerDay = {
      labels: ['Mon','Tue','Wed','Thu','Fri','Sat'],
      datasets: [
        { label: 'Scheduled', data: [4,6,3,8,5,2], backgroundColor: '#1a237e' },
        { label: 'Completed', data: [3,5,3,7,4,2], backgroundColor: '#16a34a' },
      ]
    };

    this.approvalRatio = {
      labels: ['HCM Accepted','HCM Rejected','Snoozed','Pending','CMO Rejected'],
      datasets: [{ data: [62, 18, 10, 7, 3], backgroundColor: ['#16a34a','#dc2626','#f59e0b','#6b7280','#b45309'] }]
    };

    this.schemeWise = {
      labels: ['CMSDF','CMSG','CM Care','CM Connect','CM Elevate','Focus+'],
      datasets: [
        { label: 'Approved', data: [28,18,22,10,8,14], backgroundColor: '#16a34a' },
        { label: 'Pending', data: [12,10,4,7,5,6], backgroundColor: '#f59e0b' },
        { label: 'Rejected', data: [5,4,2,2,2,2], backgroundColor: '#dc2626' },
      ]
    };
  }
}
