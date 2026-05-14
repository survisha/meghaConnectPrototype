import 'package:flutter_test/flutter_test.dart';
import 'package:meghaconnect_security_scanner/data/models/user_session.dart';
import 'package:meghaconnect_security_scanner/data/models/visitor_details.dart';

void main() {
  test('allows only security and admin roles', () {
    expect(UserSession.normalizeRole('ROLE_SECURITY'), 'SECURITY');
    expect(UserSession.normalizeRole('admin'), 'ADMIN');

    final securitySession = UserSession(
      token: 'token',
      username: 'guard',
      fullName: 'Security Guard',
      role: 'SECURITY',
      expiresAt: DateTime.now().add(const Duration(hours: 1)),
    );

    final publicSession = UserSession(
      token: 'token',
      username: 'visitor',
      fullName: 'Visitor',
      role: 'PUBLIC',
      expiresAt: DateTime.now().add(const Duration(hours: 1)),
    );

    expect(securitySession.isSecurityAllowed, isTrue);
    expect(publicSession.isSecurityAllowed, isFalse);
  });

  test('maps QR validation response into visitor details', () {
    final details = VisitorDetails.fromJson(
      {
        'visitorName': 'A. Visitor',
        'appointmentId': 'APT-42',
        'appointmentDateTime': '2026-05-14T10:30:00+05:30',
        'purpose': 'Meeting',
        'departmentName': 'General Administration',
        'personToMeet': 'Duty Officer',
        'qrStatus': 'VALID',
        'entryExitStatus': 'NOT_CHECKED_IN',
      },
      qrToken: 'secure-token',
    );

    expect(details.visitorName, 'A. Visitor');
    expect(details.appointmentId, 'APT-42');
    expect(details.canCheckIn, isTrue);
    expect(details.canCheckOut, isFalse);
  });
}
