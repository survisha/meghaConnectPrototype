import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/api_service.dart';
import '../widgets/megha_ui.dart';

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
  ('CMSDF', '2022', 'Rs. 2.5L', 'Completed'),
  ('CM Care', '2023', 'Rs. 50K', 'Completed'),
];

const _meetingHistory = [
  ('15 Jan 2024', 'CMSDF Application', 'Approved'),
  ('10 Nov 2023', 'Governance Issue', 'Forwarded'),
  ('05 Aug 2023', 'CMSG Application', 'Under Process'),
];

class PublicIdentificationScreen extends StatefulWidget {
  const PublicIdentificationScreen({super.key});

  @override
  State<PublicIdentificationScreen> createState() =>
      _PublicIdentificationScreenState();
}

class _PublicIdentificationScreenState
    extends State<PublicIdentificationScreen> {
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
  void dispose() {
    _phoneCtrl.dispose();
    _epicCtrl.dispose();
    _nameCtrl.dispose();
    super.dispose();
  }

  static _Person _mapPerson(Map<String, dynamic> m) => _Person(
        id: (m['id'] as num?)?.toInt() ?? 0,
        fullName: m['fullName'] as String? ?? '-',
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
    final epic = _epicCtrl.text.trim().toUpperCase();
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
      return _buildProfileView(_selected!);
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Row(
          children: [
            Icon(Icons.person_search_outlined, color: MeghaColors.primary),
            SizedBox(width: 8),
            Expanded(
              child: Text(
                'Public Identification',
                style: TextStyle(
                  color: MeghaColors.primary,
                  fontSize: 21,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        const Text(
          'Search for a person by phone, EPIC, name, or district. Facial recognition is ready for plug-and-play API support.',
          style:
              TextStyle(color: MeghaColors.muted, fontSize: 13, height: 1.35),
        ),
        const SizedBox(height: 16),
        _buildSearchCard(),
        const SizedBox(height: 16),
        _buildResultsCard(),
      ],
    );
  }

  Widget _buildSearchCard() {
    return MeghaSectionCard(
      title: 'Search',
      icon: Icons.search,
      child: Column(
        children: [
          TextField(
            controller: _phoneCtrl,
            keyboardType: TextInputType.phone,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(10),
            ],
            decoration: const InputDecoration(
              labelText: 'Phone Number',
              prefixIcon: Icon(Icons.phone_outlined),
              hintText: 'Mobile number',
            ),
            onSubmitted: (_) => _search(),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _epicCtrl,
            textCapitalization: TextCapitalization.characters,
            inputFormatters: [
              FilteringTextInputFormatter.allow(RegExp('[A-Za-z0-9]')),
            ],
            decoration: const InputDecoration(
              labelText: 'EPIC / Voter ID',
              prefixIcon: Icon(Icons.badge_outlined),
              hintText: 'EPIC number',
            ),
            onSubmitted: (_) => _search(),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _nameCtrl,
            textCapitalization: TextCapitalization.words,
            decoration: const InputDecoration(
              labelText: 'Name',
              prefixIcon: Icon(Icons.person_outline),
              hintText: 'Full or partial name',
            ),
            onSubmitted: (_) => _search(),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            value: _district,
            decoration: const InputDecoration(
              labelText: 'District',
              prefixIcon: Icon(Icons.place_outlined),
            ),
            items: [
              const DropdownMenuItem(value: '', child: Text('-- Clear --')),
              for (final d in _districts)
                DropdownMenuItem(value: d, child: Text(d)),
            ],
            onChanged: (value) => setState(() => _district = value ?? ''),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _searching ? null : _search,
                  icon: _searching
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.search),
                  label: const Text('Search'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _clear,
                  icon: const Icon(Icons.close),
                  label: const Text('Clear'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          OutlinedButton.icon(
            onPressed: null,
            icon: const Icon(Icons.camera_alt_outlined),
            label: const Text('Identify by Face (API)'),
          ),
        ],
      ),
    );
  }

  Widget _buildResultsCard() {
    return MeghaSectionCard(
      title: 'Results (${_results.length})',
      icon: Icons.list_alt_outlined,
      child: _buildResultsBody(),
    );
  }

  Widget _buildResultsBody() {
    if (_searching) {
      return const Padding(
        padding: EdgeInsets.all(28),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (!_searched) {
      return _emptyState(
          Icons.info_outline, 'Enter search criteria and click Search.');
    }
    if (_results.isEmpty) {
      return _emptyState(Icons.search_off_outlined, 'No results found.');
    }

    return Column(
      children: [
        for (final person in _results)
          _PersonResultTile(
            person: person,
            selected: _selected?.id == person.id,
            onTap: () => setState(() => _selected = person),
          ),
      ],
    );
  }

  Widget _emptyState(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 28, horizontal: 12),
      child: Column(
        children: [
          Icon(icon, size: 52, color: const Color(0xFFD1D5DB)),
          const SizedBox(height: 8),
          Text(
            text,
            textAlign: TextAlign.center,
            style: const TextStyle(color: MeghaColors.muted, fontSize: 13),
          ),
        ],
      ),
    );
  }

  Widget _buildProfileView(_Person person) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Row(
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => setState(() => _selected = null),
            ),
            const Expanded(
              child: Text(
                'Person Profile',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                  color: MeghaColors.primary,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        MeghaSectionCard(
          title: 'Person Profile',
          icon: Icons.badge_outlined,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  CircleAvatar(
                    radius: 32,
                    backgroundColor: MeghaColors.primary,
                    child: Text(
                      person.fullName.isNotEmpty
                          ? person.fullName[0].toUpperCase()
                          : '?',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 24,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          person.fullName,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w800,
                            color: MeghaColors.text,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(person.designation,
                            style: const TextStyle(color: MeghaColors.muted)),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 6,
                          runSpacing: 6,
                          children: const [
                            _StatusPill('Active Voter', Color(0xFF065F46)),
                            _StatusPill('EPIC Verified', Color(0xFF1E40AF)),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const Divider(height: 26),
              _InfoRow('Phone', person.phone),
              _InfoRow('EPIC', person.epic),
              _InfoRow('District', person.district),
              _InfoRow('Constituency', person.constituency),
              _InfoRow('Booth', person.booth),
              if (person.briefProfile?.trim().isNotEmpty == true) ...[
                const SizedBox(height: 10),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: MeghaColors.panelBg,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    person.briefProfile!,
                    style:
                        const TextStyle(color: MeghaColors.text, fontSize: 13),
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 14),
        MeghaSectionCard(
          title: 'Scheme & Meeting History',
          icon: Icons.history,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Scheme History',
                  style: TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              for (final item in _schemeHistory)
                _HistoryRow(
                  leading: item.$2,
                  title: item.$1,
                  trailing: item.$4,
                  subtitle: item.$3,
                ),
              const Divider(height: 24),
              const Text('Meeting History',
                  style: TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              for (final item in _meetingHistory)
                _HistoryRow(
                  leading: item.$1,
                  title: item.$2,
                  trailing: item.$3,
                ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PersonResultTile extends StatelessWidget {
  final _Person person;
  final bool selected;
  final VoidCallback onTap;

  const _PersonResultTile({
    required this.person,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          border: Border(
            bottom: const BorderSide(color: Color(0xFFF3F4F6)),
            left: BorderSide(
              color: selected ? MeghaColors.primary : Colors.transparent,
              width: 3,
            ),
          ),
        ),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: MeghaColors.primary,
              child: Text(
                person.fullName.isNotEmpty
                    ? person.fullName[0].toUpperCase()
                    : '?',
                style: const TextStyle(
                    color: Colors.white, fontWeight: FontWeight.w800),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(person.fullName,
                      style: const TextStyle(
                          color: MeghaColors.text,
                          fontWeight: FontWeight.w700)),
                  Text(person.designation,
                      style: const TextStyle(
                          color: MeghaColors.muted, fontSize: 12)),
                  Text('${person.constituency}, ${person.district}',
                      style: const TextStyle(
                          color: Color(0xFF9CA3AF), fontSize: 11)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: Color(0xFF9CA3AF)),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 104,
            child: Text(label,
                style: const TextStyle(
                    color: MeghaColors.muted,
                    fontSize: 12,
                    fontWeight: FontWeight.w700)),
          ),
          Expanded(
            child: Text(
              value.trim().isEmpty ? '-' : value,
              style: const TextStyle(
                color: MeghaColors.text,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  final String label;
  final Color color;

  const _StatusPill(this.label, this.color);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withAlpha(31),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style:
            TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700),
      ),
    );
  }
}

class _HistoryRow extends StatelessWidget {
  final String leading;
  final String title;
  final String trailing;
  final String? subtitle;

  const _HistoryRow({
    required this.leading,
    required this.title,
    required this.trailing,
    this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          SizedBox(
            width: 76,
            child: Text(
              leading,
              style: const TextStyle(
                color: MeghaColors.muted,
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: const TextStyle(
                        color: MeghaColors.text,
                        fontSize: 13,
                        fontWeight: FontWeight.w700)),
                if (subtitle != null)
                  Text(subtitle!,
                      style: const TextStyle(
                          color: MeghaColors.muted, fontSize: 12)),
              ],
            ),
          ),
          const SizedBox(width: 8),
          _StatusPill(trailing, const Color(0xFF065F46)),
        ],
      ),
    );
  }
}
