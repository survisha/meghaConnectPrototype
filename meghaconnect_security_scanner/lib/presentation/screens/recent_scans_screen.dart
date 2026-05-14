import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/date_formatters.dart';
import '../../data/models/recent_scan.dart';
import '../state/app_state.dart';
import '../widgets/status_chip.dart';

class RecentScansScreen extends StatelessWidget {
  const RecentScansScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final scans = appState.recentScans;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Recent Scans'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: appState.isBusy ? null : () => _refresh(context),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: scans.isEmpty
            ? const _EmptyRecentScans()
            : ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: scans.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  return _RecentScanTile(scan: scans[index]);
                },
              ),
      ),
    );
  }

  Future<void> _refresh(BuildContext context) async {
    try {
      await context.read<AppState>().refreshRecentScans();
    } on ApiException catch (error) {
      if (!context.mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.message)),
      );
    }
  }
}

class _RecentScanTile extends StatelessWidget {
  const _RecentScanTile({required this.scan});

  final RecentScan scan;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        side: BorderSide(color: Theme.of(context).colorScheme.outlineVariant),
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListTile(
        leading: const Icon(Icons.qr_code_2),
        title: Text(
          scan.visitorName,
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(
            "${scan.action.replaceAll('_', ' ')}  |  ${DateFormatters.dateTime(scan.scanTime)}",
          ),
        ),
        trailing: StatusChip(label: scan.status),
      ),
    );
  }
}

class _EmptyRecentScans extends StatelessWidget {
  const _EmptyRecentScans();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.history,
              size: 48,
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
            const SizedBox(height: 12),
            Text(
              'No scans yet',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ],
        ),
      ),
    );
  }
}
