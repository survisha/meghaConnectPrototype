import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';

import '../core/config/app_config.dart';

class ConnectivityService extends ChangeNotifier {
  Timer? _timer;
  bool _isOnline = true;

  bool get isOnline => _isOnline;
  bool get isOffline => !_isOnline;

  Future<void> init() async {
    await checkNow();
    _timer = Timer.periodic(const Duration(seconds: 20), (_) => checkNow());
  }

  Future<void> checkNow() async {
    final online = await _probeNetwork();
    if (_isOnline == online) return;
    _isOnline = online;
    notifyListeners();
  }

  Future<bool> _probeNetwork() async {
    try {
      final uri = Uri.parse(AppConfig.apiBaseUrl);
      final host = uri.host.isEmpty ? 'example.com' : uri.host;
      final result = await InternetAddress.lookup(host)
          .timeout(const Duration(seconds: 3));
      return result.isNotEmpty && result.first.rawAddress.isNotEmpty;
    } catch (_) {
      return false;
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
