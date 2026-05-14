import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../data/models/user_session.dart';

class SecureTokenStorage {
  SecureTokenStorage({
    FlutterSecureStorage? storage,
  }) : _storage = storage ??
            const FlutterSecureStorage(
              aOptions: AndroidOptions(encryptedSharedPreferences: true),
            );

  static const _sessionKey = 'meghaconnect.security.session';
  static const _deviceIdKey = 'meghaconnect.security.device_id';

  final FlutterSecureStorage _storage;

  Future<void> saveSession(UserSession session) {
    return _storage.write(
      key: _sessionKey,
      value: jsonEncode(session.toStorageJson()),
    );
  }

  Future<UserSession?> readSession() async {
    final raw = await _storage.read(key: _sessionKey);
    if (raw == null || raw.isEmpty) {
      return null;
    }

    try {
      return UserSession.fromStorageJson(jsonDecode(raw));
    } catch (_) {
      await clearSession();
      return null;
    }
  }

  Future<String?> readToken() async {
    final session = await readSession();
    if (session == null || session.isExpired) {
      return null;
    }
    return session.token;
  }

  Future<void> clearSession() {
    return _storage.delete(key: _sessionKey);
  }

  Future<String?> readDeviceId() {
    return _storage.read(key: _deviceIdKey);
  }

  Future<void> saveDeviceId(String deviceId) {
    return _storage.write(key: _deviceIdKey, value: deviceId);
  }
}
