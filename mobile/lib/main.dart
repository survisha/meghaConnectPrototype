import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'services/auth_service.dart';
import 'services/connectivity_service.dart';
import 'services/navigation_service.dart';
import 'services/sync_service.dart';
import 'core/i18n/app_i18n.dart';
import 'core/config/app_config.dart';
import 'screens/login_screen.dart';
import 'screens/main_shell.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  AppConfig.validateForCurrentMode();
  final connectivityService = ConnectivityService();
  await connectivityService.init();
  final authService = AuthService(connectivity: connectivityService);
  await authService.init();
  final syncService = SyncService(connectivity: connectivityService);
  await syncService.init();
  final i18n = AppI18n();
  await i18n.init();
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthService>.value(value: authService),
        ChangeNotifierProvider<ConnectivityService>.value(
            value: connectivityService),
        ChangeNotifierProvider<SyncService>.value(value: syncService),
        ChangeNotifierProvider<AppI18n>.value(value: i18n),
        ChangeNotifierProvider<NavigationService>(
            create: (_) => NavigationService()),
      ],
      child: const MeghaConnectApp(),
    ),
  );
}

class MeghaConnectApp extends StatelessWidget {
  const MeghaConnectApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MeghaConnect',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFFF0F2F5),
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1A237E),
          primary: const Color(0xFF1A237E),
          secondary: const Color(0xFF1565C0),
          tertiary: const Color(0xFF065F46),
          brightness: Brightness.light,
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF1A237E),
          foregroundColor: Colors.white,
          elevation: 2,
          centerTitle: false,
          titleTextStyle: TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        drawerTheme: const DrawerThemeData(
          backgroundColor: Colors.white,
        ),
        cardTheme: const CardTheme(
          elevation: 2,
          color: Colors.white,
          surfaceTintColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(10)),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Color(0xFFD1D5DB)),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Color(0xFF1A237E), width: 1.5),
          ),
          filled: true,
          fillColor: Colors.white,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF1A237E),
            foregroundColor: Colors.white,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
            minimumSize: const Size(0, 44),
          ),
        ),
      ),
      home: Consumer<AuthService>(
        builder: (context, auth, _) {
          return _WelcomeGate(
            child: auth.isLoggedIn ? const MainShell() : const LoginScreen(),
          );
        },
      ),
    );
  }
}

class _WelcomeGate extends StatefulWidget {
  final Widget child;

  const _WelcomeGate({required this.child});

  @override
  State<_WelcomeGate> createState() => _WelcomeGateState();
}

class _WelcomeGateState extends State<_WelcomeGate> {
  bool _showWelcome = true;

  @override
  void initState() {
    super.initState();
    Future.delayed(const Duration(milliseconds: 5800), () {
      if (mounted) setState(() => _showWelcome = false);
    });
  }

  @override
  Widget build(BuildContext context) {
    if (!_showWelcome) return widget.child;
    return const _WelcomeScreen();
  }
}

class _WelcomeScreen extends StatefulWidget {
  const _WelcomeScreen();

  @override
  State<_WelcomeScreen> createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<_WelcomeScreen> {
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    Future.delayed(const Duration(seconds: 5), () {
      if (mounted) setState(() => _loading = true);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF4F6FB),
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ClipOval(
                  child: Image.asset(
                    'assets/CM_Profile_Picture.jpg',
                    width: 94,
                    height: 94,
                    fit: BoxFit.cover,
                  ),
                ),
                const SizedBox(height: 10),
                const Text(
                  "Hon'ble Chief Minister",
                  style: TextStyle(
                    color: Color(0xFF172554),
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const Text(
                  'Government of Meghalaya',
                  style: TextStyle(color: Color(0xFF64748B), fontSize: 12),
                ),
                const SizedBox(height: 22),
                Container(
                  width: double.infinity,
                  constraints: const BoxConstraints(maxWidth: 380),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 20, vertical: 28),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(18),
                    border: Border.all(color: const Color(0xFFDBEAFE)),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withAlpha(24),
                        blurRadius: 32,
                        offset: const Offset(0, 16),
                      ),
                    ],
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      Opacity(
                        opacity: 0.1,
                        child: Image.asset(
                          'assets/state_map.png',
                          height: 210,
                          fit: BoxFit.contain,
                        ),
                      ),
                      Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Image.asset(
                            'assets/logo.png',
                            width: 78,
                            height: 78,
                            fit: BoxFit.contain,
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'MeghaConnect',
                            style: TextStyle(
                              color: Color(0xFF1A237E),
                              fontSize: 34,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            "Chief Minister's Office citizen service platform",
                            textAlign: TextAlign.center,
                            style: TextStyle(color: Color(0xFF475569)),
                          ),
                          if (_loading) ...[
                            const SizedBox(height: 20),
                            const SizedBox(
                              width: 34,
                              height: 34,
                              child: CircularProgressIndicator(strokeWidth: 3),
                            ),
                          ],
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
