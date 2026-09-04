import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../services/navigation_service.dart';
import '../utils/dashboard_metrics.dart';

class _Kpi {
  final String label;
  final int value;
  final IconData icon;
  final Color color;
  final Color bg;
  final List<UserRole> roles;
  const _Kpi(
      this.label, this.value, this.icon, this.color, this.bg, this.roles);
}

class _QuickAction {
  final String label;
  final IconData icon;
  final String route;
  final Color color;
  final List<UserRole> roles;
  const _QuickAction(this.label, this.icon, this.route, this.color, this.roles);
}

const _allStaff = [
  UserRole.HCM,
  UserRole.ADMIN,
  UserRole.APPROVER,
  UserRole.DEO,
];

const _seniorStaff = [
  UserRole.HCM,
  UserRole.ADMIN,
  UserRole.APPROVER,
];

const _allKpis = <_Kpi>[
  _Kpi("Today's Appointments", 0, Icons.calendar_today_outlined,
      Color(0xFF1A237E), Color(0xFFE8EAF6), _allStaff),
  _Kpi(
      'Pending Approvals',
      0,
      Icons.pending_actions_outlined,
      Color(0xFFB45309),
      Color(0xFFFEF3C7),
      [UserRole.HCM, UserRole.ADMIN, UserRole.APPROVER]),
  _Kpi('Active Scheme Apps', 0, Icons.workspace_premium_outlined,
      Color(0xFF065F46), Color(0xFFD1FAE5), _seniorStaff),
  _Kpi('Walk-in Pending', 0, Icons.pending_actions_outlined, Color(0xFF0369A1),
      Color(0xFFE0F2FE), [UserRole.DEO, UserRole.ADMIN, UserRole.APPROVER]),
  _Kpi('Walk-in Completed', 0, Icons.task_alt_outlined, Color(0xFF15803D),
      Color(0xFFDCFCE7), [UserRole.DEO, UserRole.ADMIN, UserRole.APPROVER]),
];

final _allQuickActions = <_QuickAction>[
  const _QuickAction(
      'New Appointment',
      Icons.add_circle_outline,
      'new_appointment',
      Color(0xFF2E7D32),
      [UserRole.ADMIN, UserRole.APPROVER, UserRole.DEO, UserRole.HCM]),
  const _QuickAction('Walk-in Counter', Icons.login_outlined, 'walkin',
      Color(0xFF2E7D32), [UserRole.ADMIN, UserRole.APPROVER, UserRole.DEO]),
  const _QuickAction(
      'Guest Registration',
      Icons.person_add_alt_1_outlined,
      'guest_registration',
      Color(0xFF0F766E),
      [UserRole.ADMIN, UserRole.APPROVER, UserRole.DEO]),
  const _QuickAction('Apply for Scheme', Icons.workspace_premium_outlined,
      'scheme_form', Color(0xFFB45309), [UserRole.ADMIN, UserRole.APPROVER]),
  const _QuickAction(
      'Identify Person',
      Icons.badge_outlined,
      'identify',
      Color(0xFF0288D1),
      [UserRole.HCM, UserRole.ADMIN, UserRole.APPROVER, UserRole.DEO]),
  const _QuickAction('View Reports', Icons.bar_chart_outlined, 'reports',
      Color(0xFF6D28D9), _seniorStaff),
  const _QuickAction('Manage Users', Icons.manage_accounts_outlined, 'users',
      Color(0xFF0369A1), [UserRole.HCM, UserRole.ADMIN, UserRole.APPROVER]),
  const _QuickAction('Audit Trail', Icons.history, 'audit', Color(0xFF374151),
      [UserRole.ADMIN]),
];

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  List<Map<String, dynamic>> _scheduleItems = [];
  List<Map<String, dynamic>> _auditItems = [];
  final Map<String, int> _metricValues = {};

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    final role = context.read<AuthService>().user?.role;
    await _loadMetrics();
    if (!mounted) return;
    if (role == UserRole.HCM || role == UserRole.APPROVER) {
      return;
    }
    final schedFuture = ApiService.getScheduleEvents();
    final auditFuture = ApiService.getAuditLogs(size: 5);
    final results = await Future.wait([schedFuture, auditFuture]);
    if (!mounted) return;
    final schedList = results[0] as List<dynamic>;
    final auditPage = results[1] as Map<String, dynamic>;
    final auditList = (auditPage['content'] as List<dynamic>?) ?? [];
    setState(() {
      _scheduleItems = schedList.map((e) => e as Map<String, dynamic>).toList();
      _auditItems = auditList.map((e) => e as Map<String, dynamic>).toList();
    });
  }

  Future<void> _loadMetrics() async {
    final results = await Future.wait([
      ApiService.getAppointments(page: 0, size: 1000),
      ApiService.getSchemeApplications(page: 0, size: 1000),
      ApiService.getWalkInDashboardCounts(),
    ]);
    if (!mounted) return;
    final appointmentPage = results[0];
    final schemePage = results[1];
    final appointments = List<Map<String, dynamic>>.from(
      appointmentPage['content'] as List? ?? const [],
    );
    final schemes = List<Map<String, dynamic>>.from(
      schemePage['content'] as List? ?? const [],
    );
    final walkIns = results[2];
    setState(() {
      _metricValues.addAll(calculateDashboardMetrics(appointments, schemes));
      _metricValues['Walk-in Pending'] =
          (walkIns['walkInPending'] as num?)?.toInt() ?? 0;
      _metricValues['Walk-in Completed'] =
          (walkIns['walkInCompleted'] as num?)?.toInt() ?? 0;
    });
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final role = auth.user!.role;
    final kpis = _allKpis
        .where((k) => k.roles.contains(role))
        .map((k) => _Kpi(k.label, _metricValues[k.label] ?? 0, k.icon, k.color,
            k.bg, k.roles))
        .toList();
    final actions =
        _allQuickActions.where((a) => a.roles.contains(role)).toList();
    final showSchedule = _seniorStaff.contains(role);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildWelcomeBanner(auth.user!),
          const SizedBox(height: 20),
          if (kpis.isNotEmpty) ...[
            _sectionLabel('Key Metrics'),
            const SizedBox(height: 10),
            _buildKpiGrid(kpis),
            const SizedBox(height: 20),
          ],
          if (actions.isNotEmpty) ...[
            _sectionLabel('Quick Actions'),
            const SizedBox(height: 10),
            _buildQuickActions(context, actions),
            const SizedBox(height: 20),
          ],
          if (showSchedule) ...[
            _sectionLabel("Today's Schedule"),
            const SizedBox(height: 10),
            _buildSchedule(),
            const SizedBox(height: 20),
          ],
          _sectionLabel('Recent Activity'),
          const SizedBox(height: 10),
          _buildRecentActivity(),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Widget _buildWelcomeBanner(AuthUser user) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF1A237E), Color(0xFF1565C0)],
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(51),
              shape: BoxShape.circle,
            ),
            child: const Text('🏛️', style: TextStyle(fontSize: 24)),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Welcome, ${user.fullName}',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  user.role.displayName,
                  style: TextStyle(
                    color: Colors.white.withAlpha(204),
                    fontSize: 13,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  _formattedDate(),
                  style: TextStyle(
                    color: Colors.white.withAlpha(153),
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _formattedDate() {
    final now = DateTime.now();
    final days = [
      'Monday',
      'Tuesday',
      'Wednesday',
      'Thursday',
      'Friday',
      'Saturday',
      'Sunday'
    ];
    final months = [
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
      'Dec',
    ];
    final day = days[now.weekday - 1];
    return '$day, ${now.day} ${months[now.month - 1]} ${now.year}';
  }

  Widget _sectionLabel(String text) {
    return Text(
      text,
      style: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.bold,
        color: Color(0xFF1F2937),
      ),
    );
  }

  Widget _buildKpiGrid(List<_Kpi> kpis) {
    return LayoutBuilder(
      builder: (context, constraints) {
        const spacing = 12.0;
        final cardWidth = (constraints.maxWidth - spacing) / 2;
        return Wrap(
          spacing: spacing,
          runSpacing: spacing,
          children: [
            for (final kpi in kpis)
              SizedBox(width: cardWidth, child: _KpiCard(kpi: kpi)),
          ],
        );
      },
    );
  }

  Widget _buildQuickActions(BuildContext context, List<_QuickAction> actions) {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: actions.map((a) => _QuickActionChip(action: a)).toList(),
    );
  }

  String _fmtTime(String? iso) {
    if (iso == null) return '';
    final dt = DateTime.tryParse(iso);
    if (dt == null) return iso;
    return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  Widget _buildSchedule() {
    if (_scheduleItems.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: Center(
              child: Text('No schedule events',
                  style: TextStyle(color: Colors.grey))),
        ),
      );
    }
    return Card(
      child: Column(
        children: _scheduleItems.map((item) {
          final title = item['title'] as String? ?? '—';
          final type = item['eventType'] as String? ?? '';
          final location = item['location'] as String? ?? '';
          final time = _fmtTime(item['startTime'] as String?);
          const color = Color(0xFF1565C0);
          return ListTile(
            dense: true,
            leading: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: color.withAlpha(26),
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                type,
                style: const TextStyle(
                  color: color,
                  fontWeight: FontWeight.bold,
                  fontSize: 11,
                ),
              ),
            ),
            title: Text(title,
                style:
                    const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
            subtitle: Text('$time  ·  $location',
                style: TextStyle(fontSize: 12, color: Colors.grey[500])),
            trailing: Container(
              width: 6,
              height: 6,
              decoration:
                  const BoxDecoration(color: color, shape: BoxShape.circle),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildRecentActivity() {
    if (_auditItems.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: Center(
              child: Text('No recent activity',
                  style: TextStyle(color: Colors.grey))),
        ),
      );
    }
    return Card(
      child: Column(
        children: _auditItems.map((a) {
          final action = a['action'] as String? ?? '';
          final details = a['details'] as String? ?? action;
          final ts = a['timestamp'] as String? ?? '';
          String timeLabel = ts;
          final dt = DateTime.tryParse(ts);
          if (dt != null) {
            timeLabel =
                '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
          }
          return ListTile(
            dense: true,
            leading: const Icon(Icons.swap_horiz,
                color: Color(0xFF1A237E), size: 20),
            title: Text(details, style: const TextStyle(fontSize: 13)),
            trailing: Text(timeLabel,
                style: TextStyle(fontSize: 11, color: Colors.grey[500])),
          );
        }).toList(),
      ),
    );
  }
}

class _KpiCard extends StatelessWidget {
  final _Kpi kpi;
  const _KpiCard({required this.kpi});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: kpi.bg,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(kpi.icon, color: kpi.color, size: 20),
                ),
                Text(
                  '${kpi.value}',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: kpi.color,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              kpi.label,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey[600],
                fontWeight: FontWeight.w500,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

class _QuickActionChip extends StatelessWidget {
  final _QuickAction action;
  const _QuickActionChip({required this.action});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: action.color.withAlpha(20),
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: () {
          context.read<NavigationService>().navigateTo(action.route);
        },
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: action.color.withAlpha(51)),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(action.icon, color: action.color, size: 18),
              const SizedBox(width: 8),
              Text(
                action.label,
                style: TextStyle(
                  color: action.color,
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
