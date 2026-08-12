import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';
import { MatIconModule } from '@angular/material/icon';
import { AccessControlService, AppFeature } from '../services/access-control.service';

interface MenuItem { labelKey: string; icon: string; route?: string; externalUrl?: string; children?: MenuItem[]; expanded?: boolean; roles?: UserRole[]; feature?: AppFeature; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, TranslateModule, LanguageSelectorComponent, MatIconModule],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit {
  sidebarOpen = false;
  private sidebarHoverOpen = false;
  menu: MenuItem[] = [];

  private ALL_MENU: MenuItem[] = [
    { labelKey: 'DASHBOARD', icon: 'dashboard', route: '/dashboard',
      feature: 'dashboard' },
    { labelKey: 'DEPARTMENTS', icon: 'apartment', route: '/admin/departments',
      roles: ['SUPER_ADMIN'] },
    { labelKey: 'DEPARTMENT_REQUESTS', icon: 'domain_add', route: '/admin/department-requests',
      roles: ['SUPER_ADMIN'] },
    { labelKey: 'MY_PORTAL', icon: 'person', route: '/visitor',
      roles: ['PUBLIC'] },
    { labelKey: 'CALENDAR_SCHEDULE', icon: 'event', route: '/scheduling',
      feature: 'calendar' },
    { labelKey: 'HCM_ACTIONS', icon: 'task_alt', route: '/hcm/appointments',
      roles: ['HCM','APPROVER','ADMIN'] },
    {
      labelKey: 'APPOINTMENTS', icon: 'groups', expanded: false,
      feature: 'appointments',
      children: [
        { labelKey: 'ALL_APPOINTMENTS', icon: 'list', route: '/appointments',
          feature: 'appointments' },
        { labelKey: 'NEW_APPOINTMENT', icon: 'add_circle', route: '/appointments/new',
          roles: ['ADMIN','APPROVER','PUBLIC'] },
        { labelKey: 'WALKIN_COUNTER', icon: 'login', route: '/appointments/walkin',
          feature: 'walkIn' },
      ]
    },
    {
      labelKey: 'CM_SCHEMES', icon: 'work', expanded: false,
      roles: ['HCM','ADMIN','APPROVER','PUBLIC'],
      children: [
        { labelKey: 'ALL_APPLICATIONS', icon: 'list', route: '/schemes',
          roles: ['HCM','ADMIN','APPROVER'] },
        { labelKey: 'NEW_APPLICATION', icon: 'description', route: '/schemes/apply',
          roles: ['ADMIN','APPROVER','PUBLIC'] },
      ]
    },
    { labelKey: 'GRIEVANCES', icon: 'chat', route: '/grievances',
      roles: ['HCM','ADMIN','APPROVER','DEO','PUBLIC'] },
    { labelKey: 'REGISTER_VISITOR', icon: 'person_add', route: '/deo/register-visitor',
      feature: 'registerVisitor' },
    { labelKey: 'PUBLIC_IDENTIFICATION', icon: 'badge', route: '/identify',
      feature: 'publicIdentification' },
    {
      labelKey: 'REPORTS', icon: 'bar_chart', expanded: false,
      feature: 'reports',
      children: [
        { labelKey: 'COMPLETED_APPOINTMENTS', icon: 'task_alt', route: '/completed-appointments',
          feature: 'completedAppointments' },
        { labelKey: 'REJECTED_APPOINTMENTS', icon: 'cancel', route: '/rejected-appointments',
          feature: 'rejectedAppointments' },
        { labelKey: 'ANALYTICS', icon: 'pie_chart', route: '/reports',
          roles: ['HCM','ADMIN','APPROVER'] },
        { labelKey: 'SCHEME_HEATMAP', icon: 'map', route: '/reports/heatmap',
          roles: ['HCM','ADMIN','APPROVER'] },
        { labelKey: 'AUDIT_TRAIL', icon: 'history', route: '/reports/audit',
          feature: 'auditTrail' },
      ]
    },
    { labelKey: 'USER_MANAGEMENT', icon: 'shield', route: '/admin/users',
      feature: 'userManagement' },
    { labelKey: 'SCHEME_MANAGEMENT', icon: 'tune', route: '/admin/schemes',
      feature: 'schemeManagement' },
    { labelKey: 'APPOINTMENT_TYPES', icon: 'event', route: '/admin/appointment-types',
      feature: 'appointmentTypes' },
    { labelKey: 'TECHNICAL_MONITORING', icon: 'monitor_heart', externalUrl: '/grafana/',
      roles: ['SUPER_ADMIN'] },
  ];

  constructor(public auth: AuthService, private router: Router, private access: AccessControlService) {}

  ngOnInit() {
    this.buildMenu();
    if (this.auth.hasRole('PUBLIC')) {
      this.router.navigate(['/visitor']);
    }
  }

  @HostListener('window:resize')
  onWindowResize() {
    if (window.innerWidth > 768) {
      this.sidebarHoverOpen = false;
    } else {
      this.sidebarOpen = false;
      this.sidebarHoverOpen = false;
    }
  }

  toggleSidebar() {
    this.sidebarHoverOpen = false;
    this.sidebarOpen = !this.sidebarOpen;
  }

  openSidebarOnHover() {
    if (window.innerWidth <= 768 || this.sidebarOpen) return;
    this.sidebarHoverOpen = true;
    this.sidebarOpen = true;
  }

  closeSidebarAfterHover() {
    if (!this.sidebarHoverOpen) return;
    this.sidebarHoverOpen = false;
    this.sidebarOpen = false;
  }

  private buildMenu() {
    const role = this.auth.user()?.role;
    if (!role) return;
    this.menu = this.ALL_MENU
      .filter(item => this.isAllowed(item, role))
      .map(item => ({
        ...item,
        children: item.children
          ? item.children.filter(c => this.isAllowed(c, role))
          : undefined,
      }))
      .filter(item => !item.children || item.children.length > 0);
  }

  private isAllowed(item: MenuItem, role: UserRole): boolean {
    return (!item.feature || this.access.can(item.feature)) && (!item.roles || item.roles.includes(role));
  }

  toggle(item: MenuItem) { item.expanded = !item.expanded; }

  navigate(route: string) { this.router.navigate([route]); }

  logout() { this.auth.logout(); }

  get roleLabel() {
    const r = this.auth.user()?.role ?? '';
    const map: Record<string,string> = {
      HCM:'HCM', ADMIN:'Admin', APPROVER:'Approver',
      SUPER_ADMIN:'Super Admin', DEPARTMENT_ADMIN:'Department Admin', DEO:'DEO',
      DEPARTMENT_PA:'Department PA', HEAD_DEPARTMENT:'Head Department', PUBLIC:'Public'
    };
    return map[r] ?? r;
  }
}
