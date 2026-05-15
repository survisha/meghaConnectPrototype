import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';
import { MatIconModule } from '@angular/material/icon';

interface MenuItem { labelKey: string; icon: string; route?: string; children?: MenuItem[]; expanded?: boolean; roles?: UserRole[]; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, TranslateModule, LanguageSelectorComponent, MatIconModule],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit {
  sidebarOpen = window.innerWidth > 768;
  menu: MenuItem[] = [];

  private ALL_MENU: MenuItem[] = [
    { labelKey: 'DASHBOARD', icon: 'dashboard', route: '/dashboard',
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
    { labelKey: 'MY_PORTAL', icon: 'person', route: '/visitor',
      roles: ['PUBLIC'] },
    { labelKey: 'CALENDAR_SCHEDULE', icon: 'event', route: '/scheduling',
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
    { labelKey: 'HCM_ACTIONS', icon: 'task_alt', route: '/hcm/appointments',
      roles: ['HCM'] },
    {
      labelKey: 'APPOINTMENTS', icon: 'groups', expanded: false,
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR','PUBLIC'],
      children: [
        { labelKey: 'ALL_APPOINTMENTS', icon: 'list', route: '/appointments',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
        { labelKey: 'NEW_APPOINTMENT', icon: 'add_circle', route: '/appointments/new',
          roles: ['ADMIN','OSD','PUBLIC'] },
        { labelKey: 'WALKIN_COUNTER', icon: 'login', route: '/appointments/walkin',
          roles: ['ADMIN','OSD','DATA_ENTRY_OPERATOR'] },
      ]
    },
    {
      labelKey: 'CM_SCHEMES', icon: 'work', expanded: false,
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','PUBLIC'],
      children: [
        { labelKey: 'ALL_APPLICATIONS', icon: 'list', route: '/schemes',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'NEW_APPLICATION', icon: 'description', route: '/schemes/apply',
          roles: ['ADMIN','OSD','PUBLIC'] },
      ]
    },
    { labelKey: 'GRIEVANCES', icon: 'chat', route: '/grievances',
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR','PUBLIC'] },
    { labelKey: 'REGISTER_VISITOR', icon: 'person_add', route: '/deo/register-visitor',
      roles: ['DATA_ENTRY_OPERATOR'] },
    { labelKey: 'PUBLIC_IDENTIFICATION', icon: 'badge', route: '/identify',
      roles: ['HCM','ADMIN','OSD','DATA_ENTRY_OPERATOR'] },
    {
      labelKey: 'REPORTS', icon: 'bar_chart', expanded: false,
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'],
      children: [
        { labelKey: 'ANALYTICS', icon: 'pie_chart', route: '/reports',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'SCHEME_HEATMAP', icon: 'map', route: '/reports/heatmap',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'PENDING_FOLLOWUPS', icon: 'schedule', route: '/reports/followups',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'AUDIT_TRAIL', icon: 'history', route: '/reports/audit',
          roles: ['ADMIN'] },
      ]
    },
    { labelKey: 'USER_MANAGEMENT', icon: 'shield', route: '/admin/users',
      roles: ['HCM','ADMIN','OSD'] },
    { labelKey: 'SCHEME_MANAGEMENT', icon: 'tune', route: '/admin/schemes',
      roles: ['ADMIN'] },
    { labelKey: 'APPOINTMENT_TYPES', icon: 'event', route: '/admin/appointment-types',
      roles: ['ADMIN'] },
  ];

  constructor(public auth: AuthService, private router: Router) {}

  ngOnInit() {
    this.buildMenu();
    if (this.auth.hasRole('PUBLIC')) {
      this.router.navigate(['/visitor']);
    }
  }

  @HostListener('window:resize')
  onWindowResize() {
    if (window.innerWidth > 768) {
      this.sidebarOpen = true;
    }
  }

  private buildMenu() {
    const role = this.auth.user()?.role;
    if (!role) return;
    this.menu = this.ALL_MENU
      .filter(item => !item.roles || item.roles.includes(role))
      .map(item => ({
        ...item,
        children: item.children
          ? item.children.filter(c => !c.roles || c.roles.includes(role))
          : undefined,
      }))
      .filter(item => !item.children || item.children.length > 0);
  }

  toggle(item: MenuItem) { item.expanded = !item.expanded; }

  navigate(route: string) { this.router.navigate([route]); }

  logout() { this.auth.logout(); }

  get roleLabel() {
    const r = this.auth.user()?.role ?? '';
    const map: Record<string,string> = {
      HCM:'HCM', ADMIN:'Admin', OSD:'OSD',
      APPROVER:'Approver', CMO_OFFICER:'CMO Officer',
      DATA_ENTRY_OPERATOR:'DEO', PUBLIC:'Public'
    };
    return map[r] ?? r;
  }
}
