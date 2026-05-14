class QrActionResult {
  const QrActionResult({
    required this.success,
    required this.message,
    required this.action,
    required this.status,
    required this.scanTime,
  });

  final bool success;
  final String message;
  final String action;
  final String status;
  final DateTime scanTime;

  factory QrActionResult.fromJson(
    Map<String, dynamic> json, {
    required String fallbackAction,
  }) {
    return QrActionResult(
      success: _bool(json['success']) ?? true,
      message: _string(json, ['message', 'statusMessage']) ?? 'Success',
      action: _string(json, ['action']) ?? fallbackAction,
      status: _string(json, ['status', 'entryExitStatus']) ?? 'SUCCESS',
      scanTime:
          _dateTime(json['scanTime'] ?? json['timestamp']) ?? DateTime.now(),
    );
  }

  static bool? _bool(Object? value) {
    return switch (value) {
      bool value => value,
      String value => value.toLowerCase() == 'true',
      _ => null,
    };
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
