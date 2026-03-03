import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../services/navigation_service.dart';

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
  const _MyScheme(this.id, this.scheme, this.project, this.status, this.amount);
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
    case 'SCHEDULED': return _amber;
    case 'COMPLETED': return _green;
    case 'HCM_PENDING': return _red;
    case 'CMO_REVIEW': return _amber;
    default: return _primaryBlue;
  }
}

Color _schemeColor(String status) {
  switch (status) {
    case 'APPROVED': return _green;
    case 'UNDER_REVIEW': return _amber;
    case 'REJECTED': return _red;
    default: return _primaryBlue;
  }
}

Color _grievanceColor(String status) {
  switch (status) {
    case 'RESOLVED': return _green;
    case 'FORWARDED': return _amber;
    case 'UNDER_REVIEW': return _amber;
    default: return _primaryBlue;
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
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return '${dt.day} ${months[dt.month - 1]} ${dt.year}';
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    final results = await Future.wait([
      ApiService.getAppointments(size: 5),
      ApiService.getSchemeApplications(size: 5),
      ApiService.getGrievances(size: 5),
    ]);
    if (!mounted) return;
    final apptPage = results[0] as Map<String, dynamic>;
    final schemePage = results[1] as Map<String, dynamic>;
    final grievancePage = results[2] as Map<String, dynamic>;
    setState(() {
      _appointments = ((apptPage['content'] as List<dynamic>?) ?? []).map((e) {
        final m = e as Map<String, dynamic>;
        final applicant = m['applicant'] as Map<String, dynamic>? ?? {};
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
        );
      }).toList();
      _grievances = ((grievancePage['content'] as List<dynamic>?) ?? []).map((e) {
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

  Future<void> _onRefresh() async {
    await _loadData();
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final nav = context.read<NavigationService>();
    final name = auth.user?.fullName ?? 'Visitor';

    final cards = [
      _SummaryCard('My Appointments', _appointments.length, Icons.calendar_today_outlined, _primaryBlue, const Color(0xFFE8EAF6)),
      _SummaryCard('Scheme Applications', _schemes.length, Icons.workspace_premium_outlined, _green, const Color(0xFFD1FAE5)),
      _SummaryCard('Grievances Raised', _grievances.length, Icons.comment_outlined, _amber, const Color(0xFFFEF3C7)),
      _SummaryCard('Pending Actions', 1, Icons.warning_amber_outlined, _red, const Color(0xFFFEE2E2)),
    ];

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text('My Portal'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Logout',
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
                boxShadow: [BoxShadow(color: _primaryBlue.withOpacity(0.25), blurRadius: 12, offset: const Offset(0, 4))],
              ),
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  Container(
                    width: 52, height: 52,
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(26),
                    ),
                    child: const Icon(Icons.person, color: Colors.white, size: 28),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Welcome, $name!', style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 4),
                        const Text(
                          'Meghalaya Entry & Governance System',
                          style: TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

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
            const Text('Quick Actions', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: _primaryBlue)),
            const SizedBox(height: 10),
            Row(
              children: [
                _QuickActionBtn(
                  icon: Icons.add_circle_outline,
                  label: 'Book\nAppointment',
                  color: _primaryBlue,
                  onTap: () => nav.navigateTo('new_appointment'),
                ),
                const SizedBox(width: 8),
                _QuickActionBtn(
                  icon: Icons.workspace_premium_outlined,
                  label: 'Apply for\nScheme',
                  color: _green,
                  onTap: () => nav.navigateTo('schemes'),
                ),
                const SizedBox(width: 8),
                _QuickActionBtn(
                  icon: Icons.comment_outlined,
                  label: 'Raise\nGrievance',
                  color: _amber,
                  onTap: () => nav.navigateTo('grievances'),
                ),
              ],
            ),
            const SizedBox(height: 20),

            // My Appointments
            _SectionCard(
              title: 'My Appointments',
              icon: Icons.calendar_today_outlined,
              action: TextButton(
                onPressed: () => nav.navigateTo('new_appointment'),
                child: const Text('+ Book New', style: TextStyle(fontSize: 12)),
              ),
              child: _appointments.isEmpty
                  ? _empty('No appointments yet.')
                  : Column(
                      children: _appointments.map((a) => _ItemRow(
                        id: a.id,
                        title: a.agenda,
                        subtitle: a.date,
                        statusLabel: a.status.replaceAll('_', ' '),
                        statusColor: _appointmentColor(a.status),
                      )).toList(),
                    ),
            ),
            const SizedBox(height: 14),

            // My Scheme Applications
            _SectionCard(
              title: 'Scheme Applications',
              icon: Icons.workspace_premium_outlined,
              action: TextButton(
                onPressed: () => nav.navigateTo('schemes'),
                child: const Text('+ Apply', style: TextStyle(fontSize: 12)),
              ),
              child: _schemes.isEmpty
                  ? _empty('No applications yet.')
                  : Column(
                      children: _schemes.map((s) => _ItemRow(
                        id: s.id,
                        title: s.project,
                        subtitle: '${s.scheme} · ${s.amount}',
                        statusLabel: s.status.replaceAll('_', ' '),
                        statusColor: _schemeColor(s.status),
                      )).toList(),
                    ),
            ),
            const SizedBox(height: 14),

            // My Grievances
            _SectionCard(
              title: 'My Grievances',
              icon: Icons.comment_outlined,
              action: TextButton(
                onPressed: () => nav.navigateTo('grievances'),
                child: const Text('+ Raise', style: TextStyle(fontSize: 12)),
              ),
              child: _grievances.isEmpty
                  ? _empty('No grievances raised yet.')
                  : Column(
                      children: _grievances.map((g) => _ItemRow(
                        id: g.id,
                        title: g.subject,
                        subtitle: g.date,
                        statusLabel: g.status.replaceAll('_', ' '),
                        statusColor: _grievanceColor(g.status),
                      )).toList(),
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
                    style: TextStyle(fontSize: 12, color: Colors.grey[600]),
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
        child: Center(child: Text(text, style: const TextStyle(color: Colors.grey, fontSize: 13))),
      );
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
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.06), blurRadius: 8)],
      ),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            width: 42, height: 42,
            decoration: BoxDecoration(color: card.color, borderRadius: BorderRadius.circular(10)),
            child: Icon(card.icon, color: Colors.white, size: 22),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(card.value.toString(), style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: card.color)),
                Text(card.label, style: const TextStyle(fontSize: 11, color: Colors.grey), maxLines: 2, overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickActionBtn extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;
  const _QuickActionBtn({required this.icon, required this.label, required this.color, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: GestureDetector(
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
              Text(label, textAlign: TextAlign.center, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: color)),
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
  const _SectionCard({required this.title, required this.icon, required this.child, this.action});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.06), blurRadius: 8)],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: const BoxDecoration(
              gradient: LinearGradient(colors: [Color(0xFF1A237E), Color(0xFF3949AB)]),
              borderRadius: BorderRadius.vertical(top: Radius.circular(12)),
            ),
            child: Row(
              children: [
                Icon(icon, color: Colors.white, size: 16),
                const SizedBox(width: 8),
                Expanded(child: Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13))),
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
  const _ItemRow({required this.id, required this.title, required this.subtitle, required this.statusLabel, required this.statusColor});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: Color(0xFFF3F4F6)))),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(id, style: const TextStyle(fontFamily: 'monospace', fontSize: 11, color: Colors.grey)),
                Text(title, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13), maxLines: 2, overflow: TextOverflow.ellipsis),
                Text(subtitle, style: TextStyle(fontSize: 11, color: Colors.grey[500])),
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
            child: Text(statusLabel, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: statusColor)),
          ),
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
  const _TimelineItem({required this.label, required this.date, required this.color, required this.isLast});

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
                  width: 16, height: 16,
                  decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(8)),
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
                  Text(label, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                  Text(date, style: TextStyle(fontSize: 11, color: Colors.grey[500])),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
