import 'package:flutter/material.dart';
import '../services/notification_service.dart';
import 'package:provider/provider.dart';

import '../services/offline_repository.dart';
import '../services/sync_service.dart';

class PendingSyncScreen extends StatefulWidget {
  const PendingSyncScreen({super.key});

  @override
  State<PendingSyncScreen> createState() => _PendingSyncScreenState();
}

class _PendingSyncScreenState extends State<PendingSyncScreen> {
  final _repo = OfflineRepository();
  late Future<List<Map<String, dynamic>>> _itemsFuture;
  late Future<Map<String, int>> _countsFuture;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  void _reload() {
    _itemsFuture = _repo.pendingQueue(includeFailed: true);
    _countsFuture = _repo.pendingCounts();
  }

  Future<void> _syncNow() async {
    await context.read<SyncService>().syncNow();
    if (!mounted) return;
    setState(_reload);
    final message = context.read<SyncService>().lastMessage ??
        'Sync completed successfully.';
    AppNotificationService.success(message);
  }

  @override
  Widget build(BuildContext context) {
    final sync = context.watch<SyncService>();
    return RefreshIndicator(
      onRefresh: () async => setState(_reload),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          FutureBuilder<Map<String, int>>(
            future: _countsFuture,
            builder: (context, snapshot) {
              final counts = snapshot.data ?? const {};
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Pending Sync',
                        style: TextStyle(
                          color: Color(0xFF1A237E),
                          fontSize: 20,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text('Total pending records: ${counts['TOTAL'] ?? 0}'),
                      Text('Failed records: ${counts['FAILED'] ?? 0}'),
                      Text(
                        'Last sync: ${sync.lastSyncAt == null ? 'Never' : sync.lastSyncAt!.toLocal()}',
                      ),
                      const SizedBox(height: 14),
                      ElevatedButton.icon(
                        onPressed: sync.syncing ? null : _syncNow,
                        icon: sync.syncing
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Icon(Icons.sync),
                        label: Text(sync.syncing ? 'Syncing...' : 'Sync Now'),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          const SizedBox(height: 12),
          FutureBuilder<List<Map<String, dynamic>>>(
            future: _itemsFuture,
            builder: (context, snapshot) {
              final items = snapshot.data ?? const [];
              if (items.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: Text('No pending sync records.')),
                  ),
                );
              }
              return Column(
                children: [
                  for (final item in items)
                    Card(
                      child: ListTile(
                        leading: Icon(_iconFor(item['entityType'].toString())),
                        title: Text(
                          '${item['entityType']} • ${item['action']}',
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                        subtitle: Text(
                          [
                            'Status: ${item['status']}',
                            if ((item['errorMessage'] ?? '')
                                .toString()
                                .isNotEmpty)
                              item['errorMessage'].toString(),
                          ].join('\n'),
                        ),
                        trailing: item['status'] == SyncState.failed ||
                                item['status'] == SyncState.needsReview
                            ? IconButton(
                                tooltip: 'Retry failed sync',
                                icon: const Icon(Icons.refresh),
                                onPressed: sync.syncing ? null : _syncNow,
                              )
                            : null,
                      ),
                    ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  IconData _iconFor(String type) {
    switch (type) {
      case SyncEntityType.visitor:
        return Icons.person_add_alt_1_outlined;
      case SyncEntityType.photo:
        return Icons.photo_camera_outlined;
      case SyncEntityType.appointment:
        return Icons.event_note_outlined;
      case SyncEntityType.aiNote:
        return Icons.auto_awesome_outlined;
      case SyncEntityType.qrScan:
        return Icons.qr_code_scanner_outlined;
      default:
        return Icons.sync_problem_outlined;
    }
  }
}
