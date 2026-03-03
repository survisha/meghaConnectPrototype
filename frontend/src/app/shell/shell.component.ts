import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models';
import { Button } from 'primeng/button';
import { Badge, BadgeDirective } from 'primeng/badge';
import { Tag } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';

interface MenuItem { label: string; icon: string; route?: string; children?: MenuItem[]; expanded?: boolean; roles?: UserRole[]; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, Button, Badge, BadgeDirective, Tag, Toast, ConfirmDialog],
  providers: [MessageService, ConfirmationService],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit {
  sidebarOpen = true;
  menu: MenuItem[] = [];

  private ALL_MENU: MenuItem[] = [
    { label: 'Dashboard', icon: 'pi-home', route: '/dashboard',
      roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
    { label: 'My Portal', icon: 'pi-user', route: '/visitor',
      roles: ['PUBLIC'] },
    { label: 'Calendar / Schedule', icon: 'pi-calendar', route: '/scheduling',
      roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
    {
      label: 'Appointments', icon: 'pi-users', expanded: false,
      roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER','DATA_ENTRY_OPERATOR','PUBLIC'],
      children: [
        { label: 'All Appointments', icon: 'pi-list', route: '/appointments',
          roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER','DATA_ENTRY_OPERATOR'] },
        { label: 'New Appointment', icon: 'pi-plus-circle', route: '/appointments/new',
          roles: ['ADMIN','SAIDUL_OSD','DATA_ENTRY_OPERATOR','PUBLIC'] },
        { label: 'Walk-in Counter', icon: 'pi-sign-in', route: '/appointments/walkin',
          roles: ['ADMIN','SAIDUL_OSD','DATA_ENTRY_OPERATOR'] },
      ]
    },
    {
      label: 'CM Schemes', icon: 'pi-briefcase', expanded: false,
      roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER','PUBLIC'],
      children: [
        { label: 'All Applications', icon: 'pi-list', route: '/schemes',
          roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
        { label: 'New Application', icon: 'pi-file-edit', route: '/schemes/apply',
          roles: ['ADMIN','SAIDUL_OSD','PUBLIC'] },
      ]
    },
    { label: 'Grievances', icon: 'pi-comments', route: '/grievances',
      roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER','DATA_ENTRY_OPERATOR','PUBLIC'] },
    { label: 'Public Identification', icon: 'pi-id-card', route: '/identify',
      roles: ['HCM','ADMIN','SAIDUL_OSD','DATA_ENTRY_OPERATOR'] },
    {
      label: 'Reports', icon: 'pi-chart-bar', expanded: false,
      roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'],
      children: [
        { label: 'Analytics', icon: 'pi-chart-pie', route: '/reports',
          roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
        { label: 'Scheme Heatmap', icon: 'pi-map', route: '/reports/heatmap',
          roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
        { label: 'Pending Follow-ups', icon: 'pi-clock', route: '/reports/followups',
          roles: ['HCM','ADMIN','SAIDUL_OSD','APPROVER_JT_SECY','CMO_OFFICER'] },
        { label: 'Audit Trail', icon: 'pi-history', route: '/reports/audit',
          roles: ['ADMIN'] },
      ]
    },
    { label: 'User Management', icon: 'pi-shield', route: '/admin/users',
      roles: ['HCM','ADMIN','SAIDUL_OSD'] },
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
      HCM:'HCM', ADMIN:'Admin', SAIDUL_OSD:'Saidul OSD',
      APPROVER_JT_SECY:'Jt Secretary', CMO_OFFICER:'CMO Officer',
      DATA_ENTRY_OPERATOR:'DEO', PUBLIC:'Public'
    };
    return map[r] ?? r;
  }
}
