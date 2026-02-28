import 'package:flutter/material.dart';

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

const _mockLogs = <_AuditEntry>[
  _AuditEntry(
    id: 1,
    entityType: 'Appointment',
    entityId: 'MC-2024-00001',
    action: 'APPROVED',
    details: 'HCM approved CM Care application for Bijoy Momin (₹3,00,000)',
    performedBy: 'hcm',
    timestamp: '15 Jul 2024 10:45',
  ),
  _AuditEntry(
    id: 2,
    entityType: 'Appointment',
    entityId: 'MC-2024-00002',
    action: 'STATUS_CHANGE',
    details: 'Status changed from CMO_REVIEW → HCM_PENDING by Joint Secretary',
    performedBy: 'jtsecy',
    timestamp: '15 Jul 2024 11:30',
  ),
  _AuditEntry(
    id: 3,
    entityType: 'Person',
    entityId: 'P-004',
    action: 'LOGIN',
    details: 'Successful login by DEO – Deibok Lyngdoh walk-in registered',
    performedBy: 'deo1',
    timestamp: '15 Jul 2024 14:15',
  ),
  _AuditEntry(
    id: 4,
    entityType: 'Direction',
    entityId: 'DIR-009',
    action: 'UPDATE',
    details: 'Direction status updated to "Under Review" by CMO',
    performedBy: 'cmo',
    timestamp: '15 Jul 2024 15:00',
  ),
  _AuditEntry(
    id: 5,
    entityType: 'User',
    entityId: 'U-003',
    action: 'DELEGATION',
    details: 'Approver (Jt Secy) delegated authority to CMO Officer for 3 days',
    performedBy: 'jtsecy',
    timestamp: '15 Jul 2024 16:20',
  ),
  _AuditEntry(
    id: 6,
    entityType: 'Appointment',
    entityId: 'MC-2024-00004',
    action: 'REJECTED',
    details: 'CMO rejected application – incomplete documentation',
    performedBy: 'cmo',
    timestamp: '15 Jul 2024 09:10',
  ),
  _AuditEntry(
    id: 7,
    entityType: 'SchemeApplication',
    entityId: 'MC-SCH-003',
    action: 'CREATED',
    details: 'New CMSG application created for road repair – Baghmara block',
    performedBy: 'deo1',
    timestamp: '14 Jul 2024 11:00',
  ),
];

class AuditTrailScreen extends StatefulWidget {
  const AuditTrailScreen({super.key});

  @override
  State<AuditTrailScreen> createState() => _AuditTrailScreenState();
}

class _AuditTrailScreenState extends State<AuditTrailScreen> {
  String _search = '';

  List<_AuditEntry> get _filtered => _mockLogs.where((l) {
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
          child: _filtered.isEmpty
              ? _buildEmpty()
              : ListView.separated(
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
