class QrValidationPayload {
  const QrValidationPayload({
    required this.qrToken,
    required this.deviceId,
    this.gateName,
    this.location,
  });

  final String qrToken;
  final String deviceId;
  final String? gateName;
  final String? location;

  Map<String, dynamic> toJson() {
    return {
      'qrToken': qrToken,
      'qrData': qrToken,
      'deviceId': deviceId,
      if (gateName != null && gateName!.isNotEmpty) 'gateName': gateName,
      if (location != null && location!.isNotEmpty) 'location': location,
    };
  }
}

class QrActionPayload {
  const QrActionPayload({
    required this.qrToken,
    required this.deviceId,
    this.gateName,
    this.location,
  });

  final String qrToken;
  final String deviceId;
  final String? gateName;
  final String? location;

  Map<String, dynamic> toJson() {
    return {
      'qrToken': qrToken,
      'qrData': qrToken,
      'deviceId': deviceId,
      if (gateName != null && gateName!.isNotEmpty) 'gateName': gateName,
      if (location != null && location!.isNotEmpty) 'location': location,
    };
  }
}
