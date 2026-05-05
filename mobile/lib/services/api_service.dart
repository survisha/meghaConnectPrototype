import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';
import '../core/config/app_config.dart';

class ApiService {
  static const String _tokenKey = 'megha_token';

  static Uri _u(String path) => Uri.parse('${AppConfig.apiV1BaseUrl}$path');

  static Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  static Future<void> setToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
  }

  static Future<void> clearToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
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
      return null;
    } catch (_) {
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
  }) async {
    try {
      final body = <String, dynamic>{'phoneNumber': phoneNumber};
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
    } catch (_) {
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
    } catch (_) {
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
    } catch (_) {
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
    } catch (_) {
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
    } catch (_) {
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
    } catch (_) {
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
        'message': 'Registration failed. Please try again.',
      };
    } catch (_) {
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
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
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
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
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
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
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
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
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
      if (resp.statusCode == 200 || resp.statusCode == 201) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
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
  static Future<Map<String, dynamic>> getAuditLogs(
      {int page = 0, int size = 50}) async {
    try {
      final headers = await _headers();
      final resp = await http
          .get(
            Uri.parse(
                '${AppConfig.apiV1BaseUrl}/audit-logs?page=$page&size=$size&sort=timestamp,desc'),
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
