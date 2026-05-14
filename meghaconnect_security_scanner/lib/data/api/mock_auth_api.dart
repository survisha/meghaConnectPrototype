import '../models/security_profile.dart';
import '../models/user_session.dart';
import 'auth_api.dart';

class MockAuthApi implements AuthApi {
  @override
  Future<UserSession> login(LoginCredentials credentials) async {
    await Future<void>.delayed(const Duration(milliseconds: 450));

    final username = credentials.username.trim();
    final role = _roleFor(username);
    return UserSession(
      token: 'mock-token-$username',
      refreshToken: 'mock-refresh-token-$username',
      username: username,
      fullName: username.isEmpty ? 'Security User' : username,
      role: role,
      expiresAt: DateTime.now().add(const Duration(hours: 8)),
      gateName: role == 'PUBLIC' ? null : 'Main Gate',
      location: role == 'PUBLIC' ? null : 'Secretariat Entry',
    );
  }

  @override
  Future<SecurityProfile?> profile() async {
    await Future<void>.delayed(const Duration(milliseconds: 150));
    return const SecurityProfile(
      username: 'security.mock',
      fullName: 'Mock Security User',
      role: 'SECURITY',
      gateName: 'Main Gate',
      location: 'Secretariat Entry',
    );
  }

  String _roleFor(String username) {
    final lower = username.toLowerCase();
    if (lower.contains('admin')) {
      return 'ADMIN';
    }
    if (lower.contains('public') || lower.contains('citizen')) {
      return 'PUBLIC';
    }
    return 'SECURITY';
  }
}
