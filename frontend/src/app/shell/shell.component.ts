import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models';
import { Toast } from 'primeng/toast';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';

interface MenuItem { labelKey: string; icon: string; route?: string; children?: MenuItem[]; expanded?: boolean; roles?: UserRole[]; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, Toast, ConfirmDialog, TranslateModule, LanguageSelectorComponent],
  providers: [MessageService, ConfirmationService],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit {
  sidebarOpen = true;
  menu: MenuItem[] = [];

  private ALL_MENU: MenuItem[] = [
    { labelKey: 'DASHBOARD', icon: 'pi-home', route: '/dashboard',
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
    { labelKey: 'MY_PORTAL', icon: 'pi-user', route: '/visitor',
      roles: ['PUBLIC'] },
    { labelKey: 'CALENDAR_SCHEDULE', icon: 'pi-calendar', route: '/scheduling',
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
    { labelKey: 'HCM_ACTIONS', icon: 'pi-hand-open', route: '/hcm/appointments',
      roles: ['HCM'] },
    {
      labelKey: 'APPOINTMENTS', icon: 'pi-users', expanded: false,
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR','PUBLIC'],
      children: [
        { labelKey: 'ALL_APPOINTMENTS', icon: 'pi-list', route: '/appointments',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
        { labelKey: 'NEW_APPOINTMENT', icon: 'pi-plus-circle', route: '/appointments/new',
          roles: ['ADMIN','OSD','DATA_ENTRY_OPERATOR','PUBLIC'] },
        { labelKey: 'WALKIN_COUNTER', icon: 'pi-sign-in', route: '/appointments/walkin',
          roles: ['ADMIN','OSD','DATA_ENTRY_OPERATOR'] },
        { labelKey: 'APPROVER_REVIEW', icon: 'pi-check-square', route: '/approver',
          roles: ['HCM','ADMIN','OSD','APPROVER'] },
        { labelKey: 'CMO_MODERATION', icon: 'pi-filter', route: '/cmo-moderation',
          roles: ['HCM','ADMIN','OSD','CMO_OFFICER'] },
      ]
    },
    {
      labelKey: 'CM_SCHEMES', icon: 'pi-briefcase', expanded: false,
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','PUBLIC'],
      children: [
        { labelKey: 'ALL_APPLICATIONS', icon: 'pi-list', route: '/schemes',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'NEW_APPLICATION', icon: 'pi-file-edit', route: '/schemes/apply',
          roles: ['ADMIN','OSD','PUBLIC'] },
      ]
    },
    { labelKey: 'GRIEVANCES', icon: 'pi-comments', route: '/grievances',
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER','DATA_ENTRY_OPERATOR','PUBLIC'] },
    { labelKey: 'REGISTER_VISITOR', icon: 'pi-user-plus', route: '/deo/register-visitor',
      roles: ['DATA_ENTRY_OPERATOR'] },
    { labelKey: 'PUBLIC_IDENTIFICATION', icon: 'pi-id-card', route: '/identify',
      roles: ['HCM','ADMIN','OSD','DATA_ENTRY_OPERATOR'] },
    {
      labelKey: 'REPORTS', icon: 'pi-chart-bar', expanded: false,
      roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'],
      children: [
        { labelKey: 'ANALYTICS', icon: 'pi-chart-pie', route: '/reports',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'SCHEME_HEATMAP', icon: 'pi-map', route: '/reports/heatmap',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'PENDING_FOLLOWUPS', icon: 'pi-clock', route: '/reports/followups',
          roles: ['HCM','ADMIN','OSD','APPROVER','CMO_OFFICER'] },
        { labelKey: 'AUDIT_TRAIL', icon: 'pi-history', route: '/reports/audit',
          roles: ['ADMIN'] },
      ]
    },
    { labelKey: 'USER_MANAGEMENT', icon: 'pi-shield', route: '/admin/users',
      roles: ['HCM','ADMIN','OSD'] },
    { labelKey: 'SCHEME_MANAGEMENT', icon: 'pi-sliders-h', route: '/admin/schemes',
      roles: ['ADMIN'] },
    { labelKey: 'APPOINTMENT_TYPES', icon: 'pi-calendar', route: '/admin/appointment-types',
      roles: ['ADMIN'] },
  ];

  constructor(public auth: AuthService, private router: Router) {}

  ngOnInit() {
    this.buildMenu();
    if (this.auth.hasRole('PUBLIC')) {
      this.router.navigate(['/visitor']);
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
