import 'package:flutter/foundation.dart';

class NavigationService extends ChangeNotifier {
  String _currentRoute = 'dashboard';
  String get currentRoute => _currentRoute;

  void navigateTo(String route) {
    if (_currentRoute != route) {
      _currentRoute = route;
      notifyListeners();
    }
  }
}
