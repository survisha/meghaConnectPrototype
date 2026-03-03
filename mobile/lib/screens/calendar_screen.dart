import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';

class _Event {
  final String title;
  final String type;
  final String location;
  final String startTime;
  final String endTime;
  final String? description;
  final int? travelMinutes;
  final bool isConflict;
  final String? shortNotes;

  const _Event({
    required this.title,
    required this.type,
    required this.location,
    required this.startTime,
    required this.endTime,
    this.description,
    this.travelMinutes,
    this.isConflict = false,
    this.shortNotes,
  });
}

const _typeDescriptions = {
  'A1': 'Cabinet / Union Minister / Media / Flight',
  'A2': 'Event / Public Programme',
  'A3': 'File Clearing / Birthday',
  'A4': 'Individual Appointment',
  'B1': 'Public Durbar',
  'B2': 'Public Walk-in',
};

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  _Event? _selectedEvent;
  final _formKey = GlobalKey<FormState>();
  List<_Event> _events = [];
  bool _loading = true;

  // Add-event form fields
  String _newType = 'A4';
  String _newLocation = 'SHILLONG';
  final _titleCtrl = TextEditingController();
  final _descCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadEvents();
  }

  Future<void> _loadEvents() async {
    setState(() => _loading = true);
    final list = await ApiService.getScheduleEvents();
    if (!mounted) return;
    setState(() {
      _events = list.map((e) {
        final m = e as Map<String, dynamic>;
        return _Event(
          title: m['title'] as String? ?? '',
          type: m['eventType'] as String? ?? '',
          location: m['location'] as String? ?? '',
          startTime: _fmtTime(m['startTime'] as String?),
          endTime: _fmtTime(m['endTime'] as String?),
          description: m['description'] as String?,
          travelMinutes: (m['travelTimeMinutes'] as num?)?.toInt(),
          isConflict: m['isConflict'] as bool? ?? false,
        );
      }).toList();
      _loading = false;
    });
  }

  static String _fmtTime(String? iso) {
    if (iso == null) return '';
    final dt = DateTime.tryParse(iso);
    if (dt == null) return iso;
    return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _descCtrl.dispose();
    super.dispose();
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
    final role = context.watch<AuthService>().user!.role;
    final canAdd = [
      UserRole.ADMIN,
      UserRole.SAIDUL_OSD,
      UserRole.CMO_OFFICER,
      UserRole.APPROVER_JT_SECY,
    ].contains(role);

    return Column(
      children: [
        _buildDateHeader(),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : ListView(
                  padding: const EdgeInsets.all(12),
                  children: [
                    _sectionLabel('Today\'s Events'),
                    const SizedBox(height: 8),
                    ..._events.map((e) => _buildEventCard(context, e)),
                    const SizedBox(height: 72),
                  ],
                ),
        ),
        if (canAdd)
          _buildAddButton(context),
      ],
    );
  }

  Widget _buildDateHeader() {
    final now = DateTime.now();
    final days = [
      'Monday', 'Tuesday', 'Wednesday', 'Thursday',
      'Friday', 'Saturday', 'Sunday'
    ];
    final months = [
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
    ];
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: const BoxDecoration(
        color: Color(0xFF1A237E),
      ),
      child: Row(
        children: [
          const Icon(Icons.calendar_today, color: Colors.white, size: 18),
          const SizedBox(width: 10),
          Text(
            '${days[now.weekday - 1]}, ${now.day} ${months[now.month - 1]} ${now.year}',
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w600,
              fontSize: 15,
            ),
          ),
          const Spacer(),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(51),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              '${_events.length} events',
              style: const TextStyle(color: Colors.white, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }

  Widget _sectionLabel(String text) {
    return Text(
      text,
      style: const TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.bold,
        color: Color(0xFF374151),
      ),
    );
  }

  Widget _buildEventCard(BuildContext context, _Event event) {
    final color = _typeColor(event.type);
    return GestureDetector(
      onTap: () => _showEventDetail(context, event),
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(10),
          border: Border(left: BorderSide(color: color, width: 4)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(13),
              blurRadius: 6,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Column(
                children: [
                  Text(
                    event.startTime,
                    style: TextStyle(
                      color: color,
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                    ),
                  ),
                  Text(
                    event.endTime,
                    style: TextStyle(
                      color: color.withAlpha(153),
                      fontSize: 11,
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: color.withAlpha(26),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            event.type,
                            style: TextStyle(
                              color: color,
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        if (event.isConflict) ...[
                          const SizedBox(width: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFEE2E2),
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: const Text(
                              '⚠ Conflict',
                              style: TextStyle(
                                color: Color(0xFF991B1B),
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      event.title,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 14,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Row(
                      children: [
                        Icon(Icons.location_on_outlined,
                            size: 12, color: Colors.grey[500]),
                        const SizedBox(width: 2),
                        Text(
                          event.location,
                          style:
                              TextStyle(fontSize: 12, color: Colors.grey[500]),
                        ),
                        if (event.travelMinutes != null) ...[
                          const SizedBox(width: 10),
                          Icon(Icons.directions_car_outlined,
                              size: 12, color: Colors.grey[500]),
                          const SizedBox(width: 2),
                          Text(
                            '${event.travelMinutes} min travel',
                            style: TextStyle(
                                fontSize: 12, color: Colors.grey[500]),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: Colors.grey[400]),
            ],
          ),
        ),
      ),
    );
  }

  void _showEventDetail(BuildContext context, _Event event) {
    final color = _typeColor(event.type);
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.55,
        minChildSize: 0.3,
        maxChildSize: 0.85,
        expand: false,
        builder: (_, controller) => ListView(
          controller: controller,
          padding: const EdgeInsets.all(20),
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: 20),
                decoration: BoxDecoration(
                  color: Colors.grey[300],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            Row(
              children: [
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: color.withAlpha(26),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: color.withAlpha(77)),
                  ),
                  child: Text(
                    '${event.type} – ${_typeDescriptions[event.type]}',
                    style: TextStyle(
                        color: color,
                        fontWeight: FontWeight.bold,
                        fontSize: 12),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              event.title,
              style: const TextStyle(
                  fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            _detailRow(Icons.access_time_outlined,
                '${event.startTime} – ${event.endTime}', color),
            const SizedBox(height: 8),
            _detailRow(Icons.location_on_outlined, event.location, color),
            if (event.travelMinutes != null) ...[
              const SizedBox(height: 8),
              _detailRow(Icons.directions_car_outlined,
                  '${event.travelMinutes} min travel time', color),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFFEF3C7),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.notifications_outlined,
                        color: Color(0xFFB45309), size: 16),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Push notification will be sent 5 min before departure',
                        style: const TextStyle(
                            color: Color(0xFFB45309), fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            if (event.description != null) ...[
              const SizedBox(height: 12),
              const Divider(),
              const SizedBox(height: 8),
              Text(
                event.description!,
                style: TextStyle(color: Colors.grey[700], fontSize: 14),
              ),
            ],
            if (event.shortNotes != null) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFEFF6FF),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: const Color(0xFFBFDBFE)),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.auto_awesome,
                        size: 16, color: Color(0xFF3B82F6)),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'AI Summary',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF1E40AF),
                              fontSize: 13,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            event.shortNotes!,
                            style: const TextStyle(
                              color: Color(0xFF1E40AF),
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.edit_outlined),
                    label: const Text('Reschedule'),
                    onPressed: () => Navigator.pop(ctx),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF1A237E),
                      side: const BorderSide(color: Color(0xFF1A237E)),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton.icon(
                    icon: const Icon(Icons.timer_outlined),
                    label: const Text('Start Timer'),
                    onPressed: () => Navigator.pop(ctx),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _detailRow(IconData icon, String text, Color color) {
    return Row(
      children: [
        Icon(icon, size: 16, color: color),
        const SizedBox(width: 8),
        Expanded(
          child: Text(text, style: const TextStyle(fontSize: 14)),
        ),
      ],
    );
  }

  Widget _buildAddButton(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: ElevatedButton.icon(
          icon: const Icon(Icons.add),
          label: const Text('Add Event'),
          onPressed: () => _showAddEventDialog(context),
        ),
      ),
    );
  }

  void _showAddEventDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Add New Event'),
        content: Form(
          key: _formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: _titleCtrl,
                  decoration: const InputDecoration(labelText: 'Title *'),
                  validator: (v) => (v == null || v.trim().isEmpty)
                      ? 'Title is required'
                      : null,
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  value: _newType,
                  decoration:
                      const InputDecoration(labelText: 'Event Type *'),
                  items: ['A1', 'A2', 'A3', 'A4', 'B1', 'B2']
                      .map((t) => DropdownMenuItem(
                          value: t,
                          child: Text('$t – ${_typeDescriptions[t]}')))
                      .toList(),
                  onChanged: (v) =>
                      setState(() => _newType = v ?? _newType),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  value: _newLocation,
                  decoration:
                      const InputDecoration(labelText: 'Location *'),
                  items: ['SHILLONG', 'TURA', 'DELHI', 'OTHERS']
                      .map((l) =>
                          DropdownMenuItem(value: l, child: Text(l)))
                      .toList(),
                  onChanged: (v) =>
                      setState(() => _newLocation = v ?? _newLocation),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _descCtrl,
                  maxLines: 2,
                  decoration:
                      const InputDecoration(labelText: 'Description'),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              if (_formKey.currentState!.validate()) {
                _titleCtrl.clear();
                _descCtrl.clear();
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Event added to calendar'),
                    backgroundColor: Color(0xFF1A237E),
                  ),
                );
              }
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }
}
