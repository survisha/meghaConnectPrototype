import 'dart:io';

import 'package:uuid/uuid.dart';

import '../security/secure_token_storage.dart';

class DeviceInfo {
  const DeviceInfo({
    required this.deviceId,
    required this.platform,
  });

  final String deviceId;
  final String platform;

  Map<String, dynamic> toJson() {
    return {
      'deviceId': deviceId,
      'platform': platform,
    };
  }
}

class DeviceIdProvider {
  DeviceIdProvider(this._tokenStorage);

  final SecureTokenStorage _tokenStorage;
  final Uuid _uuid = const Uuid();

  Future<String> getDeviceId() async {
    final existing = await _tokenStorage.readDeviceId();
    if (existing != null && existing.isNotEmpty) {
      return existing;
    }

    final generated = _uuid.v4();
    await _tokenStorage.saveDeviceId(generated);
    return generated;
  }

  Future<DeviceInfo> getDeviceInfo() async {
    return DeviceInfo(
      deviceId: await getDeviceId(),
      platform: Platform.operatingSystem,
    );
  }
}
