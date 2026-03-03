import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuditLogService } from '../../services/audit-log.service';
import { AuditEntry } from '../../models';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { InputText } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-audit-trail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TableModule, Tag, InputText],
  templateUrl: './audit-trail.component.html',
  styleUrls: ['./audit-trail.component.scss'],
})
export class AuditTrailComponent implements OnInit {
  logs: AuditEntry[] = [];
  search = '';
  loading = false;

  constructor(private auditLogService: AuditLogService) {}

  ngOnInit() {
    this.loading = true;
    this.auditLogService.getAll(0, 100).subscribe({
      next: page => { this.logs = page.content; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get filtered() {
    return this.logs.filter(l =>
      !this.search ||
      l.performedBy.toLowerCase().includes(this.search.toLowerCase()) ||
      l.action.toLowerCase().includes(this.search.toLowerCase()) ||
      l.entityType.toLowerCase().includes(this.search.toLowerCase())
    );
  }

  getSeverity(action: string) {
    if (action.includes('DELETE') || action.includes('REJECT')) return 'danger';
    if (action.includes('UPDATE') || action.includes('CHANGE')) return 'warn';
    if (action.includes('APPROVED') || action.includes('LOGIN')) return 'success';
    return 'info';
  }
}
