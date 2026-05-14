import 'qr_action_result.dart';
import 'visitor_details.dart';

class RecentScan {
  const RecentScan({
    required this.visitorName,
    required this.scanTime,
    required this.status,
    required this.action,
    this.appointmentId,
  });

  final String visitorName;
  final DateTime scanTime;
  final String status;
  final String action;
  final String? appointmentId;

  factory RecentScan.fromValidation(VisitorDetails details) {
    return RecentScan(
      visitorName: details.visitorName,
      scanTime: DateTime.now(),
      status: details.entryExitStatus,
      action: 'VALIDATE',
      appointmentId: details.appointmentId,
    );
  }

  factory RecentScan.fromAction(
    VisitorDetails details,
    QrActionResult action,
  ) {
    return RecentScan(
      visitorName: details.visitorName,
      scanTime: action.scanTime,
      status: action.status,
      action: action.action,
      appointmentId: details.appointmentId,
    );
  }

  factory RecentScan.fromJson(Map<String, dynamic> json) {
    return RecentScan(
      visitorName: _string(json, ['visitorName', 'name']) ?? 'Visitor',
      scanTime:
          _dateTime(json['scanTime'] ?? json['timestamp']) ?? DateTime.now(),
      status: _string(json, ['status', 'entryExitStatus']) ?? '-',
      action: _string(json, ['action', 'scanAction']) ?? 'SCAN',
      appointmentId: _string(json, ['appointmentId']),
    );
  }

  static String? _string(Map<String, dynamic> json, List<String> keys) {
    for (final key in keys) {
      final value = json[key];
      if (value != null && value.toString().trim().isNotEmpty) {
        return value.toString().trim();
      }
    }
    return null;
  }

  static DateTime? _dateTime(Object? value) {
    if (value == null) {
      return null;
    }
    return DateTime.tryParse(value.toString());
  }
}
