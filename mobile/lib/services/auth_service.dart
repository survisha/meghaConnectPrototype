import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:local_auth/local_auth.dart';
import '../models/user.dart';
import 'api_service.dart';
import '../core/security/secure_app_storage.dart';
import 'connectivity_service.dart';
import 'offline_repository.dart';

class AuthService extends ChangeNotifier {
  AuthService(
      {ConnectivityService? connectivity, OfflineRepository? repository})
      : _connectivity = connectivity,
        _repository = repository ?? OfflineRepository();

  final ConnectivityService? _connectivity;
  final OfflineRepository _repository;
  final LocalAuthentication _localAuth = LocalAuthentication();
  AuthUser? _user;
  String? _lastError;
  bool _offlineMode = false;
  AuthUser? get user => _user;
  String? get lastError => _lastError;
  bool get offlineMode => _offlineMode;
  bool get isLoggedIn => _user != null;

  Future<bool> get isBiometricLoginEnabled =>
      SecureAppStorage.isBiometricEnabled();

  Future<bool> get hasBiometricPromptDecision =>
      SecureAppStorage.hasBiometricPromptDecision();

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
        _offlineMode = _connectivity?.isOffline ?? false;
        await _repository.saveSession(_user!.toJson());
        notifyListeners();
      } catch (_) {
        await SecureAppStorage.clearSession();
      }
    }
  }

  Future<bool> login(String username, String password,
      {required String captchaId, required String captchaValue}) async {
    _lastError = null;
    final data = await ApiService.login(username.trim(), password.trim(),
        captchaId: captchaId, captchaValue: captchaValue);
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
    final departmentId = (data['departmentId'] as num?)?.toInt();
    final departmentCode = data['departmentCode'] as String?;
    final departmentName = data['departmentName'] as String?;
    final expiresIn = (data['expiresIn'] as num?)?.toInt();
    final passwordChangeRequired = data['passwordChangeRequired'] == true;

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
        username: uname,
        fullName: fullName,
        role: role,
        visitorId: visitorId,
        departmentId: departmentId,
        departmentCode: departmentCode,
        departmentName: departmentName,
        passwordChangeRequired: passwordChangeRequired);

    await SecureAppStorage.writeUserJson(jsonEncode(_user!.toJson()));
    await _cacheOfflineSession(_user!, token: token);
    await _repository.saveSession(_user!.toJson());
    _offlineMode = false;

    notifyListeners();
    return true;
  }

  Future<bool> enableBiometricLogin() async {
    _lastError = null;
    if (_user == null || _user!.role == UserRole.PUBLIC) {
      _lastError = 'Biometric login is available for staff accounts only.';
      return false;
    }
    final supported = await _hasEnrolledBiometrics();
    if (!supported) {
      _lastError = 'No enrolled device biometrics are available.';
      return false;
    }
    if (!await _authenticateDevice()) {
      _lastError = 'Biometric authentication was cancelled or failed.';
      return false;
    }
    await SecureAppStorage.setBiometricEnabled(true);
    await SecureAppStorage.setBiometricPromptDecision(true);
    await _cacheOfflineSession(_user!, token: await ApiService.getToken());
    return true;
  }

  Future<void> declineBiometricLogin() async {
    await SecureAppStorage.setBiometricEnabled(false);
    await SecureAppStorage.setBiometricPromptDecision(true);
  }

  Future<bool> changeTemporaryPassword(
      String currentPassword, String newPassword) async {
    _lastError = null;
    try {
      final changed = await ApiService.changeTemporaryPassword(
          currentPassword, newPassword);
      if (!changed) {
        _lastError = 'Unable to change password.';
        return false;
      }
      await logout();
      return true;
    } catch (_) {
      _lastError = 'Unable to change password. Please try again.';
      return false;
    }
  }

  /// Logs in a PUBLIC/Citizen user using the JWT returned from
  /// `/api/v1/auth/validate-otp`.
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
    await _cacheOfflineSession(_user!, token: token);
    await _repository.saveSession(_user!.toJson());
    _offlineMode = false;

    notifyListeners();
    return true;
  }

  Future<bool> loginWithCachedDeviceSession({
    String? username,
    bool validateOnline = false,
  }) async {
    _lastError = null;
    final raw = await SecureAppStorage.readOfflineSessionJson();
    if (raw == null || raw.isEmpty) {
      _lastError = 'First login requires internet connection.';
      return false;
    }

    Map<String, dynamic> session;
    try {
      session = jsonDecode(raw) as Map<String, dynamic>;
    } catch (_) {
      _lastError = 'Unexpected response. Please login online once again.';
      return false;
    }

    if (!await SecureAppStorage.isBiometricEnabled() ||
        session['offlineLoginEnabled'] != true) {
      _lastError = 'Biometric login is not enabled on this device.';
      return false;
    }
    final requestedUsername = username?.trim();
    final cachedUsername = session['username']?.toString();
    if (requestedUsername != null &&
        requestedUsername.isNotEmpty &&
        cachedUsername != null &&
        cachedUsername.isNotEmpty &&
        requestedUsername.toLowerCase() != cachedUsername.toLowerCase()) {
      _lastError = 'No cached session found for this username.';
      return false;
    }
    final expiresAt =
        DateTime.tryParse(session['offlineExpiresAt']?.toString() ?? '');
    if (expiresAt == null || DateTime.now().toUtc().isAfter(expiresAt)) {
      _lastError = 'Cached session expired. Please login online once.';
      return false;
    }

    final ok = await _authenticateDevice();
    if (!ok) {
      _lastError = 'Biometric authentication failed.';
      return false;
    }

    final userJson = session['user'];
    if (userJson is! Map<String, dynamic>) {
      _lastError = 'Unexpected response. Please login online once again.';
      return false;
    }

    try {
      _user = AuthUser.fromJson(userJson);
      final cachedToken = session['token']?.toString();
      if (cachedToken != null && cachedToken.isNotEmpty) {
        await ApiService.setToken(cachedToken);
      }
      if (validateOnline && !await ApiService.validateStaffSession()) {
        await ApiService.clearToken();
        _user = null;
        _lastError =
            'Saved session is no longer valid. Use Staff ID and Password.';
        return false;
      }
      await SecureAppStorage.writeUserJson(jsonEncode(_user!.toJson()));
      await _repository.saveSession(_user!.toJson());
      _offlineMode = !validateOnline;
      notifyListeners();
      return true;
    } catch (_) {
      _lastError = 'Unexpected response. Please login online once again.';
      return false;
    }
  }

  Future<void> _cacheOfflineSession(AuthUser user, {String? token}) async {
    final now = DateTime.now().toUtc();
    final biometricEnabled = await SecureAppStorage.isBiometricEnabled();
    final session = {
      'offlineLoginEnabled': biometricEnabled && user.role != UserRole.PUBLIC,
      'userId': user.visitorId?.toString() ?? user.username,
      'username': user.username,
      'role': user.role.name,
      'displayName': user.fullName,
      'lastSuccessfulLoginAt': now.toIso8601String(),
      'offlineExpiresAt': now.add(const Duration(days: 7)).toIso8601String(),
      'token': token,
      'user': user.toJson(),
    };
    await SecureAppStorage.writeOfflineSessionJson(jsonEncode(session));
  }

  Future<bool> _authenticateDevice() async {
    try {
      if (!await _hasEnrolledBiometrics()) return false;
      return _localAuth.authenticate(
        localizedReason: 'Unlock your saved MeghaConnect session.',
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
        ),
      );
    } catch (_) {
      return false;
    }
  }

  Future<bool> _hasEnrolledBiometrics() async {
    try {
      final supported = await _localAuth.isDeviceSupported();
      final canCheck = await _localAuth.canCheckBiometrics;
      if (!supported || !canCheck) return false;
      return (await _localAuth.getAvailableBiometrics()).isNotEmpty;
    } catch (_) {
      return false;
    }
  }

  Future<void> logout() async {
    _user = null;
    _lastError = null;
    _offlineMode = false;
    await SecureAppStorage.clearSession();
    notifyListeners();
  }

  bool hasRole(List<UserRole> roles) {
    if (_user == null) return false;
    return roles.contains(_user!.role);
  }
}
