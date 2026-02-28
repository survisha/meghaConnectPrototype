import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';

class _DemoUser {
  final String username;
  final String password;
  final String fullName;
  final UserRole role;
  const _DemoUser(this.username, this.password, this.fullName, this.role);
}

class AuthService extends ChangeNotifier {
  static const _storageKey = 'megha_user';

  AuthUser? _user;
  AuthUser? get user => _user;
  bool get isLoggedIn => _user != null;

  static const _demoUsers = [
    _DemoUser('hcm', 'hcm123', 'Hon. Chief Minister', UserRole.HCM),
    _DemoUser('admin', 'admin123', 'System Admin', UserRole.ADMIN),
    _DemoUser('saidul', 'osd123', 'Saidul OSD', UserRole.SAIDUL_OSD),
    _DemoUser('jtsecy', 'jts123', 'Joint Secretary', UserRole.APPROVER_JT_SECY),
    _DemoUser('cmo', 'cmo123', 'CMO Officer', UserRole.CMO_OFFICER),
    _DemoUser('deo1', 'deo123', 'Data Entry Operator 1', UserRole.DATA_ENTRY_OPERATOR),
    _DemoUser('9876543210', '123456', 'Public User', UserRole.PUBLIC),
  ];

  static List<Map<String, String>> get demoCredentials => _demoUsers
      .map((u) => {'username': u.username, 'password': u.password, 'role': u.role.badgeLabel})
      .toList();

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
    final match = _demoUsers.where(
      (u) => u.username == username.trim() && u.password == password.trim(),
    );
    if (match.isEmpty) return false;

    final u = match.first;
    _user = AuthUser(username: u.username, fullName: u.fullName, role: u.role);

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_storageKey, jsonEncode(_user!.toJson()));

    notifyListeners();
    return true;
  }

  Future<void> logout() async {
    _user = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_storageKey);
    notifyListeners();
  }

  bool hasRole(List<UserRole> roles) {
    if (_user == null) return false;
    return roles.contains(_user!.role);
  }
}
