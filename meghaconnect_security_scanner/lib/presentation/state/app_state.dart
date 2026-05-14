import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../core/network/api_exception.dart';
import '../../data/models/qr_action_result.dart';
import '../../data/models/recent_scan.dart';
import '../../data/models/user_session.dart';
import '../../data/models/visitor_details.dart';
import '../../data/repositories/auth_repository.dart';
import '../../data/repositories/qr_repository.dart';

class AppState extends ChangeNotifier {
  AppState({
    required AuthRepository authRepository,
    required QrRepository qrRepository,
  })  : _authRepository = authRepository,
        _qrRepository = qrRepository;

  final AuthRepository _authRepository;
  final QrRepository _qrRepository;

  UserSession? _session;
  VisitorDetails? _currentVisitor;
  bool _initializing = true;
  bool _busy = false;
  String? _errorMessage;
  String? _noticeMessage;
  Timer? _expiryTimer;

  UserSession? get session => _session;

  VisitorDetails? get currentVisitor => _currentVisitor;

  bool get isInitializing => _initializing;

  bool get isBusy => _busy;

  bool get isLoggedIn => _session != null && !_session!.isExpired;

  String? get errorMessage => _errorMessage;

  String? get noticeMessage => _noticeMessage;

  List<RecentScan> get recentScans => _qrRepository.localRecentScans;

  Future<void> restoreSession() async {
    _initializing = true;
    notifyListeners();
    _session = await _authRepository.restoreSession();
    _scheduleExpiry();
    _initializing = false;
    notifyListeners();
  }

  Future<bool> login({
    required String username,
    required String password,
  }) async {
    _setBusy(true);
    _errorMessage = null;
    _noticeMessage = null;

    try {
      _session = await _authRepository.login(
        username: username,
        password: password,
      );
      _scheduleExpiry();
      return true;
    } on ApiException catch (error) {
      _errorMessage = error.message;
      return false;
    } catch (_) {
      _errorMessage = 'Unable to login. Please try again.';
      return false;
    } finally {
      _setBusy(false);
    }
  }

  Future<void> logout({String? message}) async {
    _expiryTimer?.cancel();
    _expiryTimer = null;
    await _authRepository.logout();
    _qrRepository.clearLocalState();
    _session = null;
    _currentVisitor = null;
    _noticeMessage = message;
    _errorMessage = null;
    notifyListeners();
  }

  Future<VisitorDetails> validateQr(String qrToken) async {
    _requireActiveSession();
    _setBusy(true);
    try {
      final visitor = await _qrRepository.validateQr(
        qrToken: qrToken,
        gateName: _session?.gateName,
        location: _session?.location,
      );
      _currentVisitor = visitor;
      return visitor;
    } on ApiException catch (error) {
      await _logoutIfAuthFailure(error);
      rethrow;
    } finally {
      _setBusy(false);
    }
  }

  Future<QrActionResult> checkIn() async {
    return _runVisitorAction((visitor) {
      return _qrRepository.checkIn(
        visitor: visitor,
        gateName: _session?.gateName,
        location: _session?.location,
      );
    });
  }

  Future<QrActionResult> checkOut() async {
    return _runVisitorAction((visitor) {
      return _qrRepository.checkOut(
        visitor: visitor,
        gateName: _session?.gateName,
        location: _session?.location,
      );
    });
  }

  Future<List<RecentScan>> refreshRecentScans() async {
    _requireActiveSession();
    _setBusy(true);
    try {
      final scans = await _qrRepository.recentScans();
      notifyListeners();
      return scans;
    } on ApiException catch (error) {
      await _logoutIfAuthFailure(error);
      rethrow;
    } finally {
      _setBusy(false);
    }
  }

  void clearMessages() {
    _errorMessage = null;
    _noticeMessage = null;
    notifyListeners();
  }

  Future<QrActionResult> _runVisitorAction(
    Future<QrActionResult> Function(VisitorDetails visitor) action,
  ) async {
    _requireActiveSession();
    final visitor = _currentVisitor;
    if (visitor == null) {
      throw const ApiException('Scan and validate a QR code first.');
    }

    _setBusy(true);
    try {
      final result = await action(visitor);
      _currentVisitor = visitor.afterAction(result);
      return result;
    } on ApiException catch (error) {
      await _logoutIfAuthFailure(error);
      rethrow;
    } finally {
      _setBusy(false);
    }
  }

  void _requireActiveSession() {
    final session = _session;
    if (session == null || session.isExpired) {
      unawaited(logout(message: 'Session expired. Please login again.'));
      throw const ApiException('Session expired. Please login again.');
    }
  }

  Future<void> _logoutIfAuthFailure(ApiException error) async {
    if (error.isUnauthorized || error.isForbidden) {
      await logout(message: 'Session expired or unauthorized. Please login.');
    }
  }

  void _scheduleExpiry() {
    _expiryTimer?.cancel();
    final expiry = _session?.expiresAt;
    if (expiry == null) {
      return;
    }

    final delay = expiry.difference(DateTime.now());
    if (delay.isNegative) {
      unawaited(logout(message: 'Session expired. Please login again.'));
      return;
    }

    _expiryTimer = Timer(delay, () {
      unawaited(logout(message: 'Session expired. Please login again.'));
    });
  }

  void _setBusy(bool value) {
    _busy = value;
    notifyListeners();
  }

  @override
  void dispose() {
    _expiryTimer?.cancel();
    super.dispose();
  }
}
