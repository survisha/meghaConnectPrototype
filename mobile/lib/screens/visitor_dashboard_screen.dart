import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../services/connectivity_service.dart';
import 'pending_sync_screen.dart';
import '../core/i18n/app_i18n.dart';
import '../widgets/megha_ui.dart';
import 'new_appointment_screen.dart';
import 'guest_appointment_screen.dart';
import 'scheme_form_screen.dart';
import 'grievance_screen.dart';

class _SummaryCard {
  final String label;
  final int value;
  final IconData icon;
  final Color color;
  final Color bg;
  const _SummaryCard(this.label, this.value, this.icon, this.color, this.bg);
}

class _MyAppointment {
  final String id;
  final String agenda;
  final String status;
  final String date;
  const _MyAppointment(this.id, this.agenda, this.status, this.date);
}

class _MyScheme {
  final String id;
  final String scheme;
  final String project;
  final String status;
  final String amount;
  final String submittedDate;
  const _MyScheme(
    this.id,
    this.scheme,
    this.project,
    this.status,
    this.amount,
    this.submittedDate,
  );
}

class _MyGrievance {
  final String id;
  final String subject;
  final String status;
  final String date;
  const _MyGrievance(this.id, this.subject, this.status, this.date);
}

const _primaryBlue = Color(0xFF1A237E);
const _green = Color(0xFF065F46);
const _amber = Color(0xFFB45309);
const _red = Color(0xFFDC2626);

Color _appointmentColor(String status) {
  switch (status) {
    case 'SCHEDULED':
      return _amber;
    case 'COMPLETED':
      return _green;
    case 'HCM_PENDING':
      return _red;
    case 'CMO_REVIEW':
      return _amber;
    default:
      return _primaryBlue;
  }
}

Color _schemeColor(String status) {
  switch (status) {
    case 'APPROVED':
      return _green;
    case 'UNDER_REVIEW':
      return _amber;
    case 'REJECTED':
      return _red;
    default:
      return _primaryBlue;
  }
}

Color _grievanceColor(String status) {
  switch (status) {
    case 'RESOLVED':
      return _green;
    case 'FORWARDED':
      return _amber;
    case 'UNDER_REVIEW':
      return _amber;
    default:
      return _primaryBlue;
  }
}

class VisitorDashboardScreen extends StatefulWidget {
  const VisitorDashboardScreen({super.key});

  @override
  State<VisitorDashboardScreen> createState() => _VisitorDashboardScreenState();
}

class _VisitorDashboardScreenState extends State<VisitorDashboardScreen> {
  bool _loading = true;
  List<_MyAppointment> _appointments = [];
  List<_MyScheme> _schemes = [];
  List<_MyGrievance> _grievances = [];
  Map<String, dynamic>? _visitorProfile;
  bool _kycVerifiedGraphic = false;

  static const _timeline = [
    ('Application Submitted', '–', _primaryBlue),
    ('CMO Verification', '–', _amber),
    ('Approver Review', '–', _primaryBlue),
    ('HCM Decision Pending', '–', _red),
  ];

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  static String _fmtDate(String? iso) {
    if (iso == null) return '—';
    final dt = DateTime.tryParse(iso);
    if (dt == null) return iso;
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec'
    ];
    return '${dt.day} ${months[dt.month - 1]} ${dt.year}';
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    final visitorId = context.read<AuthService>().user?.visitorId;
    final results = await Future.wait([
      ApiService.getMyAppointments(),
      visitorId != null && visitorId > 0
          ? ApiService.getSchemeApplicationsForVisitor(visitorId, size: 5)
          : ApiService.getSchemeApplications(size: 5),
      ApiService.getGrievances(size: 5, visitorId: visitorId),
      visitorId != null && visitorId > 0
          ? ApiService.getVisitorProfileById(visitorId)
          : Future<Map<String, dynamic>?>.value(null),
    ]);
    if (!mounted) return;
    final apptPage = results[0] ?? <String, dynamic>{};
    final schemePage = results[1] ?? <String, dynamic>{};
    final grievancePage = results[2] ?? <String, dynamic>{};
    final profile = results[3];
    setState(() {
      _visitorProfile = profile;
      _appointments = ((apptPage['content'] as List<dynamic>?) ?? []).map((e) {
        final m = e as Map<String, dynamic>;
        return _MyAppointment(
          m['applicationId'] as String? ?? m['id']?.toString() ?? '',
          m['agendaBrief'] as String? ?? '',
          m['status'] as String? ?? '',
          _fmtDate(m['scheduledDateTime'] as String?),
        );
      }).toList();
      _schemes = ((schemePage['content'] as List<dynamic>?) ?? []).map((e) {
        final m = e as Map<String, dynamic>;
        final cost = (m['estimatedCost'] as num?)?.toDouble() ?? 0;
        final costLabel = cost >= 100000
            ? '₹${(cost / 100000).toStringAsFixed(1)}L'
            : '₹${(cost / 1000).toStringAsFixed(0)}K';
        return _MyScheme(
          m['id']?.toString() ?? '',
          m['schemeType'] as String? ?? '',
          m['projectName'] as String? ?? '',
          m['status'] as String? ?? '',
          costLabel,
          _fmtDate(m['createdAt'] as String?),
        );
      }).toList();
      _grievances =
          ((grievancePage['content'] as List<dynamic>?) ?? []).map((e) {
        final m = e as Map<String, dynamic>;
        return _MyGrievance(
          m['ticketId'] as String? ?? m['id']?.toString() ?? '',
          m['subject'] as String? ?? '',
          m['status'] as String? ?? '',
          _fmtDate(m['submittedAt'] as String?),
        );
      }).toList();
      _loading = false;
    });
  }

  bool get _isKycPending {
    final status = _visitorProfile?['kycStatus']?.toString().toUpperCase();
    final verified = _visitorProfile?['kycVerified'] == true;
    return status == 'KYC_PENDING' || (!verified && status == 'PENDING');
  }

  Future<void> _onRefresh() async {
    await _loadData();
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final i18n = context.watch<AppI18n>();
    final name = auth.user?.fullName ?? 'Visitor';

    final cards = [
      _SummaryCard('My Appointments', _appointments.length,
          Icons.calendar_today_outlined, _primaryBlue, const Color(0xFFE8EAF6)),
      _SummaryCard('Scheme Applications', _schemes.length,
          Icons.workspace_premium_outlined, _green, const Color(0xFFD1FAE5)),
      _SummaryCard('Grievances Raised', _grievances.length,
          Icons.comment_outlined, _amber, const Color(0xFFFEF3C7)),
      const _SummaryCard('Pending Actions', 1, Icons.warning_amber_outlined,
          _red, Color(0xFFFEE2E2)),
    ];

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        toolbarHeight: 72,
        elevation: 2,
        titleSpacing: 16,
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        flexibleSpace: const DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [MeghaColors.primary, MeghaColors.primary2],
            ),
          ),
        ),
        title: Row(
          children: [
            Image.asset('assets/logo-small.png', width: 38, height: 38),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text(
                    'MeghaConnect',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  Text(
                    i18n.t('MY_PORTAL'),
                    style: TextStyle(
                      color: Colors.white.withAlpha(214),
                      fontSize: 11,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          const MeghaLanguageSelector(dark: true, compact: true),
          IconButton(
            icon: const Icon(Icons.sync_problem_outlined),
            tooltip: 'Pending Sync',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => Scaffold(
                  appBar: AppBar(title: const Text('Pending Sync')),
                  body: const PendingSyncScreen(),
                ),
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: i18n.t('LOGOUT'),
            onPressed: () async {
              await auth.logout();
            },
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _onRefresh,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  // Welcome Banner
                  Container(
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [Color(0xFF1A237E), Color(0xFF283593)],
                      ),
                      borderRadius: BorderRadius.circular(14),
                      boxShadow: [
                        BoxShadow(
                            color: _primaryBlue.withOpacity(0.25),
                            blurRadius: 12,
                            offset: const Offset(0, 4))
                      ],
                    ),
                    padding: const EdgeInsets.all(20),
                    child: Row(
                      children: [
                        Container(
                          width: 52,
                          height: 52,
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(26),
                          ),
                          child: const Icon(Icons.person,
                              color: Colors.white, size: 28),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Welcome, $name!',
                                  style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 17,
                                      fontWeight: FontWeight.bold)),
                              const SizedBox(height: 4),
                              const Text(
                                'Meghalaya Entry & Governance System',
                                style: TextStyle(
                                    color: Colors.white70, fontSize: 12),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (context.watch<ConnectivityService>().isOffline) ...[
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFEF3C7),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Text(
                        'You are working in offline mode. Data will sync when internet is available.',
                        style:
                            TextStyle(color: Color(0xFF92400E), fontSize: 12),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],
                  if (_kycVerifiedGraphic) ...[
                    _KycSuccessCard(),
                    const SizedBox(height: 16),
                  ],
                  if (_isKycPending) ...[
                    _KycPendingCard(onVerify: () => _openKycRetrySheet(name)),
                    const SizedBox(height: 16),
                  ],

                  // Summary cards
                  GridView.count(
                    crossAxisCount: 2,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    crossAxisSpacing: 10,
                    mainAxisSpacing: 10,
                    childAspectRatio: 1.5,
                    children: cards.map((c) => _SummaryTile(card: c)).toList(),
                  ),
                  const SizedBox(height: 16),

                  // Quick Actions
                  const Text('Quick Actions',
                      style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: _primaryBlue)),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      _QuickActionBtn(
                        icon: Icons.add_circle_outline,
                        label: 'Book\nAppointment',
                        color: _primaryBlue,
                        onTap: () => _openNewAppointment(context),
                      ),
                      const SizedBox(width: 8),
                      _QuickActionBtn(
                        icon: Icons.workspace_premium_outlined,
                        label: 'Apply for\nScheme',
                        color: _green,
                        onTap: () => _openSchemeForm(context),
                      ),
                      const SizedBox(width: 8),
                      _QuickActionBtn(
                        icon: Icons.comment_outlined,
                        label: 'Raise\nGrievance',
                        color: _amber,
                        onTap: () => _openGrievanceScreen(openForm: true),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _QuickActionBtn(
                        icon: Icons.person_add_alt_1_outlined,
                        label: 'Guest\nRegistration',
                        color: const Color(0xFF0F766E),
                        onTap: () => _openGuestRegistration(context),
                      ),
                      const SizedBox(width: 8),
                      _QuickActionBtn(
                        icon: Icons.sync_problem_outlined,
                        label: 'Pending\nSync',
                        color: const Color(0xFFB45309),
                        onTap: () => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => Scaffold(
                              appBar: AppBar(title: const Text('Pending Sync')),
                              body: const PendingSyncScreen(),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),

                  // My Appointments
                  _SectionCard(
                    title: 'My Appointments',
                    icon: Icons.calendar_today_outlined,
                    action: TextButton(
                      onPressed: () => _openNewAppointment(context),
                      child: const Text('+ Book New',
                          style: TextStyle(fontSize: 12)),
                    ),
                    child: _appointments.isEmpty
                        ? _empty('No appointments yet.')
                        : Column(
                            children: _appointments
                                .map((a) => _ItemRow(
                                      id: a.id,
                                      title: a.agenda,
                                      subtitle: a.date,
                                      statusLabel:
                                          a.status.replaceAll('_', ' '),
                                      statusColor: _appointmentColor(a.status),
                                    ))
                                .toList(),
                          ),
                  ),
                  const SizedBox(height: 14),

                  // My Scheme Applications
                  _SectionCard(
                    title: 'Scheme Applications',
                    icon: Icons.workspace_premium_outlined,
                    action: TextButton(
                      onPressed: () => _openSchemeForm(context),
                      child:
                          const Text('+ Apply', style: TextStyle(fontSize: 12)),
                    ),
                    child: _schemes.isEmpty
                        ? _empty('No applications yet.')
                        : Column(
                            children: _schemes
                                .map((s) => _ItemRow(
                                      id: s.id,
                                      title: s.scheme,
                                      subtitle:
                                          'Application #${s.id} · ${s.submittedDate}',
                                      statusLabel:
                                          s.status.replaceAll('_', ' '),
                                      statusColor: _schemeColor(s.status),
                                      onView: () => _showSchemeDetails(s),
                                    ))
                                .toList(),
                          ),
                  ),
                  const SizedBox(height: 14),

                  // My Grievances
                  _SectionCard(
                    title: 'My Grievances',
                    icon: Icons.comment_outlined,
                    action: TextButton(
                      onPressed: () => _openGrievanceScreen(openForm: true),
                      child:
                          const Text('+ Raise', style: TextStyle(fontSize: 12)),
                    ),
                    child: _grievances.isEmpty
                        ? _empty('No grievances raised yet.')
                        : Column(
                            children: _grievances
                                .map((g) => _ItemRow(
                                      id: g.id,
                                      title: g.subject,
                                      subtitle: g.date,
                                      statusLabel:
                                          g.status.replaceAll('_', ' '),
                                      statusColor: _grievanceColor(g.status),
                                    ))
                                .toList(),
                          ),
                  ),
                  const SizedBox(height: 14),

                  // Application Status Timeline
                  _SectionCard(
                    title: 'Latest Application Status',
                    icon: Icons.timeline_outlined,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'MC-2024-00042 – CMSDF Application',
                          style:
                              TextStyle(fontSize: 12, color: Colors.grey[600]),
                        ),
                        const SizedBox(height: 12),
                        ..._timeline.map((t) => _TimelineItem(
                              label: t.$1,
                              date: t.$2,
                              color: t.$3,
                              isLast: t == _timeline.last,
                            )),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
    );
  }

  Widget _empty(String text) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: Center(
            child: Text(text,
                style: const TextStyle(color: Colors.grey, fontSize: 13))),
      );

  void _openNewAppointment(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => Scaffold(
          appBar: AppBar(
            title: const Text('New Appointment'),
            backgroundColor: MeghaColors.primary,
            foregroundColor: Colors.white,
          ),
          body: const NewAppointmentScreen(isPublic: true),
        ),
      ),
    );
  }

  void _openGuestRegistration(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => Scaffold(
          appBar: AppBar(
            title: const Text('Guest Registration'),
            backgroundColor: MeghaColors.primary,
            foregroundColor: Colors.white,
          ),
          body: const GuestAppointmentScreen(),
        ),
      ),
    );
  }

  void _openSchemeForm(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => Scaffold(
          appBar: AppBar(
            title: const Text('Scheme Application'),
            backgroundColor: MeghaColors.primary,
            foregroundColor: Colors.white,
          ),
          body: const SchemeFormScreen(),
        ),
      ),
    );
  }

  Future<void> _openGrievanceScreen({bool openForm = false}) async {
    final visitorId = context.read<AuthService>().user?.visitorId;
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => GrievanceScreen(
          initialOpenForm: openForm,
          visitorId: visitorId,
          visitorProfile: _visitorProfile,
        ),
      ),
    );
    if (!mounted) return;
    if (changed == true) {
      await _loadData();
    }
  }

  Future<void> _openKycRetrySheet(String fallbackName) async {
    final visitorId = context.read<AuthService>().user?.visitorId;
    if (visitorId == null || visitorId <= 0) return;
    final result = await showModalBottomSheet<Map<String, dynamic>>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _KycRetrySheet(
        initialName: _visitorProfile?['fullName']?.toString() ?? fallbackName,
        visitorId: visitorId,
      ),
    );
    if (!mounted || result == null) return;
    final ok = result['success'] == true;
    final message = result['message']?.toString() ??
        (ok
            ? 'KYC verification completed successfully.'
            : 'Unable to verify EPIC details.');
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
    if (ok) {
      setState(() {
        final profile = result['profile'];
        if (profile is Map<String, dynamic>) {
          _visitorProfile = profile;
        } else {
          _visitorProfile = {
            ...?_visitorProfile,
            'kycStatus': result['kycStatus'] ?? 'KYC_VERIFIED',
            'kycProvider': result['kycProvider'] ?? 'EPIC',
            'kycVerified': true,
          };
        }
        _kycVerifiedGraphic = true;
      });
      await _loadData();
    }
  }

  void _showSchemeDetails(_MyScheme scheme) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title:
            Text(scheme.scheme.isEmpty ? 'Scheme Application' : scheme.scheme),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _dialogLine('Application Number', scheme.id),
            _dialogLine('Submitted Date', scheme.submittedDate),
            _dialogLine('Current Status', scheme.status.replaceAll('_', ' ')),
            if (scheme.project.isNotEmpty)
              _dialogLine('Project', scheme.project),
            if (scheme.amount.isNotEmpty)
              _dialogLine('Estimated Cost', scheme.amount),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  Widget _dialogLine(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text('$label: ${value.isEmpty ? '-' : value}'),
    );
  }
}

class _SummaryTile extends StatelessWidget {
  final _SummaryCard card;
  const _SummaryTile({required this.card});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.06), blurRadius: 8)
        ],
      ),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
                color: card.color, borderRadius: BorderRadius.circular(10)),
            child: Icon(card.icon, color: Colors.white, size: 22),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(card.value.toString(),
                    style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        color: card.color)),
                Text(card.label,
                    style: const TextStyle(fontSize: 11, color: Colors.grey),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _KycPendingCard extends StatelessWidget {
  final VoidCallback onVerify;
  const _KycPendingCard({required this.onVerify});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEB),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFFCD34D)),
      ),
      child: Row(
        children: [
          const Icon(Icons.verified_user_outlined, color: Color(0xFF92400E)),
          const SizedBox(width: 10),
          const Expanded(
            child: Text(
              'Your KYC verification is pending. Please verify your identity using EPIC details.',
              style: TextStyle(color: Color(0xFF92400E), fontSize: 12),
            ),
          ),
          const SizedBox(width: 8),
          TextButton(
            onPressed: onVerify,
            child: const Text('Verify with EPIC'),
          ),
        ],
      ),
    );
  }
}

class _KycSuccessCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFECFDF5),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFA7F3D0)),
      ),
      child: const Row(
        children: [
          Icon(Icons.check_circle, color: Color(0xFF059669)),
          SizedBox(width: 10),
          Text(
            'KYC Verified Successfully',
            style: TextStyle(
              color: Color(0xFF047857),
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _KycRetrySheet extends StatefulWidget {
  final String initialName;
  final int visitorId;
  const _KycRetrySheet({required this.initialName, required this.visitorId});

  @override
  State<_KycRetrySheet> createState() => _KycRetrySheetState();
}

class _KycRetrySheetState extends State<_KycRetrySheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameCtrl;
  final _epicCtrl = TextEditingController();
  bool _submitting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _nameCtrl = TextEditingController(text: widget.initialName);
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _epicCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    final result = await ApiService.retryVisitorKyc(
      visitorId: widget.visitorId,
      name: _nameCtrl.text.trim(),
      epicNumber: _epicCtrl.text.trim().toUpperCase(),
    );
    if (!mounted) return;
    setState(() => _submitting = false);
    if (result['success'] == true) {
      Navigator.of(context).pop(result);
    } else {
      setState(() {
        _error = result['message']?.toString() ??
            'Unable to verify EPIC details. Please check the EPIC number and name.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return Padding(
      padding: EdgeInsets.only(bottom: bottomInset),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(18, 18, 18, 20),
        child: Form(
          key: _formKey,
          onChanged: () => setState(() {}),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      'Verify with EPIC',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
                    ),
                  ),
                  IconButton(
                    onPressed:
                        _submitting ? null : () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _nameCtrl,
                textCapitalization: TextCapitalization.words,
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z ]'))
                ],
                decoration: const InputDecoration(
                  labelText: 'Name',
                  border: OutlineInputBorder(),
                  errorMaxLines: 2,
                ),
                validator: (value) {
                  final name = value?.trim() ?? '';
                  if (name.isEmpty) return 'Name is required.';
                  if (!RegExp(r'^[A-Za-z ]+$').hasMatch(name)) {
                    return 'Name should contain only letters and spaces.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _epicCtrl,
                textCapitalization: TextCapitalization.characters,
                maxLength: 10,
                inputFormatters: [_EpicInputFormatter()],
                decoration: const InputDecoration(
                  labelText: 'EPIC Number',
                  hintText: 'ABC1234567',
                  border: OutlineInputBorder(),
                  counterText: '',
                  errorMaxLines: 2,
                ),
                validator: (value) {
                  final epic = value?.trim().toUpperCase() ?? '';
                  if (epic.isEmpty) return 'EPIC number is required.';
                  if (!RegExp(r'^[A-Z]{3}[0-9]{7}$').hasMatch(epic)) {
                    return 'EPIC number must be 3 letters followed by 7 digits.';
                  }
                  return null;
                },
              ),
              if (_error != null) ...[
                const SizedBox(height: 10),
                Text(_error!,
                    style: const TextStyle(color: _red, fontSize: 12)),
              ],
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _submitting
                          ? null
                          : () => Navigator.of(context).pop(),
                      child: const Text('Cancel'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: ElevatedButton(
                      onPressed:
                          _submitting || !_isLocallyValid ? null : _submit,
                      child:
                          Text(_submitting ? 'Verifying...' : 'Fetch / Verify'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  bool get _isLocallyValid {
    final name = _nameCtrl.text.trim();
    final epic = _epicCtrl.text.trim().toUpperCase();
    return RegExp(r'^[A-Za-z ]+$').hasMatch(name) &&
        RegExp(r'^[A-Z]{3}[0-9]{7}$').hasMatch(epic);
  }
}

class _EpicInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final raw =
        newValue.text.toUpperCase().replaceAll(RegExp(r'[^A-Z0-9]'), '');
    final buffer = StringBuffer();
    for (final codeUnit in raw.codeUnits) {
      final char = String.fromCharCode(codeUnit);
      if (buffer.length < 3 && RegExp(r'[A-Z]').hasMatch(char)) {
        buffer.write(char);
      } else if (buffer.length >= 3 &&
          buffer.length < 10 &&
          RegExp(r'[0-9]').hasMatch(char)) {
        buffer.write(char);
      }
      if (buffer.length == 10) break;
    }
    final text = buffer.toString();
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }
}

class _QuickActionBtn extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;
  const _QuickActionBtn(
      {required this.icon,
      required this.label,
      required this.color,
      required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: color.withOpacity(0.3)),
          ),
          child: Column(
            children: [
              Icon(icon, color: color, size: 24),
              const SizedBox(height: 6),
              Text(label,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                      fontSize: 11, fontWeight: FontWeight.w600, color: color)),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final Widget child;
  final Widget? action;
  const _SectionCard(
      {required this.title,
      required this.icon,
      required this.child,
      this.action});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.06), blurRadius: 8)
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                  colors: [Color(0xFF1A237E), Color(0xFF3949AB)]),
              borderRadius: BorderRadius.vertical(top: Radius.circular(12)),
            ),
            child: Row(
              children: [
                Icon(icon, color: Colors.white, size: 16),
                const SizedBox(width: 8),
                Expanded(
                    child: Text(title,
                        style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 13))),
                if (action != null) action!,
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: child,
          ),
        ],
      ),
    );
  }
}

class _ItemRow extends StatelessWidget {
  final String id;
  final String title;
  final String subtitle;
  final String statusLabel;
  final Color statusColor;
  final VoidCallback? onView;
  const _ItemRow(
      {required this.id,
      required this.title,
      required this.subtitle,
      required this.statusLabel,
      required this.statusColor,
      this.onView});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: Color(0xFFF3F4F6)))),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(id,
                    style: const TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 11,
                        color: Colors.grey)),
                Text(title,
                    style: const TextStyle(
                        fontWeight: FontWeight.w600, fontSize: 13),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
                Text(subtitle,
                    style: TextStyle(fontSize: 11, color: Colors.grey[500])),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: statusColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: statusColor.withOpacity(0.3)),
            ),
            child: Text(statusLabel,
                style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    color: statusColor)),
          ),
          if (onView != null) ...[
            const SizedBox(width: 6),
            TextButton(
              onPressed: onView,
              child: const Text('View', style: TextStyle(fontSize: 12)),
            ),
          ],
        ],
      ),
    );
  }
}

class _TimelineItem extends StatelessWidget {
  final String label;
  final String date;
  final Color color;
  final bool isLast;
  const _TimelineItem(
      {required this.label,
      required this.date,
      required this.color,
      required this.isLast});

  @override
  Widget build(BuildContext context) {
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 28,
            child: Column(
              children: [
                Container(
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                      color: color, borderRadius: BorderRadius.circular(8)),
                  child: const Icon(Icons.circle, color: Colors.white, size: 8),
                ),
                if (!isLast)
                  Expanded(child: Container(width: 2, color: Colors.grey[300])),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label,
                      style: const TextStyle(
                          fontWeight: FontWeight.w600, fontSize: 13)),
                  Text(date,
                      style: TextStyle(fontSize: 11, color: Colors.grey[500])),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
