import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SchemeService } from '../../services/scheme.service';
import { MockDataService } from '../../services/mock-data.service';
import { SchemeApplication, SchemeType } from '../../models';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-scheme-list',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterLink, 
    MatTableModule, 
    MatButtonModule, 
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatDialogModule,
    MatDividerModule
  ],
  templateUrl: './scheme-list.component.html',
  styleUrls: ['./scheme-list.component.scss'],
})
export class SchemeListComponent implements OnInit {
  schemes: SchemeApplication[] = [];
  selected: SchemeApplication | null = null;
  showDetail = false;
  filterScheme = '';
  loading = false;
  displayedColumns: string[] = ['applicant', 'scheme', 'project', 'category', 'estCost', 'hcmApproved', 'status', 'actions'];

  schemeOptions = [
    { label: 'All Schemes', value: '' },
    { label: 'CMSDF', value: 'CMSDF' },
    { label: 'CMSG', value: 'CMSG' },
    { label: 'CM Care', value: 'CM_CARE' },
    { label: 'CM Connect', value: 'CM_CONNECT' },
    { label: 'CM Elevate', value: 'CM_ELEVATE' },
    { label: 'Focus+', value: 'FOCUS_PLUS' },
  ];

  schemeStats = [
    { name: 'CMSDF', total: 0, approved: 0, pending: 0, rejected: 0, budget: '–' },
    { name: 'CMSG', total: 0, approved: 0, pending: 0, rejected: 0, budget: '–' },
    { name: 'CM Care', total: 0, approved: 0, pending: 0, rejected: 0, budget: '–' },
    { name: 'CM Connect', total: 0, approved: 0, pending: 0, rejected: 0, budget: '–' },
    { name: 'CM Elevate', total: 0, approved: 0, pending: 0, rejected: 0, budget: '–' },
  ];

  constructor(
    private schemeService: SchemeService,
    private mockDataService: MockDataService
  ) {}

  ngOnInit() {
    this.loading = true;
    this.schemeService.getAllApplications({ page: 0, size: 100 }).subscribe({
      next: (res: any) => {
        const content = res.content ?? res ?? [];
        
        // If API returns empty or no data, use dummy data for demo
        if (!content || content.length === 0) {
          console.log('[SchemeListComponent] API returned no data, using dummy data for demo');
          this.schemes = this.mockDataService.schemeApplications;
        } else {
          this.schemes = content;
        }
        
        this.calculateStats();
        this.loading = false;
      },
      error: (err) => { 
        console.error('[SchemeListComponent] API call failed, using dummy data for demo:', err);
        this.schemes = this.mockDataService.schemeApplications;
        this.calculateStats();
        this.loading = false;
      }
    });
  }

  calculateStats() {
    // Calculate statistics from schemes data
    const stats = new Map<string, { total: number; approved: number; pending: number; rejected: number; totalCost: number }>();
    
    this.schemes.forEach(scheme => {
      const schemeName = this.formatSchemeName(scheme.schemeType);
      if (!stats.has(schemeName)) {
        stats.set(schemeName, { total: 0, approved: 0, pending: 0, rejected: 0, totalCost: 0 });
      }
      
      const stat = stats.get(schemeName)!;
      stat.total++;
      
      if (scheme.status === 'APPROVED' || scheme.hcmDecision === 'APPROVED') {
        stat.approved++;
        stat.totalCost += scheme.hcmApprovedCost || scheme.estimatedCost;
      } else if (scheme.status === 'REJECTED' || scheme.hcmDecision === 'REJECTED') {
        stat.rejected++;
      } else {
        stat.pending++;
      }
    });
    
    // Update schemeStats with calculated values
    this.schemeStats.forEach(stat => {
      const data = stats.get(stat.name);
      if (data) {
        stat.total = data.total;
        stat.approved = data.approved;
        stat.pending = data.pending;
        stat.rejected = data.rejected;
        stat.budget = data.totalCost > 0 ? `₹${(data.totalCost / 10000000).toFixed(2)}Cr` : '–';
      }
    });
  }

  formatSchemeName(schemeType: string): string {
    if (schemeType === 'CM_CARE') return 'CM Care';
    if (schemeType === 'CM_CONNECT') return 'CM Connect';
    if (schemeType === 'CM_ELEVATE') return 'CM Elevate';
    if (schemeType === 'FOCUS_PLUS') return 'Focus+';
    return schemeType;
  }

  get filtered() {
    return this.schemes.filter(s => !this.filterScheme || s.schemeType === this.filterScheme);
  }

  view(s: SchemeApplication) { this.selected = s; this.showDetail = true; }

  getSeverity(status: string): string {
    const m: Record<string, string> = { 
      APPROVED: 'success', 
      HCM_PENDING: 'danger', 
      SCHEDULED: 'warn', 
      SUBMITTED: 'info', 
      REJECTED: 'danger' 
    };
    return m[status] ?? 'info';
  }

  getStatusClass(status: string): string {
    const severity = this.getSeverity(status);
    return `status-badge status-${severity}`;
  }

  getSchemeBadgeColor(type: string) {
    const m: Record<string,string> = { 
      CMSDF:'#1565c0', 
      CMSG:'#0288d1', 
      CM_CARE:'#2e7d32', 
      CM_CONNECT:'#4527a0', 
      CM_ELEVATE:'#f57f17', 
      FOCUS_PLUS:'#c62828' 
    };
    return m[type] ?? '#374151';
  }
}
