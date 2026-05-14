import '../../core/network/api_exception.dart';
import '../models/qr_action_result.dart';
import '../models/qr_payloads.dart';
import '../models/recent_scan.dart';
import '../models/visitor_details.dart';
import 'qr_api.dart';

class MockQrApi implements QrApi {
  final Map<String, String> _entryStates = <String, String>{};

  @override
  Future<VisitorDetails> validate(QrValidationPayload payload) async {
    await Future<void>.delayed(const Duration(milliseconds: 550));
    _throwForInvalidToken(payload.qrToken);

    final lower = payload.qrToken.toLowerCase();
    final entryStatus = _entryStates[payload.qrToken] ??
        switch (lower) {
          final value when value.contains('checked-out') => 'CHECKED_OUT',
          final value when value.contains('checked-in') => 'CHECKED_IN',
          _ => 'NOT_CHECKED_IN',
        };

    return VisitorDetails(
      qrToken: payload.qrToken,
      visitorName: 'Mock Visitor ${_suffix(payload.qrToken)}',
      visitorPhotoUrl: null,
      appointmentId: 'APT-${_suffix(payload.qrToken)}',
      appointmentDateTime: DateTime.now().add(const Duration(hours: 2)),
      purpose: 'Official meeting',
      department: 'General Administration Department',
      personToMeet: 'Duty Officer',
      qrStatus: 'VALID',
      entryExitStatus: entryStatus,
      canCheckIn: entryStatus == 'NOT_CHECKED_IN',
      canCheckOut: entryStatus == 'CHECKED_IN',
      message: 'QR token validated.',
    );
  }

  @override
  Future<QrActionResult> checkIn(QrActionPayload payload) async {
    await Future<void>.delayed(const Duration(milliseconds: 450));
    _throwForInvalidToken(payload.qrToken);

    final current = _entryStates[payload.qrToken] ?? 'NOT_CHECKED_IN';
    if (current == 'CHECKED_IN') {
      throw const ApiException(
        'Visitor is already checked in.',
        statusCode: 409,
        code: 'ALREADY_CHECKED_IN',
      );
    }
    if (current == 'CHECKED_OUT') {
      throw const ApiException(
        'Visitor is already checked out.',
        statusCode: 409,
        code: 'ALREADY_CHECKED_OUT',
      );
    }

    _entryStates[payload.qrToken] = 'CHECKED_IN';
    return QrActionResult(
      success: true,
      message: 'Visitor checked in successfully.',
      action: 'CHECK_IN',
      status: 'CHECKED_IN',
      scanTime: DateTime.now(),
    );
  }

  @override
  Future<QrActionResult> checkOut(QrActionPayload payload) async {
    await Future<void>.delayed(const Duration(milliseconds: 450));
    _throwForInvalidToken(payload.qrToken);

    final current = _entryStates[payload.qrToken] ?? 'NOT_CHECKED_IN';
    if (current == 'NOT_CHECKED_IN') {
      throw const ApiException(
        'Visitor has not checked in yet.',
        statusCode: 409,
        code: 'NOT_CHECKED_IN',
      );
    }
    if (current == 'CHECKED_OUT') {
      throw const ApiException(
        'Visitor is already checked out.',
        statusCode: 409,
        code: 'ALREADY_CHECKED_OUT',
      );
    }

    _entryStates[payload.qrToken] = 'CHECKED_OUT';
    return QrActionResult(
      success: true,
      message: 'Visitor checked out successfully.',
      action: 'CHECK_OUT',
      status: 'CHECKED_OUT',
      scanTime: DateTime.now(),
    );
  }

  @override
  Future<List<RecentScan>> recentScans() async {
    return const <RecentScan>[];
  }

  void _throwForInvalidToken(String token) {
    final lower = token.toLowerCase();
    if (token.trim().isEmpty || lower.contains('invalid')) {
      throw const ApiException(
        'Invalid QR token.',
        statusCode: 400,
        code: 'INVALID_QR',
      );
    }
    if (lower.contains('expired')) {
      throw const ApiException(
        'QR token has expired.',
        statusCode: 400,
        code: 'EXPIRED_QR',
      );
    }
    if (lower.contains('cancelled') || lower.contains('canceled')) {
      throw const ApiException(
        'Appointment has been cancelled.',
        statusCode: 409,
        code: 'CANCELLED_APPOINTMENT',
      );
    }
  }

  String _suffix(String token) {
    final value = token.hashCode.abs() % 100000;
    return value.toString().padLeft(5, '0');
  }
}
