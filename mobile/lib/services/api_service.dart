import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../models/user.dart';
import '../core/config/app_config.dart';
import '../core/security/secure_app_storage.dart';

class ApiService {
  static Uri _u(String path) => Uri.parse('${AppConfig.apiV1BaseUrl}$path');
  static String? lastLoginError;

  static Future<String?> getToken() async {
    return SecureAppStorage.readToken();
  }

  static Future<void> setToken(String token, {Duration? ttl}) async {
    await SecureAppStorage.writeToken(token, ttl: ttl);
  }

  static Future<void> clearToken() async {
    await SecureAppStorage.clearToken();
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

  static String _messageFromResponse(http.Response response, String fallback) {
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map<String, dynamic>) {
        final code = decoded['code']?.toString().toUpperCase() ?? '';
        if (code.contains('UNAUTHORIZED') || response.statusCode == 401) {
          return 'Your session has expired. Please login again.';
        }
        if (response.statusCode == 403) {
          return 'You do not have permission to perform this action.';
        }
      }
    } catch (_) {}
    return fallback;
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

  // Maps a role string from the backend (e.g. "ROLE_HCM" or "HCM") to a UserRole.
  static UserRole _parseRole(String raw) {
    final normalized = raw.startsWith('ROLE_') ? raw.substring(5) : raw;
    return UserRole.values.firstWhere(
      (r) => r.name == normalized,
      orElse: () => UserRole.PUBLIC,
    );
  }

  // Auth
  static Future<Map<String, dynamic>?> login(
      String username, String password) async {
    lastLoginError = null;
    try {
      final resp = await http
          .post(
            _u('/auth/login'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'username': username, 'password': password}),
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        final data = jsonDecode(resp.body) as Map<String, dynamic>;
        // normalise role field before returning
        if (data['role'] != null) {
          data['role'] = _parseRole(data['role'].toString()).name;
        }
        return data;
      }
      lastLoginError = resp.statusCode == 401 || resp.statusCode == 403
          ? 'Invalid username or password.'
          : 'Login failed. Please try again.';
      return null;
    } catch (error, stackTrace) {
      _logError('login', error, stackTrace);
      lastLoginError =
          'Unable to connect. Please check your network and try again.';
      return null;
    }
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
    bool registrationFlow = false,
  }) async {
    try {
      final body = <String, dynamic>{'phoneNumber': phoneNumber};
      if (registrationFlow) {
        body['purpose'] = 'REGISTRATION';
        body['registrationFlow'] = true;
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
  }) async {
    try {
      final body = <String, dynamic>{'phoneNumber': phoneNumber, 'otp': otp};
      final epic = (epicNumber ?? '').trim();
      if (epic.isNotEmpty) body['epicNumber'] = epic.toUpperCase();
      final resp = await http
          .post(
            _u('/visitor/auth/validate-otp'),
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
        'message': 'Failed to verify OTP. Please try again.',
        'requiresEpic': false,
      };
    } catch (error, stackTrace) {
      _logError('validateVisitorOtp', error, stackTrace);
      return {
        'success': false,
        'code': 'NETWORK_ERROR',
        'message': 'Network error. Please try again.',
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
      final resp = await http
          .post(
            _u('/visitor/verify-otp'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'idNumber': idNumber,
              'otp': otp,
              'phoneNumber': phoneNumber,
              'idType': idType,
            }),
          )
          .timeout(const Duration(seconds: 20));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }

      return {
        'success': false,
        'code': 'HTTP_${resp.statusCode}',
        'message': 'OTP verification failed. Please try again.',
      };
    } catch (error, stackTrace) {
      _logError('verifyVisitorRegistrationOtp', error, stackTrace);
      return {
        'success': false,
        'code': 'NETWORK_ERROR',
        'message': 'Network error. Please try again.',
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

  static Future<Map<String, dynamic>> generateAadhaarQr() async {
    try {
      final resp = await http
          .post(
            _u('/kyc/aadhaar/generate-qr'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({}),
          )
          .timeout(const Duration(seconds: 30));

      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }

      return {
        'success': false,
        'errorMessage': 'Failed to generate Aadhaar QR.',
      };
    } catch (error, stackTrace) {
      _logError('generateAadhaarQr', error, stackTrace);
      return {
        'success': false,
        'errorMessage': 'Network error. Please try again.',
      };
    }
  }

  static Future<Map<String, dynamic>> registerVisitor(
      Map<String, dynamic> payload) async {
    try {
      final resp = await http
          .post(
            _u('/visitor/auth/register'),
            headers: {'Content-Type': 'application/json'},
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
  static Future<Map<String, dynamic>> getAppointments(
      {int page = 0, int size = 50}) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/appointments?page=$page&size=$size'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (error, stackTrace) {
      _logError('getAppointments', error, stackTrace);
    }
    return _listError();
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
          return decoded;
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
        return jsonDecode(resp.body) as Map<String, dynamic>;
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
        return jsonDecode(resp.body) as Map<String, dynamic>;
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

  static Future<List<Map<String, String>>> getReferenceData(String type) async {
    try {
      final headers = await _authHeaders();
      final resp = await http
          .get(
            _u('/reference/$type'),
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
      if (remarks != null) body['remarks'] = remarks;
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
  static Future<Map<String, dynamic>> getGrievances(
      {int page = 0, int size = 50}) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/grievances?page=$page&size=$size'),
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
    } catch (_) {}
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
  static Future<List<dynamic>> getScheduleEvents() async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/schedule'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as List<dynamic>;
      }
    } catch (_) {}
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
    } catch (_) {}
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
  static Future<Map<String, dynamic>?> searchPersonByPhone(String phone) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitors/search/phone/$phone'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
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
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> searchPersonByEpic(String epic) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/visitors/search/epic/$epic'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
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
        return jsonDecode(resp.body) as List<dynamic>;
      }
    } catch (_) {}
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
        return jsonDecode(resp.body) as List<dynamic>;
      }
    } catch (_) {}
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
  static Future<List<dynamic>> getUsers() async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            _u('/users'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 20));
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as List<dynamic>;
      }
    } catch (_) {}
    return [];
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
}
