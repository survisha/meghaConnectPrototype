import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../services/mock-data.service';
import { SchemeApplication } from '../../models';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { Dialog } from 'primeng/dialog';
import { Divider } from 'primeng/divider';
import { UIChart } from 'primeng/chart';

@Component({
  selector: 'app-scheme-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Button, TableModule, Tag, Select, Dialog, Divider, UIChart],
  templateUrl: './scheme-list.component.html',
  styleUrls: ['./scheme-list.component.scss'],
})
export class SchemeListComponent implements OnInit {
  schemes: SchemeApplication[] = [];
  selected: SchemeApplication | null = null;
  showDetail = false;
  filterScheme = '';

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
    { name: 'CMSDF', total: 45, approved: 28, pending: 12, rejected: 5, budget: 'Rs. 45.2L' },
    { name: 'CMSG', total: 32, approved: 18, pending: 10, rejected: 4, budget: 'Rs. 12.8L' },
    { name: 'CM Care', total: 28, approved: 22, pending: 4, rejected: 2, budget: 'Rs. 28.0L' },
    { name: 'CM Connect', total: 19, approved: 10, pending: 7, rejected: 2, budget: 'Rs. 5.6L' },
    { name: 'CM Elevate', total: 15, approved: 8, pending: 5, rejected: 2, budget: 'Rs. 9.2L' },
  ];

  constructor(public mock: MockDataService) {}
  ngOnInit() { this.schemes = this.mock.schemeApplications; }

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
