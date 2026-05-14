import '../../core/network/api_exception.dart';
import '../../core/security/secure_token_storage.dart';
import '../api/auth_api.dart';
import '../models/security_profile.dart';
import '../models/user_session.dart';

class AuthRepository {
  AuthRepository({
    required AuthApi authApi,
    required SecureTokenStorage tokenStorage,
  })  : _authApi = authApi,
        _tokenStorage = tokenStorage;

  final AuthApi _authApi;
  final SecureTokenStorage _tokenStorage;

  Future<UserSession?> restoreSession() async {
    final session = await _tokenStorage.readSession();
    if (session == null) {
      return null;
    }
    if (session.isExpired || !session.isSecurityAllowed) {
      await _tokenStorage.clearSession();
      return null;
    }
    return session;
  }

  Future<UserSession> login({
    required String username,
    required String password,
  }) async {
    final session = await _authApi.login(
      LoginCredentials(username: username, password: password),
    );
    if (!session.isSecurityAllowed) {
      throw const ApiException(
        'Only SECURITY and ADMIN users can use this scanner app.',
        statusCode: 403,
        code: 'ROLE_NOT_ALLOWED',
      );
    }

    await _tokenStorage.saveSession(session);

    final profile = await _loadProfileSafely();
    final enriched =
        profile == null ? session : _mergeProfile(session, profile);
    if (!enriched.isSecurityAllowed) {
      await _tokenStorage.clearSession();
      throw const ApiException(
        'Only SECURITY and ADMIN users can use this scanner app.',
        statusCode: 403,
        code: 'ROLE_NOT_ALLOWED',
      );
    }

    await _tokenStorage.saveSession(enriched);
    return enriched;
  }

  Future<void> logout() {
    return _tokenStorage.clearSession();
  }

  Future<SecurityProfile?> _loadProfileSafely() async {
    try {
      return await _authApi.profile();
    } on ApiException catch (error) {
      if (error.statusCode == 404) {
        return null;
      }
      rethrow;
    }
  }

  UserSession _mergeProfile(UserSession session, SecurityProfile profile) {
    return session.copyWith(
      username:
          profile.username.isNotEmpty ? profile.username : session.username,
      fullName:
          profile.fullName.isNotEmpty ? profile.fullName : session.fullName,
      role: profile.role.isNotEmpty ? profile.role : session.role,
      gateName: profile.gateName ?? session.gateName,
      location: profile.location ?? session.location,
    );
  }
}
