import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/config/app_environment.dart';
import '../../core/network/api_exception.dart';
import '../../core/utils/date_formatters.dart';
import '../../data/models/qr_action_result.dart';
import '../../data/models/visitor_details.dart';
import '../state/app_state.dart';
import '../widgets/info_row.dart';
import '../widgets/status_chip.dart';

class VisitorDetailsScreen extends StatelessWidget {
  const VisitorDetailsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final visitor = appState.currentVisitor;

    return Scaffold(
      appBar: AppBar(title: const Text('Visitor Details')),
      body: SafeArea(
        child: visitor == null
            ? const Center(child: Text('No visitor selected.'))
            : _VisitorDetailsBody(visitor: visitor),
      ),
      bottomNavigationBar: visitor == null
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                child: Row(
                  children: [
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: appState.isBusy || !visitor.canCheckIn
                            ? null
                            : () => _runAction(context, appState.checkIn),
                        icon: const Icon(Icons.login),
                        label: const Text('Check-In'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: appState.isBusy || !visitor.canCheckOut
                            ? null
                            : () => _runAction(context, appState.checkOut),
                        icon: const Icon(Icons.logout),
                        label: const Text('Check-Out'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
    );
  }

  Future<void> _runAction(
    BuildContext context,
    Future<QrActionResult> Function() action,
  ) async {
    try {
      final result = await action();
      if (!context.mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result.message)),
      );
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

class _VisitorDetailsBody extends StatelessWidget {
  const _VisitorDetailsBody({required this.visitor});

  final VisitorDetails visitor;

  @override
  Widget build(BuildContext context) {
    final photoUrl =
        AppEnvironment.current.resolveMediaUrl(visitor.visitorPhotoUrl);

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _VisitorPhoto(photoUrl: photoUrl),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    visitor.visitorName,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      StatusChip(label: visitor.qrStatus),
                      StatusChip(label: visitor.entryExitStatus),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
        if (visitor.message != null) ...[
          const SizedBox(height: 18),
          DecoratedBox(
            decoration: BoxDecoration(
              color: Colors.teal.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.teal.withOpacity(0.25)),
            ),
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Text(visitor.message!),
            ),
          ),
        ],
        const SizedBox(height: 22),
        Card(
          elevation: 0,
          shape: RoundedRectangleBorder(
            side:
                BorderSide(color: Theme.of(context).colorScheme.outlineVariant),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                InfoRow(label: 'Appointment ID', value: visitor.appointmentId),
                InfoRow(
                  label: 'Date/Time',
                  value: DateFormatters.dateTime(visitor.appointmentDateTime),
                ),
                InfoRow(label: 'Purpose', value: visitor.purpose),
                InfoRow(label: 'Department', value: visitor.department),
                InfoRow(label: 'Person to meet', value: visitor.personToMeet),
                InfoRow(label: 'QR status', value: visitor.qrStatus),
                InfoRow(label: 'Entry/Exit', value: visitor.entryExitStatus),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _VisitorPhoto extends StatelessWidget {
  const _VisitorPhoto({required this.photoUrl});

  final String? photoUrl;

  @override
  Widget build(BuildContext context) {
    final placeholder = Container(
      width: 92,
      height: 112,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Icon(
        Icons.person_outline,
        size: 42,
        color: Theme.of(context).colorScheme.onSurfaceVariant,
      ),
    );

    if (photoUrl == null) {
      return placeholder;
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Image.network(
        photoUrl!,
        width: 92,
        height: 112,
        fit: BoxFit.cover,
        errorBuilder: (_, __, ___) => placeholder,
      ),
    );
  }
}
