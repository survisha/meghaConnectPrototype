import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import '../services/notification_service.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:open_filex/open_filex.dart';
import 'package:provider/provider.dart';

import '../models/user.dart';
import '../services/api_service.dart';
import '../widgets/authenticated_photo.dart';
import '../services/auth_service.dart';
import '../services/ai_notes_cache_service.dart';
import '../services/connectivity_service.dart';
import '../services/navigation_service.dart';
import '../services/offline_ai_notes_service.dart';
import '../services/offline_repository.dart';
import '../services/sync_service.dart';
import 'new_appointment_screen.dart';

class _Appointment {
  final int? backendId;
  final String applicationId;
  final String applicantName;
  final String phone;
  final String designation;
  final String constituency;
  final String agenda;
  final String agendaBrief;
  final String type;
  final String source;
  final String status;
  final String location;
  final String createdAt;
  final DateTime? createdDate;
  final String tokenNumber;
  final bool isWalkIn;
  final String aiNotesStatus;
  final String aiNotesPreview;
  final Map<String, dynamic> raw;

  const _Appointment({
    required this.backendId,
    required this.applicationId,
    required this.applicantName,
    required this.phone,
    required this.designation,
    required this.constituency,
    required this.agenda,
    required this.agendaBrief,
    required this.type,
    required this.source,
    required this.status,
    required this.location,
    required this.createdAt,
    required this.createdDate,
    required this.tokenNumber,
    required this.aiNotesStatus,
    required this.aiNotesPreview,
    required this.raw,
    this.isWalkIn = false,
  });

  factory _Appointment.fromJson(Map<String, dynamic> raw,
      {List<Map<String, dynamic>> aiNotes = const []}) {
    final applicant = _map(raw['applicant']);
    final source = _text(raw['appointmentSource'], 'CITIZEN');
    final createdRaw = _firstText([
      raw['submittedAt'],
      raw['createdAt'],
      raw['updatedAt'],
      raw['scheduledDateTime'],
    ]);
    final created = DateTime.tryParse(createdRaw);
    final completedNote = aiNotes.firstWhere(
      (note) =>
          _text(note['status']).toUpperCase() == 'COMPLETED' &&
          _text(note['aiSummary']).isNotEmpty,
      orElse: () => const {},
    );
    final aiStatus = aiNotes.isEmpty
        ? 'NONE'
        : aiNotes.any((note) => _text(note['status']).toUpperCase() == 'FAILED')
            ? 'FAILED'
            : aiNotes.any((note) =>
                    _text(note['status']).toUpperCase() == 'PROCESSING' ||
                    _text(note['status']).toUpperCase() == 'PENDING')
                ? 'PROCESSING'
                : 'COMPLETED';

    return _Appointment(
      backendId: _asInt(raw['id'] ?? raw['appointmentId']),
      applicationId: _firstText([raw['applicationId'], raw['id']], '-'),
      applicantName: _firstText([
        raw['guestName'],
        applicant['fullName'],
        raw['applicantName'],
      ], '-'),
      phone: _firstText([
        raw['guestMobile'],
        applicant['phoneNumber'],
        raw['applicantPhone'],
        raw['applicantMobile'],
      ]),
      designation: source == 'GUEST'
          ? _firstText([
              raw['organizationName'],
              raw['guestDesignation'],
              raw['designation'],
            ])
          : _firstText([applicant['designation'], raw['designation']]),
      constituency:
          _firstText([applicant['constituency'], raw['constituency']]),
      agenda: _firstText([
        raw['agendaType'],
        raw['appointmentType'],
        raw['subject'],
        raw['reasonForAppointment'],
      ]),
      agendaBrief: _firstText([
        raw['agendaBrief'],
        raw['reasonForAppointment'],
        applicant['briefDescription'],
        raw['description'],
      ]),
      type: _firstText([raw['eventType']], 'A4'),
      source: source,
      status: _text(raw['status'], 'SUBMITTED'),
      location: _text(raw['requestedLocation'], '-'),
      createdAt: _fmtDateTime(createdRaw),
      createdDate: created,
      tokenNumber: _firstText([raw['walkInTokenNumber'], raw['tokenNumber']]),
      aiNotesStatus: aiStatus,
      aiNotesPreview: _text(completedNote['aiSummary']),
      raw: raw,
      isWalkIn: raw['isWalkIn'] == true,
    );
  }
}

class _AppointmentListSession {
  _AppointmentListSession._();

  static final _AppointmentListSession instance = _AppointmentListSession._();

  List<_Appointment> appointments = [];
  String searchQuery = '';
  String filterStatus = '';
  String filterSource = '';
  String filterType = '';
  DateTime? fromDate;
  DateTime? toDate;
  int serverPage = 0;
  int serverTotal = 0;
  double scrollOffset = 0;

  bool get hasData => appointments.isNotEmpty;

  void save({
    required List<_Appointment> appointments,
    required String searchQuery,
    required String filterStatus,
    required String filterSource,
    required String filterType,
    required DateTime? fromDate,
    required DateTime? toDate,
    required int serverPage,
    required int serverTotal,
    required double scrollOffset,
  }) {
    this.appointments = List<_Appointment>.from(appointments);
    this.searchQuery = searchQuery;
    this.filterStatus = filterStatus;
    this.filterSource = filterSource;
    this.filterType = filterType;
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.serverPage = serverPage;
    this.serverTotal = serverTotal;
    this.scrollOffset = scrollOffset;
  }

  void clear() {
    appointments = [];
    searchQuery = '';
    filterStatus = '';
    filterSource = '';
    filterType = '';
    fromDate = null;
    toDate = null;
    serverPage = 0;
    serverTotal = 0;
    scrollOffset = 0;
  }
}

class AppointmentsScreen extends StatefulWidget {
  final bool forceApproverMode;
  final bool walkInOnly;

  const AppointmentsScreen({
    super.key,
    this.forceApproverMode = false,
    this.walkInOnly = false,
  });

  @override
  State<AppointmentsScreen> createState() => _AppointmentsScreenState();
}

class _AppointmentsScreenState extends State<AppointmentsScreen> {
  static const _pageSize = 100;
  static const _statusOptions = [
    '',
    'PENDING',
    'SCHEDULED',
    // UI-only distinct value; maps to Angular's SCHEDULED backend value.
    'RESCHEDULE_FILTER',
  ];
  static const _sourceOptions = ['', 'CITIZEN', 'GUEST'];
  static const _typeOptions = ['', 'A1', 'A2', 'A3', 'A4', 'B1', 'B2'];

  final _scrollController = ScrollController();
  String _searchQuery = '';
  String _filterStatus = '';
  String _filterSource = '';
  String _filterType = '';
  DateTime? _fromDate;
  DateTime? _toDate;
  final List<_Appointment> _appointments = [];
  int _serverPage = 0;
  int _serverTotal = 0;
  bool _loading = true;
  bool _loadingMore = false;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_maybeLoadMore);
    _restoreSessionOrLoad();
  }

  @override
  void dispose() {
    _saveSession();
    _scrollController.dispose();
    super.dispose();
  }

  void _restoreSessionOrLoad() {
    final session = _AppointmentListSession.instance;
    if (session.hasData) {
      _appointments
        ..clear()
        ..addAll(session.appointments);
      _searchQuery = session.searchQuery;
      _filterStatus = session.filterStatus;
      _filterSource = session.filterSource;
      _filterType = session.filterType;
      _fromDate = session.fromDate;
      _toDate = session.toDate;
      _serverPage = session.serverPage;
      _serverTotal = session.serverTotal;
      _loading = false;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollController.hasClients) {
          _scrollController.jumpTo(session.scrollOffset);
        }
      });
      return;
    }
    _loadAppointments(refresh: true);
  }

  void _saveSession() {
    _AppointmentListSession.instance.save(
      appointments: _appointments,
      searchQuery: _searchQuery,
      filterStatus: _filterStatus,
      filterSource: _filterSource,
      filterType: _filterType,
      fromDate: _fromDate,
      toDate: _toDate,
      serverPage: _serverPage,
      serverTotal: _serverTotal,
      scrollOffset: _scrollController.hasClients ? _scrollController.offset : 0,
    );
  }

  Future<void> _loadAppointments({required bool refresh}) async {
    if (refresh) {
      AiNotesCacheService.instance.clear();
      setState(() {
        _loading = true;
        _loadError = null;
        _serverPage = 0;
      });
    } else {
      setState(() => _loadingMore = true);
    }

    final auth = context.read<AuthService>();
    final role = auth.user?.role;
    final page = refresh ? 0 : _serverPage + 1;
    final response = await _pageForRole(role, page);
    if (!mounted) return;

    var rows = _rowsFromPage(response);
    if (response['error'] == true ||
        context.read<ConnectivityService>().isOffline) {
      final cached = await OfflineRepository().cachedAppointments();
      if (!mounted) return;
      if (cached.isNotEmpty && refresh) rows = cached;
    } else {
      for (final row in rows) {
        await OfflineRepository().cacheAppointment(row);
      }
    }

    final canViewAi = _canViewAiNotes(role);
    final notesById = <int, List<Map<String, dynamic>>>{};
    if (canViewAi) {
      // TODO: Replace one-by-one loading with a backend batch AI-notes endpoint when available.
      await Future.wait(rows.take(30).map((row) async {
        final id = _asInt(row['id']);
        if (id == null) return;
        final cached = await AiNotesCacheService.instance.getOrLoad(
          id,
          force: refresh,
        );
        notesById[id] = cached.notes;
      }));
    }

    final mapped = rows
        .map((row) => _Appointment.fromJson(
              row,
              aiNotes: notesById[_asInt(row['id'])] ?? const [],
            ))
        .toList();

    setState(() {
      if (refresh) {
        _appointments
          ..clear()
          ..addAll(mapped);
      } else {
        final byId = {
          for (final item in _appointments) item.applicationId: item
        };
        for (final item in mapped) {
          byId[item.applicationId] = item;
        }
        _appointments
          ..clear()
          ..addAll(byId.values);
      }
      _serverPage = _asInt(response['number']) ?? page;
      _serverTotal = _asInt(response['totalElements']) ?? _appointments.length;
      _loadError = response['error'] == true && _appointments.isEmpty
          ? response['message']?.toString() ??
              'Failed to load appointments. Please try again.'
          : null;
      _loading = false;
      _loadingMore = false;
    });
    _saveSession();
  }

  Future<Map<String, dynamic>> _pageForRole(UserRole? role, int page) {
    if (role == UserRole.PUBLIC) return ApiService.getMyAppointments();
    if (role == UserRole.DEO) {
      return ApiService.getDeoAppointments(page: page, size: _pageSize);
    }
    if (widget.forceApproverMode ||
        role == UserRole.APPROVER ||
        role == UserRole.HCM) {
      return ApiService.getApproverAppointments(page: page, size: _pageSize);
    }
    if (role == UserRole.APPROVER) {
      return ApiService.getAppointments(
        page: page,
        size: _pageSize,
        status: 'SUBMITTED,CMO_REVIEW',
        sort: 'createdAt,desc',
      );
    }
    return ApiService.getAppointments(
      page: page,
      size: _pageSize,
      sort: 'createdAt,desc',
    );
  }

  void _maybeLoadMore() {
    if (_loading || _loadingMore || _appointments.length >= _serverTotal) {
      return;
    }
    if (_scrollController.position.extentAfter < 360) {
      _loadAppointments(refresh: false);
    }
  }

  List<_Appointment> get _filtered {
    final q = _searchQuery.trim().toLowerCase();
    return _appointments.where((a) {
      final matchesSearch = q.isEmpty ||
          a.applicantName.toLowerCase().contains(q) ||
          a.phone.contains(q) ||
          a.applicationId.toLowerCase().contains(q) ||
          a.tokenNumber.toLowerCase().contains(q);
      final matchesStatus = _filterStatus.isEmpty ||
          a.status.toUpperCase() ==
              (_filterStatus == 'RESCHEDULE_FILTER'
                  ? 'SCHEDULED'
                  : _filterStatus) ||
          (_filterStatus == 'APPROVED' && a.status == 'APPROVED');
      final matchesSource = _filterSource.isEmpty || a.source == _filterSource;
      final matchesType = _filterType.isEmpty || a.type == _filterType;
      final matchesListMode =
          widget.walkInOnly ? a.type == 'B2' : a.type != 'B2';
      final day = a.createdDate == null
          ? null
          : DateTime(
              a.createdDate!.year, a.createdDate!.month, a.createdDate!.day);
      final matchesFrom =
          _fromDate == null || (day != null && !day.isBefore(_fromDate!));
      final matchesTo =
          _toDate == null || (day != null && !day.isAfter(_toDate!));
      return matchesSearch &&
          matchesStatus &&
          matchesSource &&
          matchesType &&
          matchesListMode &&
          matchesFrom &&
          matchesTo;
    }).toList()
      ..sort((a, b) {
        final left = a.createdDate?.millisecondsSinceEpoch ?? 0;
        final right = b.createdDate?.millisecondsSinceEpoch ?? 0;
        return right.compareTo(left);
      });
  }

  @override
  Widget build(BuildContext context) {
    final role = context.watch<AuthService>().user!.role;
    final canAddNew = [
      UserRole.ADMIN,
      UserRole.APPROVER,
      UserRole.DEO,
    ].contains(role);

    return Column(
      children: [
        _SearchAndFilters(
          searchQuery: _searchQuery,
          status: _filterStatus,
          source: _filterSource,
          type: _filterType,
          fromDate: _fromDate,
          toDate: _toDate,
          onSearch: (value) => setState(() => _searchQuery = value),
          onStatus: (value) => setState(() => _filterStatus = value ?? ''),
          onSource: (value) => setState(() => _filterSource = value ?? ''),
          onType: (value) => setState(() => _filterType = value ?? ''),
          onDates: (from, to) => setState(() {
            _fromDate = from;
            _toDate = to;
          }),
        ),
        Expanded(child: _body(role)),
        if (canAddNew) _buildBottomActions(context),
      ],
    );
  }

  Widget _body(UserRole role) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_loadError != null) return _buildLoadError();
    final rows = _filtered;
    if (rows.isEmpty) return _buildEmpty();
    return RefreshIndicator(
      onRefresh: () => _loadAppointments(refresh: true),
      child: ListView.separated(
        controller: _scrollController,
        padding: const EdgeInsets.all(12),
        itemCount: rows.length + (_loadingMore ? 1 : 0),
        separatorBuilder: (_, __) => const SizedBox(height: 10),
        itemBuilder: (_, index) {
          if (index >= rows.length) {
            return const Padding(
              padding: EdgeInsets.all(16),
              child: Center(child: CircularProgressIndicator()),
            );
          }
          return _AppointmentCard(
            appointment: rows[index],
            canViewAiNotes: _canViewAiNotes(role),
            onTap: () => _openDetails(rows[index]),
          );
        },
      ),
    );
  }

  Future<void> _openDetails(_Appointment appointment) async {
    if (appointment.backendId == null) {
      _showMessage('Appointment ID is missing.');
      return;
    }
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => _AppointmentDetailsPage(appointment: appointment),
      ),
    );
    _saveSession();
  }

  Widget _buildEmpty() {
    return RefreshIndicator(
      onRefresh: () => _loadAppointments(refresh: true),
      child: ListView(
        children: [
          const SizedBox(height: 120),
          Icon(Icons.search_off, size: 56, color: Colors.grey[400]),
          const SizedBox(height: 12),
          Text(
            'No appointments found',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey[600], fontSize: 16),
          ),
        ],
      ),
    );
  }

  Widget _buildLoadError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 56, color: Color(0xFF991B1B)),
            const SizedBox(height: 12),
            Text(
              _loadError!,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF991B1B), fontSize: 15),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: () => _loadAppointments(refresh: true),
              icon: const Icon(Icons.refresh),
              label: const Text('Try Again'),
            ),
          ],
        ),
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
              onPressed: () => _openCreateAppointment(context, false),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: OutlinedButton.icon(
              icon: const Icon(Icons.login),
              label: const Text('Walk-in'),
              onPressed: () => _openCreateAppointment(context, true),
            ),
          ),
        ],
      ),
    );
  }

  void _openCreateAppointment(BuildContext context, bool walkIn) {
    if (Navigator.of(context).canPop()) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => Scaffold(
            backgroundColor: const Color(0xFFF4F6FB),
            appBar: AppBar(
              title: Text(walkIn ? 'Walk-in Appointment' : 'New Appointment'),
            ),
            body: SafeArea(
              child: NewAppointmentScreen(
                isWalkIn: walkIn,
                onViewAppointments: () {
                  Navigator.of(context).pushReplacement(
                    MaterialPageRoute(
                      builder: (_) => Scaffold(
                        backgroundColor: const Color(0xFFF4F6FB),
                        appBar: AppBar(title: const Text('Appointment List')),
                        body: const SafeArea(child: AppointmentsScreen()),
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        ),
      );
      return;
    }
    context
        .read<NavigationService>()
        .navigateTo(walkIn ? 'walkin' : 'new_appointment');
  }

  void _showMessage(String message) {
    AppNotificationService.info(message);
  }
}

class _SearchAndFilters extends StatelessWidget {
  final String searchQuery;
  final String status;
  final String source;
  final String type;
  final DateTime? fromDate;
  final DateTime? toDate;
  final ValueChanged<String> onSearch;
  final ValueChanged<String?> onStatus;
  final ValueChanged<String?> onSource;
  final ValueChanged<String?> onType;
  final void Function(DateTime?, DateTime?) onDates;

  const _SearchAndFilters({
    required this.searchQuery,
    required this.status,
    required this.source,
    required this.type,
    required this.fromDate,
    required this.toDate,
    required this.onSearch,
    required this.onStatus,
    required this.onSource,
    required this.onType,
    required this.onDates,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFFF4F6FB),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
            child: TextField(
              decoration: InputDecoration(
                hintText: 'Search by name, mobile, ID or token',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: searchQuery.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () => onSearch(''),
                      )
                    : null,
              ),
              onChanged: onSearch,
            ),
          ),
          SizedBox(
            height: 48,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              children: [
                _FilterMenu(
                  label: status.isEmpty
                      ? 'All Statuses'
                      : status == 'RESCHEDULE_FILTER'
                          ? 'Reschedule'
                          : _label(status),
                  value: status,
                  values: _AppointmentsScreenState._statusOptions,
                  labelFor: (value) => value.isEmpty
                      ? 'All Statuses'
                      : value == 'RESCHEDULE_FILTER'
                          ? 'Reschedule'
                          : _label(value),
                  onChanged: onStatus,
                ),
                _FilterMenu(
                  label: source.isEmpty ? 'All Sources' : _label(source),
                  value: source,
                  values: _AppointmentsScreenState._sourceOptions,
                  labelFor: (value) =>
                      value.isEmpty ? 'All Sources' : _label(value),
                  onChanged: onSource,
                ),
                _FilterMenu(
                  label: type.isEmpty ? 'All Types' : type,
                  value: type,
                  values: _AppointmentsScreenState._typeOptions,
                  labelFor: (value) => value.isEmpty ? 'All Types' : value,
                  onChanged: onType,
                ),
                OutlinedButton.icon(
                  onPressed: () => _pickDate(context, true),
                  icon: const Icon(Icons.event_outlined, size: 18),
                  label: Text(_dateLabel(fromDate, 'From')),
                ),
                const SizedBox(width: 8),
                OutlinedButton.icon(
                  onPressed: () => _pickDate(context, false),
                  icon: const Icon(Icons.event_available_outlined, size: 18),
                  label: Text(_dateLabel(toDate, 'To')),
                ),
                if (fromDate != null || toDate != null)
                  IconButton(
                    tooltip: 'Clear dates',
                    icon: const Icon(Icons.clear),
                    onPressed: () => onDates(null, null),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _pickDate(BuildContext context, bool from) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      firstDate: DateTime(now.year - 2),
      lastDate: DateTime(now.year + 2),
      initialDate: (from ? fromDate : toDate) ?? now,
    );
    if (picked == null) return;
    final normalized = DateTime(picked.year, picked.month, picked.day);
    onDates(from ? normalized : fromDate, from ? toDate : normalized);
  }
}

class _FilterMenu extends StatelessWidget {
  final String label;
  final String value;
  final List<String> values;
  final String Function(String) labelFor;
  final ValueChanged<String?> onChanged;

  const _FilterMenu({
    required this.label,
    required this.value,
    required this.values,
    required this.labelFor,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: PopupMenuButton<String>(
        initialValue: value,
        onSelected: onChanged,
        itemBuilder: (_) => [
          for (final item in values)
            PopupMenuItem(value: item, child: Text(labelFor(item))),
        ],
        child: Chip(
          avatar: const Icon(Icons.tune, size: 16),
          label: Text(label),
          side: const BorderSide(color: Color(0xFFE2E8F0)),
          backgroundColor: Colors.white,
        ),
      ),
    );
  }
}

class _AppointmentCard extends StatelessWidget {
  final _Appointment appointment;
  final bool canViewAiNotes;
  final VoidCallback onTap;

  const _AppointmentCard({
    required this.appointment,
    required this.canViewAiNotes,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final statusColor = _statusColor(appointment.status);
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      appointment.applicantName,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 16,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  _Chip(
                    label: _label(appointment.status),
                    color: statusColor,
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: [
                  _Chip(
                      label: appointment.type,
                      color: _typeColor(appointment.type)),
                  if (appointment.isWalkIn)
                    const _Chip(label: 'Walk-in', color: Color(0xFF006064)),
                  _MetaText(
                      icon: Icons.badge_outlined,
                      value: appointment.designation),
                  _MetaText(
                      icon: Icons.map_outlined,
                      value: appointment.constituency),
                ],
              ),
              const SizedBox(height: 10),
              _FieldText(label: 'Agenda', value: appointment.agenda),
              if (appointment.agendaBrief.isNotEmpty)
                Text(
                  appointment.agendaBrief,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style:
                      const TextStyle(color: Color(0xFF475569), fontSize: 13),
                ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 12,
                runSpacing: 6,
                children: [
                  _MetaText(icon: Icons.tag, value: appointment.applicationId),
                  if (appointment.phone.isNotEmpty)
                    _MetaText(
                        icon: Icons.phone_outlined, value: appointment.phone),
                  _MetaText(
                      icon: Icons.place_outlined, value: appointment.location),
                  _MetaText(icon: Icons.schedule, value: appointment.createdAt),
                ],
              ),
              if (canViewAiNotes) ...[
                const Divider(height: 20),
                Row(
                  children: [
                    Icon(
                      appointment.aiNotesStatus == 'COMPLETED'
                          ? Icons.auto_awesome
                          : appointment.aiNotesStatus == 'FAILED'
                              ? Icons.error_outline
                              : Icons.hourglass_empty,
                      size: 16,
                      color: appointment.aiNotesStatus == 'COMPLETED'
                          ? const Color(0xFF7C3AED)
                          : const Color(0xFF64748B),
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        appointment.aiNotesPreview.isNotEmpty
                            ? appointment.aiNotesPreview
                            : _aiStatusLabel(appointment.aiNotesStatus),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 12),
                      ),
                    ),
                    TextButton(
                      onPressed: onTap,
                      child: const Text('Actions'),
                    ),
                  ],
                ),
              ] else
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton(
                    onPressed: onTap,
                    child: const Text('Actions'),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AppointmentDetailsPage extends StatefulWidget {
  final _Appointment appointment;

  const _AppointmentDetailsPage({required this.appointment});

  @override
  State<_AppointmentDetailsPage> createState() =>
      _AppointmentDetailsPageState();
}

class _AppointmentDetailsPageState extends State<_AppointmentDetailsPage> {
  final _remarksCtrl = TextEditingController();
  final _decisionCtrl = TextEditingController();
  final _missingInfoCtrl = TextEditingController();
  final _cmoRemarksCtrl = TextEditingController();
  final _picker = ImagePicker();
  Map<String, dynamic> _details = {};
  List<Map<String, dynamic>> _documents = [];
  List<Map<String, dynamic>> _remarks = [];
  List<Map<String, dynamic>> _aiNotes = [];
  List<Map<String, String>> _departments = [];
  String? _departmentCode;
  String _cmoEventType = 'A4';
  String _cmoLocation = 'SHILLONG';
  bool _loading = true;
  bool _saving = false;
  bool _uploading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _remarksCtrl.dispose();
    _decisionCtrl.dispose();
    _missingInfoCtrl.dispose();
    _cmoRemarksCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final id = widget.appointment.backendId!;
    final detail = await ApiService.getAppointmentById(id);
    final results = await Future.wait([
      ApiService.getAppointmentDocuments(id),
      ApiService.getAppointmentRemarks(id),
      AiNotesCacheService.instance.getOrLoad(id).then((cached) => cached.notes),
      ApiService.getReferenceData('DEPARTMENT'),
    ]);
    if (!mounted) return;
    final merged = detail ?? widget.appointment.raw;
    setState(() {
      _details = merged;
      _documents = results[0];
      _remarks = results[1];
      _aiNotes = results[2];
      _departments = results[3] as List<Map<String, String>>;
      _departmentCode = _text(merged['departmentCode'] ?? merged['department']);
      _cmoEventType = _text(merged['eventType'], widget.appointment.type);
      _cmoLocation = _text(merged['requestedLocation'], 'SHILLONG');
      _missingInfoCtrl.text = _text(merged['cmoRemarks']);
      _cmoRemarksCtrl.text = _text(merged['cmoRemarks']);
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final role = context.watch<AuthService>().user!.role;
    return Scaffold(
      backgroundColor: const Color(0xFFF4F6FB),
      appBar: AppBar(title: Text(widget.appointment.applicationId)),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  _summaryCard(),
                  _section(
                    title: 'Applicant Details',
                    icon: Icons.person_outline,
                    children: [
                      _DetailLine('Name', widget.appointment.applicantName),
                      _DetailLine('Mobile', widget.appointment.phone),
                      _DetailLine(
                          'Designation', widget.appointment.designation),
                      _DetailLine(
                          'Constituency', widget.appointment.constituency),
                      _DetailLine('Address', _applicantAddress()),
                    ],
                  ),
                  _section(
                    title: 'Appointment Details',
                    icon: Icons.event_note_outlined,
                    children: [
                      _DetailLine('Agenda', widget.appointment.agenda),
                      _DetailLine(
                          'Description', widget.appointment.agendaBrief),
                      _DetailLine('Type', widget.appointment.type),
                      _DetailLine('Location', widget.appointment.location),
                      _DetailLine('Status', _label(widget.appointment.status)),
                      _DetailLine('Created At', widget.appointment.createdAt),
                      _DetailLine('Department', _departmentLabel()),
                      _DetailLine(
                          'CMO Remarks', _text(_details['cmoRemarks'], '-')),
                      _DetailLine('Approver Remarks',
                          _text(_details['approverRemarks'], '-')),
                      _DetailLine('HCM / APPROVER Remarks',
                          _text(_details['hcmRemarks'], '-')),
                    ],
                  ),
                  _documentsSection(role),
                  if (_canViewAiNotes(role)) _aiNotesSection(role),
                  _remarksSection(role),
                  if (_actionsFor(role).isNotEmpty) _actionsSection(role),
                ],
              ),
            ),
    );
  }

  Widget _summaryCard() {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
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
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: [
                      _Chip(
                        label: _label(widget.appointment.status),
                        color: _statusColor(widget.appointment.status),
                      ),
                      _Chip(
                        label: widget.appointment.type,
                        color: _typeColor(widget.appointment.type),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    widget.appointment.agendaBrief.isEmpty
                        ? widget.appointment.agenda
                        : widget.appointment.agendaBrief,
                    maxLines: 3,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: Color(0xFF475569)),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _section({
    required String title,
    required IconData icon,
    required List<Widget> children,
  }) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: ExpansionTile(
        initiallyExpanded: true,
        leading: Icon(icon),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
        childrenPadding: const EdgeInsets.fromLTRB(14, 0, 14, 14),
        children: children,
      ),
    );
  }

  Widget _documentsSection(UserRole role) {
    return _section(
      title: 'Documents',
      icon: Icons.folder_copy_outlined,
      children: [
        if (_documents.isEmpty)
          const Align(
            alignment: Alignment.centerLeft,
            child: Text('No documents attached.'),
          )
        else
          for (final doc in _documents) _documentRow(doc),
        if (_canUploadDocuments(role)) ...[
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _uploading ? null : _pickSupportingDocument,
            icon: const Icon(Icons.upload_file),
            label: Text(
                _uploading ? 'Uploading...' : 'Upload Supporting Document'),
          ),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            onPressed: _uploading ? null : _captureMeetingProof,
            icon: const Icon(Icons.photo_camera_outlined),
            label: const Text('Capture Meeting Proof'),
          ),
        ],
      ],
    );
  }

  Widget _documentRow(Map<String, dynamic> doc) {
    final fileName =
        _firstText([doc['fileName'], doc['originalFilename']], 'Document');
    final uploadedAt =
        _fmtDateTime(_text(doc['uploadedAt'] ?? doc['createdAt']));
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
        color: const Color(0xFFF8FAFC),
      ),
      child: Row(
        children: [
          const Icon(Icons.description_outlined, color: Color(0xFF475569)),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(fileName, maxLines: 2, overflow: TextOverflow.ellipsis),
                Text(
                  '${_label(_text(doc['documentType'], 'Document'))} / $uploadedAt',
                  style:
                      const TextStyle(color: Color(0xFF64748B), fontSize: 12),
                ),
              ],
            ),
          ),
          IconButton(
            tooltip: 'Download',
            icon: const Icon(Icons.download_outlined),
            onPressed: () => _downloadDocument(doc),
          ),
        ],
      ),
    );
  }

  Widget _aiNotesSection(UserRole role) {
    return _section(
      title: 'AI Notes',
      icon: Icons.auto_awesome,
      children: [
        if (_aiNotes.isEmpty)
          const Align(
            alignment: Alignment.centerLeft,
            child: Text('No AI notes are available yet.'),
          )
        else
          for (final note in _aiNotes) _aiNoteCard(note, role),
      ],
    );
  }

  Widget _aiNoteCard(Map<String, dynamic> note, UserRole role) {
    final status = _text(note['status'], 'PENDING').toUpperCase();
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
        color: Colors.white,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  _text(note['fileName'], 'Document'),
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
              _Chip(label: status, color: _statusColor(status)),
              if (_canManageAiNotes(role) && _asInt(note['documentId']) != null)
                IconButton(
                  tooltip: 'Regenerate AI notes',
                  icon: const Icon(Icons.refresh),
                  onPressed: () => _regenerateAiNotes(note),
                ),
            ],
          ),
          _NoteBlock('Summary', _text(note['aiSummary'])),
          _NoteBlock('Important Details', _text(note['importantDetails'])),
          _NoteBlock(
              'Missing or Unclear Information', _text(note['missingInfo'])),
          _NoteBlock('Risk Flags', _text(note['riskFlags'])),
          if (status == 'FAILED')
            Text(
              _text(note['errorMessage'], 'AI notes failed.'),
              style: const TextStyle(color: Color(0xFF991B1B)),
            ),
        ],
      ),
    );
  }

  Widget _remarksSection(UserRole role) {
    return _section(
      title: 'Remarks History',
      icon: Icons.history,
      children: [
        if (_remarks.isEmpty)
          const Align(
            alignment: Alignment.centerLeft,
            child: Text('No remarks yet.'),
          )
        else
          for (final remark in _remarks)
            Container(
              margin: const EdgeInsets.only(bottom: 8),
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                border: Border.all(color: const Color(0xFFE2E8F0)),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_firstText([
                    remark['hcmRemarks'],
                    remark['remarks'],
                    remark['comment']
                  ], '-')),
                  const SizedBox(height: 4),
                  Text(
                    [
                      remark['decision'],
                      remark['departmentName'] ?? remark['departmentCode'],
                      remark['createdByRole'],
                      remark['createdBy'],
                      _fmtDateTime(_text(remark['createdAt'])),
                    ]
                        .where((value) =>
                            _text(value).isNotEmpty && _text(value) != '—')
                        .join(' / '),
                    style:
                        const TextStyle(color: Color(0xFF64748B), fontSize: 12),
                  ),
                ],
              ),
            ),
        if (_canUseJtSecForwarding(role)) ...[
          const Divider(height: 24),
          TextField(
            controller: _decisionCtrl,
            decoration: const InputDecoration(labelText: 'Decision'),
          ),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            isExpanded: true,
            value: _departments.any((d) => d['code'] == _departmentCode)
                ? _departmentCode
                : null,
            decoration:
                const InputDecoration(labelText: 'Forward to Department'),
            selectedItemBuilder: (context) => [
              const Text(
                'No department',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              for (final d in _departments)
                Text(
                  d['value'] ?? d['code'] ?? '',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
            ],
            items: [
              const DropdownMenuItem(
                value: '',
                child: Text(
                  'No department',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              for (final d in _departments)
                DropdownMenuItem(
                    value: d['code'],
                    child: Text(
                      d['value'] ?? d['code'] ?? '',
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    )),
            ],
            onChanged: (value) => setState(() => _departmentCode = value),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _remarksCtrl,
            minLines: 3,
            maxLines: 5,
            decoration: const InputDecoration(
              labelText: 'Add Remarks / Notes',
              alignLabelWithHint: true,
            ),
          ),
          const SizedBox(height: 10),
          ElevatedButton.icon(
            onPressed: _saving ? null : _saveRemark,
            icon: const Icon(Icons.save_outlined),
            label: Text(_saving ? 'Saving...' : 'Save Remarks'),
          ),
        ],
      ],
    );
  }

  Widget _actionsSection(UserRole role) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Actions',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _actionsFor(role),
            ),
            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(_error!, style: const TextStyle(color: Color(0xFF991B1B))),
            ],
          ],
        ),
      ),
    );
  }

  List<Widget> _actionsFor(UserRole role) {
    final actions = <Widget>[];
    if (_canApproveOrReject(role)) {
      actions.add(_ActionButton(
        label: 'Reject',
        icon: Icons.cancel_outlined,
        color: const Color(0xFF991B1B),
        onPressed: () => _approveReject(false),
      ));
    }
    if (_canReschedule(role)) {
      actions.add(_ActionButton(
        label: 'Schedule',
        icon: Icons.event_outlined,
        onPressed: _schedule,
      ));
    }
    if (_canUseCmoActions(role)) {
      actions.add(_ActionButton(
        label: 'Missing Info',
        icon: Icons.assignment_late_outlined,
        onPressed: _sendMissingInfo,
      ));
      actions.add(_ActionButton(
        label: 'Edit Category',
        icon: Icons.edit_outlined,
        onPressed: _editCmoCategory,
      ));
      actions.add(_ActionButton(
        label: 'Send to Approver',
        icon: Icons.send_outlined,
        onPressed: _forwardToApprover,
      ));
    }
    return actions;
  }

  Future<void> _saveRemark() async {
    final text = _remarksCtrl.text.trim();
    if (text.isEmpty) {
      setState(() => _error = 'Enter remarks before saving.');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    final offline = context.read<ConnectivityService>().isOffline;
    final id = widget.appointment.backendId!;
    final result = offline
        ? null
        : await ApiService.addAppointmentRemark(
            id,
            remarks: text,
            decision: _decisionCtrl.text.trim(),
            departmentCode: _departmentCode,
          );
    if (!mounted) return;
    if (result == null) {
      final note = const OfflineAiNotesService().generateAppointmentNote(
        citizenName: widget.appointment.applicantName,
        purpose: widget.appointment.agendaBrief,
        department: _departmentCode,
        appointmentType: widget.appointment.type,
        remarks: text,
      );
      await OfflineRepository().saveAiNoteOffline(
        appointmentLocalId: widget.appointment.applicationId,
        noteText: note,
        payload: {
          'appointmentId': id,
          'remarks': text,
          'decision': _decisionCtrl.text.trim(),
          'departmentCode': _departmentCode,
        },
      );
      await OfflineRepository().enqueue(
        entityType: SyncEntityType.action,
        localEntityId: widget.appointment.applicationId,
        action: 'CREATE_REMARK',
        payload: {
          'appointmentId': id,
          'remarks': text,
          'decision': _decisionCtrl.text.trim(),
          'departmentCode': _departmentCode,
        },
      );
      if (!mounted) return;
      context.read<SyncService>().syncNow();
      _showMessage('No internet connection. Saved offline.');
    } else {
      _showMessage('Remarks saved successfully.');
    }
    _remarksCtrl.clear();
    await _load();
    if (mounted) setState(() => _saving = false);
  }

  Future<void> _approveReject(bool approve) async {
    final remarks = await _remarksDialog(
      approve ? 'Approve Appointment' : 'Reject Appointment',
    );
    if (remarks == null) return;
    setState(() => _saving = true);
    final id = widget.appointment.backendId!;
    final result = approve
        ? await ApiService.approveAppointment(id, remarks: remarks)
        : await ApiService.rejectAppointment(id, remarks: remarks);
    if (!mounted) return;
    setState(() => _saving = false);
    if (result == null) {
      _showMessage('Failed to perform action. Please try again.');
      return;
    }
    _showMessage(approve ? 'Appointment approved.' : 'Appointment rejected.');
    await _load();
  }

  Future<void> _schedule() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      firstDate: now,
      lastDate: DateTime(now.year + 2),
      initialDate: now,
    );
    if (date == null || !mounted) return;
    final time = await showTimePicker(
      context: context,
      initialTime: const TimeOfDay(hour: 10, minute: 0),
    );
    if (time == null) return;
    final scheduled =
        DateTime(date.year, date.month, date.day, time.hour, time.minute);
    setState(() => _saving = true);
    final result = await ApiService.scheduleAppointment(
      widget.appointment.backendId!,
      _toLocalDateTime(scheduled),
      30,
    );
    if (!mounted) return;
    setState(() => _saving = false);
    if (result == null) {
      _showMessage('Failed to schedule appointment.');
      return;
    }
    _showMessage('Appointment scheduled.');
    await _load();
  }

  Future<void> _sendMissingInfo() async {
    final note = await _textDialog(
      title: 'Missing Information',
      controller: _missingInfoCtrl,
      minLines: 4,
    );
    if (note == null || note.trim().isEmpty) return;
    await _submitCmoReview(
      status: widget.appointment.status,
      cmoRemarks: note.trim(),
      pendingInformation: note.trim(),
      notifyApplicant: true,
      notifyDeo: true,
      success: 'Missing information note sent.',
    );
  }

  Future<void> _editCmoCategory() async {
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => StatefulBuilder(
        builder: (context, setSheetState) {
          return Padding(
            padding: EdgeInsets.fromLTRB(
              16,
              16,
              16,
              MediaQuery.of(context).viewInsets.bottom + 16,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('CMO Category',
                    style:
                        TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  value: _cmoEventType,
                  decoration:
                      const InputDecoration(labelText: 'Appointment Category'),
                  items: const ['A1', 'A2', 'A3', 'A4', 'B1', 'B2']
                      .map((value) =>
                          DropdownMenuItem(value: value, child: Text(value)))
                      .toList(),
                  onChanged: (value) => setSheetState(
                      () => _cmoEventType = value ?? _cmoEventType),
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  value: const ['SHILLONG', 'TURA', 'DELHI', 'OTHERS']
                          .contains(_cmoLocation)
                      ? _cmoLocation
                      : 'OTHERS',
                  decoration: const InputDecoration(labelText: 'Location'),
                  items: const ['SHILLONG', 'TURA', 'DELHI', 'OTHERS']
                      .map((value) => DropdownMenuItem(
                          value: value, child: Text(_label(value))))
                      .toList(),
                  onChanged: (value) =>
                      setSheetState(() => _cmoLocation = value ?? _cmoLocation),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _cmoRemarksCtrl,
                  minLines: 3,
                  maxLines: 5,
                  decoration: const InputDecoration(labelText: 'CMO Remarks'),
                ),
                const SizedBox(height: 14),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton.icon(
                    icon: const Icon(Icons.send_outlined),
                    label: const Text('Save & Forward'),
                    onPressed: () => Navigator.pop(context, true),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
    if (saved != true) return;
    await _submitCmoReview(
      status: 'APPROVER_REVIEW',
      eventType: _cmoEventType,
      requestedLocation: _cmoLocation,
      cmoRemarks: _cmoRemarksCtrl.text.trim(),
      success: 'Category updated and sent to approver.',
    );
  }

  Future<void> _forwardToApprover() async {
    await _submitCmoReview(
      status: 'APPROVER_REVIEW',
      eventType: widget.appointment.type,
      requestedLocation: widget.appointment.location,
      cmoRemarks: 'Forwarded to approver',
      success: 'Appointment sent to approver.',
    );
  }

  Future<void> _submitCmoReview({
    required String status,
    String? eventType,
    String? requestedLocation,
    String? cmoRemarks,
    String? pendingInformation,
    bool notifyApplicant = false,
    bool notifyDeo = false,
    required String success,
  }) async {
    setState(() => _saving = true);
    final result = await ApiService.submitCmoReview(
      appointmentId: widget.appointment.backendId!,
      eventType: eventType,
      requestedLocation: requestedLocation,
      cmoRemarks: cmoRemarks,
      pendingInformation: pendingInformation,
      status: status,
      notifyApplicant: notifyApplicant,
      notifyDeo: notifyDeo,
    );
    if (!mounted) return;
    setState(() => _saving = false);
    if (result == null) {
      _showMessage('Failed to perform action. Please try again.');
      return;
    }
    _showMessage(success);
    await _load();
  }

  Future<void> _pickSupportingDocument() async {
    final result = await FilePicker.platform.pickFiles();
    final file = result?.files.single;
    if (file?.path == null) return;
    await _uploadFile(file!.path!, fileName: file.name);
  }

  Future<void> _captureMeetingProof() async {
    try {
      final image =
          await _picker.pickImage(source: ImageSource.camera, imageQuality: 80);
      if (image == null) return;
      if (!mounted) return;
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (_) => AlertDialog(
          title: const Text('Upload Proof Photo?'),
          content: Image.file(File(image.path), fit: BoxFit.cover),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Retake')),
            ElevatedButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Upload')),
          ],
        ),
      );
      if (confirmed == true) {
        await _uploadFile(image.path, fileName: _meetingProofFileName());
      }
    } catch (_) {
      _showMessage('Camera permission denied or photo capture failed.');
    }
  }

  Future<void> _uploadFile(String path, {String? fileName}) async {
    setState(() => _uploading = true);
    final offline = context.read<ConnectivityService>().isOffline;
    final result = offline
        ? null
        : await ApiService.uploadSupportingDocument(
            widget.appointment.backendId!,
            path,
            fileName: fileName,
          );
    if (!mounted) return;
    setState(() => _uploading = false);
    if (result == null) {
      await OfflineRepository().enqueue(
        entityType: SyncEntityType.action,
        localEntityId: widget.appointment.applicationId,
        action: 'UPLOAD_SUPPORTING_DOCUMENT',
        payload: {
          'appointmentId': widget.appointment.backendId,
          'filePath': path,
          'fileName': fileName,
        },
      );
      if (!mounted) return;
      context.read<SyncService>().syncNow();
      _showMessage('No internet connection. Upload queued for sync.');
      return;
    }
    _showMessage('Document uploaded successfully.');
    await _load();
  }

  Future<void> _downloadDocument(Map<String, dynamic> doc) async {
    final id = _asInt(doc['id']);
    if (id == null) {
      _showMessage('Failed to load document.');
      return;
    }
    final bytes = await ApiService.downloadDocumentBytes(id);
    if (bytes == null) {
      _showMessage('Failed to download document.');
      return;
    }
    final dir = await getTemporaryDirectory();
    final fileName =
        _firstText([doc['fileName'], doc['originalFilename']], 'document-$id');
    final path = '${dir.path}${Platform.pathSeparator}$fileName';
    final file = await File(path).writeAsBytes(bytes);
    if (!mounted) return;
    final result = await OpenFilex.open(file.path);
    if (!mounted) return;
    if (result.type != ResultType.done) {
      _showMessage(
        result.type == ResultType.noAppToOpen
            ? 'No installed app can open this document type.'
            : 'Unable to open the downloaded document.',
      );
    }
  }

  Future<void> _regenerateAiNotes(Map<String, dynamic> note) async {
    final documentId = _asInt(note['documentId']);
    if (documentId == null) return;
    final result = await ApiService.regenerateAiNotes(documentId);
    if (result == null) {
      _showMessage('Failed to refresh AI notes.');
      return;
    }
    AiNotesCacheService.instance.invalidate(widget.appointment.backendId!);
    _showMessage('AI notes refresh started.');
    await _load();
  }

  Widget _photo() {
    final applicant = _map(_details['applicant']);
    final base64Photo = _firstText([
      applicant['livePhotoBase64'],
      applicant['photoBase64'],
      _details['livePhotoBase64'],
      _details['photoBase64'],
    ]);
    final url = _firstText([
      applicant['photoUrl'],
      _details['photoUrl'],
      _details['livePhotoUrl'],
    ]);
    if (base64Photo.isNotEmpty) {
      try {
        final raw = base64Photo.contains(',')
            ? base64Photo.split(',').last
            : base64Photo;
        return ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.memory(base64Decode(raw),
              width: 82, height: 96, fit: BoxFit.cover),
        );
      } catch (_) {}
    }
    if (url.isNotEmpty) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: SizedBox(
          width: 82,
          height: 96,
          child: AuthenticatedPhoto(
            source: url,
            fallback: _noPhoto(),
          ),
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

  Future<String?> _remarksDialog(String title) {
    final controller = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          minLines: 3,
          maxLines: 5,
          decoration: const InputDecoration(
            labelText: 'Remarks / Notes',
            alignLabelWithHint: true,
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel')),
          ElevatedButton(
              onPressed: () => Navigator.pop(context, controller.text.trim()),
              child: const Text('Confirm')),
        ],
      ),
    );
  }

  Future<String?> _textDialog({
    required String title,
    required TextEditingController controller,
    int minLines = 3,
  }) {
    return showDialog<String>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          minLines: minLines,
          maxLines: minLines + 2,
          decoration: const InputDecoration(alignLabelWithHint: true),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel')),
          ElevatedButton(
              onPressed: () => Navigator.pop(context, controller.text.trim()),
              child: const Text('Send')),
        ],
      ),
    );
  }

  bool _canApproveOrReject(UserRole role) =>
      _canUseApproverActions(role) &&
      !widget.appointment.isWalkIn &&
      widget.appointment.status == 'PENDING';

  bool _canReschedule(UserRole role) =>
      _canUseApproverActions(role) &&
      !widget.appointment.isWalkIn &&
      ['PENDING', 'SCHEDULED'].contains(widget.appointment.status);

  bool _canUseCmoActions(UserRole role) =>
      [UserRole.HCM, UserRole.ADMIN, UserRole.APPROVER].contains(role) &&
      ['SUBMITTED', 'CMO_REVIEW'].contains(widget.appointment.status);

  bool _canUseJtSecForwarding(UserRole role) =>
      [UserRole.HCM, UserRole.ADMIN, UserRole.APPROVER].contains(role);

  bool _canUploadDocuments(UserRole role) => [
        UserRole.HCM,
        UserRole.ADMIN,
        UserRole.APPROVER,
        UserRole.DEO
      ].contains(role);

  String _applicantAddress() {
    final applicant = _map(_details['applicant']);
    return _firstText([
      applicant['addressLine'],
      applicant['fullAddress'],
      applicant['address'],
      _details['guestAddress'],
      _details['address'],
    ], '-');
  }

  String _departmentLabel() {
    final code =
        _firstText([_details['departmentCode'], _details['department']]);
    return _departments.firstWhere(
          (department) => department['code'] == code,
          orElse: () => {'value': code},
        )['value'] ??
        '-';
  }

  String _meetingProofFileName() {
    final stamp = DateTime.now().toIso8601String().replaceAll(':', '-');
    return '${widget.appointment.applicationId}-meeting-proof-$stamp.jpg';
  }

  void _showMessage(String message) {
    AppNotificationService.info(message);
  }
}

class _ActionButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final Color? color;
  final VoidCallback onPressed;

  const _ActionButton({
    required this.label,
    required this.icon,
    required this.onPressed,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 18),
      label: Text(label),
      style: OutlinedButton.styleFrom(foregroundColor: color),
    );
  }
}

class _Chip extends StatelessWidget {
  final String label;
  final Color color;

  const _Chip({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withAlpha(24),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withAlpha(72)),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _MetaText extends StatelessWidget {
  final IconData icon;
  final String value;

  const _MetaText({required this.icon, required this.value});

  @override
  Widget build(BuildContext context) {
    final display = value.trim().isEmpty ? '-' : value.trim();
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 260),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: const Color(0xFF64748B)),
          const SizedBox(width: 4),
          Flexible(
            child: Text(
              display,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 12, color: Color(0xFF64748B)),
            ),
          ),
        ],
      ),
    );
  }
}

class _FieldText extends StatelessWidget {
  final String label;
  final String value;

  const _FieldText({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    if (value.trim().isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Text.rich(
        TextSpan(
          children: [
            TextSpan(
              text: '$label: ',
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
            TextSpan(text: value),
          ],
        ),
        maxLines: 2,
        overflow: TextOverflow.ellipsis,
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
    final display = value.trim().isEmpty ? '-' : value.trim();
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
                fontWeight: FontWeight.w800,
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

class _NoteBlock extends StatelessWidget {
  final String title;
  final String text;

  const _NoteBlock(this.title, this.text);

  @override
  Widget build(BuildContext context) {
    if (text.trim().isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
          const SizedBox(height: 2),
          Text(text),
        ],
      ),
    );
  }
}

bool _canViewAiNotes(UserRole? role) => [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.APPROVER,
    ].contains(role);

bool _canManageAiNotes(UserRole role) => _canViewAiNotes(role);

bool _canUseApproverActions(UserRole role) => [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.APPROVER,
    ].contains(role);

List<Map<String, dynamic>> _rowsFromPage(Map<String, dynamic> response) {
  final content = response['content'];
  return content is List
      ? content
          .whereType<Map>()
          .map((row) => Map<String, dynamic>.from(row))
          .toList()
      : <Map<String, dynamic>>[];
}

Map<String, dynamic> _map(dynamic value) {
  return value is Map ? Map<String, dynamic>.from(value) : {};
}

int? _asInt(dynamic value) {
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '');
}

String _text(dynamic value, [String fallback = '']) {
  final text = value?.toString().trim() ?? '';
  return text.isEmpty ? fallback : text;
}

String _firstText(List<dynamic> values, [String fallback = '']) {
  for (final value in values) {
    final text = _text(value);
    if (text.isNotEmpty) return text;
  }
  return fallback;
}

String _fmtDateTime(String? raw) {
  final value = raw?.trim() ?? '';
  if (value.isEmpty) return '-';
  final dt = DateTime.tryParse(value);
  if (dt == null) return value;
  final local = dt.toLocal();
  return '${local.day.toString().padLeft(2, '0')}-'
      '${_month(local.month)}-${local.year} '
      '${local.hour.toString().padLeft(2, '0')}:'
      '${local.minute.toString().padLeft(2, '0')}';
}

String _dateLabel(DateTime? date, String fallback) {
  if (date == null) return fallback;
  return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}

String _toLocalDateTime(DateTime date) {
  String two(int value) => value.toString().padLeft(2, '0');
  return '${date.year}-${two(date.month)}-${two(date.day)}T'
      '${two(date.hour)}:${two(date.minute)}:00';
}

String _month(int month) {
  const labels = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
  ];
  return labels[month - 1];
}

String _label(String value) {
  final text = value.replaceAll('_', ' ').trim();
  if (text.isEmpty) return '-';
  return text
      .split(RegExp(r'\s+'))
      .map((word) => word.isEmpty
          ? word
          : '${word[0].toUpperCase()}${word.substring(1).toLowerCase()}')
      .join(' ');
}

String _aiStatusLabel(String status) {
  switch (status) {
    case 'COMPLETED':
      return 'AI notes ready';
    case 'PROCESSING':
      return 'AI notes processing';
    case 'FAILED':
      return 'Unable to load AI notes';
    default:
      return 'No AI notes';
  }
}

Color _statusColor(String status) {
  final normalized = status.toUpperCase();
  if (normalized.contains('ACCEPTED') ||
      normalized == 'COMPLETED' ||
      normalized == 'APPROVED') {
    return const Color(0xFF15803D);
  }
  if (normalized.contains('PENDING') ||
      normalized.contains('REVIEW') ||
      normalized == 'SUBMITTED' ||
      normalized == 'PROCESSING') {
    return const Color(0xFFB45309);
  }
  if (normalized == 'SCHEDULED' ||
      normalized == 'SCHEDULED_FOR_PUBLIC_DARBAR') {
    return const Color(0xFF1D4ED8);
  }
  if (normalized.contains('REJECTED') ||
      normalized.contains('CANCELLED') ||
      normalized == 'FAILED') {
    return const Color(0xFF991B1B);
  }
  return const Color(0xFF475569);
}

Color _typeColor(String type) {
  const colors = {
    'A1': Color(0xFF1565C0),
    'A2': Color(0xFF2E7D32),
    'A3': Color(0xFFF57F17),
    'A4': Color(0xFFC62828),
    'B1': Color(0xFF4527A0),
    'B2': Color(0xFF006064),
  };
  return colors[type] ?? const Color(0xFF475569);
}
