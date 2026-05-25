import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureAppStorage {
  const SecureAppStorage._();

  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  static const _tokenKey = 'meghaconnect.auth.token';
  static const _tokenExpiresAtKey = 'meghaconnect.auth.token_expires_at';
  static const _userKey = 'meghaconnect.auth.user';

  static Future<void> writeToken(String token, {Duration? ttl}) async {
    await _storage.write(key: _tokenKey, value: token);
    if (ttl != null && ttl.inSeconds > 0) {
      final expiresAt = DateTime.now().toUtc().add(ttl).toIso8601String();
      await _storage.write(key: _tokenExpiresAtKey, value: expiresAt);
    } else {
      await _storage.delete(key: _tokenExpiresAtKey);
    }
  }

  static Future<String?> readToken() async {
    final expiresAtRaw = await _storage.read(key: _tokenExpiresAtKey);
    if (expiresAtRaw != null && expiresAtRaw.isNotEmpty) {
      final expiresAt = DateTime.tryParse(expiresAtRaw);
      if (expiresAt != null &&
          DateTime.now()
              .toUtc()
              .isAfter(expiresAt.subtract(const Duration(seconds: 30)))) {
        await clearSession();
        return null;
      }
    }
    return _storage.read(key: _tokenKey);
  }

  static Future<void> writeUserJson(String userJson) {
    return _storage.write(key: _userKey, value: userJson);
  }

  static Future<String?> readUserJson() {
    return _storage.read(key: _userKey);
  }

  static Future<void> clearSession() async {
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _tokenExpiresAtKey);
    await _storage.delete(key: _userKey);
  }

  static Future<void> clearToken() async {
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _tokenExpiresAtKey);
  }
}
