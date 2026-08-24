import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { UserRole } from '../models';

export type AppFeature =
  | 'dashboard' | 'userManagement' | 'auditTrail' | 'reports' | 'reportAnalytics'
  | 'calendar' | 'appointments' | 'appointmentTypes' | 'schemeManagement'
  | 'recentActivity' | 'walkIn' | 'registerVisitor' | 'publicIdentification'
  | 'completedAppointments' | 'rejectedAppointments' | 'legacyDataImport';
  

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
        case 'walkIn':
        case 'registerVisitor':
        case 'publicIdentification':
        case 'completedAppointments':
        case 'rejectedAppointments':
        case 'legacyDataImport':
          return false;
      }
    }

    switch (feature) {
      case 'dashboard':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'APPROVER', 'DEO');
      case 'userManagement':
        return this.auth.hasRole('SUPER_ADMIN');
      case 'auditTrail':
        return this.auth.hasRole('SUPER_ADMIN', 'ADMIN');
      case 'reports':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'APPROVER');
      case 'reportAnalytics':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'APPROVER');
      case 'calendar':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'APPROVER');
      case 'appointments':
        return this.auth.hasRole('SUPER_ADMIN', 'DEPARTMENT_PA', 'HCM', 'ADMIN', 'APPROVER', 'DEO');
      case 'appointmentTypes':
      case 'schemeManagement':
      case 'recentActivity':
        return this.auth.hasRole('SUPER_ADMIN', 'ADMIN');
      case 'walkIn':
        return this.auth.hasRole('SUPER_ADMIN', 'ADMIN', 'DEO', 'APPROVER', 'HCM');
      case 'registerVisitor':
        return this.auth.hasRole('DEO', 'APPROVER', 'HCM');
      case 'publicIdentification':
        return this.auth.hasRole('SUPER_ADMIN', 'ADMIN', 'DEO', 'APPROVER', 'HCM');
      case 'completedAppointments':
      case 'rejectedAppointments':
        return this.auth.hasRole('SUPER_ADMIN', 'APPROVER', 'HCM');
      case 'legacyDataImport':
        return this.auth.hasRole('ADMIN', 'DEO');
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
  canAccessWalkIn(): boolean { return this.can('walkIn'); }
  canRegisterVisitor(): boolean { return this.can('registerVisitor'); }
  canUsePublicIdentification(): boolean { return this.can('publicIdentification'); }
  canViewCompletedAppointments(): boolean { return this.can('completedAppointments'); }
  canViewRejectedAppointments(): boolean { return this.can('rejectedAppointments'); }
  canUseLegacyDataImport(): boolean { return this.can('legacyDataImport'); }

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
