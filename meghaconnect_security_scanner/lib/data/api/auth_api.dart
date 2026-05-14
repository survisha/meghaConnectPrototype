import '../models/security_profile.dart';
import '../models/user_session.dart';

class LoginCredentials {
  const LoginCredentials({
    required this.username,
    required this.password,
  });

  final String username;
  final String password;

  Map<String, dynamic> toJson() {
    return {
      'username': username,
      'password': password,
    };
  }
}

abstract class AuthApi {
  Future<UserSession> login(LoginCredentials credentials);

  Future<SecurityProfile?> profile();
}
