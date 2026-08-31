import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../services/auth.service';
import { ReportAnalytics, ReportAnalyticsService } from '../services/report-analytics.service';

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
  loading = true;
  errorMsg = '';

  topConstituencies: Array<{ name: string; total: number; approved: number; rejected: number }> = [];
  constructor(public readonly auth: AuthService, private readonly analytics: ReportAnalyticsService) {}

  get showAnalytics(): boolean {
    return !this.auth.hasRole('DEPARTMENT_ADMIN');
  }

  ngOnInit() {
    this.chartOpts = { plugins: { legend: { position: 'bottom' } }, responsive: true };
    this.barChartOpts = { ...this.chartOpts, scales: { x: { stacked: false }, y: { beginAtZero: true } } };

    this.analytics.load().subscribe({
      next: data => { this.applyAnalytics(data); this.loading = false; },
      error: () => { this.errorMsg = 'Unable to load report data.'; this.applyAnalytics({ meetingDates: [], statusCounts: [], topConstituencies: [], schemeDistricts: [] }); this.loading = false; }
    });
  }

  private applyAnalytics(data: ReportAnalytics): void {
    this.meetingsPerDay = {
      labels: data.meetingDates.map(row => row.meetingDate),
      datasets: [
        { label: 'Scheduled', data: data.meetingDates.map(row => Number(row.scheduled)), backgroundColor: '#1a237e' },
        { label: 'Completed', data: data.meetingDates.map(row => Number(row.completed)), backgroundColor: '#16a34a' },
      ]
    };
    this.approvalRatio = {
      labels: data.statusCounts.map(row => row.status.replaceAll('_', ' ')),
      datasets: [{ data: data.statusCounts.map(row => Number(row.total)), backgroundColor: data.statusCounts.map((_, index) => ['#16a34a','#dc2626','#f59e0b','#6b7280','#b45309'][index % 5]) }]
    };
    const schemes = [...new Set(data.schemeDistricts.map(row => row.scheme))];
    this.schemeWise = {
      labels: schemes.map(code => code.replaceAll('_', ' ')),
      datasets: [
        { label: 'Approved', data: schemes.map(code => this.schemeTotal(data, code, 'approved')), backgroundColor: '#16a34a' },
        { label: 'Pending', data: schemes.map(code => this.schemeTotal(data, code, 'total') - this.schemeTotal(data, code, 'approved') - this.schemeTotal(data, code, 'rejected')), backgroundColor: '#f59e0b' },
        { label: 'Rejected', data: schemes.map(code => this.schemeTotal(data, code, 'rejected')), backgroundColor: '#dc2626' },
      ]
    };
    this.topConstituencies = data.topConstituencies;
  }

  private schemeTotal(data: ReportAnalytics, scheme: string, field: 'total' | 'approved' | 'rejected'): number {
    return data.schemeDistricts.filter(row => row.scheme === scheme).reduce((sum, row) => sum + Number(row[field]), 0);
  }
}
