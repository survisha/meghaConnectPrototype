import '../../core/security/jwt_decoder.dart';

class UserSession {
  const UserSession({
    required this.token,
    required this.username,
    required this.fullName,
    required this.role,
    this.refreshToken,
    this.expiresAt,
    this.gateName,
    this.location,
  });

  final String token;
  final String? refreshToken;
  final String username;
  final String fullName;
  final String role;
  final DateTime? expiresAt;
  final String? gateName;
  final String? location;

  String get displayName => fullName.trim().isNotEmpty ? fullName : username;

  String get normalizedRole => normalizeRole(role);

  bool get isSecurityAllowed {
    return normalizedRole == 'SECURITY' || normalizedRole == 'ADMIN';
  }

  bool get isExpired {
    final expiry = expiresAt;
    if (expiry == null) {
      return false;
    }
    return DateTime.now().isAfter(expiry.subtract(const Duration(seconds: 15)));
  }

  UserSession copyWith({
    String? token,
    String? refreshToken,
    String? username,
    String? fullName,
    String? role,
    DateTime? expiresAt,
    String? gateName,
    String? location,
  }) {
    return UserSession(
      token: token ?? this.token,
      refreshToken: refreshToken ?? this.refreshToken,
      username: username ?? this.username,
      fullName: fullName ?? this.fullName,
      role: role ?? this.role,
      expiresAt: expiresAt ?? this.expiresAt,
      gateName: gateName ?? this.gateName,
      location: location ?? this.location,
    );
  }

  factory UserSession.fromAuthJson(Map<String, dynamic> json) {
    final token = _string(json, ['token', 'accessToken', 'jwt']);
    final expiresAt = JwtDecoder.expiresAt(token) ??
        _expiresAtFromSeconds(json['expiresIn']) ??
        _dateTime(json['expiresAt'] ?? json['expiry']);

    return UserSession(
      token: token,
      refreshToken: _optionalString(json, ['refreshToken']),
      username: _string(json, ['username', 'userName', 'loginId']),
      fullName: _optionalString(json, ['fullName', 'name', 'displayName']) ??
          _string(json, ['username', 'userName', 'loginId']),
      role: _string(json, ['role', 'authority', 'userRole']),
      expiresAt: expiresAt,
      gateName: _optionalString(json, ['gateName', 'assignedGate']),
      location: _optionalString(json, ['location', 'assignedLocation']),
    );
  }

  factory UserSession.fromStorageJson(Object? value) {
    final json = Map<String, dynamic>.from(value as Map);
    return UserSession(
      token: _string(json, ['token']),
      refreshToken: _optionalString(json, ['refreshToken']),
      username: _string(json, ['username']),
      fullName: _string(json, ['fullName']),
      role: _string(json, ['role']),
      expiresAt: _dateTime(json['expiresAt']),
      gateName: _optionalString(json, ['gateName']),
      location: _optionalString(json, ['location']),
    );
  }

  Map<String, dynamic> toStorageJson() {
    return {
      'token': token,
      'refreshToken': refreshToken,
      'username': username,
      'fullName': fullName,
      'role': role,
      'expiresAt': expiresAt?.toIso8601String(),
      'gateName': gateName,
      'location': location,
    };
  }

  static String normalizeRole(String value) {
    return value.trim().toUpperCase().replaceFirst(RegExp('^ROLE_'), '');
  }

  static String _string(Map<String, dynamic> json, List<String> keys) {
    final value = _optionalString(json, keys);
    if (value == null) {
      throw const FormatException('Required auth field is missing.');
    }
    return value;
  }

  static String? _optionalString(Map<String, dynamic> json, List<String> keys) {
    for (final key in keys) {
      final value = json[key];
      if (value != null && value.toString().trim().isNotEmpty) {
        return value.toString().trim();
      }
    }
    return null;
  }

  static DateTime? _expiresAtFromSeconds(Object? value) {
    final seconds = switch (value) {
      int seconds => seconds,
      num seconds => seconds.toInt(),
      String seconds => int.tryParse(seconds),
      _ => null,
    };
    if (seconds == null || seconds <= 0) {
      return null;
    }
    return DateTime.now().add(Duration(seconds: seconds));
  }

  static DateTime? _dateTime(Object? value) {
    if (value == null) {
      return null;
    }
    return DateTime.tryParse(value.toString());
  }
}
