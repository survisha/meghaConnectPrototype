import { AccessControlService } from './access-control.service';
import { AuthService, AuthUser } from './auth.service';

describe('AccessControlService', () => {
  let currentUser: AuthUser | null;
  let service: AccessControlService;

  beforeEach(() => {
    currentUser = null;
    const auth = {
      user: () => currentUser,
      hasRole: (...roles: string[]) => !!currentUser && roles.includes(currentUser.role),
    } as unknown as AuthService;
    service = new AccessControlService(auth);
  });

  it('allows a general Department Admin only department-safe features', () => {
    currentUser = { username: 'health-admin', fullName: 'Health Admin', role: 'DEPARTMENT_ADMIN', departmentCode: 'HEALTH' };

    expect(service.canViewDashboard()).toBeTrue();
    expect(service.canViewUserManagement()).toBeTrue();
    expect(service.canViewAuditTrail()).toBeTrue();
    expect(service.canViewCalendar()).toBeFalse();
    expect(service.canViewAppointments()).toBeFalse();
    expect(service.canViewAppointmentTypes()).toBeFalse();
    expect(service.canViewSchemeManagement()).toBeFalse();
    expect(service.canViewRecentActivity()).toBeFalse();
  });

  it('enables only the required additional CMO Department Admin features', () => {
    currentUser = { username: 'cmo-admin', fullName: 'CMO Admin', role: 'DEPARTMENT_ADMIN', departmentCode: ' cmo ' };

    expect(service.canViewAppointmentTypes()).toBeTrue();
    expect(service.canViewSchemeManagement()).toBeTrue();
    expect(service.canViewRecentActivity()).toBeTrue();
    expect(service.canViewCalendar()).toBeFalse();
    expect(service.canViewAppointments()).toBeFalse();
  });

  for (const role of ['DEO', 'APPROVER', 'HCM'] as const) {
    it(`allows ${role} to use walk-in, visitor registration, and public identification`, () => {
      currentUser = { username: role.toLowerCase(), fullName: role, role };
      expect(service.canAccessWalkIn()).toBeTrue();
      expect(service.canRegisterVisitor()).toBeTrue();
      expect(service.canUsePublicIdentification()).toBeTrue();
    });
  }

  it('does not grant the visitor functions to an unrelated role', () => {
    currentUser = { username: 'pa', fullName: 'Department PA', role: 'DEPARTMENT_PA' };
    expect(service.canAccessWalkIn()).toBeFalse();
    expect(service.canRegisterVisitor()).toBeFalse();
    expect(service.canUsePublicIdentification()).toBeFalse();
  });
});
