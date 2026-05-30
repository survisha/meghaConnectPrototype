import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';

class ConnectivityService extends ChangeNotifier {
  final Connectivity _connectivity = Connectivity();
  StreamSubscription<List<ConnectivityResult>>? _subscription;
  bool _isOnline = true;

  bool get isOnline => _isOnline;
  bool get isOffline => !_isOnline;

  Future<void> init() async {
    await _refresh(await _connectivity.checkConnectivity());
    _subscription = _connectivity.onConnectivityChanged.listen(_refresh);
  }

  Future<void> checkNow() async {
    await _refresh(await _connectivity.checkConnectivity());
  }

  Future<void> _refresh(List<ConnectivityResult> results) async {
    final online = results.any((result) => result != ConnectivityResult.none);
    if (_isOnline == online) return;
    _isOnline = online;
    notifyListeners();
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
}
