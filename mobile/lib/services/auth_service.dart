import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../models/user.dart';
import 'api_service.dart';
import '../core/security/secure_app_storage.dart';

class AuthService extends ChangeNotifier {
  AuthUser? _user;
  String? _lastError;
  AuthUser? get user => _user;
  String? get lastError => _lastError;
  bool get isLoggedIn => _user != null;

  Future<void> init() async {
    final token = await ApiService.getToken();
    if (token == null || token.isEmpty) {
      await SecureAppStorage.clearSession();
      return;
    }

    final stored = await SecureAppStorage.readUserJson();
    if (stored != null) {
      try {
        _user = AuthUser.fromJson(jsonDecode(stored) as Map<String, dynamic>);
        notifyListeners();
      } catch (_) {
        await SecureAppStorage.clearSession();
      }
    }
  }

  Future<bool> login(String username, String password) async {
    _lastError = null;
    final data = await ApiService.login(username.trim(), password.trim());
    if (data == null) {
      _lastError =
          ApiService.lastLoginError ?? 'Login failed. Please try again.';
      return false;
    }

    final token = data['token'] as String?;
    final uname = data['username'] as String? ?? username.trim();
    final fullName = data['fullName'] as String? ?? uname;
    final roleStr = data['role'] as String? ?? 'PUBLIC';
    final visitorId = (data['visitorId'] as num?)?.toInt();
    final expiresIn = (data['expiresIn'] as num?)?.toInt();

    UserRole role;
    try {
      role = UserRole.values.byName(roleStr);
    } catch (_) {
      role = UserRole.PUBLIC;
    }

    if (token != null) {
      await ApiService.setToken(
        token,
        ttl: expiresIn != null ? Duration(seconds: expiresIn) : null,
      );
    }

    _user = AuthUser(
        username: uname, fullName: fullName, role: role, visitorId: visitorId);

    await SecureAppStorage.writeUserJson(jsonEncode(_user!.toJson()));

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
    await ApiService.setToken(token, ttl: const Duration(hours: 24));

    _user = AuthUser(
      username: phoneNumber,
      fullName: fullName,
      role: UserRole.PUBLIC,
      visitorId: visitorId,
    );

    await SecureAppStorage.writeUserJson(jsonEncode(_user!.toJson()));

    notifyListeners();
    return true;
  }

  Future<void> logout() async {
    _user = null;
    _lastError = null;
    await SecureAppStorage.clearSession();
    notifyListeners();
  }

  bool hasRole(List<UserRole> roles) {
    if (_user == null) return false;
    return roles.contains(_user!.role);
  }
}
