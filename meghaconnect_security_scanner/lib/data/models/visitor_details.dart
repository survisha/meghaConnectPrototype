import '../../core/utils/json_helpers.dart';
import 'qr_action_result.dart';

class VisitorDetails {
  const VisitorDetails({
    required this.qrToken,
    required this.visitorName,
    required this.appointmentId,
    required this.purpose,
    required this.department,
    required this.personToMeet,
    required this.qrStatus,
    required this.entryExitStatus,
    required this.canCheckIn,
    required this.canCheckOut,
    this.visitorPhotoUrl,
    this.appointmentDateTime,
    this.message,
  });

  final String qrToken;
  final String visitorName;
  final String? visitorPhotoUrl;
  final String appointmentId;
  final DateTime? appointmentDateTime;
  final String purpose;
  final String department;
  final String personToMeet;
  final String qrStatus;
  final String entryExitStatus;
  final bool canCheckIn;
  final bool canCheckOut;
  final String? message;

  bool get isValid {
    final status = qrStatus.toUpperCase();
    return status == 'VALID' || status == 'ACTIVE' || status == 'QR_GENERATED';
  }

  VisitorDetails copyWith({
    String? qrStatus,
    String? entryExitStatus,
    bool? canCheckIn,
    bool? canCheckOut,
    String? message,
  }) {
    return VisitorDetails(
      qrToken: qrToken,
      visitorName: visitorName,
      visitorPhotoUrl: visitorPhotoUrl,
      appointmentId: appointmentId,
      appointmentDateTime: appointmentDateTime,
      purpose: purpose,
      department: department,
      personToMeet: personToMeet,
      qrStatus: qrStatus ?? this.qrStatus,
      entryExitStatus: entryExitStatus ?? this.entryExitStatus,
      canCheckIn: canCheckIn ?? this.canCheckIn,
      canCheckOut: canCheckOut ?? this.canCheckOut,
      message: message ?? this.message,
    );
  }

  VisitorDetails afterAction(QrActionResult result) {
    final normalizedAction = result.action.toUpperCase();
    if (normalizedAction.contains('CHECK_OUT')) {
      return copyWith(
        entryExitStatus: 'CHECKED_OUT',
        canCheckIn: false,
        canCheckOut: false,
        message: result.message,
      );
    }
    return copyWith(
      entryExitStatus: 'CHECKED_IN',
      canCheckIn: false,
      canCheckOut: true,
      message: result.message,
    );
  }

  factory VisitorDetails.fromJson(
    Map<String, dynamic> json, {
    required String qrToken,
  }) {
    final visitor = asMap(json['visitor']);
    final appointment = asMap(json['appointment']);
    final entryExitStatus = readString(json, [
          'entryExitStatus',
          'entryStatus',
          'checkStatus',
          'visitStatus',
        ]) ??
        'NOT_CHECKED_IN';
    final qrStatus =
        readString(json, ['qrStatus', 'status', 'tokenStatus']) ?? 'VALID';

    final canCheckIn = _bool(json['canCheckIn']) ??
        _canCheckIn(qrStatus: qrStatus, entryExitStatus: entryExitStatus);
    final canCheckOut = _bool(json['canCheckOut']) ??
        _canCheckOut(qrStatus: qrStatus, entryExitStatus: entryExitStatus);

    return VisitorDetails(
      qrToken: readString(json, ['qrToken']) ?? qrToken,
      visitorName: readString(json, ['visitorName', 'name', 'fullName']) ??
          readString(visitor, ['fullName', 'name', 'visitorName']) ??
          'Visitor',
      visitorPhotoUrl: readString(json, [
            'visitorPhotoUrl',
            'visitorPhotoPath',
            'photoUrl',
            'photoPath',
          ]) ??
          readString(visitor, ['photoUrl', 'photoPath', 'profilePhotoUrl']),
      appointmentId: readString(json, ['appointmentId', 'appointmentNumber']) ??
          readString(
              appointment, ['id', 'appointmentId', 'appointmentNumber']) ??
          '-',
      appointmentDateTime: _dateTime(
        json['appointmentDateTime'] ??
            json['appointmentTime'] ??
            appointment['appointmentDateTime'] ??
            appointment['scheduledAt'] ??
            appointment['dateTime'],
      ),
      purpose: readString(json, ['purpose', 'visitPurpose']) ??
          readString(appointment, ['purpose', 'visitPurpose']) ??
          '-',
      department: readString(json, ['department', 'departmentName']) ??
          readString(appointment, ['department', 'departmentName']) ??
          '-',
      personToMeet: readString(json, [
            'personToMeet',
            'officerName',
            'hostName',
            'meetingWith',
          ]) ??
          readString(appointment, [
            'personToMeet',
            'officerName',
            'hostName',
            'meetingWith',
          ]) ??
          '-',
      qrStatus: qrStatus,
      entryExitStatus: entryExitStatus,
      canCheckIn: canCheckIn,
      canCheckOut: canCheckOut,
      message: readString(json, ['message', 'statusMessage']),
    );
  }

  static bool? _bool(Object? value) {
    return switch (value) {
      bool value => value,
      String value => value.toLowerCase() == 'true',
      _ => null,
    };
  }

  static bool _canCheckIn({
    required String qrStatus,
    required String entryExitStatus,
  }) {
    final qr = qrStatus.toUpperCase();
    final entry = entryExitStatus.toUpperCase();
    return (qr == 'VALID' || qr == 'ACTIVE' || qr == 'QR_GENERATED') &&
        entry != 'CHECKED_IN' &&
        entry != 'CHECKED_OUT' &&
        entry != 'CANCELLED';
  }

  static bool _canCheckOut({
    required String qrStatus,
    required String entryExitStatus,
  }) {
    final qr = qrStatus.toUpperCase();
    final entry = entryExitStatus.toUpperCase();
    return (qr == 'VALID' || qr == 'ACTIVE' || qr == 'CHECKED_IN') && entry == 'CHECKED_IN';
  }

  static DateTime? _dateTime(Object? value) {
    if (value == null) {
      return null;
    }
    return DateTime.tryParse(value.toString());
  }
}
