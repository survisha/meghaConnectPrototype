import 'dart:math' as math;

import 'package:flutter/material.dart';
import '../services/api_service.dart';

class ReportsScreen extends StatefulWidget {
  const ReportsScreen({super.key});

  @override
  State<ReportsScreen> createState() => _ReportsScreenState();
}

class _ReportsScreenState extends State<ReportsScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabCtrl;
  bool _loading = false;
  String? _error;
  List<_MeetingDay> _meetingSeries = const [];
  List<_ChartSlice> _approvalRatio = const [];
  List<_SchemeStatus> _schemeWise = const [];
  List<_ConstituencyStat> _topConstituencies = const [];

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
    _refresh();
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _refresh() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final analytics = await ApiService.getAppointmentAnalytics();
      if (!mounted) return;
      final statuses = List<Map<String, dynamic>>.from(
        analytics['statusCounts'] as List? ?? const [],
      );
      final total =
          statuses.fold<int>(0, (sum, row) => sum + _int(row['total']));
      setState(() {
        _meetingSeries = List<Map<String, dynamic>>.from(
          analytics['meetingDates'] as List? ?? const [],
        )
            .map((row) => _MeetingDay(
                  (row['date'] ?? '').toString(),
                  _int(row['scheduled']),
                  _int(row['completed']),
                ))
            .toList();
        _approvalRatio = statuses.asMap().entries.map((entry) {
          final count = _int(entry.value['total']);
          return _ChartSlice(
            (entry.value['status'] ?? 'Unknown').toString(),
            total == 0 ? 0 : ((count * 100) / total).round(),
            _chartColors[entry.key % _chartColors.length],
          );
        }).toList();
        final schemes = <String, _SchemeStatus>{};
        for (final row in List<Map<String, dynamic>>.from(
          analytics['schemeDistricts'] as List? ?? const [],
        )) {
          final name = (row['scheme'] ?? 'Unknown').toString();
          final current = schemes[name] ?? _SchemeStatus(name, 0, 0, 0);
          final approved = _int(row['approved']);
          final rejected = _int(row['rejected']);
          schemes[name] = _SchemeStatus(
            name,
            current.approved + approved,
            current.pending + _int(row['total']) - approved - rejected,
            current.rejected + rejected,
          );
        }
        _schemeWise = schemes.values.toList();
        _topConstituencies = List<Map<String, dynamic>>.from(
          analytics['topConstituencies'] as List? ?? const [],
        )
            .map((row) => _ConstituencyStat(
                  (row['constituency'] ?? 'Unknown').toString(),
                  _int(row['total']),
                  _int(row['approved']),
                  _int(row['rejected']),
                ))
            .toList();
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Failed to load reports.';
      });
    }
  }

  int _int(dynamic value) =>
      value is num ? value.toInt() : int.tryParse('$value') ?? 0;

  static const _chartColors = [
    Color(0xFF16A34A),
    Color(0xFFDC2626),
    Color(0xFFF59E0B),
    Color(0xFF6B7280),
    Color(0xFF1A237E),
  ];

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildHeader(context),
        if (_error != null) _errorBanner(_error!),
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
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _refresh,
                  child: TabBarView(
                    controller: _tabCtrl,
                    children: [
                      _buildOverview(),
                      _buildSchemeWise(),
                      _buildConstituency(),
                    ],
                  ),
                ),
        ),
      ],
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Container(
      color: const Color(0xFF1A237E),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Analytics & Reports',
            style: TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _MiniKpi(
                label: 'This Week',
                value:
                    '${_meetingSeries.fold<int>(0, (s, d) => s + d.scheduled)}',
                icon: Icons.calendar_today,
              ),
              _MiniKpi(
                label: 'Completed',
                value:
                    '${_meetingSeries.fold<int>(0, (s, d) => s + d.completed)}',
                icon: Icons.check_circle_outline,
              ),
              _MiniKpi(
                label: 'Accepted',
                value:
                    '${_approvalRatio.isEmpty ? 0 : _approvalRatio.first.value}%',
                icon: Icons.done_all_outlined,
              ),
              _MiniKpi(
                label: 'Schemes',
                value: '${_schemeWise.length}',
                icon: Icons.workspace_premium_outlined,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildOverview() {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        _ReportCard(
          title: 'Meetings Per Day (This Week)',
          icon: Icons.bar_chart,
          child: _GroupedMeetingChart(data: _meetingSeries),
        ),
        const SizedBox(height: 12),
        _ReportCard(
          title: 'Approval vs Rejection Ratio',
          icon: Icons.pie_chart_outline,
          child: _PieChartCard(slices: _approvalRatio),
        ),
        const SizedBox(height: 12),
        _ReportCard(
          title: 'Appointment Status Breakdown',
          icon: Icons.fact_check_outlined,
          child: _StatusList(slices: _approvalRatio),
        ),
      ],
    );
  }

  Widget _buildSchemeWise() {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        _ReportCard(
          title: 'Scheme-wise Application Status',
          icon: Icons.stacked_bar_chart_outlined,
          child: _SchemeBarChart(data: _schemeWise),
        ),
        const SizedBox(height: 12),
        for (final row in _schemeWise) _SchemeStatRow(row: row),
      ],
    );
  }

  Widget _buildConstituency() {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        _ReportCard(
          title: 'Top Constituencies by Applications',
          icon: Icons.location_on_outlined,
          child: Column(
            children: [
              for (final item in _topConstituencies) _ConstituencyRow(item),
            ],
          ),
        ),
      ],
    );
  }

  Widget _errorBanner(String message) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFEE2E2),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(message, style: const TextStyle(color: Color(0xFF991B1B))),
    );
  }
}

class _MeetingDay {
  final String label;
  final int scheduled;
  final int completed;
  const _MeetingDay(this.label, this.scheduled, this.completed);
}

class _ChartSlice {
  final String label;
  final int value;
  final Color color;
  const _ChartSlice(this.label, this.value, this.color);
}

class _SchemeStatus {
  final String label;
  final int approved;
  final int pending;
  final int rejected;
  const _SchemeStatus(this.label, this.approved, this.pending, this.rejected);

  int get total => approved + pending + rejected;
}

class _ConstituencyStat {
  final String name;
  final int total;
  final int approved;
  final int rejected;
  const _ConstituencyStat(this.name, this.total, this.approved, this.rejected);
}

class _ReportCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final Widget child;

  const _ReportCard({
    required this.title,
    required this.icon,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: const Color(0xFF1A237E), size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.w900,
                      fontSize: 15,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            child,
          ],
        ),
      ),
    );
  }
}

class _GroupedMeetingChart extends StatelessWidget {
  final List<_MeetingDay> data;
  const _GroupedMeetingChart({required this.data});

  @override
  Widget build(BuildContext context) {
    final maxVal = data.fold<int>(
      1,
      (max, row) => math.max(max, math.max(row.scheduled, row.completed)),
    );
    return Column(
      children: [
        SizedBox(
          height: 170,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              for (final row in data)
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 3),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        SizedBox(
                          height: 120,
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              _ChartBar(
                                value: row.scheduled,
                                max: maxVal,
                                color: const Color(0xFF1A237E),
                              ),
                              const SizedBox(width: 4),
                              _ChartBar(
                                value: row.completed,
                                max: maxVal,
                                color: const Color(0xFF16A34A),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 6),
                        Text(row.label,
                            style: const TextStyle(
                                fontSize: 11, color: Color(0xFF64748B))),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        const Wrap(
          spacing: 14,
          runSpacing: 6,
          children: [
            _Legend(label: 'Scheduled', color: Color(0xFF1A237E)),
            _Legend(label: 'Completed', color: Color(0xFF16A34A)),
          ],
        ),
      ],
    );
  }
}

class _ChartBar extends StatelessWidget {
  final int value;
  final int max;
  final Color color;
  const _ChartBar(
      {required this.value, required this.max, required this.color});

  @override
  Widget build(BuildContext context) {
    final height = 12 + (value / max) * 80;
    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text('$value',
            style: TextStyle(
                fontSize: 10, color: color, fontWeight: FontWeight.w800)),
        const SizedBox(height: 2),
        Container(
          width: 12,
          height: height,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(4),
          ),
        ),
      ],
    );
  }
}

class _PieChartCard extends StatelessWidget {
  final List<_ChartSlice> slices;
  const _PieChartCard({required this.slices});

  @override
  Widget build(BuildContext context) {
    final total = slices.fold<int>(0, (sum, item) => sum + item.value);
    if (total == 0) {
      return const Center(child: Text('No report data found'));
    }
    return Column(
      children: [
        SizedBox(
          width: 190,
          height: 190,
          child: CustomPaint(
            painter: _PiePainter(slices: slices),
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('$total',
                      style: const TextStyle(
                          fontSize: 24, fontWeight: FontWeight.w900)),
                  const Text('Total',
                      style: TextStyle(fontSize: 12, color: Color(0xFF64748B))),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 12,
          runSpacing: 8,
          children: [
            for (final item in slices)
              _Legend(
                label: '${item.label} (${item.value}%)',
                color: item.color,
              ),
          ],
        ),
      ],
    );
  }
}

class _PiePainter extends CustomPainter {
  final List<_ChartSlice> slices;
  const _PiePainter({required this.slices});

  @override
  void paint(Canvas canvas, Size size) {
    final total = slices.fold<int>(0, (sum, item) => sum + item.value);
    if (total == 0) return;
    final rect = Offset.zero & size;
    final stroke = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 28
      ..strokeCap = StrokeCap.butt;
    var start = -math.pi / 2;
    for (final item in slices) {
      final sweep = (item.value / total) * math.pi * 2;
      stroke.color = item.color;
      canvas.drawArc(rect.deflate(20), start, sweep, false, stroke);
      start += sweep;
    }
  }

  @override
  bool shouldRepaint(covariant _PiePainter oldDelegate) =>
      oldDelegate.slices != slices;
}

class _SchemeBarChart extends StatelessWidget {
  final List<_SchemeStatus> data;
  const _SchemeBarChart({required this.data});

  @override
  Widget build(BuildContext context) {
    final maxTotal = data.fold<int>(1, (max, row) => math.max(max, row.total));
    return Column(
      children: [
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: SizedBox(
            width: math.max(MediaQuery.of(context).size.width - 56, 520),
            height: 220,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                for (final row in data)
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 5),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          SizedBox(
                            height: 155,
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                _SmallBar(
                                    value: row.approved,
                                    max: maxTotal,
                                    color: const Color(0xFF16A34A)),
                                _SmallBar(
                                    value: row.pending,
                                    max: maxTotal,
                                    color: const Color(0xFFF59E0B)),
                                _SmallBar(
                                    value: row.rejected,
                                    max: maxTotal,
                                    color: const Color(0xFFDC2626)),
                              ],
                            ),
                          ),
                          const SizedBox(height: 6),
                          SizedBox(
                            height: 34,
                            child: Text(
                              row.label,
                              textAlign: TextAlign.center,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                  fontSize: 11, color: Color(0xFF64748B)),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
        const Wrap(
          spacing: 12,
          runSpacing: 6,
          children: [
            _Legend(label: 'Approved', color: Color(0xFF16A34A)),
            _Legend(label: 'Pending', color: Color(0xFFF59E0B)),
            _Legend(label: 'Rejected', color: Color(0xFFDC2626)),
          ],
        ),
      ],
    );
  }
}

class _SmallBar extends StatelessWidget {
  final int value;
  final int max;
  final Color color;
  const _SmallBar(
      {required this.value, required this.max, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 10,
      height: 12 + (value / max) * 120,
      margin: const EdgeInsets.symmetric(horizontal: 1.5),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(3),
      ),
    );
  }
}

class _StatusList extends StatelessWidget {
  final List<_ChartSlice> slices;
  const _StatusList({required this.slices});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (final item in slices)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: [
                _Legend(label: item.label, color: item.color),
                const SizedBox(width: 10),
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(999),
                    child: LinearProgressIndicator(
                      value: item.value / 100,
                      minHeight: 8,
                      backgroundColor: const Color(0xFFE5E7EB),
                      valueColor: AlwaysStoppedAnimation<Color>(item.color),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                SizedBox(
                  width: 34,
                  child: Text(
                    '${item.value}%',
                    textAlign: TextAlign.right,
                    style: TextStyle(
                        color: item.color,
                        fontSize: 12,
                        fontWeight: FontWeight.w800),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _SchemeStatRow extends StatelessWidget {
  final _SchemeStatus row;
  const _SchemeStatRow({required this.row});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    row.label,
                    style: const TextStyle(
                        fontWeight: FontWeight.w900,
                        fontSize: 14,
                        color: Color(0xFF1A237E)),
                  ),
                ),
                Text('${row.total}',
                    style: const TextStyle(
                        fontWeight: FontWeight.w900, fontSize: 14)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                _CountPill('Approved', row.approved, const Color(0xFF16A34A)),
                const SizedBox(width: 6),
                _CountPill('Pending', row.pending, const Color(0xFFF59E0B)),
                const SizedBox(width: 6),
                _CountPill('Rejected', row.rejected, const Color(0xFFDC2626)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ConstituencyRow extends StatelessWidget {
  final _ConstituencyStat item;
  const _ConstituencyRow(this.item);

  @override
  Widget build(BuildContext context) {
    final rate = item.total == 0 ? 0.0 : item.approved / item.total;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(item.name,
                    style: const TextStyle(fontWeight: FontWeight.w800)),
              ),
              Text('${(rate * 100).round()}%',
                  style: const TextStyle(
                      color: Color(0xFF16A34A), fontWeight: FontWeight.w900)),
            ],
          ),
          const SizedBox(height: 5),
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              value: rate,
              minHeight: 9,
              backgroundColor: const Color(0xFFE5E7EB),
              valueColor:
                  const AlwaysStoppedAnimation<Color>(Color(0xFF16A34A)),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Total ${item.total} / Approved ${item.approved} / Rejected ${item.rejected}',
            style: const TextStyle(color: Color(0xFF64748B), fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _MiniKpi extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;

  const _MiniKpi({
    required this.label,
    required this.value,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Icon(icon, color: Colors.white70, size: 18),
          const SizedBox(height: 4),
          Text(value,
              style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w900,
                  fontSize: 16)),
          Text(
            label,
            textAlign: TextAlign.center,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(color: Colors.white60, fontSize: 10),
          ),
        ],
      ),
    );
  }
}

class _Legend extends StatelessWidget {
  final String label;
  final Color color;

  const _Legend({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 11,
          height: 11,
          decoration: BoxDecoration(
              color: color, borderRadius: BorderRadius.circular(3)),
        ),
        const SizedBox(width: 5),
        Text(label, style: const TextStyle(fontSize: 12)),
      ],
    );
  }
}

class _CountPill extends StatelessWidget {
  final String label;
  final int value;
  final Color color;

  const _CountPill(this.label, this.value, this.color);

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 7),
        decoration: BoxDecoration(
          color: color.withAlpha(24),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          children: [
            Text('$value',
                style: TextStyle(color: color, fontWeight: FontWeight.w900)),
            Text(label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
