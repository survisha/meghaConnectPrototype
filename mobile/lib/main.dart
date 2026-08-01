import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'services/auth_service.dart';
import 'services/connectivity_service.dart';
import 'services/navigation_service.dart';
import 'services/notification_service.dart';
import 'services/sync_service.dart';
import 'core/i18n/app_i18n.dart';
import 'core/config/app_config.dart';
import 'screens/login_screen.dart';
import 'screens/main_shell.dart';
import 'screens/change_password_screen.dart';

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
      scaffoldMessengerKey: AppNotificationService.messengerKey,
      title: 'MEGHACONNECT AI',
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
            child: !auth.isLoggedIn
                ? const LoginScreen()
                : auth.user!.passwordChangeRequired
                    ? const ChangePasswordScreen()
                    : const MainShell(),
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
    Future.delayed(const Duration(seconds: 3), () {
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

class _WelcomeScreenState extends State<_WelcomeScreen>
    with SingleTickerProviderStateMixin {
  bool _loading = false;
  late final AnimationController _pulseController;
  late final Animation<double> _pulse;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1700),
    )..repeat(reverse: true);
    _pulse = Tween<double>(begin: 0.96, end: 1.06).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
    Future.delayed(const Duration(milliseconds: 900), () {
      if (mounted) setState(() => _loading = true);
    });
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF03142F),
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: double.infinity,
                  constraints: const BoxConstraints(maxWidth: 360),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 22, vertical: 30),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [
                        Color(0xFF03142F),
                        Color(0xFF061F46),
                        Color(0xFF00525E),
                      ],
                    ),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: const Color(0x3300C8D7)),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFF00C8D7).withAlpha(42),
                        blurRadius: 36,
                        offset: const Offset(0, 16),
                      ),
                    ],
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          ScaleTransition(
                            scale: _pulse,
                            child: Image.asset(
                              'assets/branding/meghaconnect-ai-logo-transparent.png',
                              width: 280,
                              height: 280,
                              fit: BoxFit.contain,
                            ),
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'MEGHACONNECT AI',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 30,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            'Connecting Citizens Through Intelligence',
                            textAlign: TextAlign.center,
                            style: TextStyle(color: Color(0xFFC9F9FF)),
                          ),
                          const SizedBox(height: 14),
                          Wrap(
                            alignment: WrapAlignment.center,
                            spacing: 7,
                            runSpacing: 7,
                            children: [
                              'Appointment Management',
                              'Scheme Tracking',
                              'Citizen Services',
                              'Public Darbar',
                              'Visitor Management',
                            ]
                                .map((label) => Container(
                                      padding: const EdgeInsets.symmetric(
                                          horizontal: 9, vertical: 5),
                                      decoration: BoxDecoration(
                                        color: const Color(0x1417C3C8),
                                        borderRadius:
                                            BorderRadius.circular(999),
                                        border: Border.all(
                                            color: const Color(0x3317C3C8)),
                                      ),
                                      child: Text(
                                        label,
                                        style: const TextStyle(
                                          color: Color(0xFF082B7A),
                                          fontSize: 11,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                    ))
                                .toList(),
                          ),
                          if (_loading) ...[
                            const SizedBox(height: 20),
                            const SizedBox(
                              width: 34,
                              height: 34,
                              child: CircularProgressIndicator(
                                strokeWidth: 3,
                                color: Color(0xFF17C3C8),
                              ),
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
