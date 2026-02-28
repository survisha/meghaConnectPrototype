import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';

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
  final _otpCtrl = TextEditingController();
  bool _publicLoading = false;
  String? _publicError;
  bool _otpSent = false;

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
    final ok = await auth.login(_usernameCtrl.text, _passwordCtrl.text);
    if (!mounted) return;
    setState(() => _staffLoading = false);
    if (!ok) setState(() => _staffError = 'Invalid username or password.');
  }

  Future<void> _sendOtp() async {
    if (!_publicFormKey.currentState!.validate()) return;
    setState(() {
      _publicLoading = true;
      _publicError = null;
    });
    await Future.delayed(const Duration(milliseconds: 600));
    if (!mounted) return;
    setState(() {
      _publicLoading = false;
      _otpSent = true;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Demo OTP sent: 123456'),
        backgroundColor: Color(0xFF065F46),
      ),
    );
  }

  Future<void> _publicLogin() async {
    if (!_publicFormKey.currentState!.validate()) return;
    setState(() {
      _publicLoading = true;
      _publicError = null;
    });
    final auth = context.read<AuthService>();
    final ok = await auth.login(_phoneCtrl.text, _otpCtrl.text);
    if (!mounted) return;
    setState(() => _publicLoading = false);
    if (!ok) setState(() => _publicError = 'Invalid phone number or OTP.');
  }

  void _fillDemo(String user, String pass) {
    _usernameCtrl.text = user;
    _passwordCtrl.text = pass;
    setState(() => _staffError = null);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _primaryBlue,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            Expanded(
              child: Container(
                margin: const EdgeInsets.only(top: 4),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(24),
                    topRight: Radius.circular(24),
                  ),
                ),
                child: Column(
                  children: [
                    const SizedBox(height: 8),
                    _buildTabBar(),
                    Expanded(
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
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(26),
              shape: BoxShape.circle,
            ),
            child: const Text('🏛️', style: TextStyle(fontSize: 48)),
          ),
          const SizedBox(height: 16),
          const Text(
            'MeghaConnect',
            style: TextStyle(
              color: Colors.white,
              fontSize: 28,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            "Chief Minister's Office · Meghalaya",
            style: TextStyle(
              color: Colors.white.withAlpha(204),
              fontSize: 13,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            'Government of Meghalaya',
            style: TextStyle(
              color: Colors.white.withAlpha(153),
              fontSize: 12,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabBar() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        borderRadius: BorderRadius.circular(12),
      ),
      child: TabBar(
        controller: _tabController,
        indicator: BoxDecoration(
          borderRadius: BorderRadius.circular(10),
          color: _primaryBlue,
        ),
        labelColor: Colors.white,
        unselectedLabelColor: Colors.grey[600],
        dividerColor: Colors.transparent,
        tabs: const [
          Tab(text: '🔐  Staff Login'),
          Tab(text: '📱  Citizen Login'),
        ],
      ),
    );
  }

  Widget _buildStaffTab() {
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
              decoration: const InputDecoration(
                labelText: 'Username',
                prefixIcon: Icon(Icons.person_outline),
              ),
              validator: (v) => (v == null || v.isEmpty) ? 'Enter username' : null,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _passwordCtrl,
              obscureText: _staffObscure,
              decoration: InputDecoration(
                labelText: 'Password',
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  icon: Icon(_staffObscure ? Icons.visibility_off : Icons.visibility),
                  onPressed: () => setState(() => _staffObscure = !_staffObscure),
                ),
              ),
              validator: (v) => (v == null || v.isEmpty) ? 'Enter password' : null,
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
                    : const Text('Sign In', style: TextStyle(fontSize: 16)),
              ),
            ),
            const SizedBox(height: 24),
            _buildDemoButtons(),
          ],
        ),
      ),
    );
  }

  Widget _buildDemoButtons() {
    final demos = [
      ('hcm', 'hcm123', 'HCM', const Color(0xFF1A237E)),
      ('admin', 'admin123', 'ADMIN', const Color(0xFF1565C0)),
      ('saidul', 'osd123', 'OSD', const Color(0xFF0288D1)),
      ('jtsecy', 'jts123', 'JT. SECY', const Color(0xFF00838F)),
      ('cmo', 'cmo123', 'CMO', const Color(0xFF2E7D32)),
      ('deo1', 'deo123', 'DEO', const Color(0xFF558B2F)),
    ];

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
              label: Text(d.$3),
              backgroundColor: d.$4.withAlpha(26),
              labelStyle: TextStyle(color: d.$4, fontWeight: FontWeight.w600, fontSize: 12),
              side: BorderSide(color: d.$4.withAlpha(77)),
              onPressed: () => _fillDemo(d.$1, d.$2),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildPublicTab() {
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
                      'Citizens can log in with their registered mobile number to book appointments with the Chief Minister.',
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
              decoration: const InputDecoration(
                labelText: 'Mobile Number',
                prefixIcon: Icon(Icons.phone_outlined),
                prefixText: '+91 ',
                hintText: '10-digit number',
              ),
              validator: (v) {
                if (v == null || v.isEmpty) return 'Enter mobile number';
                if (v.length != 10) return 'Enter valid 10-digit number';
                return null;
              },
            ),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: _publicLoading ? null : _sendOtp,
              style: OutlinedButton.styleFrom(
                foregroundColor: _primaryBlue,
                side: const BorderSide(color: _primaryBlue),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
              child: _publicLoading && !_otpSent
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(_otpSent ? 'Resend OTP' : 'Send OTP'),
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
                decoration: const InputDecoration(
                  labelText: 'Enter OTP',
                  prefixIcon: Icon(Icons.verified_outlined),
                  hintText: '6-digit OTP',
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Enter OTP';
                  if (v.length != 6) return 'Enter valid 6-digit OTP';
                  return null;
                },
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => _publicLogin(),
              ),
              if (_publicError != null) ...[
                const SizedBox(height: 12),
                _buildError(_publicError!),
              ],
              const SizedBox(height: 20),
              SizedBox(
                height: 48,
                child: ElevatedButton(
                  onPressed: _publicLoading ? null : _publicLogin,
                  child: _publicLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text('Verify & Login', style: TextStyle(fontSize: 16)),
                ),
              ),
            ],
            const SizedBox(height: 20),
            Row(
              children: [
                const Expanded(child: Divider()),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Text(
                    'Demo Credentials',
                    style: TextStyle(color: Colors.grey[500], fontSize: 12),
                  ),
                ),
                const Expanded(child: Divider()),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              'Phone: 9876543210  ·  OTP: 123456',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600], fontSize: 12),
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
            child: Text(msg, style: const TextStyle(color: Color(0xFF991B1B), fontSize: 13)),
          ),
        ],
      ),
    );
  }
}
