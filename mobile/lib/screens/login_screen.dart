import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../core/config/app_config.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../services/connectivity_service.dart';
import '../services/sync_service.dart';
import '../core/i18n/app_i18n.dart';
import '../widgets/megha_ui.dart';
import 'visitor_registration_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  // Staff login fields
  final _staffFormKey = GlobalKey<FormState>();
  final _usernameCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _staffObscure = true;
  bool _staffLoading = false;
  String? _staffError;

  // Public login fields
  final _publicFormKey = GlobalKey<FormState>();
  final _phoneCtrl = TextEditingController();
  final _publicEpicCtrl = TextEditingController();
  final _otpCtrl = TextEditingController();
  bool _publicLoading = false;
  String? _publicNotice;
  bool _publicNoticeIsWarning = false;
  bool _otpSent = false;
  bool _otpLocked = false;
  bool _requiresEpic = false;
  String? _epicForOtpLogin;
  int? _selectedVisitorId;
  List<Map<String, dynamic>> _registrationOptions = [];

  static const _primaryBlue = Color(0xFF1A237E);
  static const _accentBlue = Color(0xFF1565C0);

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _usernameCtrl.dispose();
    _passwordCtrl.dispose();
    _phoneCtrl.dispose();
    _publicEpicCtrl.dispose();
    _otpCtrl.dispose();
    super.dispose();
  }

  Future<void> _staffLogin() async {
    if (!_staffFormKey.currentState!.validate()) return;
    setState(() {
      _staffLoading = true;
      _staffError = null;
    });
    final auth = context.read<AuthService>();
    final offline = context.read<ConnectivityService>().isOffline;
    final ok = offline
        ? await auth.loginWithCachedDeviceSession(username: _usernameCtrl.text)
        : await auth.login(_usernameCtrl.text, _passwordCtrl.text);
    if (!mounted) return;
    setState(() => _staffLoading = false);
    if (!ok) {
      setState(() =>
          _staffError = auth.lastError ?? 'Invalid username or password.');
    } else {
      context.read<SyncService>().syncNow();
    }
  }

  Future<void> _offlineSessionLogin() async {
    setState(() {
      _staffLoading = true;
      _staffError = null;
    });
    final auth = context.read<AuthService>();
    final ok =
        await auth.loginWithCachedDeviceSession(username: _usernameCtrl.text);
    if (!mounted) return;
    setState(() => _staffLoading = false);
    if (!ok) {
      setState(
          () => _staffError = auth.lastError ?? 'Offline login not available.');
    }
  }

  Future<void> _sendOtp() async {
    if (!_publicFormKey.currentState!.validate()) return;
    setState(() {
      _publicLoading = true;
      _publicNotice = null;
      _otpLocked = false;
    });
    final i18n = context.read<AppI18n>();

    final phone = _phoneCtrl.text.trim();
    if (!_otpSent && _selectedVisitorId == null) {
      final search =
          await ApiService.searchVisitorRegistrations(phoneNumber: phone);
      if (!mounted) return;
      final registrations = (search['registrations'] as List<dynamic>? ?? [])
          .whereType<Map>()
          .map((row) => Map<String, dynamic>.from(row))
          .toList();
      if (registrations.length > 1) {
        setState(() {
          _publicLoading = false;
          _requiresEpic = true;
          _registrationOptions = registrations;
          _publicNotice =
              'Multiple registrations found. Select the correct profile.';
          _publicNoticeIsWarning = true;
        });
        return;
      }
      if (registrations.length == 1) {
        _selectRegistration(registrations.first, notify: false);
      } else if (search['success'] == true &&
          (search['registered'] == false ||
              ((search['registrationCount'] as num?)?.toInt() ?? 1) == 0)) {
        setState(() {
          _publicLoading = false;
          _publicNotice = search['message']?.toString() ??
              i18n.t('ACCOUNT_NOT_FOUND_REGISTER');
          _publicNoticeIsWarning = false;
        });
        return;
      }
    }

    final epicRaw = (_epicForOtpLogin ?? _publicEpicCtrl.text).trim();
    final epic = epicRaw.isNotEmpty ? epicRaw : null;
    final result = await ApiService.generateVisitorOtp(
      phoneNumber: phone,
      epicNumber: epic,
      visitorId: _selectedVisitorId,
    );

    if (!mounted) return;
    setState(() => _publicLoading = false);

    final success = result['success'] == true;
    final requiresEpic = result['requiresEpic'] == true;
    final message = (result['message'] as String?)?.trim();

    if (success) {
      setState(() {
        _otpSent = true;
        _otpLocked = false;
        _requiresEpic = false;
        _epicForOtpLogin = epic?.trim();
        _registrationOptions = [];
        _publicNotice = null;
        _publicNoticeIsWarning = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message?.isNotEmpty == true
              ? message!
              : i18n.t('OTP_SENT_SUCCESS')),
          backgroundColor: const Color(0xFF065F46),
        ),
      );
    } else {
      setState(() {
        _otpSent = false;
        _requiresEpic = requiresEpic;
        _publicNotice = message?.isNotEmpty == true
            ? message
            : (requiresEpic
                ? i18n.t('MULTIPLE_REGISTRATIONS_EPIC_REQUIRED')
                : i18n.t('ERROR_FAILED_SEND_OTP_TRY'));
        _publicNoticeIsWarning = requiresEpic;
      });
    }
  }

  void _selectRegistration(Map<String, dynamic> visitor, {bool notify = true}) {
    final id = (visitor['visitorId'] as num?)?.toInt() ??
        (visitor['id'] as num?)?.toInt() ??
        int.tryParse(visitor['visitorId']?.toString() ?? '');
    final epic = visitor['epicNumber']?.toString().trim().toUpperCase() ?? '';
    void apply() {
      _selectedVisitorId = id;
      _epicForOtpLogin = epic.isEmpty ? null : epic;
      if (_epicForOtpLogin != null) _publicEpicCtrl.text = _epicForOtpLogin!;
      _publicNotice = notify ? null : _publicNotice;
      _publicNoticeIsWarning = false;
    }

    if (notify) {
      setState(apply);
    } else {
      apply();
    }
  }

  void _changePublicNumber() {
    setState(() {
      _phoneCtrl.clear();
      _publicEpicCtrl.clear();
      _otpCtrl.clear();
      _publicLoading = false;
      _publicNotice = null;
      _publicNoticeIsWarning = false;
      _otpSent = false;
      _otpLocked = false;
      _requiresEpic = false;
      _epicForOtpLogin = null;
      _selectedVisitorId = null;
      _registrationOptions = [];
    });
    _publicFormKey.currentState?.reset();
  }

  void _clearPublicSelectionForMobileEdit() {
    setState(() {
      _requiresEpic = false;
      _otpSent = false;
      _otpLocked = false;
      _epicForOtpLogin = null;
      _selectedVisitorId = null;
      _registrationOptions = [];
      _publicNotice = null;
      _publicNoticeIsWarning = false;
    });
    _publicEpicCtrl.clear();
    _otpCtrl.clear();
  }

  Future<void> _publicLogin() async {
    if (!_publicFormKey.currentState!.validate()) return;
    if (_otpLocked) {
      setState(() {
        _publicNotice =
            'Too many failed OTP attempts. Please try again after 30 minutes.';
        _publicNoticeIsWarning = false;
      });
      return;
    }
    setState(() {
      _publicLoading = true;
      _publicNotice = null;
    });
    final i18n = context.read<AppI18n>();

    final phone = _phoneCtrl.text.trim();
    final epic = (_epicForOtpLogin ?? _publicEpicCtrl.text).trim().isNotEmpty
        ? (_epicForOtpLogin ?? _publicEpicCtrl.text).trim()
        : null;
    final otp = _otpCtrl.text.trim();

    final result = await ApiService.validateVisitorOtp(
      phoneNumber: phone,
      otp: otp,
      epicNumber: epic,
      visitorId: _selectedVisitorId,
    );

    if (!mounted) return;

    final success = result['success'] == true;
    final requiresEpic = result['requiresEpic'] == true;
    final message = (result['message'] as String?)?.trim();

    if (!success) {
      final code = result['code']?.toString().toUpperCase() ?? '';
      final attemptsRemaining =
          (result['attemptsRemaining'] as num?)?.toInt() ??
              (result['remainingAttempts'] as num?)?.toInt();
      setState(() {
        _publicLoading = false;
        _requiresEpic = requiresEpic;
        _otpLocked = code == 'OTP_LOCKED' || attemptsRemaining == 0;
        _publicNotice = message?.isNotEmpty == true
            ? message
            : i18n.t('ERROR_INVALID_OTP_TRY');
        _publicNoticeIsWarning = requiresEpic;
      });
      return;
    }

    final token = result['token'] as String?;
    final fullName = result['fullName'] as String? ?? 'Visitor';
    final visitorId = (result['visitorId'] as num?)?.toInt();

    if (token == null || visitorId == null || visitorId <= 0) {
      setState(() {
        _publicLoading = false;
        _publicNotice = 'Login failed. Please try again.';
        _publicNoticeIsWarning = false;
      });
      return;
    }

    final auth = context.read<AuthService>();
    final ok = await auth.publicLoginWithVisitorJwt(
      phoneNumber: phone,
      token: token,
      fullName: fullName,
      visitorId: visitorId,
    );
    if (!mounted) return;
    setState(() => _publicLoading = false);
    if (!ok) {
      setState(() {
        _publicNotice = 'Login failed. Please try again.';
        _publicNoticeIsWarning = false;
      });
    } else {
      context.read<SyncService>().syncNow();
    }
  }

  void _fillDemo(String user, String pass) {
    _usernameCtrl.text = user;
    _passwordCtrl.text = pass;
    setState(() => _staffError = null);
  }

  Future<void> _openUrl(String url) async {
    final uri = Uri.parse(url);
    final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!opened && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(url)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomInset: true,
      backgroundColor: const Color(0xFFF4F6FB),
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: EdgeInsets.only(
                left: 20,
                right: 20,
                top: 20,
                bottom: MediaQuery.of(context).viewInsets.bottom + 20,
              ),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 430),
                    child: _buildCleanLoginCard(),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildCleanLoginCard() {
    final offline = context.watch<ConnectivityService>().isOffline;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Center(
          child: Container(
            width: 164,
            height: 164,
            padding: const EdgeInsets.all(8),
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
              borderRadius: BorderRadius.circular(18),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF00C8D7).withAlpha(42),
                  blurRadius: 22,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Image.asset(
              'assets/branding/meghaconnect-ai-logo-transparent.png',
              width: 148,
              height: 148,
              fit: BoxFit.contain,
            ),
          ),
        ),
        const SizedBox(height: 18),
        const Text(
          'MEGHACONNECT AI',
          textAlign: TextAlign.center,
          style: TextStyle(
            color: _primaryBlue,
            fontSize: 30,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          'Connecting Citizens Through Intelligence',
          textAlign: TextAlign.center,
          style: TextStyle(color: Colors.grey[600], fontSize: 14),
        ),
        const SizedBox(height: 22),
        if (offline) ...[
          _buildNotice(
            'No internet connection. First login must be online. Cached users can continue if their session is valid.',
            warning: true,
          ),
          const SizedBox(height: 12),
        ],
        Card(
          elevation: 3,
          surfaceTintColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 14, 14, 0),
                child: _buildTabBar(),
              ),
              SizedBox(
                height: 570,
                child: TabBarView(
                  controller: _tabController,
                  children: [
                    _buildStaffTab(),
                    _buildPublicTab(),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildStaffTab() {
    final i18n = context.watch<AppI18n>();
    return SingleChildScrollView(
      padding: const EdgeInsets.all(18),
      child: Form(
        key: _staffFormKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextFormField(
              controller: _usernameCtrl,
              decoration: InputDecoration(
                labelText: i18n.t('USERNAME'),
                prefixIcon: const Icon(Icons.person_outline),
              ),
              validator: (v) => (v == null || v.trim().isEmpty)
                  ? i18n.t('ENTER_USERNAME')
                  : null,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _passwordCtrl,
              obscureText: _staffObscure,
              decoration: InputDecoration(
                labelText: i18n.t('PASSWORD'),
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  icon: Icon(
                      _staffObscure ? Icons.visibility_off : Icons.visibility),
                  onPressed: () =>
                      setState(() => _staffObscure = !_staffObscure),
                ),
              ),
              validator: (v) =>
                  (v == null || v.isEmpty) ? i18n.t('ENTER_PASSWORD') : null,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _staffLogin(),
            ),
            if (_staffError != null) ...[
              const SizedBox(height: 12),
              _buildError(_staffError!),
            ],
            const SizedBox(height: 18),
            if (context.watch<ConnectivityService>().isOffline) ...[
              _buildNotice(
                'You are offline. Login using saved device session.',
                warning: true,
              ),
              const SizedBox(height: 12),
            ],
            SizedBox(
              height: 50,
              child: ElevatedButton(
                onPressed: _staffLoading ? null : _staffLogin,
                child: _staffLoading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text(i18n.t('SIGN_IN'),
                        style: const TextStyle(fontSize: 16)),
              ),
            ),
            if (context.watch<ConnectivityService>().isOffline) ...[
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: _staffLoading ? null : _offlineSessionLogin,
                icon: const Icon(Icons.fingerprint),
                label: const Text('Use Saved Device Session'),
              ),
            ],
            const SizedBox(height: 24),
            _buildDemoButtons(),
            const SizedBox(height: 12),
            _buildLegalLinks(),
          ],
        ),
      ),
    );
  }

  // ignore: unused_element
  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [_primaryBlue, Color(0xFF0D47A1), _accentBlue],
        ),
      ),
      child: Stack(
        children: [
          // State map background
          Positioned.fill(
            child: Opacity(
              opacity: 0.08,
              child: Image.asset(
                'assets/state_map.png',
                fit: BoxFit.contain,
              ),
            ),
          ),
          // Decorative circle
          Positioned(
            top: -80,
            right: -80,
            child: Container(
              width: 200,
              height: 200,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: Colors.white.withOpacity(0.15),
                  width: 2,
                  style: BorderStyle.solid,
                ),
              ),
            ),
          ),
          // Language selector (top-right)
          const Positioned(
            top: 0,
            right: 0,
            child: MeghaLanguageSelector(dark: true, compact: true),
          ),
          // Content
          Column(
            children: [
              // Logo
              Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(16),
                ),
                padding: const EdgeInsets.all(12),
                child: Image.asset(
                  'assets/branding/meghaconnect-ai-logo-transparent.png',
                  fit: BoxFit.contain,
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'MEGHACONNECT AI',
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(height: 6),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: const Color(0xFF22C55E).withAlpha(46),
                  borderRadius: BorderRadius.circular(999),
                  border:
                      Border.all(color: const Color(0xFF22C55E).withAlpha(77)),
                ),
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.verified_user_outlined,
                        color: Color(0xFF86EFAC), size: 14),
                    SizedBox(width: 6),
                    Text(
                      'AI Powered',
                      style: TextStyle(
                        color: Color(0xFFDCFCE7),
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Connecting Citizens Through Intelligence',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 13,
                  color: Colors.white.withOpacity(0.9),
                  height: 1.4,
                ),
              ),
              const SizedBox(height: 20),
              // CM Profile Section
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: Colors.white.withOpacity(0.15),
                    width: 1,
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: Colors.white.withOpacity(0.3),
                          width: 2,
                        ),
                        image: const DecorationImage(
                          image: AssetImage('assets/CM_Profile_Picture.jpg'),
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          "Hon'ble Chief Minister",
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                            fontSize: 14,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Connecting Citizens Through Intelligence',
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.85),
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // ignore: unused_element
  Widget _buildTabBar() {
    final i18n = context.watch<AppI18n>();
    return Container(
      decoration: BoxDecoration(
        color: Colors.grey[100],
        borderRadius: BorderRadius.circular(12),
      ),
      clipBehavior: Clip.antiAlias,
      child: TabBar(
        controller: _tabController,
        indicatorSize: TabBarIndicatorSize.tab,
        indicator: BoxDecoration(
          borderRadius: BorderRadius.circular(10),
          color: _primaryBlue,
        ),
        labelColor: Colors.white,
        unselectedLabelColor: Colors.grey[600],
        dividerColor: Colors.transparent,
        tabs: [
          Tab(
            icon: const Icon(Icons.badge_outlined, size: 18),
            text: i18n.t('STAFF_LOGIN'),
          ),
          Tab(
            icon: const Icon(Icons.people_outline, size: 18),
            text: i18n.t('PUBLIC_CITIZEN'),
          ),
        ],
      ),
    );
  }

  // ignore: unused_element
  Widget _buildLegacyStaffTab() {
    final i18n = context.watch<AppI18n>();
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Form(
        key: _staffFormKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 8),
            TextFormField(
              controller: _usernameCtrl,
              decoration: InputDecoration(
                labelText: i18n.t('USERNAME'),
                prefixIcon: const Icon(Icons.person_outline),
              ),
              validator: (v) =>
                  (v == null || v.isEmpty) ? i18n.t('ENTER_USERNAME') : null,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _passwordCtrl,
              obscureText: _staffObscure,
              decoration: InputDecoration(
                labelText: i18n.t('PASSWORD'),
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  icon: Icon(
                      _staffObscure ? Icons.visibility_off : Icons.visibility),
                  onPressed: () =>
                      setState(() => _staffObscure = !_staffObscure),
                ),
              ),
              validator: (v) =>
                  (v == null || v.isEmpty) ? i18n.t('ENTER_PASSWORD') : null,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _staffLogin(),
            ),
            if (_staffError != null) ...[
              const SizedBox(height: 12),
              _buildError(_staffError!),
            ],
            const SizedBox(height: 20),
            SizedBox(
              height: 48,
              child: ElevatedButton(
                onPressed: _staffLoading ? null : _staffLogin,
                child: _staffLoading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text(i18n.t('SIGN_IN'),
                        style: const TextStyle(fontSize: 16)),
              ),
            ),
            const SizedBox(height: 24),
            _buildDemoButtons(),
            const SizedBox(height: 12),
            _buildLegalLinks(),
          ],
        ),
      ),
    );
  }

  Widget _buildDemoButtons() {
    final demos = _demoLogins();
    if (demos.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Expanded(child: Divider()),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Text(
                'Quick Demo Access',
                style: TextStyle(color: Colors.grey[500], fontSize: 12),
              ),
            ),
            const Expanded(child: Divider()),
          ],
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: demos.map((d) {
            return ActionChip(
              label: Text(d.label),
              backgroundColor: d.color.withAlpha(26),
              labelStyle: TextStyle(
                  color: d.color, fontWeight: FontWeight.w600, fontSize: 12),
              side: BorderSide(color: d.color.withAlpha(77)),
              onPressed: () => _fillDemo(d.username, d.password),
            );
          }).toList(),
        ),
      ],
    );
  }

  List<_DemoLogin> _demoLogins() {
    if (!AppConfig.enableDemoCredentials) return const [];
    const colors = [
      Color(0xFF1A237E),
      Color(0xFF1565C0),
      Color(0xFF0288D1),
      Color(0xFF00838F),
      Color(0xFF2E7D32),
      Color(0xFF558B2F),
    ];
    final entries = AppConfig.demoLoginEntries
        .split(',')
        .map((entry) => entry.trim())
        .where((entry) => entry.isNotEmpty)
        .toList();
    final demos = <_DemoLogin>[];
    for (var i = 0; i < entries.length; i++) {
      final parts = entries[i].split('|').map((p) => p.trim()).toList();
      if (parts.length < 3 || parts.any((part) => part.isEmpty)) continue;
      demos.add(_DemoLogin(
        username: parts[0],
        password: parts[1],
        label: parts[2],
        color: colors[i % colors.length],
      ));
    }
    return demos;
  }

  Widget _buildLegalLinks() {
    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 16,
      runSpacing: 4,
      children: [
        TextButton(
          onPressed: () => _openUrl(AppConfig.privacyPolicyUrl),
          child: const Text('Privacy Policy'),
        ),
        TextButton(
          onPressed: () => _openUrl(AppConfig.termsUrl),
          child: const Text('Terms & Conditions'),
        ),
      ],
    );
  }

  Widget _buildRegistrationPicker() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Select your registration',
          style: TextStyle(
            color: _primaryBlue,
            fontSize: 13,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 8),
        ..._registrationOptions.map((visitor) {
          final id = (visitor['visitorId'] ?? visitor['id'])?.toString() ?? '';
          final selected =
              id.isNotEmpty && id == _selectedVisitorId?.toString();
          final name = visitor['fullName']?.toString() ?? 'Visitor';
          final epic = (visitor['maskedEpicNumber'] ??
                  _maskId(visitor['epicNumber']?.toString()))
              .toString();
          final district = visitor['district']?.toString() ?? '';
          final constituency = visitor['constituency']?.toString() ?? '';
          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: InkWell(
              borderRadius: BorderRadius.circular(10),
              onTap: () => _selectRegistration(visitor),
              child: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: selected ? const Color(0xFFE8EAF6) : Colors.white,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                    color: selected ? _primaryBlue : const Color(0xFFE5E7EB),
                  ),
                ),
                child: Row(
                  children: [
                    Radio<String>(
                      value: id,
                      groupValue: _selectedVisitorId?.toString(),
                      onChanged: (_) => _selectRegistration(visitor),
                    ),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            [
                              if (epic.isNotEmpty) 'EPIC $epic',
                              if (district.isNotEmpty) district,
                              if (constituency.isNotEmpty) constituency,
                            ].join(' · '),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: Color(0xFF64748B),
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),
      ],
    );
  }

  String _maskId(String? value) {
    final text = (value ?? '').trim();
    if (text.length <= 4) return text;
    return '****${text.substring(text.length - 4)}';
  }

  // ignore: unused_element
  Widget _buildPublicTab() {
    final i18n = context.watch<AppI18n>();
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Form(
        key: _publicFormKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFE8EAF6),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFF9FA8DA)),
              ),
              child: Row(
                children: [
                  const Icon(Icons.info_outline, color: _accentBlue, size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      i18n.t('CITIZENS_OTP_INFO'),
                      style: TextStyle(color: Colors.grey[700], fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            TextFormField(
              controller: _phoneCtrl,
              keyboardType: TextInputType.phone,
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
                LengthLimitingTextInputFormatter(10),
              ],
              decoration: InputDecoration(
                labelText: i18n.t('MOBILE_NUMBER'),
                prefixIcon: const Icon(Icons.phone_outlined),
                prefixText: '+91 ',
                hintText: i18n.t('ENTER_10_DIGIT_MOBILE'),
              ),
              onChanged: (_) {
                if (_requiresEpic ||
                    _otpSent ||
                    _selectedVisitorId != null ||
                    _registrationOptions.isNotEmpty ||
                    _publicNotice != null) {
                  _clearPublicSelectionForMobileEdit();
                }
              },
              validator: (v) {
                if (v == null || v.isEmpty) {
                  return i18n.t('ENTER_MOBILE_NUMBER');
                }
                if (v.length != 10) return i18n.t('ENTER_10_DIGIT_MOBILE');
                return null;
              },
            ),
            if (_requiresEpic) ...[
              const SizedBox(height: 12),
              if (_registrationOptions.isNotEmpty)
                _buildRegistrationPicker()
              else
                TextFormField(
                  controller: _publicEpicCtrl,
                  decoration: InputDecoration(
                    labelText: i18n.t('EPIC_NUMBER'),
                    prefixIcon: const Icon(Icons.credit_card_outlined),
                    hintText: i18n.t('ENTER_EPIC_NUMBER'),
                  ),
                  textCapitalization: TextCapitalization.characters,
                  validator: (v) {
                    if (!_requiresEpic) return null;
                    if (v == null || v.trim().isEmpty) {
                      return i18n.t('ENTER_EPIC_NUMBER');
                    }
                    return null;
                  },
                ),
            ],
            if (_publicNotice != null) ...[
              const SizedBox(height: 12),
              _buildNotice(_publicNotice!, warning: _publicNoticeIsWarning),
            ],
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _publicLoading ? null : _sendOtp,
              icon: _publicLoading && !_otpSent
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : Icon(_otpSent ? Icons.refresh : Icons.send_outlined),
              label: Text(
                  _otpSent ? i18n.t('RESEND_OTP') : i18n.t('GENERATE_OTP')),
            ),
            if (_otpSent) ...[
              const SizedBox(height: 16),
              TextFormField(
                controller: _otpCtrl,
                keyboardType: TextInputType.number,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(6),
                ],
                decoration: InputDecoration(
                  labelText: i18n.t('ENTER_OTP'),
                  prefixIcon: const Icon(Icons.verified_outlined),
                  hintText: i18n.t('ENTER_6_DIGIT_OTP'),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return i18n.t('ENTER_OTP');
                  if (v.length != 6) return i18n.t('ENTER_6_DIGIT_OTP');
                  return null;
                },
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _publicLogin(),
              ),
              const SizedBox(height: 20),
              SizedBox(
                height: 48,
                child: ElevatedButton(
                  onPressed: _publicLoading || _otpLocked ? null : _publicLogin,
                  child: _publicLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : Text(i18n.t('VERIFY_LOGIN'),
                          style: const TextStyle(fontSize: 16)),
                ),
              ),
              const SizedBox(height: 8),
              TextButton.icon(
                onPressed: _publicLoading ? null : _changePublicNumber,
                icon: const Icon(Icons.arrow_back),
                label: Text(i18n.t('CHANGE_NUMBER')),
              ),
            ],
            const SizedBox(height: 20),
            Row(
              children: [
                const Expanded(child: Divider()),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Text(
                    i18n.t('NEW_VISITOR'),
                    style: TextStyle(color: Colors.grey[500], fontSize: 12),
                  ),
                ),
                const Expanded(child: Divider()),
              ],
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _publicLoading
                  ? null
                  : () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => const VisitorRegistrationScreen(),
                        ),
                      );
                    },
              icon: const Icon(Icons.person_add_alt_1_outlined),
              label: Text(i18n.t('REGISTER_AS_NEW_VISITOR')),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primaryBlue,
                side: const BorderSide(color: _primaryBlue),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildError(String msg) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFEE2E2),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFFCA5A5)),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: Color(0xFF991B1B), size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(msg,
                style: const TextStyle(color: Color(0xFF991B1B), fontSize: 13)),
          ),
        ],
      ),
    );
  }

  Widget _buildNotice(String msg, {required bool warning}) {
    final bg = warning ? const Color(0xFFFEF3C7) : const Color(0xFFFEE2E2);
    final border = warning ? const Color(0xFFFCD34D) : const Color(0xFFFCA5A5);
    final iconColor =
        warning ? const Color(0xFFB45309) : const Color(0xFF991B1B);
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: border),
      ),
      child: Row(
        children: [
          Icon(warning ? Icons.warning_amber_outlined : Icons.error_outline,
              color: iconColor, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              msg,
              style: TextStyle(color: iconColor, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }
}

class _DemoLogin {
  const _DemoLogin({
    required this.username,
    required this.password,
    required this.label,
    required this.color,
  });

  final String username;
  final String password;
  final String label;
  final Color color;
}
