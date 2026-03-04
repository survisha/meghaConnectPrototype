import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

@Component({
  selector: 'app-pending-followups',
  standalone: true,
  imports: [CommonModule, RouterLink, TableModule, Tag],
  templateUrl: './pending-followups.component.html',
  styleUrls: ['./pending-followups.component.scss'],
})
export class PendingFollowupsComponent implements OnInit {
  followups: any[] = [];

  ngOnInit() {
    this.followups = [
      { id: 1, applicationId: 'MC-2024-00001', applicant: 'Ramsing Marak', direction: 'Expedite CMSDF approval for community hall', color: 'GREEN', department: 'Planning Dept', deadline: '01 Apr 2024', daysLeft: 17, status: 'Under Review', officer: 'Dy Secy Planning' },
      { id: 2, applicationId: 'MC-2024-00005', applicant: 'Monika Sangma', direction: 'Forward medical case to CMO Health', color: 'GREEN', department: 'Health Dept', deadline: '20 Mar 2024', daysLeft: -2, status: 'Overdue', officer: 'Dir Health' },
      { id: 3, applicationId: 'MC-2024-00003', applicant: 'Bijoy Momin', direction: 'Forward to concerned office', color: 'YELLOW', department: 'Finance Dept', deadline: '25 Mar 2024', daysLeft: 10, status: 'In Progress', officer: 'Dy Secy Finance' },
      { id: 4, applicationId: 'MC-2024-00007', applicant: 'Bilash Marak', direction: 'Expedite road construction file', color: 'GREEN', department: 'PWD', deadline: '15 Apr 2024', daysLeft: 31, status: 'Not Started', officer: 'SE PWD' },
    ];
  }

  getDirClass(c: string) { return { GREEN: 'dir-green', YELLOW: 'dir-yellow', BLUE: 'dir-blue' }[c] ?? ''; }
  getSeverity(s: string): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' | undefined { return s === 'Overdue' ? 'danger' : s === 'In Progress' ? 'warn' : s === 'Under Review' ? 'info' : 'secondary'; }
}
