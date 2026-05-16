enum EnvironmentName { dev, uat, prod }

class AppEnvironment {
  const AppEnvironment._({
    required this.name,
    required this.baseUri,
    required this.useMockAuth,
    required this.useMockQr,
    required this.useBackendRecentScans,
  });

  static final AppEnvironment current = AppEnvironment.fromDartDefines();

  final EnvironmentName name;
  final Uri baseUri;
  final bool useMockAuth;
  final bool useMockQr;
  final bool useBackendRecentScans;

  String get label => name.name.toUpperCase();

  static AppEnvironment fromDartDefines() {
    const envName = String.fromEnvironment('APP_ENV', defaultValue: 'dev');
    const baseUrl = String.fromEnvironment('API_BASE_URL');
    const useMockAuth =
        bool.fromEnvironment('USE_MOCK_AUTH', defaultValue: false);
    const useMockQr = bool.fromEnvironment('USE_MOCK_QR', defaultValue: false);
    const useBackendRecentScans =
        bool.fromEnvironment('USE_BACKEND_RECENT_SCANS', defaultValue: false);

    final name = switch (envName.toLowerCase()) {
      'prod' || 'production' => EnvironmentName.prod,
      'uat' => EnvironmentName.uat,
      _ => EnvironmentName.dev,
    };

    final uri = Uri.parse(
      baseUrl.trim().isNotEmpty ? baseUrl.trim() : _defaultBaseUrl(name),
    );
    _requireHttps(uri);

    return AppEnvironment._(
      name: name,
      baseUri: uri,
      useMockAuth: useMockAuth,
      useMockQr: useMockQr,
      useBackendRecentScans: useBackendRecentScans,
    );
  }

  String? resolveMediaUrl(String? pathOrUrl) {
    final value = pathOrUrl?.trim();
    if (value == null || value.isEmpty) {
      return null;
    }

    final parsed = Uri.tryParse(value);
    if (parsed != null && parsed.hasScheme) {
      return parsed.scheme == 'https' ? value : null;
    }

    return baseUri.resolve(value).toString();
  }

  static String _defaultBaseUrl(EnvironmentName name) {
    return switch (name) {
      EnvironmentName.dev => 'https://meghaconnect.cloud',
      EnvironmentName.uat => 'https://meghaconnect.cloud',
      EnvironmentName.prod => 'https://meghaconnect.cloud',
    };
  }

  static void _requireHttps(Uri uri) {
    if (uri.scheme != 'https') {
      throw StateError(
        'MeghaConnect Security Scanner requires an HTTPS API_BASE_URL.',
      );
    }
  }
}
