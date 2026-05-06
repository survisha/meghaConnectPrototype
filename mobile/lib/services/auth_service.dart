import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';
import 'api_service.dart';

class AuthService extends ChangeNotifier {
  static const _storageKey = 'megha_user';

  AuthUser? _user;
  AuthUser? get user => _user;
  bool get isLoggedIn => _user != null;

  // Demo credentials kept for the UI quick-access chips only.
  static List<Map<String, String>> get demoCredentials => const [
        {'username': 'hcm', 'password': 'hcm123', 'role': 'HCM'},
        {'username': 'admin', 'password': 'admin123', 'role': 'ADMIN'},
        {'username': 'saidul', 'password': 'osd123', 'role': 'OSD'},
        {'username': 'jtsecy', 'password': 'jts123', 'role': 'JT. SECY'},
        {'username': 'cmo', 'password': 'cmo123', 'role': 'CMO'},
        {'username': 'deo1', 'password': 'deo123', 'role': 'DEO'},
      ];

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getString(_storageKey);
    if (stored != null) {
      try {
        _user = AuthUser.fromJson(jsonDecode(stored) as Map<String, dynamic>);
        notifyListeners();
      } catch (_) {
        await prefs.remove(_storageKey);
      }
    }
  }

  Future<bool> login(String username, String password) async {
    final data = await ApiService.login(username.trim(), password.trim());
    if (data == null) return false;

    final token = data['token'] as String?;
    final uname = data['username'] as String? ?? username.trim();
    final fullName = data['fullName'] as String? ?? uname;
    final roleStr = data['role'] as String? ?? 'PUBLIC';
    final visitorId = (data['visitorId'] as num?)?.toInt();

    UserRole role;
    try {
      role = UserRole.values.byName(roleStr);
    } catch (_) {
      role = UserRole.PUBLIC;
    }

    if (token != null) await ApiService.setToken(token);

    _user = AuthUser(
        username: uname, fullName: fullName, role: role, visitorId: visitorId);

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_storageKey, jsonEncode(_user!.toJson()));

    notifyListeners();
    return true;
  }

  /// Logs in a PUBLIC/Citizen user using the JWT returned from
  /// `/api/v1/visitor/auth/validate-otp`.
  Future<bool> publicLoginWithVisitorJwt({
    required String phoneNumber,
    required String token,
    required String fullName,
    required int visitorId,
  }) async {
    await ApiService.setToken(token);

    _user = AuthUser(
      username: phoneNumber,
      fullName: fullName,
      role: UserRole.PUBLIC,
      visitorId: visitorId,
    );

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_storageKey, jsonEncode(_user!.toJson()));

    notifyListeners();
    return true;
  }

  Future<void> logout() async {
    _user = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_storageKey);
    await ApiService.clearToken();
    notifyListeners();
  }

  bool hasRole(List<UserRole> roles) {
    if (_user == null) return false;
    return roles.contains(_user!.role);
  }
}
