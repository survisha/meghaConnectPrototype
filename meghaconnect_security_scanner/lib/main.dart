import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'core/config/app_environment.dart';
import 'core/network/api_client.dart';
import 'core/security/secure_token_storage.dart';
import 'core/utils/device_id_provider.dart';
import 'data/api/auth_api.dart';
import 'data/api/mock_auth_api.dart';
import 'data/api/qr_api.dart';
import 'data/api/rest_auth_api.dart';
import 'data/api/rest_qr_api.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/qr_repository.dart';
import 'data/repositories/recent_scan_store.dart';
import 'presentation/screens/dashboard_screen.dart';
import 'presentation/screens/login_screen.dart';
import 'presentation/screens/splash_screen.dart';
import 'presentation/state/app_state.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  final environment = AppEnvironment.current;
  final tokenStorage = SecureTokenStorage();
  final apiClient = ApiClient(
    environment: environment,
    tokenStorage: tokenStorage,
  );
  final deviceIdProvider = DeviceIdProvider(tokenStorage);
  final recentScanStore = RecentScanStore();

  final AuthApi authApi =
      environment.useMockAuth ? MockAuthApi() : RestAuthApi(apiClient.dio);
  final QrApi qrApi = RestQrApi(apiClient.dio);

  final appState = AppState(
    authRepository: AuthRepository(
      authApi: authApi,
      tokenStorage: tokenStorage,
    ),
    qrRepository: QrRepository(
      qrApi: qrApi,
      deviceIdProvider: deviceIdProvider,
      recentScanStore: recentScanStore,
      environment: environment,
    ),
  );

  runApp(
    MeghaConnectScannerApp(
      appState: appState,
      environment: environment,
    ),
  );
}

class MeghaConnectScannerApp extends StatefulWidget {
  const MeghaConnectScannerApp({
    required this.appState,
    required this.environment,
    super.key,
  });

  final AppState appState;
  final AppEnvironment environment;

  @override
  State<MeghaConnectScannerApp> createState() => _MeghaConnectScannerAppState();
}

class _MeghaConnectScannerAppState extends State<MeghaConnectScannerApp> {
  @override
  void initState() {
    super.initState();
    widget.appState.restoreSession();
  }

  @override
  void dispose() {
    widget.appState.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider<AppState>.value(
      value: widget.appState,
      child: MaterialApp(
        title: 'MeghaConnect Security',
        debugShowCheckedModeBanner: false,
        theme: _buildTheme(),
        home: Consumer<AppState>(
          builder: (context, appState, _) {
            if (appState.isInitializing) {
              return const SplashScreen();
            }
            if (appState.isLoggedIn) {
              return DashboardScreen(environment: widget.environment);
            }
            return LoginScreen(environment: widget.environment);
          },
        ),
      ),
    );
  }

  ThemeData _buildTheme() {
    final scheme = ColorScheme.fromSeed(
      seedColor: const Color(0xFF006C67),
      brightness: Brightness.light,
    ).copyWith(
      secondary: const Color(0xFF8A5A00),
    );

    return ThemeData(
      colorScheme: scheme,
      useMaterial3: true,
      scaffoldBackgroundColor: const Color(0xFFF8FAF9),
      appBarTheme: const AppBarTheme(
        centerTitle: false,
        elevation: 0,
      ),
      cardTheme: const CardTheme(
        margin: EdgeInsets.zero,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
    );
  }
}
