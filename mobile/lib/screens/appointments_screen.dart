import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../services/navigation_service.dart';

class _Appointment {
  final String id;
  final int? backendId;
  final String applicantName;
  final String phone;
  final String agendaType;
  final String agendaBrief;
  final String status;
  final String location;
  final String scheduledAt;
  final DateTime? scheduledDate;
  final String tokenNumber;
  final bool isWalkIn;
  final Map<String, dynamic> raw;

  const _Appointment({
    required this.id,
    required this.backendId,
    required this.applicantName,
    required this.phone,
    required this.agendaType,
    required this.agendaBrief,
    required this.status,
    required this.location,
    required this.scheduledAt,
    required this.scheduledDate,
    required this.tokenNumber,
    required this.raw,
    this.isWalkIn = false,
  });
}

class AppointmentsScreen extends StatefulWidget {
  final bool forceApproverMode;

  const AppointmentsScreen({super.key, this.forceApproverMode = false});

  @override
  State<AppointmentsScreen> createState() => _AppointmentsScreenState();
}

class _AppointmentsScreenState extends State<AppointmentsScreen> {
  String _searchQuery = '';
  String _filterStatus = 'All';
  DateTime? _fromDate;
  DateTime? _toDate;
  List<_Appointment> _appointments = [];
  bool _loading = true;

  static const _statusFilters = [
    'All',
    'Pending',
    'Scheduled',
    'Forwarded',
    'Completed',
  ];

  @override
  void initState() {
    super.initState();
    _loadAppointments();
  }

  Future<void> _loadAppointments() async {
    setState(() => _loading = true);
    final role = context.read<AuthService>().user?.role;
    final data = widget.forceApproverMode || role == UserRole.APPROVER
        ? await ApiService.getApproverAppointments()
        : role == UserRole.PUBLIC
            ? await ApiService.getMyAppointments()
            : role == UserRole.DATA_ENTRY_OPERATOR
                ? await ApiService.getDeoAppointments()
                : await ApiService.getAppointments();
    if (!mounted) return;
    final content = (data['content'] as List<dynamic>?) ?? [];
    setState(() {
      _appointments = content.map((e) {
        final m = e as Map<String, dynamic>;
        final applicant = m['applicant'] as Map<String, dynamic>? ?? {};
        final dateRaw = m['scheduledDateTime']?.toString() ??
            m['appointmentDate']?.toString() ??
            m['createdAt']?.toString() ??
            m['submittedAt']?.toString();
        final parsedDate = dateRaw == null ? null : DateTime.tryParse(dateRaw);
        final backendId = m['id'] is num
            ? (m['id'] as num).toInt()
            : int.tryParse(m['id']?.toString() ?? '');
        return _Appointment(
          id: m['applicationId'] as String? ?? m['id']?.toString() ?? '',
          backendId: backendId,
          applicantName: applicant['fullName'] as String? ??
              m['applicantName'] as String? ??
              m['fullName'] as String? ??
              '—',
          phone: applicant['phoneNumber'] as String? ??
              m['applicantPhone'] as String? ??
              m['applicantMobile'] as String? ??
              m['phoneNumber'] as String? ??
              '',
          agendaType: m['agendaType'] as String? ??
              m['eventType'] as String? ??
              m['appointmentType'] as String? ??
              '',
          agendaBrief: m['agendaBrief'] as String? ??
              m['briefDescription'] as String? ??
              m['description'] as String? ??
              '',
          status: m['status'] as String? ?? '',
          location: m['requestedLocation'] as String? ?? '',
          scheduledAt: _fmtDateTime(dateRaw),
          scheduledDate: parsedDate,
          tokenNumber: m['walkInTokenNumber']?.toString() ??
              m['tokenNumber']?.toString() ??
              '',
          raw: m,
          isWalkIn: m['isWalkIn'] as bool? ?? false,
        );
      }).toList();
      _loading = false;
    });
  }

  static String _fmtDateTime(String? iso) {
    if (iso == null) return '—';
    final dt = DateTime.tryParse(iso);
    if (dt == null) return iso;
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} '
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  List<_Appointment> get _filtered {
    return _appointments.where((a) {
      final matchSearch = _searchQuery.isEmpty ||
          a.applicantName.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          a.id.toLowerCase().contains(_searchQuery.toLowerCase());

      final matchFilter = _filterStatus == 'All' ||
          (_filterStatus == 'Pending' &&
              (a.status.contains('PENDING') ||
                  a.status.contains('REVIEW') ||
                  a.status == 'SUBMITTED' ||
                  a.status == 'DEO_PROCESSED')) ||
          (_filterStatus == 'Scheduled' && a.status == 'SCHEDULED') ||
          (_filterStatus == 'Forwarded' && a.status.contains('FORWARDED')) ||
          (_filterStatus == 'Completed' &&
              (a.status == 'COMPLETED' || a.status == 'HCM_ACCEPTED'));

      final d = a.scheduledDate;
      final matchFrom = _fromDate == null ||
          (d != null && !DateTime(d.year, d.month, d.day).isBefore(_fromDate!));
      final matchTo = _toDate == null ||
          (d != null && !DateTime(d.year, d.month, d.day).isAfter(_toDate!));

      return matchSearch && matchFilter && matchFrom && matchTo;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final role = auth.user!.role;
    final canAddNew = [
      UserRole.ADMIN,
      UserRole.OSD,
      UserRole.DATA_ENTRY_OPERATOR,
    ].contains(role);

    return Column(
      children: [
        _buildSearchBar(),
        _buildDateFilters(),
        _buildFilterChips(),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _filtered.isEmpty
                  ? _buildEmpty()
                  : RefreshIndicator(
                      onRefresh: _loadAppointments,
                      child: ListView.separated(
                        padding: const EdgeInsets.all(12),
                        itemCount: _filtered.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (_, i) => _AppointmentCard(
                          appointment: _filtered[i],
                          onTap: () => _openDetails(_filtered[i]),
                        ),
                      ),
                    ),
        ),
        if (canAddNew) _buildBottomActions(context),
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
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        ),
        onChanged: (v) => setState(() => _searchQuery = v),
      ),
    );
  }

  Widget _buildDateFilters() {
    String label(DateTime? d, String fallback) {
      if (d == null) return fallback;
      return '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    }

    Future<void> pick(bool from) async {
      final now = DateTime.now();
      final picked = await showDatePicker(
        context: context,
        firstDate: DateTime(now.year - 2),
        lastDate: DateTime(now.year + 2),
        initialDate: (from ? _fromDate : _toDate) ?? now,
      );
      if (picked == null) return;
      setState(() {
        if (from) {
          _fromDate = DateTime(picked.year, picked.month, picked.day);
        } else {
          _toDate = DateTime(picked.year, picked.month, picked.day);
        }
      });
    }

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 6, 12, 0),
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton.icon(
              onPressed: () => pick(true),
              icon: const Icon(Icons.event_outlined, size: 18),
              label: Text(label(_fromDate, 'From Date')),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: OutlinedButton.icon(
              onPressed: () => pick(false),
              icon: const Icon(Icons.event_available_outlined, size: 18),
              label: Text(label(_toDate, 'To Date')),
            ),
          ),
          if (_fromDate != null || _toDate != null)
            IconButton(
              tooltip: 'Clear dates',
              icon: const Icon(Icons.clear),
              onPressed: () => setState(() {
                _fromDate = null;
                _toDate = null;
              }),
            ),
        ],
      ),
    );
  }

  Future<void> _openDetails(_Appointment appointment) async {
    if (appointment.backendId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Appointment ID is missing.')),
      );
      return;
    }
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => _AppointmentDetailSheet(
        appointment: appointment,
        canAct: widget.forceApproverMode ||
            context.read<AuthService>().user?.role == UserRole.APPROVER,
      ),
    );
    await _loadAppointments();
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
              onPressed: () => context
                  .read<NavigationService>()
                  .navigateTo('new_appointment'),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: OutlinedButton.icon(
              icon: const Icon(Icons.login),
              label: const Text('Walk-in'),
              onPressed: () =>
                  context.read<NavigationService>().navigateTo('walkin'),
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFF2E7D32),
                side: const BorderSide(color: Color(0xFF2E7D32)),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8)),
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
  final VoidCallback onTap;

  const _AppointmentCard({required this.appointment, required this.onTap});

  Color _statusColor(String status) {
    if (status.contains('ACCEPTED') || status == 'COMPLETED') {
      return const Color(0xFF16A34A);
    }
    if (status.contains('PENDING') || status.contains('REVIEW')) {
      return const Color(0xFFB45309);
    }
    if (status == 'SCHEDULED') return const Color(0xFF1A237E);
    if (status.contains('REJECTED') || status.contains('CANCELLED')) {
      return const Color(0xFF991B1B);
    }
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
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
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
                      color: tc.withAlpha(26),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: tc.withAlpha(77)),
                    ),
                    child: Text(
                      appointment.agendaType,
                      style: TextStyle(
                          color: tc, fontWeight: FontWeight.bold, fontSize: 11),
                    ),
                  ),
                  if (appointment.isWalkIn) ...[
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 6, vertical: 3),
                      decoration: BoxDecoration(
                        color: const Color(0xFF006064).withAlpha(26),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: const Text(
                        'Walk-in',
                        style: TextStyle(
                            color: Color(0xFF006064),
                            fontSize: 10,
                            fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                  const Spacer(),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: sc.withAlpha(20),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      _statusLabel(appointment.status),
                      style: TextStyle(
                          color: sc, fontSize: 10, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  const Icon(Icons.person_outline,
                      size: 16, color: Color(0xFF1A237E)),
                  const SizedBox(width: 6),
                  Text(
                    appointment.applicantName,
                    style: const TextStyle(
                        fontWeight: FontWeight.w600, fontSize: 15),
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
                  Text(appointment.id,
                      style: TextStyle(fontSize: 12, color: Colors.grey[500])),
                  if (appointment.tokenNumber.isNotEmpty) ...[
                    const SizedBox(width: 12),
                    Icon(Icons.confirmation_number_outlined,
                        size: 13, color: Colors.grey[500]),
                    const SizedBox(width: 4),
                    Text(appointment.tokenNumber,
                        style:
                            TextStyle(fontSize: 12, color: Colors.grey[500])),
                  ],
                  const SizedBox(width: 12),
                  Icon(Icons.location_on_outlined,
                      size: 13, color: Colors.grey[500]),
                  const SizedBox(width: 4),
                  Text(appointment.location,
                      style: TextStyle(fontSize: 12, color: Colors.grey[500])),
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
      ),
    );
  }
}

class _AppointmentDetailSheet extends StatefulWidget {
  final _Appointment appointment;
  final bool canAct;

  const _AppointmentDetailSheet({
    required this.appointment,
    required this.canAct,
  });

  @override
  State<_AppointmentDetailSheet> createState() =>
      _AppointmentDetailSheetState();
}

class _AppointmentDetailSheetState extends State<_AppointmentDetailSheet> {
  final _decisionCtrl = TextEditingController(text: 'Decision');
  final _remarksCtrl = TextEditingController();
  bool _loading = true;
  bool _saving = false;
  Map<String, dynamic> _details = {};
  List<Map<String, dynamic>> _remarks = [];
  List<Map<String, String>> _departments = [];
  String? _departmentCode;
  int? _editingRemarkId;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadDetails();
  }

  @override
  void dispose() {
    _decisionCtrl.dispose();
    _remarksCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadDetails() async {
    final id = widget.appointment.backendId!;
    final detail = await ApiService.getAppointmentById(id);
    final remarks = await ApiService.getAppointmentRemarks(id);
    final departments = await ApiService.getReferenceData('DEPARTMENT');
    if (!mounted) return;
    setState(() {
      _details = detail ?? Map<String, dynamic>.from(widget.appointment.raw);
      _remarks = remarks;
      _departments = departments;
      _departmentCode = _details['departmentCode']?.toString();
      _loading = false;
    });
  }

  Future<void> _save() async {
    final remarks = _remarksCtrl.text.trim();
    if (remarks.isEmpty) {
      setState(() => _error = 'Enter remarks before saving.');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    final id = widget.appointment.backendId!;
    final result = _editingRemarkId == null
        ? await ApiService.addAppointmentRemark(
            id,
            remarks: remarks,
            decision: _decisionCtrl.text.trim(),
            departmentCode: _departmentCode,
          )
        : await ApiService.updateAppointmentRemark(
            id,
            _editingRemarkId!,
            remarks: remarks,
            decision: _decisionCtrl.text.trim(),
            departmentCode: _departmentCode,
          );
    if (!mounted) return;
    if (result == null) {
      setState(() {
        _saving = false;
        _error = 'Unable to save remarks. Please try again.';
      });
      return;
    }
    _remarksCtrl.clear();
    _editingRemarkId = null;
    await _loadDetails();
    if (!mounted) return;
    setState(() => _saving = false);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Remarks saved successfully.')),
    );
  }

  void _editRemark(Map<String, dynamic> remark) {
    setState(() {
      _editingRemarkId = _asInt(remark['id']);
      _remarksCtrl.text =
          (remark['hcmRemarks'] ?? remark['remarks'] ?? remark['comment'] ?? '')
              .toString();
      _decisionCtrl.text =
          (remark['decision'] ?? remark['actionTaken'] ?? 'Decision')
              .toString();
      _departmentCode = (remark['departmentCode'] ??
              remark['departmentName'] ??
              _departmentCode)
          ?.toString();
    });
  }

  static int? _asInt(dynamic value) {
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }

  String _value(String key, [String fallback = '-']) {
    final raw = _details[key] ?? widget.appointment.raw[key];
    final text = raw?.toString().trim() ?? '';
    return text.isEmpty ? fallback : text;
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.9,
      maxChildSize: 0.96,
      minChildSize: 0.5,
      builder: (context, controller) {
        return Material(
          color: const Color(0xFFF4F6FB),
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : ListView(
                  controller: controller,
                  padding: EdgeInsets.fromLTRB(
                    16,
                    16,
                    16,
                    MediaQuery.of(context).viewInsets.bottom + 24,
                  ),
                  children: [
                    Row(
                      children: [
                        const Expanded(
                          child: Text(
                            'Appointment Details',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                              color: Color(0xFF1A237E),
                            ),
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.close),
                          onPressed: () => Navigator.pop(context),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    _detailsCard(),
                    const SizedBox(height: 12),
                    _remarksHistory(),
                    if (widget.canAct) ...[
                      const SizedBox(height: 12),
                      _actionCard(),
                    ],
                  ],
                ),
        );
      },
    );
  }

  Widget _detailsCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _photo(),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.appointment.applicantName,
                        style: const TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(widget.appointment.phone),
                      const SizedBox(height: 6),
                      Text(_value('status', widget.appointment.status)),
                    ],
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            _DetailLine('Agenda', widget.appointment.agendaType),
            _DetailLine('Brief Description', widget.appointment.agendaBrief),
            _DetailLine('Appointment Date', widget.appointment.scheduledAt),
            _DetailLine('Token Number', widget.appointment.tokenNumber),
            _DetailLine('Constituency', _value('constituency')),
            _DetailLine('Booth', _value('booth')),
            _DetailLine('Part Number', _value('partNumber')),
            _DetailLine('Scheme', _value('schemeName')),
            _DetailLine(
              'Department',
              _value('departmentName', _value('departmentCode')),
            ),
          ],
        ),
      ),
    );
  }

  Widget _photo() {
    final base64Photo =
        (_details['photoBase64'] ?? _details['livePhotoBase64'])?.toString();
    final url = (_details['photoUrl'] ?? _details['livePhotoUrl'])?.toString();
    if (base64Photo != null && base64Photo.trim().isNotEmpty) {
      try {
        final raw = base64Photo.contains(',')
            ? base64Photo.split(',').last
            : base64Photo;
        return ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.memory(
            base64Decode(raw),
            width: 82,
            height: 96,
            fit: BoxFit.cover,
          ),
        );
      } catch (_) {}
    }
    if (url != null && url.trim().isNotEmpty) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.network(
          url,
          width: 82,
          height: 96,
          fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => _noPhoto(),
        ),
      );
    }
    return _noPhoto();
  }

  Widget _noPhoto() {
    return Container(
      width: 82,
      height: 96,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0xFFE8EAF6),
        borderRadius: BorderRadius.circular(8),
      ),
      child: const Icon(Icons.person_outline, color: Color(0xFF1A237E)),
    );
  }

  Widget _remarksHistory() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Remarks History',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 10),
            if (_remarks.isEmpty)
              const Text(
                'No remarks yet.',
                style: TextStyle(color: Color(0xFF64748B)),
              )
            else
              ..._remarks.map((r) {
                final text =
                    (r['hcmRemarks'] ?? r['remarks'] ?? r['comment'] ?? '')
                        .toString();
                final meta = [
                  r['departmentName'] ?? r['departmentCode'],
                  r['createdByRole'],
                  r['createdBy'],
                ]
                    .where((v) => v != null && v.toString().trim().isNotEmpty)
                    .join(' / ');
                return Container(
                  margin: const EdgeInsets.only(bottom: 10),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF8FAFC),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: const Color(0xFFE2E8F0)),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(text.isEmpty ? '-' : text),
                            if (meta.isNotEmpty) ...[
                              const SizedBox(height: 5),
                              Text(
                                meta,
                                style: const TextStyle(
                                  color: Color(0xFF64748B),
                                  fontSize: 12,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                      if (widget.canAct)
                        IconButton(
                          tooltip: 'Edit remarks',
                          icon: const Icon(Icons.edit_outlined, size: 20),
                          onPressed: () => _editRemark(r),
                        ),
                    ],
                  ),
                );
              }),
          ],
        ),
      ),
    );
  }

  Widget _actionCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextFormField(
              controller: _decisionCtrl,
              decoration: const InputDecoration(labelText: 'Decision'),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _departments.any((d) => d['code'] == _departmentCode)
                  ? _departmentCode
                  : null,
              decoration:
                  const InputDecoration(labelText: 'Forward to Department'),
              items: [
                const DropdownMenuItem(value: '', child: Text('No department')),
                for (final d in _departments)
                  DropdownMenuItem(
                    value: d['code'],
                    child: Text(d['value'] ?? d['code'] ?? ''),
                  ),
              ],
              onChanged: (v) => setState(() => _departmentCode = v),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _remarksCtrl,
              minLines: 3,
              maxLines: 5,
              decoration: const InputDecoration(
                labelText: 'Add Remarks / Notes',
                alignLabelWithHint: true,
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(_error!, style: const TextStyle(color: Color(0xFF991B1B))),
            ],
            const SizedBox(height: 14),
            ElevatedButton.icon(
              onPressed: _saving ? null : _save,
              icon: _saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Icon(_editingRemarkId == null
                      ? Icons.save_outlined
                      : Icons.edit_outlined),
              label: Text(
                _editingRemarkId == null ? 'Save Remarks' : 'Update Remarks',
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DetailLine extends StatelessWidget {
  final String label;
  final String value;

  const _DetailLine(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    final display = value.trim().isEmpty ? '-' : value;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 118,
            child: Text(
              label,
              style: const TextStyle(
                color: Color(0xFF64748B),
                fontWeight: FontWeight.w700,
                fontSize: 12,
              ),
            ),
          ),
          Expanded(child: Text(display, style: const TextStyle(fontSize: 13))),
        ],
      ),
    );
  }
}
