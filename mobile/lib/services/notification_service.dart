import 'package:flutter/material.dart';

enum AppMessageType { success, error, warning, info }

/// Application-wide, context-free transient messaging through one messenger.
class AppNotificationService {
  AppNotificationService._();

  static final messengerKey = GlobalKey<ScaffoldMessengerState>();
  static String? _lastFingerprint;
  static DateTime? _lastShownAt;

  static void success(String message) => show(message, AppMessageType.success);
  static void error(String message) => show(message, AppMessageType.error);
  static void warning(String message) => show(message, AppMessageType.warning);
  static void info(String message) => show(message, AppMessageType.info);

  static void show(String message, AppMessageType type) {
    final text = message.trim();
    if (text.isEmpty) return;
    final now = DateTime.now();
    final fingerprint = '${type.name}:$text';
    if (_lastFingerprint == fingerprint &&
        _lastShownAt != null &&
        now.difference(_lastShownAt!) < const Duration(seconds: 1)) {
      return;
    }
    _lastFingerprint = fingerprint;
    _lastShownAt = now;

    final messenger = messengerKey.currentState;
    if (messenger == null) return;
    messenger.showSnackBar(SnackBar(
      content: Text(text),
      behavior: SnackBarBehavior.floating,
      duration: Duration(
          seconds: type == AppMessageType.error
              ? 6
              : type == AppMessageType.warning
                  ? 5
                  : 3),
      backgroundColor: switch (type) {
        AppMessageType.success => const Color(0xFF166534),
        AppMessageType.error => const Color(0xFFB91C1C),
        AppMessageType.warning => const Color(0xFF92400E),
        AppMessageType.info => const Color(0xFF1D4ED8),
      },
      showCloseIcon: true,
      closeIconColor: Colors.white,
    ));
  }

  static void clear() => messengerKey.currentState?.clearSnackBars();
}
