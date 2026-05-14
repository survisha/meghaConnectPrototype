import 'package:dio/dio.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/json_helpers.dart';
import '../models/security_profile.dart';
import '../models/user_session.dart';
import 'auth_api.dart';

class RestAuthApi implements AuthApi {
  RestAuthApi(this._dio);

  final Dio _dio;

  @override
  Future<UserSession> login(LoginCredentials credentials) async {
    try {
      final response = await _dio.post(
        '/api/v1/auth/login',
        data: credentials.toJson(),
      );
      return UserSession.fromAuthJson(unwrapData(response.data));
    } on DioException catch (error) {
      throw ApiException.fromDio(error);
    } on FormatException {
      throw const ApiException('MeghaConnect returned an invalid login body.');
    }
  }

  @override
  Future<SecurityProfile?> profile() async {
    try {
      final response = await _dio.get('/api/v1/security/profile');
      return SecurityProfile.fromJson(unwrapData(response.data));
    } on DioException catch (error) {
      if (error.response?.statusCode == 404) {
        return null;
      }
      throw ApiException.fromDio(error);
    }
  }
}
