import 'package:dio/dio.dart';

import '../config/app_environment.dart';
import '../security/secure_token_storage.dart';

class ApiClient {
  ApiClient({
    required AppEnvironment environment,
    required SecureTokenStorage tokenStorage,
  })  : _environment = environment,
        _tokenStorage = tokenStorage {
    dio = Dio(
      BaseOptions(
        baseUrl: _environment.baseUri.toString(),
        connectTimeout: const Duration(seconds: 20),
        receiveTimeout: const Duration(seconds: 20),
        sendTimeout: const Duration(seconds: 20),
        contentType: Headers.jsonContentType,
        responseType: ResponseType.json,
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _tokenStorage.readToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          options.headers['X-MeghaConnect-Client'] = 'security-scanner';
          handler.next(options);
        },
      ),
    );
  }

  final AppEnvironment _environment;
  final SecureTokenStorage _tokenStorage;

  late final Dio dio;
}
