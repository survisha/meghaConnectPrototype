import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { UserRole } from '../models';

export type AppFeature =
  | 'dashboard' | 'userManagement' | 'auditTrail' | 'reports' | 'reportAnalytics'
  | 'calendar' | 'appointments' | 'appointmentTypes' | 'schemeManagement'
  | 'recentActivity';

@Injectable({ providedIn: 'root' })
export class AccessControlService {
  constructor(private readonly auth: AuthService) {}

  can(feature: AppFeature): boolean {
    const role = this.role;
    if (!role) return false;

    if (role === 'DEPARTMENT_ADMIN') {
      switch (feature) {
        case 'dashboard':
        case 'userManagement':
        case 'reports':
        case 'auditTrail':
          return true;
        case 'appointmentTypes':
        case 'schemeManagement':
        case 'recentActivity':
          return this.isCmoDepartmentAdmin;
        case 'calendar':
        case 'appointments':
        case 'reportAnalytics':
          return false;
      }
    }

    switch (feature) {
      case 'dashboard':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER', 'DATA_ENTRY_OPERATOR');
      case 'userManagement':
        return this.auth.hasRole('SUPER_ADMIN');
      case 'auditTrail':
        return this.auth.hasRole('SUPER_ADMIN', 'ADMIN');
      case 'reports':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER');
      case 'reportAnalytics':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER');
      case 'calendar':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER');
      case 'appointments':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'OSD', 'APPROVER', 'CMO_OFFICER', 'DATA_ENTRY_OPERATOR');
      case 'appointmentTypes':
      case 'schemeManagement':
      case 'recentActivity':
        return this.auth.hasRole('SUPER_ADMIN', 'ADMIN');
    }
  }

  canViewDashboard(): boolean { return this.can('dashboard'); }
  canViewUserManagement(): boolean { return this.can('userManagement'); }
  canViewAuditTrail(): boolean { return this.can('auditTrail'); }
  canViewCalendar(): boolean { return this.can('calendar'); }
  canViewAppointments(): boolean { return this.can('appointments'); }
  canViewAppointmentTypes(): boolean { return this.can('appointmentTypes'); }
  canViewSchemeManagement(): boolean { return this.can('schemeManagement'); }
  canViewRecentActivity(): boolean { return this.can('recentActivity'); }

  get isCmoDepartmentAdmin(): boolean {
    return this.role === 'DEPARTMENT_ADMIN' && this.departmentCode === 'CMO';
  }

  private get role(): UserRole | null {
    return this.auth.user()?.role ?? null;
  }

  private get departmentCode(): string {
    return (this.auth.user()?.departmentCode ?? '').trim().toUpperCase();
  }
}
