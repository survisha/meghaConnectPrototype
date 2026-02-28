import 'package:flutter/material.dart';

const _topConstituencies = [
  ('Ampati', 12, 8, 2),
  ('Shillong East', 9, 6, 1),
  ('Baghmara', 8, 5, 2),
  ('Umsning', 7, 4, 1),
  ('Tura', 11, 7, 2),
];

const _schemeWise = [
  ('CMSDF', 45, 28, 12, 5, '₹45.2L'),
  ('CMSG', 32, 18, 10, 4, '₹12.8L'),
  ('CM Care', 28, 22, 4, 2, '₹28.0L'),
  ('CM Connect', 19, 10, 7, 2, '₹5.6L'),
  ('CM Elevate', 15, 8, 5, 2, '₹9.2L'),
];

class ReportsScreen extends StatefulWidget {
  const ReportsScreen({super.key});

  @override
  State<ReportsScreen> createState() => _ReportsScreenState();
}

class _ReportsScreenState extends State<ReportsScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabCtrl;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildKpiBar(),
        TabBar(
          controller: _tabCtrl,
          labelColor: const Color(0xFF1A237E),
          unselectedLabelColor: Colors.grey,
          indicatorColor: const Color(0xFF1A237E),
          tabs: const [
            Tab(text: 'Overview'),
            Tab(text: 'Scheme-wise'),
            Tab(text: 'Constituency'),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabCtrl,
            children: [
              _buildOverview(),
              _buildSchemeWise(),
              _buildConstituency(),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildKpiBar() {
    return Container(
      color: const Color(0xFF1A237E),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _MiniKpi(label: 'This Month', value: '28', icon: Icons.calendar_today),
          _MiniKpi(label: 'Approved', value: '62%', icon: Icons.check_circle_outline),
          _MiniKpi(label: 'Schemes', value: '139', icon: Icons.workspace_premium_outlined),
          _MiniKpi(label: 'Pending', value: '17', icon: Icons.pending_actions),
        ],
      ),
    );
  }

  Widget _buildOverview() {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        _sectionLabel('Meetings Per Day (This Week)'),
        const SizedBox(height: 8),
        _buildBarChart(
          labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
          values: [4, 6, 3, 8, 5, 2],
          maxVal: 10,
          barColor: const Color(0xFF1A237E),
        ),
        const SizedBox(height: 20),
        _sectionLabel('Approval vs Rejection Ratio'),
        const SizedBox(height: 8),
        _buildDonutPlaceholder(),
        const SizedBox(height: 20),
        _sectionLabel('Appointment Status Breakdown'),
        const SizedBox(height: 8),
        _buildStatusBreakdown(),
      ],
    );
  }

  Widget _buildSchemeWise() {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        _sectionLabel('Scheme-wise Summary'),
        const SizedBox(height: 8),
        ..._schemeWise.map((s) {
          final (name, total, approved, pending, rejected, budget) = s;
          return _SchemeStatRow(
            name: name,
            total: total,
            approved: approved,
            pending: pending,
            rejected: rejected,
            budget: budget,
          );
        }),
      ],
    );
  }

  Widget _buildConstituency() {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        _sectionLabel('Top Constituencies by Application Volume'),
        const SizedBox(height: 8),
        Card(
          child: Column(
            children: [
              // Header
              Container(
                padding: const EdgeInsets.symmetric(
                    horizontal: 14, vertical: 8),
                decoration: BoxDecoration(
                  color: const Color(0xFFE8EAF6),
                  borderRadius: const BorderRadius.vertical(
                      top: Radius.circular(12)),
                ),
                child: Row(
                  children: [
                    const Expanded(
                        flex: 3,
                        child: Text('Constituency',
                            style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF1A237E)))),
                    const Expanded(
                        child: Text('Total',
                            textAlign: TextAlign.center,
                            style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold))),
                    const Expanded(
                        child: Text('Approved',
                            textAlign: TextAlign.center,
                            style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold))),
                    const Expanded(
                        child: Text('Rejected',
                            textAlign: TextAlign.center,
                            style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold))),
                  ],
                ),
              ),
              ..._topConstituencies.asMap().entries.map((e) {
                final (name, total, approved, rejected) = e.value;
                return Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: e.key.isEven ? Colors.white : Colors.grey[50],
                  ),
                  child: Row(
                    children: [
                      Expanded(
                          flex: 3,
                          child: Text(name,
                              style: const TextStyle(
                                  fontSize: 13, fontWeight: FontWeight.w500))),
                      Expanded(
                          child: Text('$total',
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF1A237E)))),
                      Expanded(
                          child: Text('$approved',
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF16A34A)))),
                      Expanded(
                          child: Text('$rejected',
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF991B1B)))),
                    ],
                  ),
                );
              }),
            ],
          ),
        ),
        const SizedBox(height: 20),
        _sectionLabel('Heatmap (District-level)'),
        const SizedBox(height: 8),
        _buildHeatmapPlaceholder(),
      ],
    );
  }

  Widget _sectionLabel(String text) {
    return Text(
      text,
      style: const TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.bold,
          color: Color(0xFF374151)),
    );
  }

  Widget _buildBarChart({
    required List<String> labels,
    required List<int> values,
    required int maxVal,
    required Color barColor,
  }) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          children: [
            SizedBox(
              height: 120,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: List.generate(labels.length, (i) {
                  final h = values[i] / maxVal;
                  return Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text('${values[i]}',
                          style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: barColor)),
                      const SizedBox(height: 2),
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 500),
                        width: 28,
                        height: h * 80,
                        decoration: BoxDecoration(
                          color: barColor,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(labels[i],
                          style: TextStyle(
                              fontSize: 11, color: Colors.grey[600])),
                    ],
                  );
                }),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDonutPlaceholder() {
    const data = [
      ('HCM Accepted', 62, Color(0xFF16A34A)),
      ('HCM Rejected', 18, Color(0xFFDC2626)),
      ('Snoozed', 10, Color(0xFFF59E0B)),
      ('Pending', 7, Color(0xFF6B7280)),
      ('CMO Rejected', 3, Color(0xFFB45309)),
    ];
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          children: data.map((item) {
            final (label, pct, color) = item;
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                children: [
                  Container(
                    width: 12,
                    height: 12,
                    decoration: BoxDecoration(
                        color: color, shape: BoxShape.circle),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(label,
                        style: const TextStyle(fontSize: 13)),
                  ),
                  SizedBox(
                    width: 140,
                    child: Row(
                      children: [
                        Expanded(
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(4),
                            child: LinearProgressIndicator(
                              value: pct / 100,
                              backgroundColor: Colors.grey[200],
                              valueColor:
                                  AlwaysStoppedAnimation<Color>(color),
                              minHeight: 8,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Text('$pct%',
                            style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: color)),
                      ],
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  Widget _buildStatusBreakdown() {
    const rows = [
      ('Scheduled', 28, Color(0xFF1A237E)),
      ('Completed', 22, Color(0xFF16A34A)),
      ('Pending CMO', 14, Color(0xFFB45309)),
      ('Pending HCM', 7, Color(0xFFDC2626)),
      ('Walk-ins', 9, Color(0xFF006064)),
    ];
    return Card(
      child: Column(
        children: rows.map((r) {
          final (label, count, color) = r;
          return ListTile(
            dense: true,
            leading: Container(
              width: 12,
              height: 12,
              decoration:
                  BoxDecoration(color: color, shape: BoxShape.circle),
            ),
            title: Text(label,
                style: const TextStyle(fontSize: 13)),
            trailing: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: color.withAlpha(26),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                '$count',
                style: TextStyle(
                    color: color,
                    fontWeight: FontWeight.bold,
                    fontSize: 13),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildHeatmapPlaceholder() {
    const districts = [
      ('West Garo Hills', 61, Color(0xFFDC2626)),
      ('East Khasi Hills', 82, Color(0xFFDC2626)),
      ('East Garo Hills', 47, Color(0xFFF59E0B)),
      ('West Khasi Hills', 34, Color(0xFFF59E0B)),
      ('Ri Bhoi', 28, Color(0xFFF59E0B)),
      ('South Garo Hills', 23, Color(0xFF16A34A)),
      ('East Jaintia Hills', 19, Color(0xFF16A34A)),
      ('North Garo Hills', 17, Color(0xFF16A34A)),
    ];
    return Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 12, 14, 4),
            child: Row(
              children: [
                _HeatLegend(label: 'Hot', color: const Color(0xFFDC2626)),
                const SizedBox(width: 16),
                _HeatLegend(label: 'Warm', color: const Color(0xFFF59E0B)),
                const SizedBox(width: 16),
                _HeatLegend(label: 'Cool', color: const Color(0xFF16A34A)),
              ],
            ),
          ),
          ...districts.map((d) {
            final (name, apps, color) = d;
            return Padding(
              padding: const EdgeInsets.symmetric(
                  horizontal: 14, vertical: 5),
              child: Row(
                children: [
                  SizedBox(
                    width: 130,
                    child: Text(name,
                        style: const TextStyle(
                            fontSize: 12, fontWeight: FontWeight.w500)),
                  ),
                  Expanded(
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value: apps / 100,
                        backgroundColor: Colors.grey[200],
                        valueColor:
                            AlwaysStoppedAnimation<Color>(color),
                        minHeight: 14,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text('$apps',
                      style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: color)),
                ],
              ),
            );
          }),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}

class _MiniKpi extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  const _MiniKpi(
      {required this.label, required this.value, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Icon(icon, color: Colors.white70, size: 18),
        const SizedBox(height: 4),
        Text(value,
            style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 16)),
        Text(label,
            style: const TextStyle(color: Colors.white60, fontSize: 10)),
      ],
    );
  }
}

class _SchemeStatRow extends StatelessWidget {
  final String name;
  final int total;
  final int approved;
  final int pending;
  final int rejected;
  final String budget;
  const _SchemeStatRow({
    required this.name,
    required this.total,
    required this.approved,
    required this.pending,
    required this.rejected,
    required this.budget,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(name,
                    style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                        color: Color(0xFF1A237E))),
                Text(budget,
                    style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF16A34A))),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Text('Total: $total',
                    style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                const SizedBox(width: 12),
                Text('✓ $approved',
                    style: const TextStyle(
                        fontSize: 12, color: Color(0xFF16A34A))),
                const SizedBox(width: 8),
                Text('⏳ $pending',
                    style: const TextStyle(
                        fontSize: 12, color: Color(0xFFB45309))),
                const SizedBox(width: 8),
                Text('✗ $rejected',
                    style: const TextStyle(
                        fontSize: 12, color: Color(0xFF991B1B))),
              ],
            ),
            const SizedBox(height: 6),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: total > 0 ? approved / total : 0,
                backgroundColor: Colors.grey[200],
                valueColor: const AlwaysStoppedAnimation<Color>(
                    Color(0xFF16A34A)),
                minHeight: 6,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HeatLegend extends StatelessWidget {
  final String label;
  final Color color;
  const _HeatLegend({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2)),
        ),
        const SizedBox(width: 4),
        Text(label, style: const TextStyle(fontSize: 12)),
      ],
    );
  }
}
