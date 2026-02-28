import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/navigation_service.dart';

class _Appointment {
  final String id;
  final String applicantName;
  final String phone;
  final String agendaType;
  final String agendaBrief;
  final String status;
  final String location;
  final String scheduledAt;
  final bool isWalkIn;

  const _Appointment({
    required this.id,
    required this.applicantName,
    required this.phone,
    required this.agendaType,
    required this.agendaBrief,
    required this.status,
    required this.location,
    required this.scheduledAt,
    this.isWalkIn = false,
  });
}

const _mockAppointments = <_Appointment>[
  _Appointment(
    id: 'MC-2024-00001',
    applicantName: 'Bijoy Momin',
    phone: '9876500001',
    agendaType: 'A4',
    agendaBrief: 'Request for medical assistance under CM Care scheme',
    status: 'HCM_ACCEPTED',
    location: 'Tura',
    scheduledAt: '2024-07-15 10:00',
  ),
  _Appointment(
    id: 'MC-2024-00002',
    applicantName: 'Ramsing Marak',
    phone: '9876500002',
    agendaType: 'A4',
    agendaBrief: 'Agricultural development grant request',
    status: 'HCM_PENDING',
    location: 'Tura',
    scheduledAt: '2024-07-15 11:00',
  ),
  _Appointment(
    id: 'MC-2024-00003',
    applicantName: 'Deibok Lyngdoh',
    phone: '9876500003',
    agendaType: 'B2',
    agendaBrief: 'Walk-in: Employment assistance',
    status: 'DEO_PROCESSED',
    location: 'Shillong',
    scheduledAt: '2024-07-15 14:00',
    isWalkIn: true,
  ),
  _Appointment(
    id: 'MC-2024-00004',
    applicantName: 'Merina Sangma',
    phone: '9876500004',
    agendaType: 'A4',
    agendaBrief: 'Infrastructure request for Rongram constituency',
    status: 'CMO_REVIEW',
    location: 'Tura',
    scheduledAt: '2024-07-16 09:00',
  ),
  _Appointment(
    id: 'MC-2024-00005',
    applicantName: 'Phillip Shullai',
    phone: '9876500005',
    agendaType: 'A4',
    agendaBrief: 'Education scholarship under CM Elevate scheme',
    status: 'APPROVER_REVIEW',
    location: 'Shillong',
    scheduledAt: '2024-07-16 11:30',
  ),
  _Appointment(
    id: 'MC-2024-00006',
    applicantName: 'Sengrik D. Shira',
    phone: '9876500006',
    agendaType: 'A4',
    agendaBrief: 'Community hall construction under CMSDF',
    status: 'SCHEDULED',
    location: 'Jowai',
    scheduledAt: '2024-07-17 14:00',
  ),
];

class AppointmentsScreen extends StatefulWidget {
  const AppointmentsScreen({super.key});

  @override
  State<AppointmentsScreen> createState() => _AppointmentsScreenState();
}

class _AppointmentsScreenState extends State<AppointmentsScreen> {
  String _searchQuery = '';
  String _filterStatus = 'All';

  static const _statusFilters = [
    'All',
    'Pending',
    'Scheduled',
    'Completed',
  ];

  List<_Appointment> get _filtered {
    return _mockAppointments.where((a) {
      final matchSearch = _searchQuery.isEmpty ||
          a.applicantName.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          a.id.toLowerCase().contains(_searchQuery.toLowerCase());

      final matchFilter = _filterStatus == 'All' ||
          (_filterStatus == 'Pending' &&
              (a.status.contains('PENDING') || a.status.contains('REVIEW') || a.status == 'SUBMITTED' || a.status == 'DEO_PROCESSED')) ||
          (_filterStatus == 'Scheduled' && a.status == 'SCHEDULED') ||
          (_filterStatus == 'Completed' && (a.status == 'COMPLETED' || a.status == 'HCM_ACCEPTED'));

      return matchSearch && matchFilter;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final role = auth.user!.role;
    final canAddNew = [
      UserRole.ADMIN,
      UserRole.SAIDUL_OSD,
      UserRole.DATA_ENTRY_OPERATOR,
    ].contains(role);

    return Column(
      children: [
        _buildSearchBar(),
        _buildFilterChips(),
        Expanded(
          child: _filtered.isEmpty
              ? _buildEmpty()
              : ListView.separated(
                  padding: const EdgeInsets.all(12),
                  itemCount: _filtered.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 8),
                  itemBuilder: (_, i) => _AppointmentCard(appointment: _filtered[i]),
                ),
        ),
        if (canAddNew)
          _buildBottomActions(context),
      ],
    );
  }

  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
      child: TextField(
        decoration: InputDecoration(
          hintText: 'Search by name or ID...',
          prefixIcon: const Icon(Icons.search),
          suffixIcon: _searchQuery.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: () => setState(() => _searchQuery = ''),
                )
              : null,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        ),
        onChanged: (v) => setState(() => _searchQuery = v),
      ),
    );
  }

  Widget _buildFilterChips() {
    return SizedBox(
      height: 44,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        itemCount: _statusFilters.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (_, i) {
          final f = _statusFilters[i];
          final selected = _filterStatus == f;
          return FilterChip(
            label: Text(f),
            selected: selected,
            onSelected: (_) => setState(() => _filterStatus = f),
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
          Icon(Icons.search_off, size: 56, color: Colors.grey[400]),
          const SizedBox(height: 12),
          Text(
            'No appointments found',
            style: TextStyle(color: Colors.grey[500], fontSize: 16),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomActions(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(20),
            blurRadius: 8,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: ElevatedButton.icon(
              icon: const Icon(Icons.add),
              label: const Text('New Appointment'),
              onPressed: () => context.read<NavigationService>().navigateTo('new_appointment'),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: OutlinedButton.icon(
              icon: const Icon(Icons.login),
              label: const Text('Walk-in'),
              onPressed: () => context.read<NavigationService>().navigateTo('walkin'),
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFF2E7D32),
                side: const BorderSide(color: Color(0xFF2E7D32)),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _AppointmentCard extends StatelessWidget {
  final _Appointment appointment;
  const _AppointmentCard({required this.appointment});

  Color _statusColor(String status) {
    if (status.contains('ACCEPTED') || status == 'COMPLETED') return const Color(0xFF16A34A);
    if (status.contains('PENDING') || status.contains('REVIEW')) return const Color(0xFFB45309);
    if (status == 'SCHEDULED') return const Color(0xFF1A237E);
    if (status.contains('REJECTED') || status.contains('CANCELLED')) return const Color(0xFF991B1B);
    return const Color(0xFF4B5563);
  }

  String _statusLabel(String status) {
    return status.replaceAll('_', ' ').split(' ').map((w) {
      if (w.isEmpty) return w;
      return w[0].toUpperCase() + w.substring(1).toLowerCase();
    }).join(' ');
  }

  Color _typeColor(String type) {
    const m = {
      'A1': Color(0xFF1565C0),
      'A2': Color(0xFF2E7D32),
      'A3': Color(0xFFF57F17),
      'A4': Color(0xFFC62828),
      'B1': Color(0xFF4527A0),
      'B2': Color(0xFF006064),
    };
    return m[type] ?? Colors.grey;
  }

  @override
  Widget build(BuildContext context) {
    final sc = _statusColor(appointment.status);
    final tc = _typeColor(appointment.agendaType);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: tc.withAlpha(26),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: tc.withAlpha(77)),
                  ),
                  child: Text(
                    appointment.agendaType,
                    style: TextStyle(color: tc, fontWeight: FontWeight.bold, fontSize: 11),
                  ),
                ),
                if (appointment.isWalkIn) ...[
                  const SizedBox(width: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
                    decoration: BoxDecoration(
                      color: const Color(0xFF006064).withAlpha(26),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Text(
                      'Walk-in',
                      style: TextStyle(color: Color(0xFF006064), fontSize: 10, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: sc.withAlpha(20),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    _statusLabel(appointment.status),
                    style: TextStyle(color: sc, fontSize: 10, fontWeight: FontWeight.w600),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                const Icon(Icons.person_outline, size: 16, color: Color(0xFF1A237E)),
                const SizedBox(width: 6),
                Text(
                  appointment.applicantName,
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              appointment.agendaBrief,
              style: TextStyle(fontSize: 13, color: Colors.grey[600]),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.tag, size: 13, color: Colors.grey[500]),
                const SizedBox(width: 4),
                Text(appointment.id, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
                const SizedBox(width: 12),
                Icon(Icons.location_on_outlined, size: 13, color: Colors.grey[500]),
                const SizedBox(width: 4),
                Text(appointment.location, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
                const SizedBox(width: 12),
                Icon(Icons.access_time, size: 13, color: Colors.grey[500]),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    appointment.scheduledAt,
                    style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
