import 'dart:convert';
import 'package:flutter/material.dart';
import '../services/api_service.dart';

class _AuditEntry {
  final int id;
  final String timestamp;
  final String module;
  final String action;
  final String user;
  final String role;
  final String entityId;
  final String description;
  final String status;
  final String requestId;
  final String oldValue;
  final String newValue;
  final String ipAddress;
  final String endpoint;

  const _AuditEntry({
    required this.id,
    required this.timestamp,
    required this.module,
    required this.action,
    required this.user,
    required this.role,
    required this.entityId,
    required this.description,
    required this.status,
    required this.requestId,
    required this.oldValue,
    required this.newValue,
    required this.ipAddress,
    required this.endpoint,
  });
}

class AuditTrailScreen extends StatefulWidget {
  const AuditTrailScreen({super.key});

  @override
  State<AuditTrailScreen> createState() => _AuditTrailScreenState();
}

class _AuditTrailScreenState extends State<AuditTrailScreen> {
  final _userController = TextEditingController();
  final _requestIdController = TextEditingController();
  List<_AuditEntry> _logs = [];
  List<String> _modules = [];
  List<String> _actions = [];
  String _module = '';
  String _action = '';
  String _role = '';
  DateTimeRange? _range;
  bool _loading = true;
  String _error = '';

  static const _roles = [
    'ADMIN',
    'HCM',
    'OSD',
    'APPROVER',
    'CMO_OFFICER',
    'DATA_ENTRY_OPERATOR',
    'PUBLIC',
  ];

  @override
  void initState() {
    super.initState();
    _loadLogs();
  }

  @override
  void dispose() {
    _userController.dispose();
    _requestIdController.dispose();
    super.dispose();
  }

  Future<void> _loadLogs() async {
    setState(() {
      _loading = true;
      _error = '';
    });
    try {
      final data = await ApiService.getAuditLogs(
        module: _module,
        action: _action,
        role: _role,
        user: _userController.text,
        requestId: _requestIdController.text,
        from: _formatApiDate(_range?.start, false),
        to: _formatApiDate(_range?.end, true),
      );
      if (!mounted) return;
      final content = (data['content'] as List<dynamic>?) ?? [];
      final rows =
          content.map((e) => _mapEntry(e as Map<String, dynamic>)).toList();
      setState(() {
        _logs = rows;
        _modules = _mergeOptions(_modules, rows.map((e) => e.module));
        _actions = _mergeOptions(_actions, rows.map((e) => e.action));
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _logs = [];
        _error = 'Unable to load audit logs.';
        _loading = false;
      });
    }
  }

  _AuditEntry _mapEntry(Map<String, dynamic> m) {
    return _AuditEntry(
      id: (m['id'] as num?)?.toInt() ?? 0,
      timestamp: _formatDisplayTime(m['timestamp']?.toString() ?? ''),
      module: _first(m['module'], m['entity'], m['entityType']),
      action: _first(m['action']),
      user: _first(m['user'], m['performedBy']),
      role: _first(m['role'], m['userRole']),
      entityId: _first(m['entityId']?.toString()),
      description: _maskSensitive(_first(m['description'], m['details'])),
      status: _first(m['status']),
      requestId: _first(m['requestId']),
      oldValue: _prettyJson(_maskSensitive(_first(m['oldValue']))),
      newValue: _prettyJson(_maskSensitive(_first(m['newValue']))),
      ipAddress: _first(m['ipAddress']),
      endpoint: _first(m['endpoint']),
    );
  }

  String _first(Object? a, [Object? b, Object? c]) {
    for (final value in [a, b, c]) {
      final text = value?.toString().trim() ?? '';
      if (text.isNotEmpty) return text;
    }
    return '';
  }

  List<String> _mergeOptions(List<String> existing, Iterable<String> incoming) {
    final values = <String>{
      ...existing.where((e) => e.isNotEmpty),
      ...incoming.where((e) => e.isNotEmpty)
    };
    return values.toList()..sort();
  }

  Color _actionColor(String action) {
    final value = action.toUpperCase();
    if (value.contains('DELETE') || value.contains('REJECT')) {
      return const Color(0xFFB91C1C);
    }
    if (value.contains('CREATE') ||
        value.contains('APPROVE') ||
        value.contains('LOGIN')) return const Color(0xFF166534);
    if (value.contains('UPDATE') ||
        value.contains('CHANGE') ||
        value.contains('SCHEDULE')) return const Color(0xFF1D4ED8);
    return const Color(0xFF374151);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildFilters(context),
        _buildSummaryRow(),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _error.isNotEmpty
                  ? Center(
                      child: Text(_error,
                          style: const TextStyle(color: Color(0xFFB91C1C))))
                  : _logs.isEmpty
                      ? _buildEmpty()
                      : RefreshIndicator(
                          onRefresh: _loadLogs,
                          child: ListView.separated(
                            padding: const EdgeInsets.all(12),
                            itemCount: _logs.length,
                            separatorBuilder: (_, __) =>
                                const SizedBox(height: 8),
                            itemBuilder: (_, i) => _AuditCard(
                              entry: _logs[i],
                              color: _actionColor(_logs[i].action),
                            ),
                          ),
                        ),
        ),
      ],
    );
  }

  Widget _buildFilters(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 6),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: _DropdownFilter(
                  label: 'Module',
                  value: _module,
                  options: _modules,
                  onChanged: (v) {
                    setState(() => _module = v);
                    _loadLogs();
                  },
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _DropdownFilter(
                  label: 'Action',
                  value: _action,
                  options: _actions,
                  onChanged: (v) {
                    setState(() => _action = v);
                    _loadLogs();
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: _DropdownFilter(
                  label: 'Role',
                  value: _role,
                  options: _roles,
                  onChanged: (v) {
                    setState(() => _role = v);
                    _loadLogs();
                  },
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () async {
                    final picked = await showDateRangePicker(
                      context: context,
                      firstDate: DateTime(2020),
                      lastDate: DateTime.now().add(const Duration(days: 365)),
                      initialDateRange: _range,
                    );
                    if (picked != null) {
                      setState(() => _range = picked);
                      _loadLogs();
                    }
                  },
                  icon: const Icon(Icons.date_range),
                  label: Text(_range == null
                      ? 'Date range'
                      : '${_range!.start.day}/${_range!.start.month} - ${_range!.end.day}/${_range!.end.month}'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _userController,
                  decoration: const InputDecoration(
                      prefixIcon: Icon(Icons.person_search), hintText: 'User'),
                  onSubmitted: (_) => _loadLogs(),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: _requestIdController,
                  decoration: const InputDecoration(
                      prefixIcon: Icon(Icons.fingerprint),
                      hintText: 'Request ID'),
                  onSubmitted: (_) => _loadLogs(),
                ),
              ),
              IconButton(
                tooltip: 'Apply',
                onPressed: _loadLogs,
                icon: const Icon(Icons.search),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryRow() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      color: const Color(0xFFE8EAF6),
      child: Row(
        children: [
          const Icon(Icons.history, size: 16, color: Color(0xFF1A237E)),
          const SizedBox(width: 8),
          Text('${_logs.length} audit records',
              style: const TextStyle(
                  color: Color(0xFF1A237E),
                  fontWeight: FontWeight.w600,
                  fontSize: 13)),
          const Spacer(),
          TextButton(
            onPressed: () {
              setState(() {
                _module = '';
                _action = '';
                _role = '';
                _range = null;
                _userController.clear();
                _requestIdController.clear();
              });
              _loadLogs();
            },
            child: const Text('Clear'),
          ),
        ],
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
          Text('No audit records found',
              style: TextStyle(color: Colors.grey[600], fontSize: 16)),
        ],
      ),
    );
  }

  String? _formatApiDate(DateTime? date, bool endOfDay) {
    if (date == null) return null;
    final value = DateTime(date.year, date.month, date.day, endOfDay ? 23 : 0,
        endOfDay ? 59 : 0, endOfDay ? 59 : 0);
    String two(int n) => n.toString().padLeft(2, '0');
    return '${value.year}-${two(value.month)}-${two(value.day)}T${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
  }

  String _formatDisplayTime(String value) {
    final dt = DateTime.tryParse(value);
    if (dt == null) return value;
    String two(int n) => n.toString().padLeft(2, '0');
    return '${two(dt.day)}-${two(dt.month)}-${dt.year} ${two(dt.hour)}:${two(dt.minute)}:${two(dt.second)}';
  }

  String _prettyJson(String value) {
    if (value.isEmpty) return '-';
    try {
      const encoder = JsonEncoder.withIndent('  ');
      return encoder.convert(jsonDecode(value));
    } catch (_) {
      return value;
    }
  }

  String _maskSensitive(String value) {
    if (value.isEmpty) return '';
    final masked = value.replaceAll(
        RegExp(r'\b\d{4}\s?\d{4}\s?\d{4}\b'), '**** **** ****');
    return masked.replaceAllMapped(
      RegExp(r'(otp|password|aadhaar|aadhar|token|secret)(\s*[=:]\s*)\S+',
          caseSensitive: false),
      (match) => '${match.group(1)}${match.group(2)}***',
    );
  }
}

class _DropdownFilter extends StatelessWidget {
  final String label;
  final String value;
  final List<String> options;
  final ValueChanged<String> onChanged;

  const _DropdownFilter({
    required this.label,
    required this.value,
    required this.options,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<String>(
      value: value,
      decoration: InputDecoration(labelText: label),
      items: [
        const DropdownMenuItem(value: '', child: Text('All')),
        ...options.map((e) => DropdownMenuItem(
            value: e, child: Text(e, overflow: TextOverflow.ellipsis))),
      ],
      onChanged: (v) => onChanged(v ?? ''),
    );
  }
}

class _AuditCard extends StatelessWidget {
  final _AuditEntry entry;
  final Color color;

  const _AuditCard({required this.entry, required this.color});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ExpansionTile(
        leading: CircleAvatar(
          backgroundColor: color.withAlpha(26),
          child: Icon(Icons.history, color: color, size: 18),
        ),
        title: Row(
          children: [
            Expanded(
                child: Text(entry.module.isEmpty ? '-' : entry.module,
                    style: const TextStyle(fontWeight: FontWeight.w700))),
            _ActionBadge(label: entry.action, color: color),
          ],
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 6),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(entry.description.isEmpty ? '-' : entry.description,
                  maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: 4),
              Wrap(
                spacing: 10,
                runSpacing: 4,
                children: [
                  _Meta(icon: Icons.person_outline, text: entry.user),
                  _Meta(icon: Icons.badge_outlined, text: entry.role),
                  _Meta(icon: Icons.numbers, text: entry.entityId),
                  _Meta(icon: Icons.access_time, text: entry.timestamp),
                ],
              ),
            ],
          ),
        ),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        children: [
          _DetailLine(label: 'Status', value: entry.status),
          _DetailLine(label: 'Request ID', value: entry.requestId),
          _DetailLine(label: 'IP Address', value: entry.ipAddress),
          _DetailLine(label: 'Endpoint', value: entry.endpoint),
          _JsonBlock(label: 'Old Value', value: entry.oldValue),
          _JsonBlock(label: 'New Value', value: entry.newValue),
        ],
      ),
    );
  }
}

class _ActionBadge extends StatelessWidget {
  final String label;
  final Color color;

  const _ActionBadge({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
          color: color.withAlpha(26), borderRadius: BorderRadius.circular(6)),
      child: Text(label.isEmpty ? '-' : label,
          style: TextStyle(
              color: color, fontSize: 11, fontWeight: FontWeight.bold)),
    );
  }
}

class _Meta extends StatelessWidget {
  final IconData icon;
  final String text;

  const _Meta({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 13, color: Colors.grey[600]),
        const SizedBox(width: 4),
        Text(text.isEmpty ? '-' : text,
            style: TextStyle(fontSize: 11, color: Colors.grey[700])),
      ],
    );
  }
}

class _DetailLine extends StatelessWidget {
  final String label;
  final String value;

  const _DetailLine({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
              width: 88,
              child: Text(label,
                  style: TextStyle(color: Colors.grey[600], fontSize: 12))),
          Expanded(
              child: Text(value.isEmpty ? '-' : value,
                  style: const TextStyle(fontSize: 12))),
        ],
      ),
    );
  }
}

class _JsonBlock extends StatelessWidget {
  final String label;
  final String value;

  const _JsonBlock({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(top: 10),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
          color: const Color(0xFFF8FAFC),
          borderRadius: BorderRadius.circular(6)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label,
              style:
                  const TextStyle(fontSize: 12, fontWeight: FontWeight.w700)),
          const SizedBox(height: 6),
          Text(value,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 11)),
        ],
      ),
    );
  }
}
