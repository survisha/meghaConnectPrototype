import 'package:flutter/material.dart';

import '../services/api_service.dart';

class HeatmapScreen extends StatefulWidget {
  const HeatmapScreen({super.key});

  @override
  State<HeatmapScreen> createState() => _HeatmapScreenState();
}

class _HeatmapScreenState extends State<HeatmapScreen> {
  String _selectedScheme = 'ALL';
  List<Map<String, String>> _schemeOptions = [
    {'code': 'ALL', 'value': 'All Schemes'},
  ];
  bool _loading = true;
  String? _error;

  List<_DistrictHeat> get _districts {
    return _allDistricts.map((district) {
      final schemeData = district.schemes[_selectedScheme] ??
          const _SchemeHeat(applications: 0, approved: 0);
      final applications = schemeData.applications;
      final approved = schemeData.approved;
      final rate =
          applications == 0 ? 0 : ((approved / applications) * 100).round();
      return _DistrictHeat(
        name: district.name,
        applications: applications,
        approved: approved,
        pending: applications - approved,
        approvalRate: rate,
        color: _heatColor(applications),
      );
    }).toList()
      ..sort((a, b) => b.applications.compareTo(a.applications));
  }

  int get _totalApplications =>
      _districts.fold(0, (sum, row) => sum + row.applications);
  int get _totalApproved =>
      _districts.fold(0, (sum, row) => sum + row.approved);
  int get _totalPending => _districts.fold(0, (sum, row) => sum + row.pending);
  double get _approvalRate =>
      _totalApplications == 0 ? 0 : (_totalApproved / _totalApplications) * 100;

  @override
  void initState() {
    super.initState();
    _loadSchemeOptions();
  }

  Future<void> _loadSchemeOptions() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final values = await ApiService.getReferenceData('CM_SCHEME');
      if (!mounted) return;
      setState(() {
        _schemeOptions = [
          {'code': 'ALL', 'value': 'All Schemes'},
          ...values,
        ];
        if (!_schemeOptions
            .any((option) => option['code'] == _selectedScheme)) {
          _selectedScheme = 'ALL';
        }
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Failed to load heatmap.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _header(),
        if (_error != null) _errorBanner(_error!),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _loadSchemeOptions,
                  child: ListView(
                    padding: const EdgeInsets.all(12),
                    children: [
                      _summaryGrid(),
                      const SizedBox(height: 12),
                      _legend(),
                      const SizedBox(height: 12),
                      if (_districts.every((row) => row.applications == 0))
                        _emptyState()
                      else
                        for (final district in _districts)
                          _DistrictHeatCard(
                            district: district,
                            maxApplications: _districts.first.applications,
                            onTap: () => _showDistrictDetails(district),
                          ),
                      const SizedBox(height: 8),
                      const _InfoNote(),
                    ],
                  ),
                ),
        ),
      ],
    );
  }

  Widget _header() {
    return Container(
      color: const Color(0xFFF4F6FB),
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.map_outlined, color: Color(0xFF1A237E)),
              SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Scheme Heatmap - Meghalaya',
                  style: TextStyle(fontSize: 19, fontWeight: FontWeight.w900),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          DropdownButtonFormField<String>(
            isExpanded: true,
            value: _selectedScheme,
            decoration: const InputDecoration(labelText: 'Scheme'),
            selectedItemBuilder: (_) => [
              for (final option in _schemeOptions)
                Text(
                  option['value'] ?? option['code'] ?? '',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
            ],
            items: [
              for (final option in _schemeOptions)
                DropdownMenuItem(
                  value: option['code'],
                  child: Text(
                    option['value'] ?? option['code'] ?? '',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
            ],
            onChanged: (value) =>
                setState(() => _selectedScheme = value ?? 'ALL'),
          ),
        ],
      ),
    );
  }

  Widget _summaryGrid() {
    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: 2,
      mainAxisSpacing: 10,
      crossAxisSpacing: 10,
      childAspectRatio: 1.65,
      children: [
        _SummaryCard(
            label: 'Total Applications',
            value: '$_totalApplications',
            color: const Color(0xFF1A237E)),
        _SummaryCard(
            label: 'Approved',
            value: '$_totalApproved',
            color: const Color(0xFF16A34A)),
        _SummaryCard(
            label: 'Pending',
            value: '$_totalPending',
            color: const Color(0xFFF59E0B)),
        _SummaryCard(
            label: 'Approval Rate',
            value: '${_approvalRate.toStringAsFixed(1)}%',
            color: const Color(0xFF2563EB)),
      ],
    );
  }

  Widget _legend() {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: const Padding(
        padding: EdgeInsets.all(12),
        child: Wrap(
          spacing: 14,
          runSpacing: 8,
          children: [
            _Legend(label: 'High (40+)', color: Color(0xFFDC2626)),
            _Legend(label: 'Medium (20-40)', color: Color(0xFFF59E0B)),
            _Legend(label: 'Low (0-20)', color: Color(0xFF16A34A)),
          ],
        ),
      ),
    );
  }

  Widget _emptyState() {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: const Padding(
        padding: EdgeInsets.all(24),
        child: Center(
          child: Text(
            'No heatmap data found.',
            style: TextStyle(color: Color(0xFF64748B)),
          ),
        ),
      ),
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

  void _showDistrictDetails(_DistrictHeat district) {
    final schemeLabel = _schemeOptions.firstWhere(
      (option) => option['code'] == _selectedScheme,
      orElse: () => {'value': _selectedScheme},
    )['value'];
    showModalBottomSheet<void>(
      context: context,
      useSafeArea: true,
      builder: (_) => Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    district.name,
                    style: const TextStyle(
                        fontSize: 20, fontWeight: FontWeight.w900),
                  ),
                ),
                _HeatBadge(count: district.applications, color: district.color),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              schemeLabel ?? 'All Schemes',
              style: const TextStyle(color: Color(0xFF64748B)),
            ),
            const Divider(height: 24),
            _DetailLine('Applications', '${district.applications}'),
            _DetailLine('Approved', '${district.approved}'),
            _DetailLine('Pending', '${district.pending}'),
            _DetailLine('Approval Rate', '${district.approvalRate}%'),
          ],
        ),
      ),
    );
  }
}

class _DistrictSource {
  final String name;
  final Map<String, _SchemeHeat> schemes;
  const _DistrictSource(this.name, this.schemes);
}

class _SchemeHeat {
  final int applications;
  final int approved;
  const _SchemeHeat({required this.applications, required this.approved});
}

class _DistrictHeat {
  final String name;
  final int applications;
  final int approved;
  final int pending;
  final int approvalRate;
  final Color color;

  const _DistrictHeat({
    required this.name,
    required this.applications,
    required this.approved,
    required this.pending,
    required this.approvalRate,
    required this.color,
  });
}

const _allDistricts = [
  _DistrictSource('East Khasi Hills', {
    'ALL': _SchemeHeat(applications: 142, approved: 98),
    'CMSDF': _SchemeHeat(applications: 45, approved: 32),
    'CMSG': _SchemeHeat(applications: 38, approved: 27),
    'CM_CARE': _SchemeHeat(applications: 22, approved: 15),
    'CM_CONNECT': _SchemeHeat(applications: 18, approved: 12),
    'CM_ELEVATE': _SchemeHeat(applications: 12, approved: 8),
    'FOCUS_PLUS': _SchemeHeat(applications: 7, approved: 4),
  }),
  _DistrictSource('West Garo Hills', {
    'ALL': _SchemeHeat(applications: 118, approved: 79),
    'CMSDF': _SchemeHeat(applications: 38, approved: 25),
    'CMSG': _SchemeHeat(applications: 32, approved: 22),
    'CM_CARE': _SchemeHeat(applications: 19, approved: 13),
    'CM_CONNECT': _SchemeHeat(applications: 15, approved: 10),
    'CM_ELEVATE': _SchemeHeat(applications: 9, approved: 6),
    'FOCUS_PLUS': _SchemeHeat(applications: 5, approved: 3),
  }),
  _DistrictSource('East Garo Hills', {
    'ALL': _SchemeHeat(applications: 87, approved: 61),
    'CMSDF': _SchemeHeat(applications: 28, approved: 20),
    'CMSG': _SchemeHeat(applications: 24, approved: 17),
    'CM_CARE': _SchemeHeat(applications: 14, approved: 10),
    'CM_CONNECT': _SchemeHeat(applications: 11, approved: 8),
    'CM_ELEVATE': _SchemeHeat(applications: 7, approved: 4),
    'FOCUS_PLUS': _SchemeHeat(applications: 3, approved: 2),
  }),
  _DistrictSource('West Khasi Hills', {
    'ALL': _SchemeHeat(applications: 64, approved: 43),
    'CMSDF': _SchemeHeat(applications: 20, approved: 14),
    'CMSG': _SchemeHeat(applications: 17, approved: 12),
    'CM_CARE': _SchemeHeat(applications: 11, approved: 7),
    'CM_CONNECT': _SchemeHeat(applications: 8, approved: 5),
    'CM_ELEVATE': _SchemeHeat(applications: 5, approved: 3),
    'FOCUS_PLUS': _SchemeHeat(applications: 3, approved: 2),
  }),
  _DistrictSource('Ri Bhoi', {
    'ALL': _SchemeHeat(applications: 56, approved: 39),
    'CMSDF': _SchemeHeat(applications: 18, approved: 13),
    'CMSG': _SchemeHeat(applications: 15, approved: 11),
    'CM_CARE': _SchemeHeat(applications: 9, approved: 6),
    'CM_CONNECT': _SchemeHeat(applications: 7, approved: 5),
    'CM_ELEVATE': _SchemeHeat(applications: 5, approved: 3),
    'FOCUS_PLUS': _SchemeHeat(applications: 2, approved: 1),
  }),
  _DistrictSource('South Garo Hills', {
    'ALL': _SchemeHeat(applications: 48, approved: 32),
    'CMSDF': _SchemeHeat(applications: 15, approved: 10),
    'CMSG': _SchemeHeat(applications: 13, approved: 9),
    'CM_CARE': _SchemeHeat(applications: 8, approved: 5),
    'CM_CONNECT': _SchemeHeat(applications: 6, approved: 4),
    'CM_ELEVATE': _SchemeHeat(applications: 4, approved: 3),
    'FOCUS_PLUS': _SchemeHeat(applications: 2, approved: 1),
  }),
  _DistrictSource('West Jaintia Hills', {
    'ALL': _SchemeHeat(applications: 39, approved: 26),
    'CMSDF': _SchemeHeat(applications: 12, approved: 8),
    'CMSG': _SchemeHeat(applications: 10, approved: 7),
    'CM_CARE': _SchemeHeat(applications: 7, approved: 5),
    'CM_CONNECT': _SchemeHeat(applications: 5, approved: 3),
    'CM_ELEVATE': _SchemeHeat(applications: 3, approved: 2),
    'FOCUS_PLUS': _SchemeHeat(applications: 2, approved: 1),
  }),
  _DistrictSource('East Jaintia Hills', {
    'ALL': _SchemeHeat(applications: 35, approved: 24),
    'CMSDF': _SchemeHeat(applications: 11, approved: 8),
    'CMSG': _SchemeHeat(applications: 9, approved: 6),
    'CM_CARE': _SchemeHeat(applications: 6, approved: 4),
    'CM_CONNECT': _SchemeHeat(applications: 5, approved: 3),
    'CM_ELEVATE': _SchemeHeat(applications: 3, approved: 2),
    'FOCUS_PLUS': _SchemeHeat(applications: 1, approved: 1),
  }),
  _DistrictSource('North Garo Hills', {
    'ALL': _SchemeHeat(applications: 31, approved: 21),
    'CMSDF': _SchemeHeat(applications: 10, approved: 7),
    'CMSG': _SchemeHeat(applications: 8, approved: 6),
    'CM_CARE': _SchemeHeat(applications: 5, approved: 3),
    'CM_CONNECT': _SchemeHeat(applications: 4, approved: 3),
    'CM_ELEVATE': _SchemeHeat(applications: 3, approved: 2),
    'FOCUS_PLUS': _SchemeHeat(applications: 1, approved: 0),
  }),
  _DistrictSource('South West Khasi Hills', {
    'ALL': _SchemeHeat(applications: 24, approved: 16),
    'CMSDF': _SchemeHeat(applications: 8, approved: 5),
    'CMSG': _SchemeHeat(applications: 6, approved: 4),
    'CM_CARE': _SchemeHeat(applications: 4, approved: 3),
    'CM_CONNECT': _SchemeHeat(applications: 3, approved: 2),
    'CM_ELEVATE': _SchemeHeat(applications: 2, approved: 1),
    'FOCUS_PLUS': _SchemeHeat(applications: 1, approved: 1),
  }),
  _DistrictSource('Eastern West Khasi Hills', {
    'ALL': _SchemeHeat(applications: 18, approved: 12),
    'CMSDF': _SchemeHeat(applications: 6, approved: 4),
    'CMSG': _SchemeHeat(applications: 5, approved: 3),
    'CM_CARE': _SchemeHeat(applications: 3, approved: 2),
    'CM_CONNECT': _SchemeHeat(applications: 2, approved: 2),
    'CM_ELEVATE': _SchemeHeat(applications: 1, approved: 1),
    'FOCUS_PLUS': _SchemeHeat(applications: 1, approved: 0),
  }),
];

class _DistrictHeatCard extends StatelessWidget {
  final _DistrictHeat district;
  final int maxApplications;
  final VoidCallback onTap;

  const _DistrictHeatCard({
    required this.district,
    required this.maxApplications,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final ratio =
        maxApplications == 0 ? 0.0 : district.applications / maxApplications;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      district.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.w900),
                    ),
                  ),
                  _HeatBadge(
                      count: district.applications, color: district.color),
                ],
              ),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(999),
                child: LinearProgressIndicator(
                  value: ratio,
                  minHeight: 14,
                  backgroundColor: const Color(0xFFE5E7EB),
                  valueColor: AlwaysStoppedAnimation<Color>(district.color),
                ),
              ),
              const SizedBox(height: 6),
              Text(
                '${district.approved}/${district.applications} approved / Rate: ${district.approvalRate}%',
                style: const TextStyle(color: Color(0xFF64748B), fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _SummaryCard({
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(value,
                style: TextStyle(
                    color: color, fontSize: 24, fontWeight: FontWeight.w900)),
            const SizedBox(height: 4),
            Text(
              label,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: Color(0xFF64748B), fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }
}

class _HeatBadge extends StatelessWidget {
  final int count;
  final Color color;

  const _HeatBadge({required this.count, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        '$count',
        style: const TextStyle(
            color: Colors.white, fontWeight: FontWeight.w900, fontSize: 12),
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
          width: 12,
          height: 12,
          decoration: BoxDecoration(
              color: color, borderRadius: BorderRadius.circular(3)),
        ),
        const SizedBox(width: 5),
        Text(label, style: const TextStyle(fontSize: 12)),
      ],
    );
  }
}

class _DetailLine extends StatelessWidget {
  final String label;
  final String value;

  const _DetailLine(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          SizedBox(
            width: 128,
            child: Text(
              label,
              style: const TextStyle(
                  color: Color(0xFF64748B), fontWeight: FontWeight.w800),
            ),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}

class _InfoNote extends StatelessWidget {
  const _InfoNote();

  @override
  Widget build(BuildContext context) {
    return Card(
      color: const Color(0xFFEFF6FF),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: const Padding(
        padding: EdgeInsets.all(12),
        child: Row(
          children: [
            Icon(Icons.info_outline, color: Color(0xFF2563EB), size: 20),
            SizedBox(width: 8),
            Expanded(
              child: Text(
                'Mobile heatmap uses district intensity cards. Tap a district for detailed statistics.',
                style: TextStyle(color: Color(0xFF1E3A8A), fontSize: 12),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

Color _heatColor(int applications) {
  if (applications > 40) return const Color(0xFFDC2626);
  if (applications > 20) return const Color(0xFFF59E0B);
  return const Color(0xFF16A34A);
}
