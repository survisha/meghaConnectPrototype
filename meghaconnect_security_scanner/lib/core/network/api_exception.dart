import 'package:dio/dio.dart';

class ApiException implements Exception {
  const ApiException(
    this.message, {
    this.statusCode,
    this.code,
  });

  final String message;
  final int? statusCode;
  final String? code;

  bool get isUnauthorized => statusCode == 401;

  bool get isForbidden => statusCode == 403;

  static ApiException fromDio(DioException error) {
    final response = error.response;
    if (response != null) {
      return fromResponse(response.data, response.statusCode);
    }

    return switch (error.type) {
      DioExceptionType.connectionTimeout ||
      DioExceptionType.receiveTimeout ||
      DioExceptionType.sendTimeout =>
        const ApiException('Connection timed out. Please try again.'),
      DioExceptionType.connectionError => const ApiException(
          'Unable to connect to MeghaConnect. Check the network and API URL.',
        ),
      DioExceptionType.badCertificate => const ApiException(
          'The server certificate is not trusted.',
        ),
      _ => ApiException(error.message ?? 'Unexpected network error.'),
    };
  }

  static ApiException fromResponse(Object? data, int? statusCode) {
    final map = data is Map ? Map<String, dynamic>.from(data) : null;
    final message = map?['message'] ??
        map?['error'] ??
        map?['errorMessage'] ??
        _messageForStatus(statusCode);
    final code = map?['code'] ?? map?['errorCode'];

    return ApiException(
      message.toString(),
      statusCode: statusCode,
      code: code?.toString(),
    );
  }

  static String _messageForStatus(int? statusCode) {
    return switch (statusCode) {
      400 => 'Invalid request.',
      401 => 'Session expired. Please login again.',
      403 => 'You are not authorized to perform this action.',
      404 => 'Requested MeghaConnect API was not found.',
      409 => 'This QR action conflicts with the current appointment status.',
      500 => 'MeghaConnect server error. Please try again later.',
      _ => 'Unexpected MeghaConnect API error.',
    };
  }

  @override
  String toString() => message;
}
