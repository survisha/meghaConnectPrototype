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
}
