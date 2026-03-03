import 'package:flutter/material.dart';
import '../services/api_service.dart';

class _AuditEntry {
  final int id;
  final String entityType;
  final String entityId;
  final String action;
  final String details;
  final String performedBy;
  final String timestamp;

  const _AuditEntry({
    required this.id,
    required this.entityType,
    required this.entityId,
    required this.action,
    required this.details,
    required this.performedBy,
    required this.timestamp,
  });
}

class AuditTrailScreen extends StatefulWidget {
  const AuditTrailScreen({super.key});

  @override
  State<AuditTrailScreen> createState() => _AuditTrailScreenState();
}

class _AuditTrailScreenState extends State<AuditTrailScreen> {
  String _search = '';
  List<_AuditEntry> _logs = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadLogs();
  }

  Future<void> _loadLogs() async {
    setState(() => _loading = true);
    final data = await ApiService.getAuditLogs();
    if (!mounted) return;
    final content = (data['content'] as List<dynamic>?) ?? [];
    setState(() {
      _logs = content.map((e) {
        final m = e as Map<String, dynamic>;
        final ts = m['timestamp'] as String? ?? '';
        String timeLabel = ts;
        final dt = DateTime.tryParse(ts);
        if (dt != null) {
          final months = [
            'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
            'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
          ];
          timeLabel =
              '${dt.day} ${months[dt.month - 1]} ${dt.year} '
              '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
        }
        return _AuditEntry(
          id: (m['id'] as num?)?.toInt() ?? 0,
          entityType: m['entityType'] as String? ?? '',
          entityId: m['entityId']?.toString() ?? '',
          action: m['action'] as String? ?? '',
          details: m['details'] as String? ?? '',
          performedBy: m['performedBy'] as String? ?? '',
          timestamp: timeLabel,
        );
      }).toList();
      _loading = false;
    });
  }

  List<_AuditEntry> get _filtered => _logs.where((l) {
        if (_search.isEmpty) return true;
        final q = _search.toLowerCase();
        return l.performedBy.toLowerCase().contains(q) ||
            l.action.toLowerCase().contains(q) ||
            l.entityType.toLowerCase().contains(q) ||
            l.details.toLowerCase().contains(q) ||
            l.entityId.toLowerCase().contains(q);
      }).toList();

  Color _actionColor(String action) {
    if (action.contains('DELETE') || action.contains('REJECT')) {
      return const Color(0xFF991B1B);
    }
    if (action.contains('UPDATE') || action.contains('CHANGE') ||
        action == 'DELEGATION') {
      return const Color(0xFFB45309);
    }
    if (action.contains('APPROVED') || action == 'LOGIN') {
      return const Color(0xFF16A34A);
    }
    return const Color(0xFF1A237E);
  }

  IconData _actionIcon(String action) {
    if (action.contains('REJECT') || action.contains('DELETE')) {
      return Icons.cancel_outlined;
    }
    if (action.contains('APPROVED')) return Icons.check_circle_outline;
    if (action == 'LOGIN') return Icons.login;
    if (action.contains('CREATED')) return Icons.add_circle_outline;
    if (action == 'DELEGATION') return Icons.admin_panel_settings_outlined;
    return Icons.swap_horiz;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildSearchBar(),
        _buildSummaryRow(),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _filtered.isEmpty
                  ? _buildEmpty()
                  : RefreshIndicator(
                      onRefresh: _loadLogs,
                      child: ListView.separated(
                        padding: const EdgeInsets.all(12),
                        itemCount: _filtered.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (_, i) => _AuditCard(
                          entry: _filtered[i],
                          color: _actionColor(_filtered[i].action),
                          icon: _actionIcon(_filtered[i].action),
                        ),
                      ),
                    ),
        ),
      ],
    );
  }

  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
      child: TextField(
        decoration: InputDecoration(
          hintText: 'Search by user, action, entity...',
          prefixIcon: const Icon(Icons.search),
          suffixIcon: _search.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: () => setState(() => _search = ''),
                )
              : null,
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        ),
        onChanged: (v) => setState(() => _search = v),
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
          Text(
            '${_filtered.length} audit records',
            style: const TextStyle(
                color: Color(0xFF1A237E),
                fontWeight: FontWeight.w500,
                fontSize: 13),
          ),
          const Spacer(),
          Text(
            'Visible to Admin only',
            style: TextStyle(color: Colors.grey[500], fontSize: 11),
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
              style: TextStyle(color: Colors.grey[500], fontSize: 16)),
        ],
      ),
    );
  }
}

class _AuditCard extends StatelessWidget {
  final _AuditEntry entry;
  final Color color;
  final IconData icon;
  const _AuditCard(
      {required this.entry, required this.color, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: color.withAlpha(26),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: color, size: 18),
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
                          entry.action,
                          style: TextStyle(
                              color: color,
                              fontSize: 10,
                              fontWeight: FontWeight.bold),
                        ),
                      ),
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: Colors.grey[100],
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          entry.entityType,
                          style: TextStyle(
                              color: Colors.grey[600], fontSize: 10),
                        ),
                      ),
                      const Spacer(),
                      Text(
                        entry.entityId,
                        style: TextStyle(
                            fontSize: 11,
                            color: Colors.grey[500],
                            fontFamily: 'monospace'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    entry.details,
                    style: const TextStyle(fontSize: 13),
                    maxLines: 3,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(Icons.person_outline,
                          size: 12, color: Colors.grey[500]),
                      const SizedBox(width: 4),
                      Text(
                        entry.performedBy,
                        style: TextStyle(
                            fontSize: 11,
                            color: Colors.grey[600],
                            fontWeight: FontWeight.w500),
                      ),
                      const Spacer(),
                      Icon(Icons.access_time,
                          size: 12, color: Colors.grey[500]),
                      const SizedBox(width: 4),
                      Text(
                        entry.timestamp,
                        style: TextStyle(
                            fontSize: 11, color: Colors.grey[500]),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
