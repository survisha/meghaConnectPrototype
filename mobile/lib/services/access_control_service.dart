import '../models/user.dart';

class AccessControlService {
  static bool canAccessRoute(AuthUser user, String? route) {
    if (route == null) return true;
    if (route == 'walkin') {
      return const {UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.DEO,
        UserRole.APPROVER, UserRole.HCM}.contains(user.role);
    }
    if (route == 'register_visitor') {
      return const {UserRole.DEO, UserRole.APPROVER, UserRole.HCM}.contains(user.role);
    }
    if (route == 'identify') {
      return const {UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.DEO,
        UserRole.APPROVER, UserRole.HCM}.contains(user.role);
    }
    if (user.role != UserRole.DEPARTMENT_ADMIN) return true;
    return const {'dashboard', 'users', 'reports', 'audit'}.contains(route);
  }

  static bool canViewRecentActivity(AuthUser user) =>
      user.role == UserRole.DEPARTMENT_ADMIN &&
      (user.departmentCode ?? '').trim().toUpperCase() == 'CMO';
}
