import 'package:flutter/foundation.dart';

/// Central app configuration.
///
/// Keep all environment-like values in one place so we can switch UAT/Prod
/// without hunting down hardcoded URLs across services/screens.
class AppConfig {
  /// Backend API base URL.
  ///
  /// Keep the host/base segment here. Versioned endpoints append `/v1`.
  static const String _configuredApiBaseUrl = String.fromEnvironment(
    'MEGHA_API_BASE_URL',
    defaultValue: '',
  );
  static const bool enableDemoCredentials = bool.fromEnvironment(
    'ENABLE_DEMO_CREDENTIALS',
    defaultValue: false,
  );
  static const String demoLoginEntries = String.fromEnvironment(
    'DEMO_LOGIN_ENTRIES',
    defaultValue: '',
  );

  static const String privacyPolicyUrl = String.fromEnvironment(
    'MEGHA_PRIVACY_POLICY_URL',
    defaultValue: 'https://www.meghaconnect.com/privacy-policy',
  );
  static const String termsUrl = String.fromEnvironment(
    'MEGHA_TERMS_URL',
    defaultValue: 'https://www.meghaconnect.com/terms',
  );
  static const String consentVersion = String.fromEnvironment(
    'MEGHA_CONSENT_VERSION',
    defaultValue: '2026-05-25',
  );

  static String get apiBaseUrl {
    final configured = _configuredApiBaseUrl.trim();
    if (configured.isNotEmpty) {
      _requireHttps(configured);
      return configured;
    }
    if (kReleaseMode) {
      throw StateError('MEGHA_API_BASE_URL is required for production builds.');
    }
    return 'https://meghaconnect.cloud/api';
  }

  static String get apiV1BaseUrl => '$apiBaseUrl/v1';

  /// Shared preference key for persisted language selection.
  static const String languageStorageKey = 'app_language';

  static void validateForCurrentMode() {
    final url = apiBaseUrl;
    _requireHttps(url);
    if (kReleaseMode && enableDemoCredentials) {
      throw StateError('Demo credentials must not be enabled in production.');
    }
  }

  static void _requireHttps(String url) {
    final uri = Uri.tryParse(url);
    if (uri == null || uri.scheme != 'https') {
      throw StateError('MeghaConnect requires an HTTPS MEGHA_API_BASE_URL.');
    }
  }
}
