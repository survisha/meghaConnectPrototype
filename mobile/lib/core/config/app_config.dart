/// Central app configuration.
///
/// Keep all environment-like values in one place so we can switch UAT/Prod
/// without hunting down hardcoded URLs across services/screens.
class AppConfig {
  /// Backend API base URL (UAT).
  ///
  /// Keep the host/base segment here. Versioned endpoints append `/v1`.
  /// Do NOT use localhost in mobile builds.
  ///
  /// Future prod switch: change this value (or wire it via build flavors).
  static const String apiBaseUrl = 'https://meghaconnect.cloud/api';
  static const String apiV1BaseUrl = '$apiBaseUrl/v1';

  /// Shared preference key for persisted language selection.
  static const String languageStorageKey = 'app_language';
}
