import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

// Angular Material
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';

type ChipSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

@Component({
  selector: 'app-pending-followups',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatCardModule
  ],
  templateUrl: './pending-followups.component.html',
  styleUrls: ['./pending-followups.component.scss'],
})
export class PendingFollowupsComponent implements OnInit {
  followups: any[] = [];
  displayedColumns: string[] = [
    'applicationId', 'applicant', 'direction', 'color', 
    'department', 'officer', 'deadline', 'daysLeft', 'status'
  ];

  // KPI stats
  get overdueCount(): number {
    return this.followups.filter(f => f.daysLeft < 0).length;
  }

  get inProgressCount(): number {
    return this.followups.filter(f => f.status === 'In Progress').length;
  }

  get notStartedCount(): number {
    return this.followups.filter(f => f.status === 'Not Started').length;
  }

  ngOnInit() {
    this.followups = [
      { 
        id: 1, 
        applicationId: 'MC-2024-00142', 
        applicant: 'Shri Ramsing Marak', 
        direction: 'Expedite CMSDF approval for community hall construction worth ₹35 lakhs. Submit compliance report within 15 days.', 
        color: 'GREEN', 
        department: 'Planning Department', 
        deadline: '2026-04-18', 
        daysLeft: 17, 
        status: 'Under Review', 
        officer: 'Dy Secretary (Planning)' 
      },
      { 
        id: 2, 
        applicationId: 'MC-2024-00118', 
        applicant: 'Kong Monika Sangma', 
        direction: 'Forward medical equipment request to CMO Health for emergency approval. High priority case requiring immediate attention.', 
        color: 'GREEN', 
        department: 'Health & Family Welfare', 
        deadline: '2026-03-28', 
        daysLeft: -4, 
        status: 'Overdue', 
        officer: 'Director (Health Services)' 
      },
      { 
        id: 3, 
        applicationId: 'MC-2024-00167', 
        applicant: 'Shri Bijoy Momin', 
        direction: 'Review financial assistance application for small business loan. Verify documents and forward to concerned bank.', 
        color: 'YELLOW', 
        department: 'Finance Department', 
        deadline: '2026-04-11', 
        daysLeft: 10, 
        status: 'In Progress', 
        officer: 'Dy Secretary (Finance)' 
      },
      { 
        id: 4, 
        applicationId: 'MC-2024-00089', 
        applicant: 'Shri Bilash Marak', 
        direction: 'Expedite road construction file for 12km village connectivity road. Budget allocation of ₹145 crores pending approval.', 
        color: 'GREEN', 
        department: 'Public Works Department', 
        deadline: '2026-05-01', 
        daysLeft: 30, 
        status: 'Not Started', 
        officer: 'Superintending Engineer (PWD)' 
      },
      { 
        id: 5, 
        applicationId: 'MC-2024-00203', 
        applicant: 'Dr. Carness Lyngdoh', 
        direction: 'Coordinate with NRHM for mobile medical unit deployment. Submit detailed project report with cost estimates.', 
        color: 'GREEN', 
        department: 'Health & Family Welfare', 
        deadline: '2026-04-08', 
        daysLeft: 7, 
        status: 'In Progress', 
        officer: 'Joint Director (NHM)' 
      },
      { 
        id: 6, 
        applicationId: 'MC-2024-00156', 
        applicant: 'Kong Evaristarisha Warjri', 
        direction: 'Process SHG loan application through DRDA. Training program funding for 200+ women members.', 
        color: 'BLUE', 
        department: 'Rural Development', 
        deadline: '2026-03-25', 
        daysLeft: -7, 
        status: 'Overdue', 
        officer: 'Project Director (DRDA)' 
      },
      { 
        id: 7, 
        applicationId: 'MC-2024-00134', 
        applicant: 'Shri Tengrik M. Sangma', 
        direction: 'Inspect village infrastructure and submit feasibility report for power supply extension project.', 
        color: 'YELLOW', 
        department: 'Power Department', 
        deadline: '2026-04-15', 
        daysLeft: 14, 
        status: 'Under Review', 
        officer: 'Executive Engineer (Power)' 
      },
      { 
        id: 8, 
        applicationId: 'MC-2024-00178', 
        applicant: 'Dr. Wallambok Nongkhlaw', 
        direction: 'Approve library expansion and computer lab setup for government college. Budget: ₹22 lakhs under state plan.', 
        color: 'GREEN', 
        department: 'Education Department', 
        deadline: '2026-04-22', 
        daysLeft: 21, 
        status: 'Not Started', 
        officer: 'Director (Higher Education)' 
      },
    ];
  }

  getDirClass(c: string) { 
    return { 
      GREEN: 'dir-green', 
      YELLOW: 'dir-yellow', 
      BLUE: 'dir-blue' 
    }[c] ?? ''; 
  }

  getSeverity(s: string): ChipSeverity { 
    if (s === 'Overdue') return 'danger';
    if (s === 'In Progress') return 'warn';
    if (s === 'Under Review') return 'info';
    return 'secondary';
  }

  formatDeadline(deadline: string): string {
    const date = new Date(deadline);
    return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
