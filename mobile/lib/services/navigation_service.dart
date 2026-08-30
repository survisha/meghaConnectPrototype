import 'package:flutter/foundation.dart';

class AppRoutes {
  static const dashboard = 'dashboard';
  static const visitor = 'visitor';
  static const appointments = 'appointments';
  static const newAppointment = 'new_appointment';
  static const guestRegistration = 'guest_registration';
  static const walkIn = 'walkin';
  static const walkInAppointments = 'walkin_appointments';
  static const completedAppointments = 'completed_appointments';
  static const rejectedAppointments = 'rejected_appointments';
  static const closedAppointments = 'closed_appointments';
  static const registerVisitor = 'register_visitor';
  static const qrScanner = 'qr_scanner';
  static const calendar = 'calendar';
  static const approver = 'approver';
  static const schemes = 'schemes';
  static const grievances = 'grievances';
  static const identify = 'identify';
  static const reports = 'reports';
  static const heatmap = 'heatmap';
  static const audit = 'audit';
  static const users = 'users';
  static const pendingSync = 'pending_sync';

  static const all = <String>{
    dashboard,
    visitor,
    appointments,
    newAppointment,
    guestRegistration,
    walkIn,
    walkInAppointments,
    completedAppointments,
    rejectedAppointments,
    closedAppointments,
    registerVisitor,
    qrScanner,
    calendar,
    approver,
    schemes,
    grievances,
    identify,
    reports,
    heatmap,
    audit,
    users,
    pendingSync,
  };
}

class NavigationService extends ChangeNotifier {
  String _currentRoute = AppRoutes.dashboard;
  String get currentRoute => _currentRoute;

  void navigateTo(String route) {
    final nextRoute =
        AppRoutes.all.contains(route) ? route : AppRoutes.dashboard;
    if (kDebugMode) {
      debugPrint(
        'NavigationService.navigateTo clicked=$route resolved=$nextRoute current=$_currentRoute',
      );
    }
    if (_currentRoute == nextRoute) return;
    _currentRoute = nextRoute;
    notifyListeners();
  }

  void reset() {
    navigateTo(AppRoutes.dashboard);
  }
}
