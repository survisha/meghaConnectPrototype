import 'package:flutter_test/flutter_test.dart';
import 'package:megha_connect/models/user.dart';
import 'package:megha_connect/services/access_control_service.dart';

void main() {
  test('general Department Admin is limited to supported department-safe routes', () {
    const user = AuthUser(username: 'health', fullName: 'Health Admin', role: UserRole.DEPARTMENT_ADMIN, departmentCode: 'HEALTH');
    expect(AccessControlService.canAccessRoute(user, 'dashboard'), isTrue);
    expect(AccessControlService.canAccessRoute(user, 'users'), isTrue);
    expect(AccessControlService.canAccessRoute(user, 'audit'), isTrue);
    expect(AccessControlService.canAccessRoute(user, 'calendar'), isFalse);
    expect(AccessControlService.canAccessRoute(user, 'appointments'), isFalse);
  });

  test('CMO Department Admin is identified for Recent Activity', () {
    const user = AuthUser(username: 'cmo', fullName: 'CMO Admin', role: UserRole.DEPARTMENT_ADMIN, departmentCode: 'cmo');
    expect(AccessControlService.canViewRecentActivity(user), isTrue);
  });

  for (final role in const [UserRole.DEO, UserRole.APPROVER, UserRole.HCM]) {
    test('${role.name} can access all visitor counter functions', () {
      final user = AuthUser(username: role.name, fullName: role.name, role: role);
      expect(AccessControlService.canAccessRoute(user, 'walkin'), isTrue);
      expect(AccessControlService.canAccessRoute(user, 'register_visitor'), isTrue);
      expect(AccessControlService.canAccessRoute(user, 'identify'), isTrue);
    });
  }

  test('unrelated authenticated role cannot access visitor counter functions', () {
    const user = AuthUser(username: 'pa', fullName: 'PA', role: UserRole.DEPARTMENT_PA);
    expect(AccessControlService.canAccessRoute(user, 'walkin'), isFalse);
    expect(AccessControlService.canAccessRoute(user, 'register_visitor'), isFalse);
    expect(AccessControlService.canAccessRoute(user, 'identify'), isFalse);
  });
}
