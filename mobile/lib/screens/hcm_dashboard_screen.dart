import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/user.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/connectivity_service.dart';
import '../services/offline_repository.dart';

class HcmDashboardScreen extends StatefulWidget {
  const HcmDashboardScreen({super.key});

  @override
  State<HcmDashboardScreen> createState() => _HcmDashboardScreenState();
}

class _HcmDashboardScreenState extends State<HcmDashboardScreen> {
  final _pageController = PageController();
  final _remarksCtrl = TextEditingController();
  final _decisionCtrl = TextEditingController();
  final _clarificationCtrl = TextEditingController();

  DateTime _selectedDate = DateTime.now();
  List<Map<String, dynamic>> _appointments = [];
  List<Map<String, String>> _departments = [];
  int _pageIndex = 0;
  bool _loading = true;
  bool _submitting = false;
  String? _error;
  String? _departmentCode;
  String _snoozeType = 'DAYS_7';
  DateTime? _modifiedDateTime;

  @override
  void initState() {
    super.initState();
    _loadDashboard();
  }

  @override
  void dispose() {
    _pageController.dispose();
    _remarksCtrl.dispose();
    _decisionCtrl.dispose();
    _clarificationCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadDashboard() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final date = _dateParam(_selectedDate);
    final results = await Future.wait([
      ApiService.getHcmActionAppointments(date),
      ApiService.getReferenceData('DEPARTMENT'),
    ]);
    if (!mounted) return;

    var appointments =
        results[0].where(_isPendingHcmActionAppointment).toList();
    if (appointments.isEmpty && context.read<ConnectivityService>().isOffline) {
      final cached = await OfflineRepository().cachedAppointments();
      appointments = cached.where(_isPendingHcmActionAppointment).toList();
    }
    if (!mounted) return;

    setState(() {
      _appointments = appointments;
      _departments = results[1] as List<Map<String, String>>;
      _loading = false;
      _error = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthService>().user!;
    final title = user.role == UserRole.OSD ? 'OSD Dashboard' : 'HCM Dashboard';
    return Column(
      children: [
        _header(user, title),
        _dateFilter(),
        if (_error != null) _errorBanner(),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _loadDashboard,
                  child: PageView(
                    controller: _pageController,
                    onPageChanged: (index) =>
                        setState(() => _pageIndex = index),
                    children: [
                      _scrollPage(_overviewPage()),
                      _scrollPage(_appointmentsPage()),
                      _scrollPage(_recentPage()),
                    ],
                  ),
                ),
        ),
        _pageDots(),
      ],
    );
  }

  Widget _header(AuthUser user, String title) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
      color: const Color(0xFFF4F6FB),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: const TextStyle(
                        fontSize: 20, fontWeight: FontWeight.w900)),
                const SizedBox(height: 2),
                Text(
                  user.fullName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style:
                      const TextStyle(color: Color(0xFF64748B), fontSize: 13),
                ),
              ],
            ),
          ),
          const Icon(Icons.notifications_outlined,
              color: Color(0xFF1A237E), size: 28),
        ],
      ),
    );
  }

  Widget _dateFilter() {
    return Container(
      color: const Color(0xFFF4F6FB),
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton.icon(
              onPressed: _pickDate,
              icon: const Icon(Icons.event_outlined, size: 18),
              label: Text(_dateLabel(_selectedDate)),
            ),
          ),
          const SizedBox(width: 10),
          OutlinedButton.icon(
            onPressed: _loadDashboard,
            icon: const Icon(Icons.refresh, size: 18),
            label: const Text('Refresh'),
          ),
        ],
      ),
    );
  }

  Widget _errorBanner() {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFEE2E2),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        _error!,
        style: const TextStyle(color: Color(0xFF991B1B)),
      ),
    );
  }

  Widget _scrollPage(Widget child) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      children: [child],
    );
  }

  Widget _overviewPage() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Overview'),
        const SizedBox(height: 10),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisSpacing: 10,
          mainAxisSpacing: 10,
          childAspectRatio: 1.45,
          children: [
            _SummaryCard(
              label: 'Pending Actions',
              value: _appointments.length,
              icon: Icons.pending_actions_outlined,
              color: const Color(0xFFB45309),
            ),
            _SummaryCard(
              label: 'Accepted',
              value: _appointments
                  .where((e) => _text(e['status']).contains('ACCEPT'))
                  .length,
              icon: Icons.done_all_outlined,
              color: const Color(0xFF15803D),
            ),
            _SummaryCard(
              label: 'Need Review',
              value: _appointments.length,
              icon: Icons.assignment_late_outlined,
              color: const Color(0xFF7C3AED),
            ),
          ],
        ),
        const SizedBox(height: 16),
        _hintCard(),
      ],
    );
  }

  Widget _appointmentsPage() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Appointments'),
        const SizedBox(height: 10),
        if (_appointments.isEmpty)
          _emptyState('No appointments for selected date.')
        else
          for (final appointment in _appointments)
            _AppointmentActionCard(
              appointment: appointment,
              onView: () => _openDetails(appointment),
              onRightSwipe: () => _showActionSheet(appointment, true),
              onLeftSwipe: () => _showActionSheet(appointment, false),
            ),
      ],
    );
  }

  Widget _recentPage() {
    final recent = _appointments.take(5).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Recent Updates'),
        const SizedBox(height: 10),
        if (recent.isEmpty)
          _emptyState('No dashboard data found.')
        else
          for (final appointment in recent) _recentRow(appointment),
      ],
    );
  }

  Widget _hintCard() {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: const Padding(
        padding: EdgeInsets.all(14),
        child: Row(
          children: [
            Icon(Icons.swipe_outlined, color: Color(0xFF1A237E)),
            SizedBox(width: 10),
            Expanded(
              child: Text(
                'Swipe between dashboard sections. On appointment cards, swipe right for Accept/Modify or left for Reject/Delay.',
                style: TextStyle(color: Color(0xFF475569)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _recentRow(Map<String, dynamic> data) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: ListTile(
        leading:
            const Icon(Icons.event_note_outlined, color: Color(0xFF1A237E)),
        title: Text(
          _appointmentSubject(data),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: Text(
          _appointmentApplicant(data),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ),
    );
  }

  Widget _sectionTitle(String title) {
    return Row(
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900),
        ),
        const Spacer(),
        Text(
          ['Overview', 'Pending', 'Work', 'Recent'][_pageIndex],
          style: const TextStyle(color: Color(0xFF64748B), fontSize: 12),
        ),
      ],
    );
  }

  Widget _emptyState(String text) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Center(
          child: Column(
            children: [
              const Icon(Icons.inbox_outlined,
                  size: 42, color: Color(0xFF94A3B8)),
              const SizedBox(height: 8),
              Text(text, style: const TextStyle(color: Color(0xFF64748B))),
            ],
          ),
        ),
      ),
    );
  }

  Widget _pageDots() {
    return Container(
      color: const Color(0xFFF4F6FB),
      padding: const EdgeInsets.only(bottom: 10, top: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: List.generate(3, (index) {
          final active = index == _pageIndex;
          return AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            width: active ? 22 : 7,
            height: 7,
            margin: const EdgeInsets.symmetric(horizontal: 3),
            decoration: BoxDecoration(
              color: active ? const Color(0xFF1A237E) : const Color(0xFFCBD5E1),
              borderRadius: BorderRadius.circular(999),
            ),
          );
        }),
      ),
    );
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      firstDate: DateTime(DateTime.now().year - 2),
      lastDate: DateTime(DateTime.now().year + 2),
      initialDate: _selectedDate,
    );
    if (picked == null) return;
    setState(() => _selectedDate = picked);
    await _loadDashboard();
  }

  Future<void> _openDetails(Map<String, dynamic> appointment) async {
    final applicant = _map(appointment['applicant']);
    final citizenId = _asInt(appointment['applicantId'] ?? applicant['id']);
    Map<String, dynamic>? history;
    if (citizenId != null) {
      history = await ApiService.getPublicIdentificationHistory(citizenId);
    }
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.88,
        maxChildSize: 0.96,
        builder: (context, controller) {
          final appointments = history?['appointments'] is List
              ? history!['appointments'] as List<dynamic>
              : <dynamic>[];
          return ListView(
            controller: controller,
            padding: const EdgeInsets.all(16),
            children: [
              Row(
                children: [
                  const Expanded(
                    child: Text('Appointment Details',
                        style: TextStyle(
                            fontSize: 18, fontWeight: FontWeight.w900)),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),
              _InfoLine(
                  'Application', _text(appointment['applicationId'], '-')),
              _InfoLine('Subject', _appointmentSubject(appointment)),
              _InfoLine('Citizen', _appointmentApplicant(appointment)),
              _InfoLine(
                  'Mobile',
                  _firstText(
                      [applicant['phoneNumber'], appointment['applicantPhone']],
                      '-')),
              _InfoLine('EPIC', _text(applicant['epicNumber'], '-')),
              _InfoLine('KYC Status', _text(applicant['kycStatus'], '-')),
              _InfoLine(
                  'Schedule', _fmtDateTime(appointment['scheduledDateTime'])),
              _InfoLine('Status', _label(_text(appointment['status'], '-'))),
              _InfoLine('Department',
                  _text(appointment['department'], 'Not allocated')),
              _InfoLine(
                  'Purpose',
                  _firstText(
                      [appointment['agendaBrief'], appointment['shortNotes']],
                      '-')),
              const Divider(height: 28),
              const Text('Visitor History',
                  style: TextStyle(fontWeight: FontWeight.w900)),
              const SizedBox(height: 8),
              _InfoLine('Total Visits', _text(history?['visitCount'], '0')),
              _InfoLine('Last Visit', _fmtDateTime(history?['lastVisitedAt'])),
              if (appointments.isEmpty)
                const Text('No previous visitor history found.',
                    style: TextStyle(color: Color(0xFF64748B)))
              else
                for (final visit in appointments.take(5))
                  _historyVisitCard(_map(visit)),
            ],
          );
        },
      ),
    );
  }

  Widget _historyVisitCard(Map<String, dynamic> visit) {
    return Container(
      margin: const EdgeInsets.only(top: 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
              _firstText([visit['applicationId'], visit['appointmentId']], '-'),
              style: const TextStyle(fontWeight: FontWeight.w800)),
          Text(_text(visit['purpose'], 'No purpose recorded.')),
          Text(
            '${_fmtDateTime(visit['dateTime'])} / ${_text(visit['department'], 'No department')}',
            style: const TextStyle(color: Color(0xFF64748B), fontSize: 12),
          ),
        ],
      ),
    );
  }

  Future<void> _showActionSheet(
      Map<String, dynamic> appointment, bool rightSwipe) async {
    _remarksCtrl.clear();
    _decisionCtrl.clear();
    _clarificationCtrl.clear();
    _departmentCode = null;
    _modifiedDateTime = null;
    _snoozeType = 'DAYS_7';

    await showModalBottomSheet<void>(
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
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    rightSwipe ? 'Accept or Modify' : 'Reject or Delay',
                    style: const TextStyle(
                        fontSize: 18, fontWeight: FontWeight.w900),
                  ),
                  const SizedBox(height: 8),
                  Text(_appointmentSubject(appointment),
                      style: const TextStyle(color: Color(0xFF64748B))),
                  const SizedBox(height: 14),
                  if (rightSwipe) ...[
                    OutlinedButton.icon(
                      onPressed: () async {
                        final picked = await _pickDateTime();
                        if (picked != null) {
                          setSheetState(() => _modifiedDateTime = picked);
                        }
                      },
                      icon: const Icon(Icons.edit_calendar_outlined),
                      label: Text(_modifiedDateTime == null
                          ? 'Select modified date'
                          : _fmtDateTime(_modifiedDateTime!.toIso8601String())),
                    ),
                  ] else ...[
                    DropdownButtonFormField<String>(
                      value: _snoozeType,
                      decoration: const InputDecoration(labelText: 'Snooze'),
                      items: const [
                        DropdownMenuItem(
                            value: 'DAYS_7', child: Text('7 days')),
                        DropdownMenuItem(
                            value: 'DAYS_15', child: Text('15 days')),
                        DropdownMenuItem(
                            value: 'DAYS_30', child: Text('30 days')),
                        DropdownMenuItem(
                            value: 'CUSTOM', child: Text('Custom')),
                      ],
                      onChanged: (value) =>
                          setSheetState(() => _snoozeType = value ?? 'DAYS_7'),
                    ),
                    const SizedBox(height: 10),
                    TextField(
                      controller: _clarificationCtrl,
                      minLines: 2,
                      maxLines: 4,
                      decoration: const InputDecoration(
                        labelText: 'Clarification Needed',
                        alignLabelWithHint: true,
                      ),
                    ),
                  ],
                  const SizedBox(height: 10),
                  TextField(
                    controller: _decisionCtrl,
                    decoration: const InputDecoration(labelText: 'Decision'),
                  ),
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    value: _departmentCode,
                    decoration: const InputDecoration(
                        labelText: 'Forward to Department'),
                    items: [
                      const DropdownMenuItem(
                          value: '', child: Text('No department')),
                      for (final department in _departments)
                        DropdownMenuItem(
                          value: department['code'],
                          child: Text(
                              department['value'] ?? department['code'] ?? ''),
                        ),
                    ],
                    onChanged: (value) =>
                        setSheetState(() => _departmentCode = value),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _remarksCtrl,
                    minLines: 2,
                    maxLines: 4,
                    decoration: const InputDecoration(
                      labelText: 'Add Remarks / Notes',
                      alignLabelWithHint: true,
                    ),
                  ),
                  const SizedBox(height: 14),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      if (rightSwipe) ...[
                        _ActionChipButton(
                          label: 'Accept',
                          icon: Icons.done_all_outlined,
                          onPressed: _submitting
                              ? null
                              : () => _submitAction(appointment, 'accept'),
                        ),
                        _ActionChipButton(
                          label: 'Mark Important',
                          icon: Icons.star_outline,
                          onPressed: _submitting
                              ? null
                              : () =>
                                  _submitAction(appointment, 'mark-important'),
                        ),
                        _ActionChipButton(
                          label: 'Modify',
                          icon: Icons.edit_outlined,
                          onPressed: _submitting
                              ? null
                              : () => _submitAction(appointment, 'modify'),
                        ),
                      ] else ...[
                        _ActionChipButton(
                          label: 'Snooze',
                          icon: Icons.schedule_outlined,
                          color: const Color(0xFFB45309),
                          onPressed: _submitting
                              ? null
                              : () => _submitAction(appointment, 'snooze'),
                        ),
                        _ActionChipButton(
                          label: 'Reject',
                          icon: Icons.cancel_outlined,
                          color: const Color(0xFF991B1B),
                          onPressed: _submitting
                              ? null
                              : () => _submitAction(appointment, 'reject'),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Future<DateTime?> _pickDateTime() async {
    final date = await showDatePicker(
      context: context,
      firstDate: DateTime.now(),
      lastDate: DateTime(DateTime.now().year + 2),
      initialDate: DateTime.now(),
    );
    if (date == null || !mounted) return null;
    final time = await showTimePicker(
      context: context,
      initialTime: const TimeOfDay(hour: 10, minute: 0),
    );
    if (time == null) return null;
    return DateTime(date.year, date.month, date.day, time.hour, time.minute);
  }

  Future<void> _submitAction(
      Map<String, dynamic> appointment, String action) async {
    if (action == 'modify' && _modifiedDateTime == null) {
      _showMessage('Please select a new date and time.');
      return;
    }
    final appointmentId = _asInt(appointment['id']);
    if (appointmentId == null) {
      _showMessage('Failed to perform action.');
      return;
    }
    setState(() => _submitting = true);
    final dateTime = _firstText([
      appointment['scheduledDateTime'],
      appointment['createdAt'],
      appointment['submittedAt'],
    ]);
    final payload = {
      'acceptedDateTime': action == 'accept'
          ? _toApiDateTime(dateTime)
          : _toApiDateTime(_modifiedDateTime?.toIso8601String()),
      'requestedEarlierDateTime': action == 'mark-important'
          ? _toApiDateTime(_modifiedDateTime?.toIso8601String())
          : null,
      'snoozeType': action == 'snooze' ? _snoozeType : null,
      'clarificationRequested':
          action == 'reject' ? _clarificationCtrl.text.trim() : null,
      'originalDateTime': _toApiDateTime(dateTime),
      'originalLocation': _text(appointment['requestedLocation'], '-'),
      'appointmentSubject': _appointmentSubject(appointment),
      'hcmRemarks': _remarksCtrl.text.trim(),
      'decision': _decisionCtrl.text.trim(),
      'departmentCode': _departmentCode,
    }..removeWhere((_, value) => value == null);

    final ok = await ApiService.submitHcmAction(
      appointmentId,
      action,
      payload: payload,
    );
    if (!mounted) return;
    setState(() => _submitting = false);
    if (!ok) {
      _showMessage('Failed to perform action. Please try again.');
      return;
    }
    Navigator.pop(context);
    _showMessage('Action submitted successfully.');
    await _loadDashboard();
  }

  bool _isPendingHcmActionAppointment(Map<String, dynamic> appointment) {
    const handled = {
      'HCM_ACCEPTED',
      'HCM_REJECTED',
      'FORWARDED_TO_DEPARTMENT',
      'COMPLETED',
      'CANCELLED',
      'REJECTED',
    };
    return !handled.contains(_text(appointment['status']));
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }
}

class _AppointmentActionCard extends StatelessWidget {
  final Map<String, dynamic> appointment;
  final VoidCallback onView;
  final VoidCallback onRightSwipe;
  final VoidCallback onLeftSwipe;

  const _AppointmentActionCard({
    required this.appointment,
    required this.onView,
    required this.onRightSwipe,
    required this.onLeftSwipe,
  });

  @override
  Widget build(BuildContext context) {
    return Dismissible(
      key: ValueKey(
          _firstText([appointment['id'], appointment['applicationId']])),
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          onRightSwipe();
        } else {
          onLeftSwipe();
        }
        return false;
      },
      background: _swipeBg(Icons.check_circle_outline, 'Accept / Modify',
          const Color(0xFF15803D), Alignment.centerLeft),
      secondaryBackground: _swipeBg(Icons.block_outlined, 'Reject / Delay',
          const Color(0xFF991B1B), Alignment.centerRight),
      child: Card(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const CircleAvatar(
                    backgroundColor: Color(0xFF1A237E),
                    child: Icon(Icons.event, color: Colors.white),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _appointmentSubject(appointment),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontWeight: FontWeight.w900),
                        ),
                        Text(
                          _fmtDateTime(_firstText([
                            appointment['scheduledDateTime'],
                            appointment['createdAt'],
                            appointment['submittedAt'],
                          ])),
                          style: const TextStyle(
                              color: Color(0xFF64748B), fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              _InfoLine('Applicant', _appointmentApplicant(appointment)),
              _InfoLine(
                  'Location', _text(appointment['requestedLocation'], '-')),
              _InfoLine(
                  'Type',
                  _firstText([
                    appointment['appointmentType'],
                    appointment['eventType']
                  ], '-')),
              _InfoLine(
                  'Category',
                  _firstText(
                      [appointment['department'], appointment['agendaType']],
                      'General')),
              if (_firstText(
                      [appointment['agendaBrief'], appointment['shortNotes']])
                  .isNotEmpty)
                _InfoLine(
                    'Description',
                    _firstText([
                      appointment['agendaBrief'],
                      appointment['shortNotes']
                    ])),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: onView,
                  icon: const Icon(Icons.visibility_outlined, size: 18),
                  label: const Text('View'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _swipeBg(
      IconData icon, String label, Color color, Alignment alignment) {
    return Container(
      alignment: alignment,
      padding: const EdgeInsets.symmetric(horizontal: 18),
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: color.withAlpha(30),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisAlignment: alignment == Alignment.centerLeft
            ? MainAxisAlignment.start
            : MainAxisAlignment.end,
        children: [
          Icon(icon, color: color),
          const SizedBox(width: 8),
          Text(label,
              style: TextStyle(color: color, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final String label;
  final int value;
  final IconData icon;
  final Color color;

  const _SummaryCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Icon(icon, color: color),
            Text('$value',
                style: TextStyle(
                    color: color, fontSize: 24, fontWeight: FontWeight.w900)),
            Text(label,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Color(0xFF64748B))),
          ],
        ),
      ),
    );
  }
}

class _ActionChipButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final Color? color;
  final VoidCallback? onPressed;

  const _ActionChipButton({
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

class _InfoLine extends StatelessWidget {
  final String label;
  final String value;

  const _InfoLine(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    final display = value.trim().isEmpty ? '-' : value.trim();
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 116,
            child: Text(label,
                style: const TextStyle(
                    color: Color(0xFF64748B),
                    fontSize: 12,
                    fontWeight: FontWeight.w800)),
          ),
          Expanded(child: Text(display, style: const TextStyle(fontSize: 13))),
        ],
      ),
    );
  }
}

Map<String, dynamic> _map(dynamic value) =>
    value is Map ? Map<String, dynamic>.from(value) : {};

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

String _appointmentSubject(Map<String, dynamic> appointment) {
  return _firstText([
    appointment['subject'],
    appointment['agendaType'],
    appointment['applicationId'],
    appointment['id'] == null ? null : 'Appointment #${appointment['id']}',
  ], 'Appointment');
}

String _appointmentApplicant(Map<String, dynamic> appointment) {
  final applicant = _map(appointment['applicant']);
  return _firstText([
    applicant['fullName'],
    appointment['applicantName'],
    appointment['guestName'],
  ], 'Not specified');
}

String _fmtDateTime(dynamic raw) {
  final value = raw?.toString().trim() ?? '';
  if (value.isEmpty) return '-';
  final dt = DateTime.tryParse(value);
  if (dt == null) return value;
  final local = dt.toLocal();
  return '${local.day.toString().padLeft(2, '0')}-${_month(local.month)}-${local.year} '
      '${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
}

String _dateLabel(DateTime date) =>
    '${date.day.toString().padLeft(2, '0')}-${_month(date.month)}-${date.year}';

String _dateParam(DateTime date) =>
    '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

String? _toApiDateTime(String? raw) {
  final value = raw?.trim() ?? '';
  if (value.isEmpty) return null;
  final date = DateTime.tryParse(value);
  if (date == null) return null;
  String two(int part) => part.toString().padLeft(2, '0');
  return '${date.year}-${two(date.month)}-${two(date.day)} '
      '${two(date.hour)}:${two(date.minute)}:${two(date.second)}';
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
