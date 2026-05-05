import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';

class _SchemeApp {
  final String id;
  final String applicantName;
  final String schemeType;
  final String projectName;
  final String projectCategory;
  final String status;
  final String constituency;
  final double estimatedCost;
  final double? hcmApprovedCost;

  const _SchemeApp({
    required this.id,
    required this.applicantName,
    required this.schemeType,
    required this.projectName,
    required this.projectCategory,
    required this.status,
    required this.constituency,
    required this.estimatedCost,
    this.hcmApprovedCost,
  });
}

const _schemeStats = [
  ('CMSDF', 45, 28, 12, 5),
  ('CMSG', 32, 18, 10, 4),
  ('CM Care', 28, 22, 4, 2),
  ('CM Connect', 19, 10, 7, 2),
  ('CM Elevate', 15, 8, 5, 2),
];

const _schemeTypeColors = {
  'CMSDF': Color(0xFF1565C0),
  'CMSG': Color(0xFF0288D1),
  'CM_CARE': Color(0xFF2E7D32),
  'CM_CONNECT': Color(0xFF4527A0),
  'CM_ELEVATE': Color(0xFFF57F17),
  'FOCUS_PLUS': Color(0xFFC62828),
};

String _schemeLabel(String type) {
  const m = {
    'CMSDF': 'CMSDF',
    'CMSG': 'CMSG',
    'CM_CARE': 'CM Care',
    'CM_CONNECT': 'CM Connect',
    'CM_ELEVATE': 'CM Elevate',
    'FOCUS_PLUS': 'Focus+',
  };
  return m[type] ?? type;
}

class SchemesScreen extends StatefulWidget {
  const SchemesScreen({super.key});

  @override
  State<SchemesScreen> createState() => _SchemesScreenState();
}

class _SchemesScreenState extends State<SchemesScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabCtrl;
  String _filterScheme = '';
  List<_SchemeApp> _schemes = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _loadSchemes();
  }

  Future<void> _loadSchemes() async {
    setState(() => _loading = true);
    final data = await ApiService.getSchemeApplications();
    if (!mounted) return;
    final content = (data['content'] as List<dynamic>?) ?? [];
    setState(() {
      _schemes = content.map((e) {
        final m = e as Map<String, dynamic>;
        final applicant = m['applicant'] as Map<String, dynamic>? ?? {};
        return _SchemeApp(
          id: m['id']?.toString() ?? '',
          applicantName: applicant['fullName'] as String? ?? '—',
          schemeType: m['schemeType'] as String? ?? '',
          projectName: m['projectName'] as String? ?? '',
          projectCategory: m['projectCategory'] as String? ?? '',
          status: m['status'] as String? ?? '',
          constituency: applicant['constituency'] as String? ?? '',
          estimatedCost: (m['estimatedCost'] as num?)?.toDouble() ?? 0,
          hcmApprovedCost: (m['hcmApprovedCost'] as num?)?.toDouble(),
        );
      }).toList();
      _loading = false;
    });
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  List<_SchemeApp> get _filtered => _schemes
      .where((s) => _filterScheme.isEmpty || s.schemeType == _filterScheme)
      .toList();

  Color _statusColor(String s) {
    if (s == 'APPROVED') return const Color(0xFF16A34A);
    if (s == 'HCM_PENDING' || s == 'SUBMITTED') return const Color(0xFFB45309);
    if (s == 'SCHEDULED') return const Color(0xFF1A237E);
    if (s == 'REJECTED') return const Color(0xFF991B1B);
    return const Color(0xFF4B5563);
  }

  String _statusLabel(String s) => s
      .replaceAll('_', ' ')
      .split(' ')
      .map((w) =>
          w.isEmpty ? w : w[0].toUpperCase() + w.substring(1).toLowerCase())
      .join(' ');

  @override
  Widget build(BuildContext context) {
    final role = context.watch<AuthService>().user!.role;
    return Column(
      children: [
        _buildFilterBar(),
        TabBar(
          controller: _tabCtrl,
          labelColor: const Color(0xFF1A237E),
          unselectedLabelColor: Colors.grey,
          indicatorColor: const Color(0xFF1A237E),
          tabs: const [
            Tab(text: 'Applications'),
            Tab(text: 'Statistics'),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabCtrl,
            children: [
              _buildApplicationsList(),
              _buildStatistics(),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildFilterBar() {
    final options = [
      ('', 'All'),
      ('CMSDF', 'CMSDF'),
      ('CMSG', 'CMSG'),
      ('CM_CARE', 'CM Care'),
      ('CM_CONNECT', 'CM Connect'),
      ('CM_ELEVATE', 'CM Elevate'),
      ('FOCUS_PLUS', 'Focus+'),
    ];
    return SizedBox(
      height: 44,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        itemCount: options.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (_, i) {
          final (val, label) = options[i];
          final selected = _filterScheme == val;
          return FilterChip(
            label: Text(label),
            selected: selected,
            onSelected: (_) => setState(() => _filterScheme = val),
            selectedColor: const Color(0xFF1A237E).withAlpha(26),
            checkmarkColor: const Color(0xFF1A237E),
            labelStyle: TextStyle(
              color: selected ? const Color(0xFF1A237E) : Colors.grey[700],
              fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
              fontSize: 12,
            ),
            side: BorderSide(
              color: selected
                  ? const Color(0xFF1A237E)
                  : Colors.grey.withAlpha(77),
            ),
          );
        },
      ),
    );
  }

  Widget _buildApplicationsList() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_filtered.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.workspace_premium_outlined,
                size: 56, color: Colors.grey[400]),
            const SizedBox(height: 12),
            Text('No applications found',
                style: TextStyle(color: Colors.grey[500], fontSize: 16)),
          ],
        ),
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.all(12),
      itemCount: _filtered.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (ctx, i) => _SchemeCard(
        app: _filtered[i],
        onTap: () => _showSchemeDetail(ctx, _filtered[i]),
      ),
    );
  }

  void _showSchemeDetail(BuildContext context, _SchemeApp app) {
    final color = _schemeTypeColors[app.schemeType] ?? Colors.grey;
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.6,
        expand: false,
        builder: (_, ctrl) => ListView(
          controller: ctrl,
          padding: const EdgeInsets.all(20),
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(
                  color: Colors.grey[300],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            Row(
              children: [
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: color.withAlpha(26),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: color.withAlpha(77)),
                  ),
                  child: Text(
                    _schemeLabel(app.schemeType),
                    style: TextStyle(
                        color: color,
                        fontWeight: FontWeight.bold,
                        fontSize: 12),
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: _statusColor(app.status).withAlpha(20),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    _statusLabel(app.status),
                    style: TextStyle(
                        color: _statusColor(app.status), fontSize: 11),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(app.projectName,
                style:
                    const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            _detailRow('Applicant', app.applicantName),
            _detailRow('Constituency', app.constituency),
            _detailRow('Category', app.projectCategory),
            _detailRow('Application ID', app.id),
            _detailRow(
                'Estimated Cost', '₹${app.estimatedCost.toStringAsFixed(0)}'),
            if (app.hcmApprovedCost != null)
              _detailRow('HCM Approved',
                  '₹${app.hcmApprovedCost!.toStringAsFixed(0)}'),
          ],
        ),
      ),
    );
  }

  Widget _detailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(label,
                style: TextStyle(
                    color: Colors.grey[600],
                    fontSize: 13,
                    fontWeight: FontWeight.w500)),
          ),
          Expanded(
            child: Text(value,
                style:
                    const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }

  Widget _buildStatistics() {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        const Text(
          'Scheme-wise Summary',
          style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: Color(0xFF1A237E)),
        ),
        const SizedBox(height: 10),
        ..._schemeStats.map((s) {
          final (name, total, approved, pending, rejected) = s;
          return _StatCard(
            schemeName: name,
            total: total,
            approved: approved,
            pending: pending,
            rejected: rejected,
          );
        }),
      ],
    );
  }
}

class _SchemeCard extends StatelessWidget {
  final _SchemeApp app;
  final VoidCallback onTap;
  const _SchemeCard({required this.app, required this.onTap});

  Color _statusColor(String s) {
    if (s == 'APPROVED') return const Color(0xFF16A34A);
    if (s == 'HCM_PENDING' || s == 'SUBMITTED') return const Color(0xFFB45309);
    if (s == 'SCHEDULED') return const Color(0xFF1A237E);
    if (s == 'REJECTED') return const Color(0xFF991B1B);
    return const Color(0xFF4B5563);
  }

  String _statusLabel(String s) => s
      .replaceAll('_', ' ')
      .split(' ')
      .map((w) =>
          w.isEmpty ? w : w[0].toUpperCase() + w.substring(1).toLowerCase())
      .join(' ');

  @override
  Widget build(BuildContext context) {
    final color = _schemeTypeColors[app.schemeType] ?? Colors.grey;
    final sc = _statusColor(app.status);
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: color.withAlpha(26),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: color.withAlpha(77)),
                    ),
                    child: Text(
                      _schemeLabel(app.schemeType),
                      style: TextStyle(
                          color: color,
                          fontWeight: FontWeight.bold,
                          fontSize: 11),
                    ),
                  ),
                  const Spacer(),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: sc.withAlpha(20),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      _statusLabel(app.status),
                      style: TextStyle(color: sc, fontSize: 10),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                app.projectName,
                style:
                    const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 4),
              Row(
                children: [
                  Icon(Icons.person_outline, size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text(app.applicantName,
                      style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                  const SizedBox(width: 12),
                  Icon(Icons.map_outlined, size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text(app.constituency,
                      style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                  const Spacer(),
                  Text(
                    '₹${app.estimatedCost.toStringAsFixed(0)}',
                    style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF1A237E)),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String schemeName;
  final int total;
  final int approved;
  final int pending;
  final int rejected;
  const _StatCard({
    required this.schemeName,
    required this.total,
    required this.approved,
    required this.pending,
    required this.rejected,
  });

  @override
  Widget build(BuildContext context) {
    final approvalRate =
        total > 0 ? (approved / total * 100).toStringAsFixed(0) : '0';
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  schemeName,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                      color: Color(0xFF1A237E)),
                ),
                Text(
                  '$total total',
                  style: TextStyle(color: Colors.grey[600], fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                _StatPill(
                    label: 'Approved',
                    count: approved,
                    color: const Color(0xFF16A34A)),
                const SizedBox(width: 8),
                _StatPill(
                    label: 'Pending',
                    count: pending,
                    color: const Color(0xFFB45309)),
                const SizedBox(width: 8),
                _StatPill(
                    label: 'Rejected',
                    count: rejected,
                    color: const Color(0xFF991B1B)),
                const Spacer(),
                Text(
                  '$approvalRate% approval',
                  style: const TextStyle(
                      fontSize: 12,
                      color: Color(0xFF16A34A),
                      fontWeight: FontWeight.w600),
                ),
              ],
            ),
            const SizedBox(height: 8),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: total > 0 ? approved / total : 0,
                backgroundColor: Colors.grey[200],
                valueColor:
                    const AlwaysStoppedAnimation<Color>(Color(0xFF16A34A)),
                minHeight: 6,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatPill extends StatelessWidget {
  final String label;
  final int count;
  final Color color;
  const _StatPill(
      {required this.label, required this.count, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withAlpha(20),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        '$count $label',
        style:
            TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w600),
      ),
    );
  }
}
