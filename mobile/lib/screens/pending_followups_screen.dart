import 'package:flutter/material.dart';

class _Followup {
  final int id;
  final String applicationId;
  final String applicant;
  final String direction;
  final String color;
  final String department;
  final String deadline;
  final int daysLeft;
  final String status;
  final String officer;

  const _Followup({
    required this.id,
    required this.applicationId,
    required this.applicant,
    required this.direction,
    required this.color,
    required this.department,
    required this.deadline,
    required this.daysLeft,
    required this.status,
    required this.officer,
  });
}

const _mockFollowups = <_Followup>[
  _Followup(
    id: 1,
    applicationId: 'MC-2024-00001',
    applicant: 'Ramsing Marak',
    direction: 'Expedite CMSDF approval for community hall',
    color: 'GREEN',
    department: 'Planning Dept',
    deadline: '01 Apr 2024',
    daysLeft: 17,
    status: 'Under Review',
    officer: 'Dy Secy Planning',
  ),
  _Followup(
    id: 2,
    applicationId: 'MC-2024-00005',
    applicant: 'Monika Sangma',
    direction: 'Forward medical case to CMO Health',
    color: 'GREEN',
    department: 'Health Dept',
    deadline: '20 Mar 2024',
    daysLeft: -2,
    status: 'Overdue',
    officer: 'Dir Health',
  ),
  _Followup(
    id: 3,
    applicationId: 'MC-2024-00003',
    applicant: 'Bijoy Momin',
    direction: 'Forward to Finance for budget clearance',
    color: 'YELLOW',
    department: 'Finance Dept',
    deadline: '25 Mar 2024',
    daysLeft: 10,
    status: 'In Progress',
    officer: 'Dy Secy Finance',
  ),
  _Followup(
    id: 4,
    applicationId: 'MC-2024-00007',
    applicant: 'Bilash Marak',
    direction: 'Expedite road construction file',
    color: 'GREEN',
    department: 'PWD',
    deadline: '15 Apr 2024',
    daysLeft: 31,
    status: 'Not Started',
    officer: 'SE PWD',
  ),
];

class PendingFollowupsScreen extends StatefulWidget {
  const PendingFollowupsScreen({super.key});

  @override
  State<PendingFollowupsScreen> createState() =>
      _PendingFollowupsScreenState();
}

class _PendingFollowupsScreenState extends State<PendingFollowupsScreen> {
  String _filterColor = 'ALL';

  List<_Followup> get _filtered => _mockFollowups
      .where((f) => _filterColor == 'ALL' || f.color == _filterColor)
      .toList();

  Color _dirColor(String c) {
    if (c == 'GREEN') return const Color(0xFF16A34A);
    if (c == 'YELLOW') return const Color(0xFFB45309);
    if (c == 'BLUE') return const Color(0xFF1565C0);
    return Colors.grey;
  }

  Color _statusColor(String s) {
    if (s == 'Overdue') return const Color(0xFF991B1B);
    if (s == 'In Progress') return const Color(0xFFB45309);
    if (s == 'Under Review') return const Color(0xFF1A237E);
    return const Color(0xFF4B5563);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildSummaryBar(),
        _buildFilterChips(),
        Expanded(
          child: _filtered.isEmpty
              ? _buildEmpty()
              : ListView.separated(
                  padding: const EdgeInsets.all(12),
                  itemCount: _filtered.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 8),
                  itemBuilder: (_, i) => _FollowupCard(
                    followup: _filtered[i],
                    dirColor: _dirColor(_filtered[i].color),
                    statusColor: _statusColor(_filtered[i].status),
                  ),
                ),
        ),
      ],
    );
  }

  Widget _buildSummaryBar() {
    final overdue = _mockFollowups.where((f) => f.daysLeft < 0).length;
    final green = _mockFollowups.where((f) => f.color == 'GREEN').length;
    final yellow = _mockFollowups.where((f) => f.color == 'YELLOW').length;
    return Container(
      color: const Color(0xFF1A237E),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _SummaryPill(
              label: 'Total', value: '${_mockFollowups.length}',
              color: Colors.white70),
          _SummaryPill(
              label: 'Green', value: '$green',
              color: const Color(0xFF86EFAC)),
          _SummaryPill(
              label: 'Yellow', value: '$yellow',
              color: const Color(0xFFFDE68A)),
          _SummaryPill(
              label: 'Overdue', value: '$overdue',
              color: const Color(0xFFFCA5A5)),
        ],
      ),
    );
  }

  Widget _buildFilterChips() {
    const filters = [
      ('ALL', 'All'),
      ('GREEN', 'Green'),
      ('YELLOW', 'Yellow'),
      ('BLUE', 'Blue'),
    ];
    return SizedBox(
      height: 44,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        itemCount: filters.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (_, i) {
          final (val, label) = filters[i];
          final selected = _filterColor == val;
          return FilterChip(
            label: Text(label),
            selected: selected,
            onSelected: (_) => setState(() => _filterColor = val),
            selectedColor: const Color(0xFF1A237E).withAlpha(26),
            checkmarkColor: const Color(0xFF1A237E),
            labelStyle: TextStyle(
              color: selected ? const Color(0xFF1A237E) : Colors.grey[700],
              fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
              fontSize: 13,
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

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.check_circle_outline, size: 56, color: Colors.grey[400]),
          const SizedBox(height: 12),
          Text('No pending follow-ups',
              style: TextStyle(color: Colors.grey[500], fontSize: 16)),
        ],
      ),
    );
  }
}

class _FollowupCard extends StatelessWidget {
  final _Followup followup;
  final Color dirColor;
  final Color statusColor;

  const _FollowupCard({
    required this.followup,
    required this.dirColor,
    required this.statusColor,
  });

  @override
  Widget build(BuildContext context) {
    final isOverdue = followup.daysLeft < 0;
    return Card(
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          border: Border(left: BorderSide(color: dirColor, width: 4)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Status row
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: dirColor.withAlpha(26),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: dirColor.withAlpha(77)),
                    ),
                    child: Text(
                      followup.color,
                      style: TextStyle(
                          color: dirColor,
                          fontWeight: FontWeight.bold,
                          fontSize: 11),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: statusColor.withAlpha(20),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      followup.status,
                      style: TextStyle(
                          color: statusColor,
                          fontSize: 10,
                          fontWeight: FontWeight.w600),
                    ),
                  ),
                  const Spacer(),
                  if (isOverdue)
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFEE2E2),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: const Text(
                        '⚠ Overdue',
                        style: TextStyle(
                            color: Color(0xFF991B1B),
                            fontSize: 10,
                            fontWeight: FontWeight.bold),
                      ),
                    )
                  else
                    Text(
                      '${followup.daysLeft}d left',
                      style: TextStyle(
                          fontSize: 12,
                          color: followup.daysLeft <= 7
                              ? const Color(0xFFB45309)
                              : Colors.grey[500]),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              // Direction
              Text(
                followup.direction,
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 14),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 6),
              // Applicant and App ID
              Row(
                children: [
                  Icon(Icons.person_outline,
                      size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text(followup.applicant,
                      style: TextStyle(
                          fontSize: 12, color: Colors.grey[600])),
                  const SizedBox(width: 10),
                  Icon(Icons.tag, size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text(followup.applicationId,
                      style: TextStyle(
                          fontSize: 12, color: Colors.grey[600])),
                ],
              ),
              const SizedBox(height: 4),
              // Department and deadline
              Row(
                children: [
                  Icon(Icons.business, size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text(followup.department,
                      style: TextStyle(
                          fontSize: 12, color: Colors.grey[600])),
                  const SizedBox(width: 10),
                  Icon(Icons.calendar_today,
                      size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text(followup.deadline,
                      style: TextStyle(
                          fontSize: 12,
                          color: isOverdue
                              ? const Color(0xFF991B1B)
                              : Colors.grey[600])),
                ],
              ),
              const SizedBox(height: 4),
              Row(
                children: [
                  Icon(Icons.person_pin_outlined,
                      size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text('Officer: ${followup.officer}',
                      style: TextStyle(
                          fontSize: 12, color: Colors.grey[600])),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SummaryPill extends StatelessWidget {
  final String label;
  final String value;
  final Color color;
  const _SummaryPill(
      {required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value,
            style: TextStyle(
                color: color, fontWeight: FontWeight.bold, fontSize: 18)),
        Text(label,
            style: const TextStyle(color: Colors.white60, fontSize: 10)),
      ],
    );
  }
}
