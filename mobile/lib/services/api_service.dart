import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:8080';
  static const String _tokenKey = 'megha_token';

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
      final resp = await http.post(
        Uri.parse('$baseUrl/api/v1/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'username': username, 'password': password}),
      );
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

  // OTP
  static Future<bool> sendOtp(String phone) async {
    try {
      final resp = await http.post(
        Uri.parse('$baseUrl/api/v1/public/otp/send'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'phoneNumber': phone}),
      );
      return resp.statusCode == 200 || resp.statusCode == 201;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> verifyOtp(String phone, String otp) async {
    try {
      final resp = await http.post(
        Uri.parse('$baseUrl/api/v1/public/otp/verify'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'phoneNumber': phone, 'otp': otp}),
      );
      return resp.statusCode == 200 || resp.statusCode == 201;
    } catch (_) {
      return false;
    }
  }

  // Appointments
  static Future<Map<String, dynamic>> getAppointments(
      {int page = 0, int size = 50}) async {
    try {
      final headers = await _headers();
      final resp = await http.get(
        Uri.parse('$baseUrl/api/v1/appointments?page=$page&size=$size'),
        headers: headers,
      );
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
  }

  static Future<Map<String, dynamic>?> updateAppointmentStatus(
      int id, String status,
      {String? remarks}) async {
    try {
      final headers = await _headers();
      final body = <String, dynamic>{'status': status};
      if (remarks != null) body['remarks'] = remarks;
      final resp = await http.patch(
        Uri.parse('$baseUrl/api/v1/appointments/$id/status'),
        headers: headers,
        body: jsonEncode(body),
      );
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
      final resp = await http.post(
        Uri.parse('$baseUrl/api/v1/appointments/$id/schedule'),
        headers: headers,
        body: jsonEncode({
          'scheduledDateTime': scheduledDateTime,
          'durationMinutes': durationMinutes,
        }),
      );
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
      final resp = await http.get(
        Uri.parse('$baseUrl/api/v1/grievances?page=$page&size=$size'),
        headers: headers,
      );
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
      final resp = await http.post(
        Uri.parse('$baseUrl/api/v1/grievances'),
        headers: headers,
        body: jsonEncode(body),
      );
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
      final resp = await http.patch(
        Uri.parse('$baseUrl/api/v1/grievances/$id/status'),
        headers: headers,
        body: jsonEncode(body),
      );
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
      final resp = await http.get(
        Uri.parse('$baseUrl/api/v1/schedule'),
        headers: headers,
      );
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
      final resp = await http.post(
        Uri.parse('$baseUrl/api/v1/schedule'),
        headers: headers,
        body: jsonEncode(body),
      );
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
      final resp = await http.get(
        Uri.parse(
            '$baseUrl/api/v1/audit-logs?page=$page&size=$size&sort=timestamp,desc'),
        headers: headers,
      );
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
  }

  // Persons
  static Future<Map<String, dynamic>?> searchPersonByPhone(
      String phone) async {
    try {
      final headers = await _headers();
      final resp = await http.get(
        Uri.parse('$baseUrl/api/v1/persons/search/phone/$phone'),
        headers: headers,
      );
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> searchPersonByEpic(String epic) async {
    try {
      final headers = await _headers();
      final resp = await http.get(
        Uri.parse('$baseUrl/api/v1/persons/search/epic/$epic'),
        headers: headers,
      );
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  static Future<List<dynamic>> searchPersonsByName(String q) async {
    try {
      final headers = await _headers();
      final resp = await http.get(
        Uri.parse(
            '$baseUrl/api/v1/persons/search/name?q=${Uri.encodeComponent(q)}'),
        headers: headers,
      );
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as List<dynamic>;
      }
    } catch (_) {}
    return [];
  }

  static Future<List<dynamic>> searchPersonsByDistrict(String d) async {
    try {
      final headers = await _headers();
      final resp = await http.get(
        Uri.parse(
            '$baseUrl/api/v1/persons/search/district/${Uri.encodeComponent(d)}'),
        headers: headers,
      );
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
      final resp = await http.get(
        Uri.parse('$baseUrl/api/v1/directions'),
        headers: headers,
      );
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
      final resp = await http.get(
        Uri.parse('$baseUrl/api/v1/users'),
        headers: headers,
      );
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
      final resp = await http.get(
        Uri.parse(
            '$baseUrl/api/v1/scheme-applications?page=$page&size=$size'),
        headers: headers,
      );
      if (resp.statusCode == 200) {
        return jsonDecode(resp.body) as Map<String, dynamic>;
      }
    } catch (_) {}
    return {'content': [], 'totalElements': 0};
  }
}
