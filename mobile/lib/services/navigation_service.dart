import 'package:flutter/foundation.dart';

class AppRoutes {
  static const dashboard = 'dashboard';
  static const visitor = 'visitor';
  static const appointments = 'appointments';
  static const newAppointment = 'new_appointment';
  static const guestRegistration = 'guest_registration';
  static const walkIn = 'walkin';
  static const calendar = 'calendar';
  static const approver = 'approver';
  static const schemes = 'schemes';
  static const grievances = 'grievances';
  static const identify = 'identify';
  static const reports = 'reports';
  static const followups = 'followups';
  static const audit = 'audit';
  static const users = 'users';

  static const all = <String>{
    dashboard,
    visitor,
    appointments,
    newAppointment,
    guestRegistration,
    walkIn,
    calendar,
    approver,
    schemes,
    grievances,
    identify,
    reports,
    followups,
    audit,
    users,
  };
}

class NavigationService extends ChangeNotifier {
  String _currentRoute = AppRoutes.dashboard;
  String get currentRoute => _currentRoute;

  void navigateTo(String route) {
    final nextRoute =
        AppRoutes.all.contains(route) ? route : AppRoutes.dashboard;
    if (_currentRoute == nextRoute) return;
    _currentRoute = nextRoute;
    notifyListeners();
  }

  void reset() {
    navigateTo(AppRoutes.dashboard);
  }
}
