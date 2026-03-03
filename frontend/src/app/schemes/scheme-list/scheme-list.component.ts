import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SchemeService } from '../../services/scheme.service';
import { SchemeApplication } from '../../models';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { Dialog } from 'primeng/dialog';
import { Divider } from 'primeng/divider';

@Component({
  selector: 'app-scheme-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TableModule, Tag, Select, Dialog, Divider],
  templateUrl: './scheme-list.component.html',
  styleUrls: ['./scheme-list.component.scss'],
})
export class SchemeListComponent implements OnInit {
  schemes: SchemeApplication[] = [];
  selected: SchemeApplication | null = null;
  showDetail = false;
  filterScheme = '';
  loading = false;

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

  constructor(private schemeService: SchemeService) {}

  ngOnInit() {
    this.loading = true;
    this.schemeService.getAllApplications({ page: 0, size: 100 }).subscribe({
      next: (res: any) => {
        this.schemes = res.content ?? res ?? [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get filtered() {
    return this.schemes.filter(s => !this.filterScheme || s.schemeType === this.filterScheme);
  }

  view(s: SchemeApplication) { this.selected = s; this.showDetail = true; }

  getSeverity(status: string): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | null | undefined {
    const m: Record<string,'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | null | undefined> = { APPROVED:'success', HCM_PENDING:'danger', SCHEDULED:'warn', SUBMITTED:'info', REJECTED:'danger' };
    return m[status] ?? 'info';
  }

  getSchemeBadgeColor(type: string) {
    const m: Record<string,string> = { CMSDF:'#1565c0', CMSG:'#0288d1', CM_CARE:'#2e7d32', CM_CONNECT:'#4527a0', CM_ELEVATE:'#f57f17', FOCUS_PLUS:'#c62828' };
    return m[type] ?? '#374151';
  }
}
