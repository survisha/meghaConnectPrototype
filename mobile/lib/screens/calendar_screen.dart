import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../models/user.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/connectivity_service.dart';

enum _CalendarView { day, week, month }

class _Event {
  final int? id;
  final String title;
  final String type;
  final String location;
  final DateTime start;
  final DateTime end;
  final String? description;
  final int? travelMinutes;
  final bool isConflict;
  final String? shortNotes;
  final String sourceType;
  final int? appointmentId;
  final Map<String, dynamic>? appointment;
  final List<Map<String, dynamic>> appointments;
  final bool pendingSync;

  const _Event({
    required this.id,
    required this.title,
    required this.type,
    required this.location,
    required this.start,
    required this.end,
    this.description,
    this.travelMinutes,
    this.isConflict = false,
    this.shortNotes,
    this.sourceType = 'SCHEDULE_EVENT',
    this.appointmentId,
    this.appointment,
    this.appointments = const [],
    this.pendingSync = false,
  });

  factory _Event.fromJson(Map<String, dynamic> m) {
    final start = DateTime.tryParse(_text(m['startTime'])) ?? DateTime.now();
    final end = DateTime.tryParse(_text(m['endTime'])) ??
        start.add(const Duration(hours: 1));
    final rows = m['appointments'] is List
        ? (m['appointments'] as List)
            .whereType<Map>()
            .map((row) => Map<String, dynamic>.from(row))
            .toList()
        : <Map<String, dynamic>>[];
    return _Event(
      id: (m['id'] as num?)?.toInt(),
      title: _text(m['title'], 'Untitled Event'),
      type: _text(m['eventType'], 'A4'),
      location: _text(m['location'], 'SHILLONG'),
      start: start,
      end: end,
      description: _textOrNull(m['description']),
      travelMinutes: (m['travelTimeMinutes'] as num?)?.toInt(),
      isConflict: m['isConflict'] == true,
      shortNotes: _textOrNull(m['shortNotes']),
      sourceType: _text(m['sourceType'], 'SCHEDULE_EVENT'),
      appointmentId: (m['appointmentId'] as num?)?.toInt(),
      appointment: m['appointment'] is Map
          ? Map<String, dynamic>.from(m['appointment'] as Map)
          : null,
      appointments: rows,
    );
  }

  Map<String, dynamic> toPayload() => {
        'title': title,
        'eventType': type,
        'startTime': _localIso(start),
        'endTime': _localIso(end),
        'location': location,
        'travelTimeMinutes': travelMinutes,
        'description': description,
        'shortNotes': shortNotes,
        'isConflict': isConflict,
      };
}

const _typeDescriptions = {
  'A1': 'Cabinet / Union Minister / Media / Flight',
  'A2': 'Event / Public Programme',
  'A3': 'File Clearing / Birthday',
  'A4': 'Individual Appointment',
  'B1': 'Public Durbar',
  'B2': 'Public Walk-in',
};

const _fallbackTypes = [
  {'code': 'A1', 'value': 'A1 - Cabinet / Union Minister / Media / Flight'},
  {'code': 'A2', 'value': 'A2 - Event / Public Programme'},
  {'code': 'A3', 'value': 'A3 - File Clearing / Birthday'},
  {'code': 'A4', 'value': 'A4 - Individual Appointment'},
  {'code': 'B1', 'value': 'B1 - Public Durbar'},
  {'code': 'B2', 'value': 'B2 - Public Walk-in'},
];

const _locations = [
  {'code': 'SHILLONG', 'value': 'Shillong'},
  {'code': 'TURA', 'value': 'Tura'},
  {'code': 'DELHI', 'value': 'Delhi'},
  {'code': 'OTHERS', 'value': 'Others'},
];

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  List<_Event> _events = [];
  List<Map<String, String>> _eventTypes = _fallbackTypes;
  _CalendarView _view = _CalendarView.day;
  DateTime _selectedDate = _startOfDay(DateTime.now());
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadInitial();
  }

  Future<void> _loadInitial() async {
    await Future.wait([_loadEventTypes(), _loadEvents()]);
  }

  Future<void> _loadEventTypes() async {
    final values = await ApiService.getReferenceData('APPOINMENT_TYPES');
    if (!mounted || values.isEmpty) return;
    setState(() => _eventTypes = values);
  }

  DateTime get _rangeStart {
    if (_view == _CalendarView.day) return _startOfDay(_selectedDate);
    if (_view == _CalendarView.week) return _startOfWeek(_selectedDate);
    return DateTime(_selectedDate.year, _selectedDate.month, 1);
  }

  DateTime get _rangeEnd {
    if (_view == _CalendarView.day) {
      return _startOfDay(_selectedDate).add(const Duration(days: 1));
    }
    if (_view == _CalendarView.week) {
      return _startOfWeek(_selectedDate).add(const Duration(days: 7));
    }
    return DateTime(_selectedDate.year, _selectedDate.month + 1, 1);
  }

  Future<void> _loadEvents() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final list =
        await ApiService.getScheduleEvents(start: _rangeStart, end: _rangeEnd);
    if (!mounted) return;
    setState(() {
      _events = list
          .whereType<Map>()
          .map((row) => _Event.fromJson(Map<String, dynamic>.from(row)))
          .where((event) => event.end.isAfter(_rangeStart))
          .where((event) => event.start.isBefore(_rangeEnd))
          .toList()
        ..sort((a, b) => a.start.compareTo(b.start));
      _loading = false;
    });
  }

  List<_Event> _eventsForDay(DateTime date) =>
      _events.where((event) => _isSameDay(event.start, date)).toList()
        ..sort((a, b) => a.start.compareTo(b.start));

  bool get _canManage {
    final role = context.watch<AuthService>().user!.role;
    return {
      UserRole.ADMIN,
      UserRole.OSD,
      UserRole.HCM,
      UserRole.CMO_OFFICER,
      UserRole.APPROVER,
    }.contains(role);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          _buildHeader(),
          if (_error != null) _errorBanner(_error!),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: _loadEvents,
                    child: ListView(
                      padding: const EdgeInsets.all(12),
                      children: [
                        _buildCalendarSurface(),
                        const SizedBox(height: 12),
                        _buildEventList(),
                        const SizedBox(height: 80),
                      ],
                    ),
                  ),
          ),
        ],
      ),
      floatingActionButton: _canManage
          ? FloatingActionButton.extended(
              onPressed: () => _showEventForm(),
              icon: const Icon(Icons.add),
              label: const Text('Add Event'),
            )
          : null,
    );
  }

  Widget _buildHeader() {
    return Container(
      color: const Color(0xFF1A237E),
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 10),
      child: Column(
        children: [
          Row(
            children: [
              const Icon(Icons.event, color: Colors.white),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  _titleForView(),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              IconButton(
                tooltip: 'Previous',
                color: Colors.white,
                onPressed: () => _shiftDate(-1),
                icon: const Icon(Icons.chevron_left),
              ),
              IconButton(
                tooltip: 'Next',
                color: Colors.white,
                onPressed: () => _shiftDate(1),
                icon: const Icon(Icons.chevron_right),
              ),
              IconButton(
                tooltip: 'Refresh',
                color: Colors.white,
                onPressed: _loadEvents,
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          const SizedBox(height: 8),
          SegmentedButton<_CalendarView>(
            style: ButtonStyle(
              foregroundColor: WidgetStateProperty.resolveWith(
                (states) => states.contains(WidgetState.selected)
                    ? const Color(0xFF1A237E)
                    : Colors.white,
              ),
              backgroundColor: WidgetStateProperty.resolveWith(
                (states) => states.contains(WidgetState.selected)
                    ? Colors.white
                    : Colors.white.withAlpha(26),
              ),
            ),
            segments: const [
              ButtonSegment(value: _CalendarView.day, label: Text('Day')),
              ButtonSegment(value: _CalendarView.week, label: Text('Week')),
              ButtonSegment(value: _CalendarView.month, label: Text('Month')),
            ],
            selected: {_view},
            onSelectionChanged: (value) async {
              setState(() => _view = value.first);
              await _loadEvents();
            },
          ),
        ],
      ),
    );
  }

  Widget _buildCalendarSurface() {
    if (_view == _CalendarView.month) return _buildMonthView();
    if (_view == _CalendarView.week) return _buildWeekView();
    return _buildDayTimeline();
  }

  Widget _buildWeekView() {
    final start = _startOfWeek(_selectedDate);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const _SectionTitle(icon: Icons.view_week, title: 'Week View'),
            const SizedBox(height: 10),
            Row(
              children: [
                for (var i = 0; i < 7; i++)
                  Expanded(
                    child: _DayChip(
                      date: start.add(Duration(days: i)),
                      count: _eventsForDay(start.add(Duration(days: i))).length,
                      selected: _isSameDay(
                          start.add(Duration(days: i)), _selectedDate),
                      disabled: _isPastDay(start.add(Duration(days: i))),
                      onTap: () {
                        if (_isPastDay(start.add(Duration(days: i)))) {
                          _snack(
                              'Previous dates cannot be selected for scheduling.',
                              success: false);
                          return;
                        }
                        setState(() {
                          _selectedDate = start.add(Duration(days: i));
                          _view = _CalendarView.day;
                        });
                        _loadEvents();
                      },
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMonthView() {
    final first = DateTime(_selectedDate.year, _selectedDate.month, 1);
    final leading = first.weekday - 1;
    final gridStart = first.subtract(Duration(days: leading));
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const _SectionTitle(
                icon: Icons.calendar_month, title: 'Month View'),
            const SizedBox(height: 10),
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: 42,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 7,
                childAspectRatio: 0.86,
                crossAxisSpacing: 4,
                mainAxisSpacing: 4,
              ),
              itemBuilder: (_, i) {
                final date = gridStart.add(Duration(days: i));
                final currentMonth = date.month == _selectedDate.month;
                final count = _eventsForDay(date).length;
                final selected = _isSameDay(date, _selectedDate);
                final disabled = _isPastDay(date);
                return InkWell(
                  onTap: () {
                    if (disabled) {
                      _snack(
                          'Previous dates cannot be selected for scheduling.',
                          success: false);
                      return;
                    }
                    setState(() {
                      _selectedDate = date;
                      _view = _CalendarView.day;
                    });
                    _loadEvents();
                  },
                  borderRadius: BorderRadius.circular(8),
                  child: Container(
                    padding: const EdgeInsets.all(5),
                    decoration: BoxDecoration(
                      color: selected
                          ? const Color(0xFFE8EAF6)
                          : count > 0
                              ? _typeColor(_eventsForDay(date).first.type)
                                  .withAlpha(22)
                              : null,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: selected
                            ? const Color(0xFF1A237E)
                            : const Color(0xFFE5E7EB),
                      ),
                    ),
                    child: Column(
                      children: [
                        Text(
                          '${date.day}',
                          style: TextStyle(
                            color: currentMonth
                                ? disabled
                                    ? const Color(0xFF9CA3AF)
                                    : const Color(0xFF111827)
                                : const Color(0xFF9CA3AF),
                            fontWeight: FontWeight.w800,
                            fontSize: 12,
                            decoration: disabled
                                ? TextDecoration.lineThrough
                                : TextDecoration.none,
                          ),
                        ),
                        const Spacer(),
                        if (count > 0)
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 5, vertical: 2),
                            decoration: BoxDecoration(
                              color: _typeColor(_eventsForDay(date).first.type),
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(
                              '$count',
                              style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 10,
                                  fontWeight: FontWeight.w800),
                            ),
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDayTimeline() {
    final events = _eventsForDay(_selectedDate);
    const hours = [
      '08:00',
      '09:00',
      '10:00',
      '11:00',
      '12:00',
      '13:00',
      '14:00',
      '15:00',
      '16:00',
      '17:00',
      '18:00',
      '19:00',
      '20:00',
    ];
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _SectionTitle(
              icon: Icons.event_note,
              title: '${_formatDate(_selectedDate)} - Daily Schedule',
            ),
            const SizedBox(height: 8),
            for (final hour in hours)
              _HourRow(
                hour: hour,
                events: events
                    .where((event) =>
                        event.start.hour == int.parse(hour.substring(0, 2)))
                    .toList(),
                colorForType: _typeColor,
                onTap: _showEventDetail,
                onLongPress: _canManage ? _showEventForm : null,
              ),
            if (events.isEmpty)
              const Padding(
                padding: EdgeInsets.all(18),
                child: Center(
                  child: Text(
                    'No schedule events or scheduled appointments for this date.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Color(0xFF64748B)),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildEventList() {
    final events = _eventsForDay(_selectedDate);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _SectionTitle(
              icon: Icons.list_alt,
              title: '${events.length} event(s) on selected date',
            ),
            const SizedBox(height: 8),
            if (events.isEmpty)
              const Padding(
                padding: EdgeInsets.all(12),
                child: Text('No events found.',
                    style: TextStyle(color: Color(0xFF64748B))),
              )
            else
              for (final event in events)
                _EventTile(
                  event: event,
                  color: _typeColor(event.type),
                  onTap: () => _showEventDetail(event),
                  onEdit: _canManage && event.sourceType != 'APPOINTMENT'
                      ? () => _showEventForm(event)
                      : null,
                ),
          ],
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

  void _showEventDetail(_Event event) {
    final color = _typeColor(event.type);
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.72,
        minChildSize: 0.35,
        maxChildSize: 0.92,
        expand: false,
        builder: (_, controller) => ListView(
          controller: controller,
          padding: const EdgeInsets.all(20),
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: 18),
                decoration: BoxDecoration(
                  color: Colors.grey[300],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _Pill('${event.type} - ${_eventTypeLabel(event.type)}', color),
                _Pill(
                    event.sourceType == 'APPOINTMENT'
                        ? 'Appointment'
                        : event.pendingSync
                            ? 'Pending Sync'
                            : 'Schedule Event',
                    event.pendingSync ? const Color(0xFFB45309) : color),
                if (event.isConflict)
                  const _Pill('Conflict', Color(0xFF991B1B)),
              ],
            ),
            const SizedBox(height: 12),
            Text(event.title,
                style:
                    const TextStyle(fontSize: 19, fontWeight: FontWeight.w900)),
            if ((event.description ?? '').isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(event.description!,
                  style: const TextStyle(color: Color(0xFF4B5563))),
            ],
            const Divider(height: 26),
            _DetailRow(Icons.access_time,
                '${_formatDateTime(event.start)} - ${_formatDateTime(event.end)}'),
            _DetailRow(Icons.location_on_outlined, event.location),
            _DetailRow(
                Icons.directions_car_outlined,
                event.travelMinutes == null
                    ? '-'
                    : '${event.travelMinutes} minutes'),
            if ((event.shortNotes ?? '').isNotEmpty) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFEFF6FF),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(event.shortNotes!,
                    style: const TextStyle(color: Color(0xFF1E40AF))),
              ),
            ],
            if (event.appointment != null) ...[
              const Divider(height: 26),
              const _SectionTitle(icon: Icons.assignment, title: 'Appointment'),
              const SizedBox(height: 8),
              _MapRow('Application ID', event.appointment!['applicationId']),
              _MapRow('Status', _statusLabel(event.appointment!['status'])),
              _MapRow('Department', event.appointment!['department']),
              _MapRow('Subject', event.appointment!['subject']),
              _MapRow('Agenda', event.appointment!['agendaType']),
            ],
            if (event.appointments.length > 1) ...[
              const Divider(height: 26),
              const _SectionTitle(
                  icon: Icons.groups, title: 'Appointments in This Event'),
              const SizedBox(height: 8),
              for (final appointment in event.appointments)
                _AppointmentAssignmentRow(
                  appointment: appointment,
                  onRemove: _canManage && event.id != null
                      ? () => _removeAssignedAppointment(event, appointment)
                      : null,
                ),
            ],
            const SizedBox(height: 18),
            if (_canManage && event.sourceType != 'APPOINTMENT')
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        Navigator.pop(ctx);
                        _showEventForm(event);
                      },
                      icon: const Icon(Icons.edit_outlined),
                      label: const Text('Edit'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        Navigator.pop(ctx);
                        _confirmDelete(event);
                      },
                      icon: const Icon(Icons.delete_outline),
                      label: const Text('Delete'),
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _showEventForm([_Event? existing]) async {
    final titleCtrl = TextEditingController(text: existing?.title ?? '');
    final descCtrl = TextEditingController(text: existing?.description ?? '');
    final notesCtrl = TextEditingController(text: existing?.shortNotes ?? '');
    final travelCtrl =
        TextEditingController(text: existing?.travelMinutes?.toString() ?? '');
    String type = existing?.type ?? (_eventTypes.first['code'] ?? 'A4');
    String location = existing?.location ?? 'SHILLONG';
    final defaultDate =
        _isPastDay(_selectedDate) ? _startOfDay(DateTime.now()) : _selectedDate;
    DateTime startDate = existing?.start ?? defaultDate;
    DateTime endDate = existing?.end ?? defaultDate;
    TimeOfDay startTime = TimeOfDay.fromDateTime(
        existing?.start ?? _defaultStartDateTime(defaultDate));
    TimeOfDay endTime = TimeOfDay.fromDateTime(existing?.end ??
        _defaultStartDateTime(defaultDate).add(const Duration(hours: 1)));
    bool saving = false;

    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) {
          Future<void> pickDate(bool start) async {
            final picked = await showDatePicker(
              context: ctx,
              initialDate: _isPastDay(start ? startDate : endDate)
                  ? _startOfDay(DateTime.now())
                  : (start ? startDate : endDate),
              firstDate: _startOfDay(DateTime.now()),
              lastDate: DateTime.now().add(const Duration(days: 730)),
            );
            if (picked == null) return;
            setLocalState(() {
              if (start) {
                startDate = picked;
                if (endDate.isBefore(startDate)) endDate = picked;
              } else {
                endDate = picked;
              }
            });
          }

          Future<void> pickTime(bool start) async {
            final picked = await showTimePicker(
              context: ctx,
              initialTime: start ? startTime : endTime,
            );
            if (picked == null) return;
            setLocalState(() {
              if (start) {
                startTime = picked;
              } else {
                endTime = picked;
              }
            });
          }

          return AlertDialog(
            title: Text(existing == null ? 'Add New Event' : 'Edit Event'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: titleCtrl,
                    decoration:
                        const InputDecoration(labelText: 'Event Title *'),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: type,
                    isExpanded: true,
                    decoration:
                        const InputDecoration(labelText: 'Event Type *'),
                    items: [
                      for (final option in _eventTypes)
                        DropdownMenuItem(
                          value: option['code'],
                          child: Text(
                            option['value'] ?? option['code'] ?? '',
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                    onChanged: (value) =>
                        setLocalState(() => type = value ?? type),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => pickDate(true),
                          icon: const Icon(Icons.date_range),
                          label: Text(_dateInput(startDate)),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => pickTime(true),
                          icon: const Icon(Icons.schedule),
                          label: Text(startTime.format(ctx)),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => pickDate(false),
                          icon: const Icon(Icons.date_range),
                          label: Text(_dateInput(endDate)),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => pickTime(false),
                          icon: const Icon(Icons.schedule),
                          label: Text(endTime.format(ctx)),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: location,
                    decoration: const InputDecoration(labelText: 'Location *'),
                    items: [
                      for (final loc in _locations)
                        DropdownMenuItem(
                          value: loc['code'],
                          child: Text(loc['value'] ?? loc['code'] ?? ''),
                        ),
                    ],
                    onChanged: (value) =>
                        setLocalState(() => location = value ?? location),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: descCtrl,
                    maxLines: 2,
                    decoration: const InputDecoration(labelText: 'Description'),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: notesCtrl,
                    maxLines: 2,
                    decoration: const InputDecoration(
                        labelText: 'Short Notes / AI Summary'),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: travelCtrl,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    decoration: const InputDecoration(
                        labelText: 'Travel Time (minutes)'),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: saving ? null : () => Navigator.pop(ctx),
                child: const Text('Cancel'),
              ),
              ElevatedButton.icon(
                onPressed: saving
                    ? null
                    : () async {
                        final start = _combineDateAndTime(startDate, startTime);
                        final end = _combineDateAndTime(endDate, endTime);
                        final validation = _validateEvent(
                          title: titleCtrl.text,
                          type: type,
                          location: location,
                          start: start,
                          end: end,
                        );
                        if (validation != null) {
                          _snack(validation, success: false);
                          return;
                        }
                        setLocalState(() => saving = true);
                        final ok = await _saveEvent(
                          existing: existing,
                          event: _Event(
                            id: existing?.id,
                            title: titleCtrl.text.trim(),
                            type: type,
                            location: location,
                            start: start,
                            end: end,
                            description: _emptyToNull(descCtrl.text),
                            shortNotes: _emptyToNull(notesCtrl.text),
                            travelMinutes: int.tryParse(travelCtrl.text),
                            isConflict: existing?.isConflict ?? false,
                          ),
                        );
                        if (!ctx.mounted) return;
                        if (ok) Navigator.pop(ctx);
                        setLocalState(() => saving = false);
                      },
                icon: Icon(existing == null ? Icons.check : Icons.save),
                label: Text(existing == null ? 'Add Event' : 'Save'),
              ),
            ],
          );
        },
      ),
    );
    titleCtrl.dispose();
    descCtrl.dispose();
    notesCtrl.dispose();
    travelCtrl.dispose();
  }

  Future<bool> _saveEvent({
    required _Event? existing,
    required _Event event,
  }) async {
    final online = context.read<ConnectivityService>().isOnline;
    if (!online && existing == null) {
      setState(() {
        _events = [
          ..._events,
          _Event(
            id: null,
            title: event.title,
            type: event.type,
            location: event.location,
            start: event.start,
            end: event.end,
            description: event.description,
            travelMinutes: event.travelMinutes,
            shortNotes: event.shortNotes,
            pendingSync: true,
          )
        ]..sort((a, b) => a.start.compareTo(b.start));
      });
      _snack('Event saved offline. It will sync when internet is available.');
      return true;
    }
    if (!online) {
      _snack('Network unavailable. Please try again when online.',
          success: false);
      return false;
    }

    final result = existing == null
        ? await ApiService.createScheduleEvent(event.toPayload())
        : await ApiService.updateScheduleEvent(existing.id!, event.toPayload());
    if (result == null) {
      _snack(
          existing == null
              ? 'Failed to create event.'
              : 'Failed to update event.',
          success: false);
      return false;
    }
    _snack(existing == null
        ? 'Event created successfully.'
        : 'Event updated successfully.');
    await _loadEvents();
    return true;
  }

  Future<void> _removeAssignedAppointment(
    _Event event,
    Map<String, dynamic> appointment,
  ) async {
    final eventId = event.id;
    final appointmentId = (appointment['id'] as num?)?.toInt();
    if (eventId == null || appointmentId == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Remove Application'),
        content: const Text(
            'Remove this application from the event and return it to follow-up?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Remove'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    final result = await ApiService.removeAppointmentFromScheduleEvent(
      eventId,
      appointmentId,
    );
    if (result == null) {
      _snack('Failed to remove application from event.', success: false);
      return;
    }
    _snack('Application removed from event and moved back to follow-up.');
    if (mounted) Navigator.pop(context);
    await _loadEvents();
  }

  Future<void> _confirmDelete(_Event event) async {
    if (event.id == null) {
      setState(() => _events = _events.where((e) => e != event).toList());
      _snack('Pending offline event removed.');
      return;
    }
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Event'),
        content: const Text('Are you sure you want to delete this event?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel')),
          ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed != true) return;
    final ok = await ApiService.deleteScheduleEvent(event.id!);
    if (!ok) {
      _snack('Failed to delete event.', success: false);
      return;
    }
    _snack('Event deleted successfully.');
    await _loadEvents();
  }

  String? _validateEvent({
    required String title,
    required String type,
    required String location,
    required DateTime start,
    required DateTime end,
  }) {
    if (title.trim().isEmpty) {
      return 'Title is required.';
    }
    if (type.trim().isEmpty) {
      return 'Event type is required.';
    }
    if (location.trim().isEmpty) {
      return 'Location is required.';
    }
    if (_isPastDay(start)) {
      return 'Start date cannot be in the past.';
    }
    if (start.isBefore(DateTime.now())) {
      return 'Start time cannot be in the past.';
    }
    if (_isPastDay(end)) {
      return 'End date cannot be in the past.';
    }
    if (end.isBefore(start)) {
      return 'End date cannot be before start date.';
    }
    if (!end.isAfter(start)) {
      return 'End time must be after start time.';
    }
    return null;
  }

  void _shiftDate(int delta) {
    setState(() {
      if (_view == _CalendarView.day) {
        _selectedDate = _selectedDate.add(Duration(days: delta));
      } else if (_view == _CalendarView.week) {
        _selectedDate = _selectedDate.add(Duration(days: delta * 7));
      } else {
        _selectedDate =
            DateTime(_selectedDate.year, _selectedDate.month + delta, 1);
      }
    });
    _loadEvents();
  }

  String _titleForView() {
    if (_view == _CalendarView.day) return _formatDate(_selectedDate);
    if (_view == _CalendarView.week) {
      final start = _startOfWeek(_selectedDate);
      final end = start.add(const Duration(days: 6));
      return '${_shortDate(start)} - ${_shortDate(end)}';
    }
    return '${_month(_selectedDate.month)} ${_selectedDate.year}';
  }

  String _eventTypeLabel(String type) =>
      _eventTypes.firstWhere(
        (row) => row['code'] == type,
        orElse: () => {'value': _typeDescriptions[type] ?? type},
      )['value'] ??
      type;

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

  void _snack(String message, {bool success = true}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor:
            success ? const Color(0xFF065F46) : const Color(0xFF991B1B),
      ),
    );
  }
}

class _DayChip extends StatelessWidget {
  final DateTime date;
  final int count;
  final bool selected;
  final bool disabled;
  final VoidCallback onTap;

  const _DayChip({
    required this.date,
    required this.count,
    required this.selected,
    this.disabled = false,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 2),
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFE8EAF6) : const Color(0xFFF9FAFB),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
              color:
                  selected ? const Color(0xFF1A237E) : const Color(0xFFE5E7EB)),
        ),
        child: Column(
          children: [
            Text(_weekdayShort(date.weekday),
                style: TextStyle(
                    color: disabled ? const Color(0xFF9CA3AF) : null,
                    decoration: disabled
                        ? TextDecoration.lineThrough
                        : TextDecoration.none,
                    fontSize: 11,
                    fontWeight: FontWeight.w700)),
            const SizedBox(height: 3),
            Text('${date.day}',
                style: TextStyle(
                    color: disabled ? const Color(0xFF9CA3AF) : null,
                    decoration: disabled
                        ? TextDecoration.lineThrough
                        : TextDecoration.none,
                    fontSize: 16,
                    fontWeight: FontWeight.w900)),
            if (count > 0) Text('$count', style: const TextStyle(fontSize: 10)),
          ],
        ),
      ),
    );
  }
}

class _HourRow extends StatelessWidget {
  final String hour;
  final List<_Event> events;
  final Color Function(String type) colorForType;
  final ValueChanged<_Event> onTap;
  final ValueChanged<_Event>? onLongPress;

  const _HourRow({
    required this.hour,
    required this.events,
    required this.colorForType,
    required this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 7),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: Color(0xFFE5E7EB))),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 52,
            child: Text(hour,
                style: const TextStyle(color: Color(0xFF64748B), fontSize: 12)),
          ),
          Expanded(
            child: events.isEmpty
                ? const Text('No events',
                    style: TextStyle(color: Color(0xFF9CA3AF), fontSize: 12))
                : Column(
                    children: [
                      for (final event in events)
                        _CompactEvent(
                          event: event,
                          color: colorForType(event.type),
                          onTap: () => onTap(event),
                          onLongPress: onLongPress == null
                              ? null
                              : () => onLongPress!(event),
                        ),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}

class _CompactEvent extends StatelessWidget {
  final _Event event;
  final Color color;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const _CompactEvent({
    required this.event,
    required this.color,
    required this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      onLongPress: onLongPress,
      child: Container(
        width: double.infinity,
        margin: const EdgeInsets.only(bottom: 6),
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: color.withAlpha(20),
          borderRadius: BorderRadius.circular(8),
          border: Border(left: BorderSide(color: color, width: 4)),
        ),
        child: Text(
          '${event.type}  ${event.title}',
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
        ),
      ),
    );
  }
}

class _EventTile extends StatelessWidget {
  final _Event event;
  final Color color;
  final VoidCallback onTap;
  final VoidCallback? onEdit;

  const _EventTile({
    required this.event,
    required this.color,
    required this.onTap,
    this.onEdit,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      onTap: onTap,
      leading: Container(width: 5, height: 48, color: color),
      title: Text(event.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontWeight: FontWeight.w800)),
      subtitle: Text(
        '${_time(event.start)} - ${_time(event.end)} | ${event.location}',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: onEdit == null
          ? const Icon(Icons.chevron_right)
          : IconButton(
              icon: const Icon(Icons.edit_outlined),
              onPressed: onEdit,
            ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final IconData icon;
  final String title;
  const _SectionTitle({required this.icon, required this.title});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 18, color: const Color(0xFF1A237E)),
        const SizedBox(width: 6),
        Expanded(
          child: Text(title,
              style:
                  const TextStyle(fontWeight: FontWeight.w900, fontSize: 14)),
        ),
      ],
    );
  }
}

class _DetailRow extends StatelessWidget {
  final IconData icon;
  final String text;
  const _DetailRow(this.icon, this.text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 16, color: const Color(0xFF1A237E)),
          const SizedBox(width: 8),
          Expanded(child: Text(text)),
        ],
      ),
    );
  }
}

class _MapRow extends StatelessWidget {
  final String label;
  final dynamic value;
  const _MapRow(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 112,
            child: Text(label,
                style: const TextStyle(
                    color: Color(0xFF64748B), fontWeight: FontWeight.w800)),
          ),
          Expanded(child: Text(_text(value, '-'))),
        ],
      ),
    );
  }
}

class _AppointmentAssignmentRow extends StatelessWidget {
  final Map<String, dynamic> appointment;
  final VoidCallback? onRemove;

  const _AppointmentAssignmentRow({
    required this.appointment,
    this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    final applicationId = _text(appointment['applicationId'], '-');
    final applicantName = _firstText([
      appointment['applicantName'],
      appointment['applicant'] is Map
          ? (appointment['applicant'] as Map)['fullName']
          : null,
    ], '-');
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(applicationId,
                    style: const TextStyle(
                        color: Color(0xFF1A237E),
                        fontWeight: FontWeight.w900,
                        decoration: TextDecoration.underline)),
                const SizedBox(height: 2),
                Text(applicantName),
                Text(
                  _priorityInsight(appointment),
                  style: const TextStyle(
                      color: Color(0xFF92400E),
                      fontSize: 12,
                      fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
          if (onRemove != null)
            TextButton.icon(
              onPressed: onRemove,
              icon: const Icon(Icons.remove_circle_outline),
              label: const Text('Remove'),
            ),
        ],
      ),
    );
  }
}

class _Pill extends StatelessWidget {
  final String label;
  final Color color;
  const _Pill(this.label, this.color);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withAlpha(24),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(label,
          style: TextStyle(
              color: color, fontSize: 11, fontWeight: FontWeight.w800)),
    );
  }
}

DateTime _startOfDay(DateTime date) =>
    DateTime(date.year, date.month, date.day);

bool _isPastDay(DateTime date) =>
    _startOfDay(date).isBefore(_startOfDay(DateTime.now()));

DateTime _startOfWeek(DateTime date) {
  return _startOfDay(date).subtract(Duration(days: date.weekday - 1));
}

DateTime _withTime(DateTime date, int hour, int minute) =>
    DateTime(date.year, date.month, date.day, hour, minute);

DateTime _defaultStartDateTime(DateTime date) {
  if (!_isSameDay(date, DateTime.now())) return _withTime(date, 10, 0);
  final next = DateTime.now();
  return DateTime(next.year, next.month, next.day, next.hour + 1);
}

DateTime _combineDateAndTime(DateTime date, TimeOfDay time) =>
    DateTime(date.year, date.month, date.day, time.hour, time.minute);

bool _isSameDay(DateTime a, DateTime b) =>
    a.year == b.year && a.month == b.month && a.day == b.day;

String _localIso(DateTime value) {
  String two(int v) => v.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)}T'
      '${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
}

String _formatDate(DateTime date) =>
    '${_weekday(date.weekday)}, ${date.day} ${_month(date.month)} ${date.year}';

String _formatDateTime(DateTime date) => '${_shortDate(date)} ${_time(date)}';

String _shortDate(DateTime date) =>
    '${date.day.toString().padLeft(2, '0')}-${_month(date.month)}-${date.year}';

String _dateInput(DateTime date) =>
    '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';

String _time(DateTime date) =>
    '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';

String _weekday(int weekday) {
  const labels = [
    'Monday',
    'Tuesday',
    'Wednesday',
    'Thursday',
    'Friday',
    'Saturday',
    'Sunday',
  ];
  return labels[weekday - 1];
}

String _weekdayShort(int weekday) {
  const labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  return labels[weekday - 1];
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

String _text(dynamic value, [String fallback = '']) {
  final text = value?.toString().trim() ?? '';
  return text.isEmpty ? fallback : text;
}

String? _textOrNull(dynamic value) {
  final text = _text(value);
  return text.isEmpty ? null : text;
}

String? _emptyToNull(String value) {
  final text = value.trim();
  return text.isEmpty ? null : text;
}

String _firstText(List<dynamic> values, [String fallback = '']) {
  for (final value in values) {
    final text = _text(value);
    if (text.isNotEmpty) return text;
  }
  return fallback;
}

String _statusLabel(dynamic value) =>
    _text(value, '-').replaceAll('_', ' ').toUpperCase();

String _priorityInsight(Map<String, dynamic> appointment) {
  final created = DateTime.tryParse(_text(appointment['createdAt']));
  final ageDays = created == null
      ? 0
      : DateTime.now().difference(created).inDays.clamp(0, 9999);
  final meetings =
      (appointment['meetingCountLast6Months'] as num?)?.toInt() ?? 0;
  if (meetings > 0 || ageDays >= 14) {
    return 'High priority: $meetings recent CM visit(s), follow-up age $ageDays day(s).';
  }
  if (ageDays >= 7) {
    return 'Medium priority: Follow-up pending for $ageDays day(s).';
  }
  return 'Low priority: First or recent follow-up request.';
}
