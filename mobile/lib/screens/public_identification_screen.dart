import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/api_service.dart';

class _Person {
  final int id;
  final String fullName;
  final String phone;
  final String epic;
  final String designation;
  final String district;
  final String constituency;
  final String booth;
  final String? briefProfile;

  const _Person({
    required this.id,
    required this.fullName,
    required this.phone,
    required this.epic,
    required this.designation,
    required this.district,
    required this.constituency,
    required this.booth,
    this.briefProfile,
  });
}

const _schemeHistory = [
  ('CMSDF', '2022', '₹2.5L', 'Completed'),
  ('CM Care', '2023', '₹50K', 'Completed'),
];

const _meetingHistory = [
  ('15 Jan 2024', 'CMSDF Application', 'Approved'),
  ('10 Nov 2023', 'Governance Issue', 'Forwarded to Dept'),
  ('05 Aug 2023', 'CMSG Application', 'Under Process'),
];

class PublicIdentificationScreen extends StatefulWidget {
  const PublicIdentificationScreen({super.key});

  @override
  State<PublicIdentificationScreen> createState() =>
      _PublicIdentificationScreenState();
}

class _PublicIdentificationScreenState
    extends State<PublicIdentificationScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabCtrl;

  final _phoneCtrl = TextEditingController();
  final _epicCtrl = TextEditingController();
  final _nameCtrl = TextEditingController();
  String _district = '';

  List<_Person> _results = [];
  _Person? _selected;
  bool _searched = false;
  bool _searching = false;

  static const _districts = [
    'East Khasi Hills',
    'West Khasi Hills',
    'South West Khasi Hills',
    'Ri Bhoi',
    'East Jaintia Hills',
    'West Jaintia Hills',
    'East Garo Hills',
    'West Garo Hills',
    'South Garo Hills',
    'North Garo Hills',
    'Eastern West Khasi Hills',
    'Western South Garo Hills',
  ];

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    _phoneCtrl.dispose();
    _epicCtrl.dispose();
    _nameCtrl.dispose();
    super.dispose();
  }

  static _Person _mapPerson(Map<String, dynamic> m) => _Person(
        id: (m['id'] as num?)?.toInt() ?? 0,
        fullName: m['fullName'] as String? ?? '—',
        phone: m['phoneNumber'] as String? ?? '',
        epic: m['epicNumber'] as String? ?? '',
        designation: m['designation'] as String? ?? '',
        district: m['district'] as String? ?? '',
        constituency: m['constituency'] as String? ?? '',
        booth: m['booth'] as String? ?? '',
        briefProfile: m['briefProfile'] as String?,
      );

  Future<void> _search() async {
    final phone = _phoneCtrl.text.trim();
    final epic = _epicCtrl.text.trim();
    final name = _nameCtrl.text.trim();
    final district = _district;

    setState(() {
      _searching = true;
      _searched = true;
      _selected = null;
      _results = [];
    });

    List<_Person> results = [];

    if (phone.isNotEmpty) {
      final m = await ApiService.searchPersonByPhone(phone);
      if (m != null) results.add(_mapPerson(m));
    } else if (epic.isNotEmpty) {
      final m = await ApiService.searchPersonByEpic(epic);
      if (m != null) results.add(_mapPerson(m));
    } else if (name.isNotEmpty) {
      final list = await ApiService.searchPersonsByName(name);
      results = list.map((e) => _mapPerson(e as Map<String, dynamic>)).toList();
    } else if (district.isNotEmpty) {
      final list = await ApiService.searchPersonsByDistrict(district);
      results = list.map((e) => _mapPerson(e as Map<String, dynamic>)).toList();
    }

    if (!mounted) return;
    setState(() {
      _results = results;
      _searching = false;
    });
  }

  void _clear() {
    _phoneCtrl.clear();
    _epicCtrl.clear();
    _nameCtrl.clear();
    setState(() {
      _district = '';
      _results = [];
      _selected = null;
      _searched = false;
      _searching = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_selected != null) {
      return _buildProfileView(context, _selected!);
    }
    return Column(
      children: [
        _buildSearchHeader(),
        TabBar(
          controller: _tabCtrl,
          labelColor: const Color(0xFF1A237E),
          unselectedLabelColor: Colors.grey,
          indicatorColor: const Color(0xFF1A237E),
          tabs: const [
            Tab(text: 'Phone'),
            Tab(text: 'EPIC'),
            Tab(text: 'Name'),
          ],
        ),
        SizedBox(
          height: 120,
          child: TabBarView(
            controller: _tabCtrl,
            children: [
              _buildPhoneTab(),
              _buildEpicTab(),
              _buildNameTab(),
            ],
          ),
        ),
        Expanded(child: _buildResults()),
      ],
    );
  }

  Widget _buildSearchHeader() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      color: const Color(0xFF1A237E),
      child: Row(
        children: [
          const Icon(Icons.badge_outlined, color: Colors.white, size: 20),
          const SizedBox(width: 10),
          const Expanded(
            child: Text(
              'Identify by Phone, EPIC, or Name',
              style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                  fontSize: 14),
            ),
          ),
          if (_searched)
            TextButton(
              onPressed: _clear,
              child: const Text('Clear',
                  style: TextStyle(color: Colors.white70, fontSize: 12)),
            ),
        ],
      ),
    );
  }

  Widget _buildPhoneTab() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Row(
        children: [
          Expanded(
            child: TextField(
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
              ),
              onSubmitted: (_) => _search(),
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton(
            onPressed: _search,
            child: const Text('Search'),
          ),
        ],
      ),
    );
  }

  Widget _buildEpicTab() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _epicCtrl,
              textCapitalization: TextCapitalization.characters,
              decoration: const InputDecoration(
                labelText: 'EPIC / Voter ID Number',
                prefixIcon: Icon(Icons.credit_card_outlined),
              ),
              onSubmitted: (_) => _search(),
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton(
            onPressed: _search,
            child: const Text('Search'),
          ),
        ],
      ),
    );
  }

  Widget _buildNameTab() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _nameCtrl,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: 'Full Name',
                prefixIcon: Icon(Icons.person_search_outlined),
              ),
              onSubmitted: (_) => _search(),
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton(
            onPressed: _search,
            child: const Text('Search'),
          ),
        ],
      ),
    );
  }

  Widget _buildResults() {
    if (_searching) {
      return const Center(child: CircularProgressIndicator());
    }
    if (!_searched) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.search, size: 56, color: Colors.grey[400]),
              const SizedBox(height: 12),
              Text(
                'Enter phone, EPIC, or name to identify a person',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey[500], fontSize: 14),
              ),
            ],
          ),
        ),
      );
    }

    if (_results.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.person_off_outlined, size: 56, color: Colors.grey[400]),
            const SizedBox(height: 12),
            Text(
              'No records found',
              style: TextStyle(color: Colors.grey[500], fontSize: 16),
            ),
          ],
        ),
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(12),
      itemCount: _results.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (_, i) => _PersonResultCard(
        person: _results[i],
        onTap: () => setState(() => _selected = _results[i]),
      ),
    );
  }

  Widget _buildProfileView(BuildContext context, _Person person) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => setState(() => _selected = null),
              ),
              const Text(
                'Person Profile',
                style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF1A237E)),
              ),
            ],
          ),
          const SizedBox(height: 8),
          // Profile Header
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 30,
                    backgroundColor: const Color(0xFF1A237E),
                    child: Text(
                      person.fullName.isNotEmpty
                          ? person.fullName[0].toUpperCase()
                          : '?',
                      style: const TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.bold),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          person.fullName,
                          style: const TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          person.designation,
                          style: TextStyle(
                              color: Colors.grey[600], fontSize: 13),
                        ),
                        Text(
                          '${person.constituency}, ${person.district}',
                          style: TextStyle(
                              color: Colors.grey[500], fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          // Identity details
          _buildSection(
            '🪪 Identity',
            [
              _rowPair('Phone', person.phone),
              _rowPair('EPIC', person.epic),
              _rowPair('Booth', person.booth),
            ],
          ),
          const SizedBox(height: 12),
          // Profile
          if (person.briefProfile != null)
            _buildSection(
              '📋 Brief Profile',
              [Text(person.briefProfile!,
                  style: TextStyle(color: Colors.grey[700], fontSize: 13))],
            ),
          const SizedBox(height: 12),
          // Scheme History
          _buildSection(
            '🏆 Scheme History',
            [
              Table(
                columnWidths: const {
                  0: FlexColumnWidth(2),
                  1: FlexColumnWidth(1),
                  2: FlexColumnWidth(2),
                  3: FlexColumnWidth(2),
                },
                children: [
                  TableRow(
                    decoration:
                        BoxDecoration(color: Colors.grey[100]),
                    children: ['Scheme', 'Year', 'Amount', 'Status']
                        .map((h) => Padding(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 8, vertical: 6),
                              child: Text(h,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 12)),
                            ))
                        .toList(),
                  ),
                  ..._schemeHistory.map(
                    (s) => TableRow(
                      children: [s.$1, s.$2, s.$3, s.$4]
                          .map((v) => Padding(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 8, vertical: 6),
                                child: Text(v,
                                    style: const TextStyle(fontSize: 12)),
                              ))
                          .toList(),
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          // Meeting History
          _buildSection(
            '📅 Last 3 Meetings',
            [
              ..._meetingHistory.map(
                (m) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    children: [
                      const Icon(Icons.event,
                          size: 14, color: Color(0xFF1A237E)),
                      const SizedBox(width: 6),
                      Text(m.$1,
                          style: const TextStyle(
                              fontSize: 12, fontWeight: FontWeight.w500)),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(m.$2,
                            style: TextStyle(
                                fontSize: 12, color: Colors.grey[600])),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: const Color(0xFFE8EAF6),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(m.$3,
                            style: const TextStyle(
                                fontSize: 11,
                                color: Color(0xFF1A237E))),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSection(String title, List<Widget> children) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1A237E)),
            ),
            const SizedBox(height: 10),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _rowPair(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          SizedBox(
            width: 60,
            child: Text(label,
                style: TextStyle(
                    color: Colors.grey[500],
                    fontSize: 12,
                    fontWeight: FontWeight.w500)),
          ),
          Expanded(
            child: Text(value,
                style: const TextStyle(
                    fontSize: 13, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}

class _PersonResultCard extends StatelessWidget {
  final _Person person;
  final VoidCallback onTap;
  const _PersonResultCard({required this.person, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        onTap: onTap,
        leading: CircleAvatar(
          backgroundColor: const Color(0xFF1A237E),
          child: Text(
            person.fullName.isNotEmpty ? person.fullName[0].toUpperCase() : '?',
            style:
                const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
        ),
        title: Text(person.fullName,
            style:
                const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(person.designation,
                style: TextStyle(fontSize: 12, color: Colors.grey[600])),
            Text('${person.constituency} · ${person.district}',
                style: TextStyle(fontSize: 11, color: Colors.grey[500])),
          ],
        ),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(person.phone,
                style: const TextStyle(fontSize: 12, color: Color(0xFF1A237E))),
            const Icon(Icons.chevron_right, size: 16, color: Colors.grey),
          ],
        ),
        isThreeLine: true,
      ),
    );
  }
}
