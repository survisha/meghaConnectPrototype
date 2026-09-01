import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../models/user.dart';
import '../core/config/app_config.dart';
import '../core/security/secure_app_storage.dart';

class ApiService {
  static Uri _u(String path) => Uri.parse('${AppConfig.apiV1BaseUrl}$path');

  static Future<Map<String, dynamic>> getHcmDashboardSummary() async {
    try {
      final response = await http
          .get(_u('/hcm/dashboard-summary'), headers: await _headers())
          .timeout(const Duration(seconds: 20));
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return const {};
  }

  static Uri _fileApi(String path) => Uri.parse('${AppConfig.apiBaseUrl}$path');
  static String? lastLoginError;
  static String? lastLoginErrorCode;
  static bool lastLoginReachedServer = false;

  static Future<Map<String, dynamic>?> generateCaptcha() async {
    try {
      final response = await http
          .get(_u('/captcha/generate'))
          .timeout(const Duration(seconds: 20));
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
      return null;
    } catch (error, stackTrace) {
      _logError('generateCaptcha', error, stackTrace);
      return null;
    }
  }

  static Future<String?> getToken() async {
    return SecureAppStorage.readToken();
  }

  static Future<void> setToken(String token, {Duration? ttl}) async {
    await SecureAppStorage.writeToken(token, ttl: ttl);
  }

  static Future<void> clearToken() async {
    await SecureAppStorage.clearToken();
  }

  /// Verifies the saved staff JWT against an existing staff-authorized API.
  /// Biometrics only unlock local credentials; this call keeps the backend
  /// authoritative for session validity and account authorization.
  static Future<bool> validateStaffSession() async {
    try {
      final response = await http
          .get(_u('/appointments?page=0&size=1'), headers: await _headers())
          .timeout(const Duration(seconds: 20));
      return response.statusCode >= 200 && response.statusCode < 300;
    } catch (_) {
      return false;
    }
  }

  static Future<Map<String, String>> _headers() async {
    final token = await getToken();
    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  static Future<Map<String, String>> _authHeaders() async {
    final token = await getToken();
    final headers = <String, String>{};
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  /// Headers for authenticated media widgets (for example stored visitor photos).
  static Future<Map<String, String>> authenticatedMediaHeaders() =>
      _authHeaders();

  static Future<Map<String, dynamic>> extractVisitorForm(
      String imagePath) async {
    try {
      final request =
          http.MultipartRequest('POST', _u('/visitor-form-extraction/extract'));
      request.headers.addAll(await _authHeaders());
      request.fields['formType'] = 'VISITOR_REGISTRATION';
      request.files.add(await http.MultipartFile.fromPath('image', imagePath));
      final streamed =
          await request.send().timeout(const Duration(seconds: 430));
      final response = await http.Response.fromStream(streamed);
      final decoded = jsonDecode(response.body);
      if (response.statusCode >= 200 &&
          response.statusCode < 300 &&
          decoded is Map<String, dynamic>) return decoded;
      return {
        'success': false,
        'message': _messageFromResponse(response,
            'Unable to extract the form. Please enter the details manually.')
      };
    } catch (error, stackTrace) {
      _logError('extractVisitorForm', error, stackTrace);
      return {
        'success': false,
        'message':
            'Unable to extract the form. Please enter the details manually.'
      };
    }
  }

  static String _messageFromResponse(http.Response response, String fallback) {
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map<String, dynamic>) {
        final code = decoded['code']?.toString().toUpperCase() ?? '';
        final message = decoded['message']?.toString().trim();
        if (code.contains('UNAUTHORIZED') || response.statusCode == 401) {
          return 'Your session has expired. Please login again.';
        }
        if (response.statusCode == 403) {
          return 'You do not have permission to perform this action.';
        }
        if (message != null && message.isNotEmpty) return message;
      }
    } catch (_) {}
    return fallback;
  }

  static String _otpFailureMessage({
    required http.Response response,
    Map<String, dynamic>? body,
  }) {
    final code =
        (body?['code'] ?? body?['errorCode'])?.toString().toUpperCase() ?? '';
    final rawMessage = body?['message']?.toString().trim() ?? '';
    final message = rawMessage.toLowerCase();
    final remainingAttempts =
        ((body?['attemptsRemaining'] ?? body?['remainingAttempts']) as num?)
            ?.toInt();
    final waitTimeMinutes = (body?['waitTimeMinutes'] as num?)?.toInt();

    if (code.contains('EXPIRED') || message.contains('expired')) {
      return 'OTP expired. Please request a new OTP.';
    }
    if (code.contains('MAX') ||
        code.contains('LOCK') ||
        message.contains('maximum otp') ||
        message.contains('locked')) {
      final wait =
          waitTimeMinutes != null && waitTimeMinutes > 0 ? waitTimeMinutes : 30;
      return 'Too many failed OTP attempts. Please try again after $wait minutes.';
    }
    if (code.contains('INVALID') ||
        code.contains('VALIDATION') ||
        message.contains('invalid otp') ||
        message.contains('wrong otp')) {
      if (remainingAttempts != null && remainingAttempts >= 0) {
        if (remainingAttempts == 0) {
          return 'Account locked. Please request a new OTP or try again later.';
        }
        final suffix = remainingAttempts == 1 ? 'chance' : 'chances';
        return 'Invalid OTP. $remainingAttempts more $suffix.';
      }
      if (rawMessage.isNotEmpty) return rawMessage;
      return 'Invalid OTP. Please try again.';
    }
    if (response.statusCode >= 500) {
      return 'Server error. Please try again later.';
    }
    if (response.statusCode == 408 || response.statusCode == 429) {
      return 'OTP validation is temporarily unavailable. Please try again.';
    }
    return 'Unexpected response. Please try again.';
  }

  static void _logError(String action, Object error, [StackTrace? stackTrace]) {
    debugPrint('ApiService.$action failed: $error');
    if (stackTrace != null) debugPrint(stackTrace.toString());
  }

  static Map<String, dynamic> _listError({String? message}) => {
        'content': [],
        'totalElements': 0,
        'error': true,
        'message': message ?? 'Unable to load data. Please try again.',
      };

  static Map<String, dynamic> _normalizePageResponse(dynamic decoded) {
    final data = decoded is Map<String, dynamic> &&
            decoded.containsKey('data') &&
            decoded.containsKey('success')
        ? decoded['data']
        : decoded;
    if (data is List) {
      return {
        'content': data,
        'totalElements': data.length,
        'totalPages': 1,
        'number': 0,
        'size': data.length,
      };
    }
    if (data is Map<String, dynamic>) {
      final content = data['content'];
      return {
        ...data,
        'content': content is List ? content : <dynamic>[],
        'totalElements':
            data['totalElements'] ?? (content is List ? content.length : 0),
        'totalPages': data['totalPages'] ?? 1,
        'number': data['number'] ?? 0,
        'size': data['size'] ?? (content is List ? content.length : 0),
      };
    }
    return _listError(message: 'Unexpected response. Please try again.');
  }

  static List<Map<String, dynamic>> _normalizeList(dynamic decoded) {
    final data = decoded is Map<String, dynamic> &&
            decoded.containsKey('data') &&
            decoded.containsKey('success')
        ? decoded['data']
        : decoded;
    final rows = data is List
        ? data
        : data is Map<String, dynamic> && data['content'] is List
            ? data['content'] as List<dynamic>
            : <dynamic>[];
    return rows
        .whereType<Map>()
        .map((row) => Map<String, dynamic>.from(row))
        .toList();
  }

  static Map<String, dynamic> _unwrapObject(dynamic decoded) {
    final data = decoded is Map<String, dynamic> &&
            decoded.containsKey('data') &&
            decoded.containsKey('success')
        ? decoded['data']
        : decoded;
    if (data is Map<String, dynamic>) return data;
    return {};
  }

  // Maps a role string from the backend (e.g. "ROLE_HCM" or "HCM") to a UserRole.
  static UserRole _parseRole(String raw) {
    final normalized = raw.startsWith('ROLE_') ? raw.substring(5) : raw;
    return UserRole.values.firstWhere(
      (r) => r.name == normalized,
      orElse: () => UserRole.PUBLIC,
    );
  }

  // Auth
  static Future<Map<String, dynamic>?> login(String username, String password,
      {required String captchaId, required String captchaValue}) async {
    lastLoginError = null;
    lastLoginErrorCode = null;
    lastLoginReachedServer = false;
    try {
      final resp = await http
          .post(
            _u('/auth/login'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'username': username,
              'password': password,
              'captchaId': captchaId,
              'captchaValue': captchaValue,
            }),
          )
          .timeout(const Duration(seconds: 20));
      lastLoginReachedServer = true;
      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body) as Map<String, dynamic>;
        // normalise role field before returning
        if (data['role'] != null) {
          data['role'] = _parseRole(data['role'].toString()).name;
        }
        return data;
      }
      try {
        final error = jsonDecode(resp.body) as Map<String, dynamic>;
        lastLoginErrorCode = error['errorCode']?.toString();
      } catch (_) {}
      lastLoginError = _messageFromResponse(
        resp,
        resp.statusCode == 401 ||
                resp.statusCode == 403 ||
                resp.statusCode == 423
            ? 'Invalid username or password'
            : 'Login failed. Please try again.',
      );
      return null;
    } catch (error, stackTrace) {
      _logError('login', error, stackTrace);
      lastLoginError =
          'Unable to connect. Please check your network and try again.';
      return null;
    }
  }

  static Future<bool> changeTemporaryPassword(
      String currentPassword, String newPassword) async {
    final response = await http.post(_u('/auth/change-temporary-password'),
        headers: await _headers(),
        body: jsonEncode(
            {'currentPassword': currentPassword, 'newPassword': newPassword}));
    return response.statusCode == 204;
  }

  // ── Visitor (Citizen) OTP Auth ─────────────────────────────────────────────
  //
  // Backend: /api/v1/visitor/auth/**
  //
  // Business rule: one mobile can map to multiple visitor registrations;
  // EPIC is the unique identity. If multiple registrations are found, the API
  // responds with requiresEpic=true (and a clear message).

  static Future<Map<String, dynamic>> generateVisitorOtp({
    required String phoneNumber,
    String? epicNumber,
    int? visitorId,
    bool registrationFlow = false,
  }) async {
    try {
      final body = <String, dynamic>{'phoneNumber': phoneNumber};
      if (visitorId != null && visitorId > 0) {
        body['visitorId'] = visitorId.toString();
      }
      if (registrationFlow) {
        body['purpose'] = 'REGISTRATION';
        body['registrationFlow'] = 'true';
      }
      final epic = (epicNumber ?? '').trim();
      if (epic.isNotEmpty) body['epicNumber'] = epic.toUpperCase();
      final resp = await http
          .post(
            _u('/visitor/auth/generate-otp'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));

      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body) as Map<String, dynamic>;
        return data;
      }

      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': 'Failed to generate OTP. Please try again.',
        'requiresEpic': false,
      };
    } catch (error, stackTrace) {
      _logError('generateVisitorOtp', error, stackTrace);
      return {
        'success': false,
        'code': 'NETWORK_ERROR',
        'message': 'Network error. Please try again.',
        'requiresEpic': false,
      };
    }
  }

  static Future<Map<String, dynamic>> validateVisitorOtp({
    required String phoneNumber,
    required String otp,
    String? epicNumber,
    int? visitorId,
  }) async {
    try {
      final body = <String, dynamic>{'phoneNumber': phoneNumber, 'otp': otp};
      if (visitorId != null && visitorId > 0) {
        body['visitorId'] = visitorId.toString();
      }
      body['purpose'] = 'LOGIN';
      body['registrationFlow'] = false;
      final epic = (epicNumber ?? '').trim();
      if (epic.isNotEmpty) body['epicNumber'] = epic.toUpperCase();
      final resp = await http
          .post(
            _u('/auth/validate-otp'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));

      if (resp.statusCode == 200) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) return decoded;
        return {
          'success': false,
          'code': 'UNEXPECTED_RESPONSE',
          'message': 'Unexpected response. Please try again.',
          'requiresEpic': false,
        };
      }

      Map<String, dynamic>? decoded;
      try {
        final body = jsonDecode(resp.body);
        if (body is Map<String, dynamic>) decoded = body;
      } catch (_) {}
      return {
        'success': false,
        'code': decoded?['code'] ??
            decoded?['errorCode'] ??
            'HTTP_${resp.statusCode}',
        'message': _otpFailureMessage(response: resp, body: decoded),
        'requiresEpic': decoded?['requiresEpic'] == true,
        'attemptsRemaining':
            decoded?['attemptsRemaining'] ?? decoded?['remainingAttempts'],
        'remainingAttempts':
            decoded?['remainingAttempts'] ?? decoded?['attemptsRemaining'],
        'waitTimeMinutes': decoded?['waitTimeMinutes'],
      };
    } catch (error, stackTrace) {
      _logError('validateVisitorOtp', error, stackTrace);
      return {
        'success': false,
        'code': 'NETWORK_ERROR',
        'message': 'Network issue. Please check your connection and try again.',
        'requiresEpic': false,
      };
    }
  }

  static Future<Map<String, dynamic>> verifyEpic({
    required String epicNumber,
    required String visitorName,
    String? phoneNumber,
  }) async {
    try {
      final body = <String, dynamic>{
        'epicNumber': epicNumber.trim().toUpperCase(),
        'visitorName': visitorName.trim().toUpperCase(),
        'consentGranted': true,
        'consentVersion': AppConfig.consentVersion,
        'consentChannel': 'MOBILE',
      };
      final phone = (phoneNumber ?? '').trim();
      if (phone.isNotEmpty) body['phoneNumber'] = phone;
      final resp = await http
          .post(
            _u('/kyc/verify/epic'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 30));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }

      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': 'EPIC verification failed. Please try again.',
      };
    } catch (error, stackTrace) {
      _logError('verifyEpic', error, stackTrace);
      return {
        'success': false,
        'code': 'NETWORK_ERROR',
        'message': 'Network error. Please try again.',
      };
    }
  }

  static Future<Map<String, dynamic>> verifyVisitorRegistrationOtp({
    required String idNumber,
    required String otp,
    required String phoneNumber,
    required String idType,
  }) async {
    try {
      final body = <String, dynamic>{
        'otp': otp,
        'phoneNumber': phoneNumber,
        'idType': idType,
        'purpose': 'REGISTRATION',
        'registrationFlow': true,
      };
      if (idType.toUpperCase() == 'EPIC') {
        body['epicNumber'] = idNumber.trim().toUpperCase();
      } else {
        body['idNumber'] = idNumber;
      }
      final resp = await http
          .post(
            _u('/auth/validate-otp'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) return decoded;
        return {
          'success': false,
          'code': 'UNEXPECTED_RESPONSE',
          'message': 'Unexpected response. Please try again.',
        };
      }

      Map<String, dynamic>? decoded;
      try {
        final body = jsonDecode(resp.body);
        if (body is Map<String, dynamic>) decoded = body;
      } catch (_) {}
      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': _otpFailureMessage(response: resp, body: decoded),
      };
    } catch (error, stackTrace) {
      _logError('verifyVisitorRegistrationOtp', error, stackTrace);
      return {
        'success': false,
        'code': 'NETWORK_ERROR',
        'message': 'Network issue. Please check your connection and try again.',
      };
    }
  }

  static Future<Map<String, dynamic>> validateRegistrationOtp({
    required String phoneNumber,
    required String otp,
  }) async {
    try {
      final resp = await http
          .post(
            _u('/auth/validate-otp'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'otp': otp,
              'phoneNumber': phoneNumber,
              'purpose': 'REGISTRATION',
              'registrationFlow': true,
            }),
          )
          .timeout(const Duration(seconds: 20));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) return decoded;
        return {
          'success': false,
          'code': 'UNEXPECTED_RESPONSE',
          'message': 'Unexpected response. Please try again.',
        };
      }

      Map<String, dynamic>? decoded;
      try {
        final body = jsonDecode(resp.body);
        if (body is Map<String, dynamic>) decoded = body;
      } catch (_) {}
      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': _otpFailureMessage(response: resp, body: decoded),
      };
    } catch (error, stackTrace) {
      _logError('validateRegistrationOtp', error, stackTrace);
      return {
        'success': false,
        'code': 'NETWORK_ERROR',
        'message': 'Network issue. Please check your connection and try again.',
      };
    }
  }

  static Future<Map<String, dynamic>> checkVisitorRegistration({
    required String phoneNumber,
    String? epicNumber,
  }) async {
    try {
      final body = <String, dynamic>{'phoneNumber': phoneNumber};
      final epic = (epicNumber ?? '').trim();
      if (epic.isNotEmpty) body['epicNumber'] = epic.toUpperCase();
      final resp = await http
          .post(
            _u('/visitor/auth/check-registration'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }

      return {
        'success': false,
        'message': 'Unable to validate mobile number.',
      };
    } catch (error, stackTrace) {
      _logError('checkVisitorRegistration', error, stackTrace);
      return {
        'success': false,
        'message': 'Network error. Please try again.',
      };
    }
  }

  static Future<Map<String, dynamic>> searchVisitorRegistrations({
    required String phoneNumber,
  }) async {
    try {
      final resp = await http
          .post(
            _u('/visitor/auth/search-registrations'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'phoneNumber': phoneNumber}),
          )
          .timeout(const Duration(seconds: 20));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) return decoded;
      }

      return {
        'success': false,
        'registrations': [],
        'message': _messageFromResponse(
          resp,
          'Unable to load registrations for this mobile number.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('searchVisitorRegistrations', error, stackTrace);
      return {
        'success': false,
        'registrations': [],
        'message': 'Network issue. Please check your connection and try again.',
      };
    }
  }

  static Future<Map<String, dynamic>> registerVisitor(
      Map<String, dynamic> payload,
      {bool staffRegistration = false}) async {
    try {
      final resp = await http
          .post(
            _u(staffRegistration
                ? '/visitors/staff-register'
                : '/visitor/auth/register'),
            headers: staffRegistration
                ? await _headers()
                : {'Content-Type': 'application/json'},
            body: jsonEncode(payload),
          )
          .timeout(const Duration(seconds: 30));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }

      return {
        'success': false,
        'message': _messageFromResponse(
          resp,
          'Registration failed. Please try again.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('registerVisitor', error, stackTrace);
      return {
        'success': false,
        'message': 'Network error. Please try again.',
      };
    }
  }

  // Appointments
  static Future<Map<String, dynamic>> getAppointments({
    int page = 0,
    int size = 50,
    String? status,
    String? source,
    String? appointmentType,
    String? referredOffice,
    String? sort,
  }) async {
    try {
      final headers = await _headers();
      final params = <String, String>{
        'page': page.toString(),
        'size': size.toString(),
      };
      void addParam(String key, String? value) {
        final trimmed = value?.trim() ?? '';
        if (trimmed.isNotEmpty) params[key] = trimmed;
      }

      addParam('status', status);
      addParam('source', source);
      addParam('appointmentType', appointmentType);
      addParam('referredOffice', referredOffice);
      addParam('sort', sort);
      final resp = await http
          .get(
            _u('/appointments').replace(queryParameters: params),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return _normalizePageResponse(jsonDecode(resp.body));
      }
      return _listError(
        message: _messageFromResponse(
          resp,
          'Failed to load appointments. Please try again.',
        ),
      );
    } catch (error, stackTrace) {
      _logError('getAppointments', error, stackTrace);
    }
    return _listError(message: 'No internet connection. Please try again.');
  }

  static Future<Map<String, dynamic>> getAppointmentReport({
    required String report,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await http
          .get(
            _u('/reports/$report').replace(queryParameters: {
              'page': '$page',
              'size': '$size',
              'sort': report == 'completed-appointments'
                  ? 'completedAt,desc'
                  : 'rejectedAt,desc',
            }),
            headers: await _headers(),
          )
          .timeout(const Duration(seconds: 20));
      if (response.statusCode == 200) {
        return _normalizePageResponse(jsonDecode(response.body));
      }
      return _listError(message: 'Unable to load appointment report.');
    } catch (error, stackTrace) {
      _logError('getAppointmentReport', error, stackTrace);
      return _listError(message: 'Unable to load appointment report.');
    }
  }

  static Future<Map<String, dynamic>> getAppointmentAnalytics() async {
    try {
      final response = await http
          .get(_u('/reports/appointments/analytics'), headers: await _headers())
          .timeout(const Duration(seconds: 20));
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(jsonDecode(response.body) as Map);
      }
    } catch (error, stackTrace) {
      _logError('getAppointmentAnalytics', error, stackTrace);
    }
    return const {};
  }

  static Future<Map<String, dynamic>?> getAppointmentReportDetail({
    required String report,
    required int appointmentId,
  }) async {
    try {
      final response = await http
          .get(_u('/reports/$report/$appointmentId'), headers: await _headers())
          .timeout(const Duration(seconds: 20));
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return _unwrapObject(jsonDecode(response.body));
      }
    } catch (error, stackTrace) {
      _logError('getAppointmentReportDetail', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>> getMyAppointments() async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/appointments/my'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        final decoded = jsonDecode(resp.body);
        if (decoded is List) {
          return {'content': decoded, 'totalElements': decoded.length};
        }
        if (decoded is Map<String, dynamic>) {
          final data = decoded['data'];
          if (data is List) {
            return {'content': data, 'totalElements': data.length};
          }
          return _normalizePageResponse(decoded);
        }
      }
    } catch (error, stackTrace) {
      _logError('getMyAppointments', error, stackTrace);
    }
    return _listError();
  }

  static Future<Map<String, dynamic>> getDeoAppointments(
      {int page = 0, int size = 100}) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/appointments/deo?page=$page&size=$size'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return _normalizePageResponse(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getDeoAppointments', error, stackTrace);
    }
    return _listError();
  }

  static Future<Map<String, dynamic>> getApproverAppointments(
      {int page = 0, int size = 100}) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/appointments/approver?page=$page&size=$size'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return _normalizePageResponse(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getApproverAppointments', error, stackTrace);
    }
    return _listError();
  }

  static Future<Map<String, dynamic>?> getAppointmentById(int id) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(_u('/appointments/$id'), headers: headers)
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) {
          final data = decoded['data'];
          return data is Map<String, dynamic> ? data : decoded;
        }
      }
      if (resp.statusCode == 401 || resp.statusCode == 403) {
        await clearToken();
      }
    } catch (error, stackTrace) {
      _logError('getAppointmentById', error, stackTrace);
    }
    return null;
  }

  static Future<List<Map<String, dynamic>>> getAppointmentDocuments(
      int appointmentId) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(_u('/appointments/$appointmentId/documents'), headers: headers)
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _normalizeList(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getAppointmentDocuments', error, stackTrace);
    }
    return [];
  }

  static Future<List<Map<String, dynamic>>> getAiNotesByAppointment(
      int appointmentId) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(_u('/appointments/$appointmentId/ai-notes'), headers: headers)
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _normalizeList(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getAiNotesByAppointment', error, stackTrace);
    }
    return [];
  }

  static Future<Map<String, dynamic>?> regenerateAiNotes(int documentId) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/appointments/documents/$documentId/ai-notes/regenerate'),
            headers: headers,
            body: jsonEncode({}),
          )
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('regenerateAiNotes', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> getAiHealth() async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(_u('/ai/health'), headers: headers)
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getAiHealth', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> getAppointmentPriorityInsight(
      int appointmentId) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/ai/appointments/$appointmentId/priority-insight'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getAppointmentPriorityInsight', error, stackTrace);
    }
    return null;
  }

  static Future<List<Map<String, dynamic>>> getHcmActionAppointments(
      String date) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/appointments/hcm-actions')
                .replace(queryParameters: {'date': date}),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _normalizeList(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getHcmActionAppointments', error, stackTrace);
    }
    return [];
  }

  static Future<bool> submitHcmAction(
    int appointmentId,
    String action, {
    required Map<String, dynamic> payload,
  }) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/hcm/actions/appointment/$appointmentId/$action'),
            headers: headers,
            body: jsonEncode(payload),
          )
          .timeout(const Duration(seconds: 20));
      return resp.statusCode >= 200 && resp.statusCode < 300;
    } catch (error, stackTrace) {
      _logError('submitHcmAction', error, stackTrace);
    }
    return false;
  }

  static Future<Map<String, dynamic>?> getPublicIdentificationHistory(
      int citizenId) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/public-identification/citizens/$citizenId/full-history'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('getPublicIdentificationHistory', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> createAppointment(
      Map<String, dynamic> body) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/appointments'),
            headers: headers,
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) {
          final data = decoded['data'];
          return data is Map<String, dynamic> ? data : decoded;
        }
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          resp,
          'Unable to submit appointment. Please try again.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('createAppointment', error, stackTrace);
      return {
        'success': false,
        'message': 'Network error. Please try again.',
      };
    }
  }

  static Future<Map<String, dynamic>?> createAppointmentMultipart({
    required Map<String, String> fields,
    List<Map<String, String>> documents = const [],
  }) async {
    try {
      final request = http.MultipartRequest('POST', _u('/appointments'));
      request.headers.addAll(await _authHeaders());
      fields.forEach((key, value) {
        final trimmed = value.trim();
        if (trimmed.isNotEmpty) request.fields[key] = trimmed;
      });

      for (final document in documents) {
        final path = document['path']?.trim();
        final fieldName = document['fieldName']?.trim();
        if (path == null ||
            path.isEmpty ||
            fieldName == null ||
            fieldName.isEmpty) {
          continue;
        }
        request.files.add(await http.MultipartFile.fromPath(
          fieldName,
          path,
          filename: document['fileName'],
        ));
      }

      final streamed =
          await request.send().timeout(const Duration(seconds: 45));
      final response = await http.Response.fromStream(streamed);
      if (response.statusCode >= 200 && response.statusCode < 300) {
        final decoded = jsonDecode(response.body);
        if (decoded is Map<String, dynamic>) {
          final data = decoded['data'];
          return data is Map<String, dynamic> ? data : decoded;
        }
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          response,
          'Unable to submit appointment. Please try again.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('createAppointmentMultipart', error, stackTrace);
      return {
        'success': false,
        'message': 'Network error. Please try again.',
      };
    }
  }

  // Offline sync endpoints. Backend may not have these routes yet; callers keep
  // local records queued if any endpoint returns a non-success response.
  static Future<Map<String, dynamic>> syncVisitor(
      Map<String, dynamic> payload) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(_u('/sync/visitors'),
              headers: headers, body: jsonEncode(payload))
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': _messageFromResponse(
            resp, 'Visitor sync failed. Please try again.'),
      };
    } catch (error, stackTrace) {
      _logError('syncVisitor', error, stackTrace);
      return {'success': false, 'message': 'Network error. Please try again.'};
    }
  }

  static Future<Map<String, dynamic>> syncVisitorPhoto(
      Map<String, dynamic> payload) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(_u('/sync/visitor-photo'),
              headers: headers, body: jsonEncode(payload))
          .timeout(const Duration(seconds: 45));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message':
            _messageFromResponse(resp, 'Photo sync failed. Please try again.'),
      };
    } catch (error, stackTrace) {
      _logError('syncVisitorPhoto', error, stackTrace);
      return {'success': false, 'message': 'Network error. Please try again.'};
    }
  }

  static Future<Map<String, dynamic>> syncAppointment(
      Map<String, dynamic> payload) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(_u('/sync/appointments'),
              headers: headers, body: jsonEncode(payload))
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': _messageFromResponse(
            resp, 'Appointment sync failed. Please try again.'),
      };
    } catch (error, stackTrace) {
      _logError('syncAppointment', error, stackTrace);
      return {'success': false, 'message': 'Network error. Please try again.'};
    }
  }

  static Future<Map<String, dynamic>> syncAiNote(
      Map<String, dynamic> payload) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(_u('/sync/ai-notes'),
              headers: headers, body: jsonEncode(payload))
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': _messageFromResponse(
            resp, 'AI note sync failed. Please try again.'),
      };
    } catch (error, stackTrace) {
      _logError('syncAiNote', error, stackTrace);
      return {'success': false, 'message': 'Network error. Please try again.'};
    }
  }

  static Future<Map<String, dynamic>> syncStatusUpdate(
      Map<String, dynamic> payload) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(_u('/sync/status-update'),
              headers: headers, body: jsonEncode(payload))
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message':
            _messageFromResponse(resp, 'Action sync failed. Please try again.'),
      };
    } catch (error, stackTrace) {
      _logError('syncStatusUpdate', error, stackTrace);
      return {'success': false, 'message': 'Network error. Please try again.'};
    }
  }

  static Future<Map<String, dynamic>> getSyncMasterData() async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(_u('/sync/master-data'), headers: headers)
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return {'success': true, 'data': jsonDecode(resp.body)};
      }
    } catch (error, stackTrace) {
      _logError('getSyncMasterData', error, stackTrace);
    }
    return {'success': false, 'message': 'Unable to refresh master data.'};
  }

  static Future<Map<String, dynamic>> getSyncAppointmentPreload() async {
    try {
      final headers = await _headers();
      final from = DateTime.now()
          .toUtc()
          .subtract(const Duration(days: 7))
          .toIso8601String();
      final resp = await http
          .get(
            _u('/sync/appointments/preload')
                .replace(queryParameters: {'from': from, 'days': '7'}),
            headers: headers,
          )
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        if (decoded is List) return {'content': decoded};
        if (decoded is Map<String, dynamic>) return decoded;
      }
    } catch (error, stackTrace) {
      _logError('getSyncAppointmentPreload', error, stackTrace);
    }
    return {'content': []};
  }

  static Future<Map<String, dynamic>> getSyncQrCache() async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(_u('/sync/qr-cache'), headers: headers)
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return {'success': true, 'data': jsonDecode(resp.body)};
      }
    } catch (error, stackTrace) {
      _logError('getSyncQrCache', error, stackTrace);
    }
    return {'success': false, 'message': 'Unable to refresh QR cache.'};
  }

  static Future<List<Map<String, String>>> getReferenceData(String type,
      {String? parentCode}) async {
    try {
      final headers = await _authHeaders();
      final resp = await http
          .get(
            _u('/reference/$type${parentCode == null ? '' : '?parentCode=${Uri.encodeQueryComponent(parentCode)}'}'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        final rows = decoded is List
            ? decoded
            : decoded is Map<String, dynamic> && decoded['data'] is List
                ? decoded['data'] as List<dynamic>
                : <dynamic>[];
        return rows.whereType<Map>().map((row) {
          final code = row['code']?.toString() ?? '';
          final value = row['value']?.toString() ?? code;
          return {'code': code, 'value': value};
        }).where((row) {
          return row['code']!.isNotEmpty;
        }).toList();
      }
    } catch (_) {}
    return [];
  }

  static Future<Map<String, dynamic>> submitDepartmentAccessRequest(
      Map<String, dynamic> request) async {
    try {
      final response = await http
          .post(_u('/department-access-requests'),
              headers: const {'Content-Type': 'application/json'},
              body: jsonEncode(request))
          .timeout(const Duration(seconds: 30));
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return {'success': true};
      }
      return {
        'success': false,
        'duplicate': response.statusCode == 409,
        'message': _messageFromResponse(
            response, 'Unable to submit the request. Please try again.'),
      };
    } catch (error, stackTrace) {
      _logError('submitDepartmentAccessRequest', error, stackTrace);
      return {
        'success': false,
        'message': 'Network unavailable. Please try again.'
      };
    }
  }

  static Future<Map<String, dynamic>> searchVisitorByFace(String photo) async {
    try {
      final request = <String, dynamic>{
        'photo': normalizeFacePhoto(photo),
        'includeMatchedPhoto': false,
      };
      final resp = await http
          .post(_u('/face-recognition/search'),
              headers: await _headers(), body: jsonEncode(request))
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return Map<String, dynamic>.from(jsonDecode(resp.body) as Map);
      }
      return {
        'success': false,
        'message': _messageFromResponse(resp, 'Face search is unavailable.')
      };
    } catch (error, stackTrace) {
      _logError('searchVisitorByFace', error, stackTrace);
      return {'success': false, 'message': 'Face search is unavailable.'};
    }
  }

  static Future<Map<String, dynamic>> searchEpicByFace(String photo) async {
    try {
      final resp = await http
          .post(_u('/epic/face/search'),
              headers: await _headers(),
              body: jsonEncode({
                'photo': normalizeFacePhoto(photo),
                'source': 'MOBILE_WALK_IN'
              }))
          .timeout(const Duration(seconds: 95));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return Map<String, dynamic>.from(jsonDecode(resp.body) as Map);
      }
      return {
        'matched': false,
        'providerUnavailable': true,
        'message': _messageFromResponse(
            resp, 'EPIC face search is temporarily unavailable.')
      };
    } catch (error, stackTrace) {
      _logError('searchEpicByFace', error, stackTrace);
      return {
        'matched': false,
        'providerUnavailable': true,
        'message': 'EPIC face search is temporarily unavailable.'
      };
    }
  }

  static Future<Map<String, dynamic>> verifyEpicFace(
      String epicNumber, String photo) async {
    try {
      final resp = await http
          .post(_u('/epic/face/verify'),
              headers: await _headers(),
              body: jsonEncode({
                'epicNumber': epicNumber,
                'photo': normalizeFacePhoto(photo)
              }))
          .timeout(const Duration(seconds: 95));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return Map<String, dynamic>.from(jsonDecode(resp.body) as Map);
      }
      return {
        'matched': false,
        'providerUnavailable': true,
        'message': _messageFromResponse(
            resp, 'EPIC face verification is temporarily unavailable.')
      };
    } catch (error, stackTrace) {
      _logError('verifyEpicFace', error, stackTrace);
      return {
        'matched': false,
        'providerUnavailable': true,
        'message': 'EPIC face verification is temporarily unavailable.'
      };
    }
  }

  static String normalizeFacePhoto(String photo) {
    final value = photo.trim();
    if (value.isEmpty) {
      throw const FormatException('Face photo is required.');
    }
    final prefix =
        RegExp(r'^data:image/(jpeg|png);base64,', caseSensitive: false);
    if (value.toLowerCase().startsWith('data:') && !prefix.hasMatch(value)) {
      throw const FormatException(
          'Only JPEG or PNG face images are supported.');
    }
    final normalized = value.replaceFirst(prefix, '');
    if (normalized.isEmpty) {
      throw const FormatException('Face photo is required.');
    }
    return normalized;
  }

  static Future<Map<String, dynamic>> createGuestAppointment({
    required Map<String, String> fields,
    required String livePhotoBase64,
    String? supportingDocumentPath,
    String? supportingDocumentName,
  }) async {
    try {
      final request = http.MultipartRequest('POST', _u('/guest-appointments'));
      request.headers.addAll(await _authHeaders());
      fields.forEach((key, value) {
        final trimmed = value.trim();
        if (trimmed.isNotEmpty) request.fields[key] = trimmed;
      });
      request.fields['livePhotoBase64'] = livePhotoBase64;

      final documentPath = supportingDocumentPath?.trim();
      if (documentPath != null && documentPath.isNotEmpty) {
        request.files.add(await http.MultipartFile.fromPath(
          'supportingDocument',
          documentPath,
          filename: supportingDocumentName,
        ));
      }

      final streamed =
          await request.send().timeout(const Duration(seconds: 45));
      final response = await http.Response.fromStream(streamed);
      if (response.statusCode >= 200 && response.statusCode < 300) {
        final decoded = jsonDecode(response.body);
        if (decoded is Map<String, dynamic>) return decoded;
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          response,
          'Unable to submit guest appointment.',
        ),
      };
    } catch (_) {
      return {
        'success': false,
        'message': 'Network error. Please try again.',
      };
    }
  }

  static Future<Map<String, dynamic>?> updateAppointmentStatus(
      int id, String status,
      {String? remarks}) async {
    try {
      final headers = await _headers();
      final body = <String, dynamic>{'status': status};
      if (remarks != null) {
        body['remarks'] = remarks;
        if (status == 'REJECTED') body['rejectionReason'] = remarks;
      }
      final resp = await http
          .patch(
            _u('/appointments/$id/status'),
            headers: headers,
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> schedulePendingAppointment(
      int id, DateTime date,
      {String? remarks}) async {
    final start = DateTime(date.year, date.month, date.day, 10);
    final end = start.add(const Duration(minutes: 30));
    return _appointmentJsonPut(
        '/appointments/$id/schedule',
        {
          'startTime': start.toIso8601String(),
          'endTime': end.toIso8601String(),
          'location': 'CM_OFFICE',
          'remarks': remarks,
        },
        'schedulePendingAppointment');
  }

  static Future<Map<String, dynamic>?> rejectPendingAppointment(
      int id, String reason) async {
    return _appointmentJsonPost('/appointments/approver/$id/reject',
        {'reason': reason}, 'rejectPendingAppointment');
  }

  static Future<Map<String, dynamic>?> routePendingAppointment(int id,
      {required String officer, String? direction}) async {
    try {
      final response = await http
          .post(_u('/appointments/$id/route'),
              headers: await _headers(),
              body: jsonEncode({
                'officer': officer,
                'direction': direction,
                'followUpRequired': false
              }))
          .timeout(const Duration(seconds: 20));
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> approveAppointment(
    int id, {
    String? remarks,
  }) async {
    return _appointmentJsonPut(
      '/appointments/$id/approve',
      {'remarks': remarks},
      'approveAppointment',
    );
  }

  static Future<Map<String, dynamic>?> rejectAppointment(
    int id, {
    String? remarks,
  }) async {
    return _appointmentJsonPost(
      '/appointments/approver/$id/reject',
      {'reason': remarks},
      'rejectAppointment',
    );
  }

  static Future<Map<String, dynamic>?> submitCmoReview({
    required int appointmentId,
    String? eventType,
    String? requestedLocation,
    String? cmoRemarks,
    String? pendingInformation,
    required String status,
    bool notifyApplicant = false,
    bool notifyDeo = false,
  }) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/appointments/$appointmentId/cmo-review'),
            headers: headers,
            body: jsonEncode({
              'eventType': eventType,
              'requestedLocation': requestedLocation,
              'cmoRemarks': cmoRemarks,
              'pendingInformation': pendingInformation,
              'status': status,
              'notifyApplicant': notifyApplicant,
              'notifyDeo': notifyDeo,
            }),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('submitCmoReview', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> uploadSupportingDocument(
    int appointmentId,
    String filePath, {
    String? fileName,
    String documentType = 'SUPPORTING_DOCUMENT',
    String? remarks,
  }) async {
    try {
      final request = http.MultipartRequest(
        'POST',
        _u('/appointments/$appointmentId/supporting-documents'),
      );
      request.headers.addAll(await _authHeaders());
      request.fields['documentType'] = documentType;
      if ((remarks ?? '').trim().isNotEmpty) {
        request.fields['remarks'] = remarks!.trim();
      }
      request.files.add(await http.MultipartFile.fromPath(
        'file',
        filePath,
        filename: fileName,
      ));
      final streamed =
          await request.send().timeout(const Duration(seconds: 45));
      final response = await http.Response.fromStream(streamed);
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return _unwrapObject(jsonDecode(response.body));
      }
    } catch (error, stackTrace) {
      _logError('uploadSupportingDocument', error, stackTrace);
    }
    return null;
  }

  static Future<Uint8List?> downloadDocumentBytes(int documentId) async {
    try {
      final headers = await _authHeaders();
      final resp = await http
          .get(_fileApi('/files/download/$documentId'), headers: headers)
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return resp.bodyBytes;
      }
    } catch (error, stackTrace) {
      _logError('downloadDocumentBytes', error, stackTrace);
    }
    return null;
  }

  static Future<Uint8List?> downloadVisitorPass(int appointmentId) async {
    try {
      final response = await http
          .get(
            _u('/appointments/$appointmentId/visitor-pass/download'),
            headers: await _headers(),
          )
          .timeout(const Duration(seconds: 30));
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return response.bodyBytes;
      }
    } catch (error, stackTrace) {
      _logError('downloadVisitorPass', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>> validateQr({
    required String qrToken,
    required String deviceId,
  }) async {
    try {
      final response = await http
          .post(
            _u('/qr/validate'),
            headers: await _headers(),
            body: jsonEncode({
              'qrToken': qrToken,
              'qrData': null,
              'deviceId': deviceId,
              'location': 'MeghaConnect Mobile',
            }),
          )
          .timeout(const Duration(seconds: 30));
      final decoded = jsonDecode(response.body);
      if (response.statusCode >= 200 &&
          response.statusCode < 300 &&
          decoded is Map<String, dynamic>) {
        final data = decoded['data'];
        return data is Map<String, dynamic>
            ? {'success': true, ...data}
            : {'success': false, 'message': 'QR validation returned no data.'};
      }
      return {
        'success': false,
        'message': decoded is Map
            ? decoded['message']?.toString() ?? 'Unable to validate QR code.'
            : 'Unable to validate QR code.',
      };
    } catch (error, stackTrace) {
      _logError('validateQr', error, stackTrace);
      return {
        'success': false,
        'message': 'QR validation service is unavailable. Please try again.',
      };
    }
  }

  static Future<Uint8List?> previewDocumentBytes(int documentId) async {
    try {
      final headers = await _authHeaders();
      final resp = await http
          .get(_fileApi('/files/preview/$documentId'), headers: headers)
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return resp.bodyBytes;
      }
    } catch (error, stackTrace) {
      _logError('previewDocumentBytes', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> _appointmentJsonPut(
    String path,
    Map<String, dynamic> body,
    String action,
  ) async {
    try {
      final headers = await _headers();
      final sanitized = Map<String, dynamic>.from(body)
        ..removeWhere((_, value) => value == null);
      final resp = await http
          .put(
            _u(path),
            headers: headers,
            body: jsonEncode(sanitized),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError(action, error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> _appointmentJsonPost(
    String path,
    Map<String, dynamic> body,
    String action,
  ) async {
    try {
      final headers = await _headers();
      final sanitized = Map<String, dynamic>.from(body)
        ..removeWhere((_, value) => value == null);
      final resp = await http
          .post(
            _u(path),
            headers: headers,
            body: jsonEncode(sanitized),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError(action, error, stackTrace);
    }
    return null;
  }

  static Future<List<Map<String, dynamic>>> getAppointmentRemarks(
      int appointmentId) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(_u('/appointments/$appointmentId/remarks'), headers: headers)
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        final rows = decoded is List
            ? decoded
            : decoded is Map<String, dynamic> && decoded['data'] is List
                ? decoded['data'] as List<dynamic>
                : <dynamic>[];
        return rows
            .whereType<Map>()
            .map((row) => Map<String, dynamic>.from(row))
            .toList();
      }
    } catch (_) {}
    return [];
  }

  static Future<Map<String, dynamic>?> addAppointmentRemark(
    int appointmentId, {
    required String remarks,
    String? decision,
    String? departmentCode,
  }) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/appointments/$appointmentId/remarks'),
            headers: headers,
            body: jsonEncode({
              'hcmRemarks': remarks,
              'decision': decision,
              'departmentCode': departmentCode,
            }),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> requestAppointmentMissingInformation(
          int appointmentId,
          {String remarks = ''}) =>
      _postAppointmentAction(
          '/appointments/$appointmentId/request-missing-information',
          {'remarks': remarks});

  static Future<Map<String, dynamic>?> closeAppointment(int appointmentId,
          {String remarks = ''}) =>
      _postAppointmentAction(
          '/appointments/$appointmentId/close', {'remarks': remarks});

  static Future<Map<String, dynamic>?> _postAppointmentAction(
      String path, Map<String, dynamic> body) async {
    try {
      final response = await http
          .post(_u(path), headers: await _headers(), body: jsonEncode(body))
          .timeout(const Duration(seconds: 20));
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return _unwrapObject(jsonDecode(response.body));
      }
    } catch (error, stackTrace) {
      _logError('appointmentAction', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> updateAppointmentRemark(
    int appointmentId,
    int remarkId, {
    required String remarks,
    String? decision,
    String? departmentCode,
  }) async {
    try {
      final headers = await _headers();
      final resp = await http
          .put(
            _u('/appointments/$appointmentId/remarks/$remarkId'),
            headers: headers,
            body: jsonEncode({
              'hcmRemarks': remarks,
              'decision': decision,
              'departmentCode': departmentCode,
            }),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> scheduleAppointment(
      int id, String scheduledDateTime, int durationMinutes) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/appointments/$id/schedule'),
            headers: headers,
            body: jsonEncode({
              'scheduledDateTime': scheduledDateTime,
              'durationMinutes': durationMinutes,
            }),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  // Grievances
  static Future<Map<String, dynamic>> getGrievances({
    int page = 0,
    int size = 50,
    int? visitorId,
  }) async {
    try {
      final headers = await _headers();
      final path = visitorId != null && visitorId > 0
          ? '/grievances/visitor/$visitorId'
          : '/grievances';
      final resp = await http
          .get(
            _u(path).replace(queryParameters: {
              'page': page.toString(),
              'size': size.toString(),
            }),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
  }

  static Future<Map<String, dynamic>?> createGrievance(
      Map<String, dynamic> body) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/grievances'),
            headers: headers,
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200 || resp.statusCode == 201) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          resp,
          'Failed to submit grievance. Please try again.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('createGrievance', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> updateGrievanceStatus(
      int id, String status,
      {String? remarks}) async {
    try {
      final headers = await _headers();
      final body = <String, dynamic>{'status': status};
      if (remarks != null) body['remarks'] = remarks;
      final resp = await http
          .patch(
            _u('/grievances/$id/status'),
            headers: headers,
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  // Schedule events
  static Future<List<dynamic>> getScheduleEvents({
    DateTime? start,
    DateTime? end,
  }) async {
    try {
      final headers = await _headers();
      final params = <String, String>{};
      if (start != null) params['start'] = _localIso(start);
      if (end != null) params['end'] = _localIso(end);
      final resp = await http
          .get(
            _u('/schedule').replace(queryParameters: params),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        final decoded = jsonDecode(resp.body);
        return decoded is List ? decoded : _normalizeList(decoded);
      }
    } catch (error, stackTrace) {
      _logError('getScheduleEvents', error, stackTrace);
    }
    return [];
  }

  static Future<Map<String, dynamic>?> createScheduleEvent(
      Map<String, dynamic> body) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/schedule'),
            headers: headers,
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200 || resp.statusCode == 201) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          resp,
          'Failed to create event. Please try again.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('createScheduleEvent', error, stackTrace);
    }
    return {
      'success': false,
      'message': 'Network issue. Please check your connection and try again.',
    };
  }

  static Future<Map<String, dynamic>?> updateScheduleEvent(
    int id,
    Map<String, dynamic> body,
  ) async {
    try {
      final headers = await _headers();
      final resp = await http
          .put(
            _u('/schedule/$id'),
            headers: headers,
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          resp,
          'Failed to update event. Please try again.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('updateScheduleEvent', error, stackTrace);
    }
    return {
      'success': false,
      'message': 'Network issue. Please check your connection and try again.',
    };
  }

  static Future<bool> deleteScheduleEvent(int id) async {
    try {
      final headers = await _headers();
      final resp = await http
          .delete(_u('/schedule/$id'), headers: headers)
          .timeout(const Duration(seconds: 20));
      return resp.statusCode >= 200 && resp.statusCode < 300;
    } catch (error, stackTrace) {
      _logError('deleteScheduleEvent', error, stackTrace);
    }
    return false;
  }

  static String _localIso(DateTime value) {
    String two(int v) => v.toString().padLeft(2, '0');
    return '${value.year}-${two(value.month)}-${two(value.day)}T'
        '${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
  }

  static Future<Map<String, dynamic>?> assignAppointmentsToScheduleEvent(
    int id, {
    required List<int> appointmentIds,
    String remarks = 'Scheduled',
  }) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/schedule/$id/appointments'),
            headers: headers,
            body: jsonEncode({
              'appointmentIds': appointmentIds,
              'remarks': remarks,
            }),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (error, stackTrace) {
      _logError('assignAppointmentsToScheduleEvent', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> removeAppointmentFromScheduleEvent(
    int eventId,
    int appointmentId,
  ) async {
    try {
      final headers = await _headers();
      final resp = await http
          .delete(
            _u('/schedule/$eventId/appointments/$appointmentId'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (error, stackTrace) {
      _logError('removeAppointmentFromScheduleEvent', error, stackTrace);
    }
    return null;
  }

  // Audit logs
  static Future<Map<String, dynamic>> getAuditLogs({
    int page = 0,
    int size = 100,
    String? module,
    String? action,
    String? user,
    String? role,
    String? requestId,
    String? from,
    String? to,
  }) async {
    try {
      final headers = await _headers();
      final params = <String, String>{
        'page': page.toString(),
        'size': size.toString(),
        'sort': 'timestamp,desc',
      };
      void addParam(String key, String? value) {
        final trimmed = (value ?? '').trim();
        if (trimmed.isNotEmpty) params[key] = trimmed;
      }

      addParam('module', module);
      addParam('action', action);
      addParam('user', user);
      addParam('role', role);
      addParam('requestId', requestId);
      addParam('from', from);
      addParam('to', to);

      final resp = await http
          .get(
            Uri.parse('${AppConfig.apiV1BaseUrl}/audit-logs')
                .replace(queryParameters: params),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
  }

  // Visitors (used by "Public Identification" screen)
  static Future<List<Map<String, dynamic>>> searchVisitorsByPhone(
      String phone) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitors/search/phone/${Uri.encodeComponent(phone)}'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _normalizeList(jsonDecode(resp.body));
      }
      if (resp.statusCode == 401 || resp.statusCode == 403) {
        await clearToken();
      }
    } catch (error, stackTrace) {
      _logError('searchVisitorsByPhone', error, stackTrace);
    }
    return [];
  }

  static Future<Map<String, dynamic>?> searchPersonByPhone(String phone) async {
    final rows = await searchVisitorsByPhone(phone);
    return rows.isEmpty ? null : rows.first;
  }

  static Future<List<dynamic>> searchVisitors({
    String? mobile,
    String? epic,
    String? referenceId,
  }) async {
    try {
      final headers = await _headers();
      final params = <String, String>{};
      final m = (mobile ?? '').trim();
      final e = (epic ?? '').trim();
      final r = (referenceId ?? '').trim();
      if (m.isNotEmpty) params['mobile'] = m;
      if (e.isNotEmpty) params['epic'] = e.toUpperCase();
      if (r.isNotEmpty) params['referenceId'] = r;
      final resp = await http
          .get(
            _u('/visitors/search').replace(queryParameters: params),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as List<dynamic>;
      }
    } catch (_) {}
    return [];
  }

  static Future<Map<String, dynamic>?> getVisitorById(int id) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitors/$id'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) {
          return _unwrapObject(decoded);
        }
      }
    } catch (error, stackTrace) {
      _logError('searchPersonByEpic', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> getVisitorProfileById(int id) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitor/auth/profile/$id'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) {
          final data = decoded['data'];
          return data is Map<String, dynamic> ? data : decoded;
        }
      }
      if (resp.statusCode == 401 || resp.statusCode == 403) {
        await clearToken();
      }
    } catch (error, stackTrace) {
      _logError('getVisitorProfileById', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>> retryVisitorKyc({
    required int visitorId,
    required String name,
    required String epicNumber,
  }) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/visitor/auth/profile/$visitorId/kyc/retry'),
            headers: headers,
            body: jsonEncode({
              'name': name.trim(),
              'epicNumber': epicNumber.trim().toUpperCase(),
            }),
          )
          .timeout(const Duration(seconds: 30));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        if (decoded is Map<String, dynamic>) return decoded;
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          resp,
          'Unable to verify EPIC details. Please try again.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('retryVisitorKyc', error, stackTrace);
      return {
        'success': false,
        'message': 'Network issue. Please check your connection and try again.',
      };
    }
  }

  static Future<Map<String, dynamic>?> searchPersonByEpic(String epic) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitors/search/epic/${Uri.encodeComponent(epic)}'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final decoded = jsonDecode(resp.body);
        final visitor = _unwrapObject(decoded);
        return visitor.isEmpty ? null : visitor;
      }
      if (resp.statusCode == 401 || resp.statusCode == 403) {
        await clearToken();
      }
    } catch (error, stackTrace) {
      _logError('searchPersonByEpic', error, stackTrace);
    }
    return null;
  }

  static Future<List<dynamic>> searchPersonsByName(String q) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitors/search/name?q=${Uri.encodeComponent(q)}'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return _normalizeList(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('searchPersonsByName', error, stackTrace);
    }
    return [];
  }

  static Future<List<dynamic>> searchPersonsByDistrict(String d) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitors/search/district/${Uri.encodeComponent(d)}'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return _normalizeList(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('searchPersonsByDistrict', error, stackTrace);
    }
    return [];
  }

  // Directions (follow-ups)
  static Future<List<dynamic>> getDirections() async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/directions'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as List<dynamic>;
      }
    } catch (_) {}
    return [];
  }

  // Users
  static Future<Map<String, dynamic>?> getUsers({
    int page = 0,
    int size = 20,
    String? search,
    String? role,
    String? active,
    String? locked,
  }) async {
    try {
      final headers = await _headers();
      final query = <String, String>{'page': '$page', 'size': '$size'};
      if (search != null && search.trim().isNotEmpty) {
        query['search'] = search.trim();
      }
      if (role != null && role.isNotEmpty) query['role'] = role;
      if (active != null && active.isNotEmpty) query['active'] = active;
      if (locked != null && locked.isNotEmpty) query['locked'] = locked;
      final resp = await http
          .get(
            _u('/users').replace(queryParameters: query),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> createUser(
      Map<String, dynamic> payload) async {
    try {
      final headers = await _headers();
      final resp = await http
          .post(
            _u('/users'),
            headers: headers,
            body: jsonEncode(payload),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('createUser', error, stackTrace);
    }
    return null;
  }

  static Future<Map<String, dynamic>?> updateUser(
    int id,
    Map<String, dynamic> payload,
  ) async {
    try {
      final headers = await _headers();
      final resp = await http
          .put(
            _u('/users/$id'),
            headers: headers,
            body: jsonEncode(payload),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError('updateUser', error, stackTrace);
    }
    return null;
  }

  static Future<bool> deleteUser(int id) async {
    try {
      final headers = await _headers();
      final resp = await http
          .delete(_u('/users/$id'), headers: headers)
          .timeout(const Duration(seconds: 20));
      return resp.statusCode >= 200 && resp.statusCode < 300;
    } catch (error, stackTrace) {
      _logError('deleteUser', error, stackTrace);
    }
    return false;
  }

  static Future<Map<String, dynamic>?> setUserActive(
    int id,
    bool active,
  ) async {
    return _userPatch(id, active ? 'activate' : 'deactivate', 'setUserActive');
  }

  static Future<Map<String, dynamic>?> unlockUser(int id) async {
    return _userPatch(id, 'unlock', 'unlockUser');
  }

  static Future<Map<String, dynamic>?> _userPatch(
    int id,
    String action,
    String logAction,
  ) async {
    try {
      final headers = await _headers();
      final resp = await http
          .patch(
            _u('/users/$id/$action'),
            headers: headers,
            body: jsonEncode({}),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return _unwrapObject(jsonDecode(resp.body));
      }
    } catch (error, stackTrace) {
      _logError(logAction, error, stackTrace);
    }
    return null;
  }

  // Scheme applications
  static Future<Map<String, dynamic>> getSchemeApplications(
      {int page = 0, int size = 50}) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            Uri.parse(
                '${AppConfig.apiV1BaseUrl}/scheme-applications?page=$page&size=$size'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
  }

  static Future<Map<String, dynamic>> getSchemeApplicationsForVisitor(
    int visitorId, {
    int size = 5,
  }) async {
    if (visitorId <= 0) return {'content': [], 'totalElements': 0};
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/scheme-applications/visitor/$visitorId'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        final decoded = jsonDecode(resp.body);
        if (decoded is List) {
          final content = decoded
              .whereType<Map>()
              .map((row) => Map<String, dynamic>.from(row))
              .take(size)
              .toList();
          return {'content': content, 'totalElements': decoded.length};
        }
      }
    } catch (error, stackTrace) {
      _logError('getSchemeApplicationsForVisitor', error, stackTrace);
    }
    return {'content': [], 'totalElements': 0};
  }

  static Future<Map<String, dynamic>> createSchemeApplicationMultipart({
    required Map<String, String> fields,
    List<Map<String, String>> documents = const [],
  }) async {
    try {
      final request = http.MultipartRequest('POST', _u('/scheme-applications'));
      request.headers.addAll(await _authHeaders());
      fields.forEach((key, value) {
        final trimmed = value.trim();
        if (trimmed.isNotEmpty) request.fields[key] = trimmed;
      });
      for (final document in documents) {
        final path = document['path']?.trim();
        final fieldName = document['fieldName']?.trim();
        if (path == null ||
            path.isEmpty ||
            fieldName == null ||
            fieldName.isEmpty) {
          continue;
        }
        request.files.add(await http.MultipartFile.fromPath(
          fieldName,
          path,
          filename: document['fileName'],
        ));
      }
      final streamed =
          await request.send().timeout(const Duration(seconds: 45));
      final response = await http.Response.fromStream(streamed);
      if (response.statusCode >= 200 && response.statusCode < 300) {
        final decoded = jsonDecode(response.body);
        if (decoded is Map<String, dynamic>) return decoded;
      }
      return {
        'success': false,
        'message': _messageFromResponse(
          response,
          'Failed to submit scheme application.',
        ),
      };
    } catch (error, stackTrace) {
      _logError('createSchemeApplicationMultipart', error, stackTrace);
      return {
        'success': false,
        'message': 'No internet connection. Please try again.',
      };
    }
  }
}
