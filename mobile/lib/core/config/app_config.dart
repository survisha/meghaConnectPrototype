/// Central app configuration.
///
/// Keep all environment-like values in one place so we can switch UAT/Prod
/// without hunting down hardcoded URLs across services/screens.
class AppConfig {
  /// Backend API base URL.
  ///
  /// Keep the host/base segment here. Versioned endpoints append `/v1`.
  /// Do NOT use localhost in mobile builds.
  ///
  /// Override per environment with:
  /// `--dart-define=MEGHA_API_BASE_URL=http://10.0.2.2:8080/api`
  static const String apiBaseUrl = String.fromEnvironment(
    'MEGHA_API_BASE_URL',
    defaultValue: 'https://meghaconnect.cloud/api',
  );
  static const String apiV1BaseUrl = '$apiBaseUrl/v1';

  /// Shared preference key for persisted language selection.
  static const String languageStorageKey = 'app_language';
}
