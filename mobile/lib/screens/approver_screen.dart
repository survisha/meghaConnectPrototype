import 'package:flutter/material.dart';
import '../services/notification_service.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';
import '../services/connectivity_service.dart';
import '../services/offline_repository.dart';
import '../services/sync_service.dart';

class _ApproverAppointment {
  final int backendId;
  final String id;
  final String applicantName;
  final String district;
  final String agendaType;
  final String agendaBrief;
  final String eventType;
  final String location;
  final String appointmentCategory;
  String status;
  String? approverRemarks;
  final String? shortNotes;

  _ApproverAppointment({
    required this.backendId,
    required this.id,
    required this.applicantName,
    required this.district,
    required this.agendaType,
    required this.agendaBrief,
    required this.eventType,
    required this.location,
    required this.appointmentCategory,
    required this.status,
    this.shortNotes,
  });
}

class ApproverWorkflowScreen extends StatefulWidget {
  const ApproverWorkflowScreen({super.key});

  @override
  State<ApproverWorkflowScreen> createState() => _ApproverWorkflowScreenState();
}

class _ApproverWorkflowScreenState extends State<ApproverWorkflowScreen> {
  static const _primaryBlue = Color(0xFF1A237E);

  List<_ApproverAppointment> _appointments = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadAppointments();
  }

  Future<void> _loadAppointments() async {
    setState(() => _loading = true);
    final data = await ApiService.getApproverAppointments(size: 100);
    if (!mounted) return;
    final content = (data['content'] as List<dynamic>?) ?? [];
    const reviewStatuses = {
      'PENDING',
      'SCHEDULED',
      'REJECTED',
      'ROUTED_TO_OFFICIAL'
    };
    setState(() {
      _appointments = content
          .map((e) => e as Map<String, dynamic>)
          .where((m) => reviewStatuses.contains(m['status'] as String? ?? ''))
          .map((m) {
        final applicant = m['applicant'] as Map<String, dynamic>? ?? {};
        return _ApproverAppointment(
          backendId: (m['id'] as num?)?.toInt() ?? 0,
          id: m['applicationId'] as String? ?? m['id']?.toString() ?? '',
          applicantName: applicant['fullName'] as String? ??
              m['applicantName'] as String? ??
              '—',
          district: applicant['district'] as String? ??
              m['district'] as String? ??
              '',
          agendaType: m['agendaType'] as String? ?? '',
          agendaBrief:
              m['agendaBrief'] as String? ?? m['description'] as String? ?? '',
          eventType: m['eventType'] as String? ?? '',
          location: m['requestedLocation'] as String? ?? '',
          appointmentCategory: m['appointmentCategory'] as String? ??
              (m['isWalkIn'] == true ? 'WALK_IN' : 'SCHEDULED'),
          status: m['status'] as String? ?? '',
          shortNotes: m['shortNotes'] as String?,
        );
      }).toList();
      _loading = false;
    });
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'PENDING':
        return const Color(0xFFF57F17);
      case 'SCHEDULED':
        return const Color(0xFF1565C0);
      case 'HCM_ACCEPTED':
        return const Color(0xFF2E7D32);
      case 'REJECTED':
        return const Color(0xFFC62828);
      default:
        return Colors.grey;
    }
  }

  void _openDetailSheet(_ApproverAppointment appt) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => _ApproverDetailSheet(
        appointment: appt,
        onApprove: (remarks) => _handleRoute(appt, remarks),
        onReject: (remarks) => _handleAction(appt, 'REJECT', remarks),
        onReschedule: (date) => _handleReschedule(appt, date),
      ),
    );
  }

  Future<void> _handleAction(
      _ApproverAppointment appt, String action, String remarks) async {
    const newStatus = 'REJECTED';
    final offline = context.read<ConnectivityService>().isOffline;
    final result = offline
        ? null
        : await ApiService.rejectPendingAppointment(appt.backendId,
            remarks.isNotEmpty ? remarks : 'Rejected by Approver');
    if (!mounted) return;
    if (result != null) {
      setState(() {
        appt.approverRemarks = remarks;
        appt.status = newStatus;
      });
    } else {
      await OfflineRepository().enqueue(
        entityType: SyncEntityType.action,
        localEntityId: appt.id,
        action: action,
        payload: {
          'appointmentId': appt.backendId,
          'applicationId': appt.id,
          'status': newStatus,
          'remarks': remarks,
        },
      );
      if (!mounted) return;
      context.read<SyncService>().syncNow();
      setState(() {
        appt.approverRemarks = remarks;
        appt.status = '$newStatus (Pending Sync)';
      });
    }
    final message = result == null
        ? 'No internet connection. Saved offline.'
        : '${appt.id} rejected.';
    result == null
        ? AppNotificationService.info(message)
        : AppNotificationService.success(message);
  }

  Future<void> _handleReschedule(
      _ApproverAppointment appt, String newDate) async {
    final date = DateTime.tryParse(newDate);
    final result = date == null
        ? null
        : await ApiService.schedulePendingAppointment(appt.backendId, date);
    if (!mounted) return;
    if (result == null) {
      AppNotificationService.error('Unable to schedule ${appt.id}.');
      return;
    }
    setState(() => appt.status = 'SCHEDULED');
    AppNotificationService.success('${appt.id} scheduled for $newDate.');
  }

  Future<void> _handleRoute(_ApproverAppointment appt, String remarks) async {
    final result = await ApiService.routePendingAppointment(appt.backendId,
        officer: 'Responsible Official', direction: remarks);
    if (!mounted) return;
    if (result == null) {
      AppNotificationService.error('Unable to route ${appt.id}.');
      return;
    }
    setState(() => appt.status = 'ROUTED_TO_OFFICIAL');
    AppNotificationService.success(
        '${appt.id} routed to the responsible official.');
  }

  @override
  Widget build(BuildContext context) {
    final role = context.watch<AuthService>().user!.role;
    final pendingCount = _appointments
        .where((a) =>
            a.status == 'PENDING' && a.appointmentCategory == 'SCHEDULED')
        .length;

    return Column(
      children: [
        _buildHeader(role, pendingCount),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _appointments.isEmpty
                  ? _buildEmpty()
                  : RefreshIndicator(
                      onRefresh: _loadAppointments,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: _appointments.length,
                        itemBuilder: (_, i) =>
                            _buildAppointmentCard(_appointments[i]),
                      ),
                    ),
        ),
      ],
    );
  }

  Widget _buildHeader(UserRole role, int pendingCount) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: const BoxDecoration(color: _primaryBlue),
      child: Row(
        children: [
          const Icon(Icons.how_to_reg_outlined, color: Colors.white, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Approver Workflow',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                  ),
                ),
                Text(
                  role == UserRole.APPROVER
                      ? 'Approver Review'
                      : 'HCM Final View',
                  style: TextStyle(
                    color: Colors.white.withAlpha(204),
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(51),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              '$pendingCount pending',
              style: const TextStyle(color: Colors.white, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAppointmentCard(_ApproverAppointment appt) {
    final statusColor = _statusColor(appt.status);
    final isPending = appt.status == 'PENDING';
    return GestureDetector(
      onTap: () => _openDetailSheet(appt),
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(10),
          border: Border(
            left: BorderSide(
              color: statusColor,
              width: 4,
            ),
          ),
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
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _eventTypeBadge(appt.eventType),
                  const SizedBox(width: 8),
                  Text(
                    appt.id,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                      color: Color(0xFF1E40AF),
                    ),
                  ),
                  const Spacer(),
                  _statusBadge(appt.status, statusColor),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                appt.applicantName,
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 15,
                ),
              ),
              Text(
                '${appt.district} · ${appt.agendaType}',
                style: TextStyle(
                  color: Colors.grey[600],
                  fontSize: 12,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                appt.agendaBrief,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: Colors.grey[700],
                  fontSize: 13,
                ),
              ),
              if (appt.shortNotes != null) ...[
                const SizedBox(height: 8),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: const Color(0xFFEFF6FF),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: const Color(0xFFBFDBFE)),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.auto_awesome,
                          size: 14, color: Color(0xFF3B82F6)),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          appt.shortNotes!,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 12,
                            color: Color(0xFF1E40AF),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              if (isPending && appt.appointmentCategory == 'SCHEDULED') ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        icon: const Icon(Icons.check_circle_outline, size: 16),
                        label: const Text('Review'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: const Color(0xFF2E7D32),
                          side: const BorderSide(color: Color(0xFF2E7D32)),
                          padding: const EdgeInsets.symmetric(vertical: 6),
                        ),
                        onPressed: () => _openDetailSheet(appt),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: OutlinedButton.icon(
                        icon: const Icon(Icons.cancel_outlined, size: 16),
                        label: const Text('Reject'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: const Color(0xFFC62828),
                          side: const BorderSide(color: Color(0xFFC62828)),
                          padding: const EdgeInsets.symmetric(vertical: 6),
                        ),
                        onPressed: () => _openDetailSheet(appt),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _eventTypeBadge(String type) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: const Color(0xFFE8EAF6),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        type,
        style: const TextStyle(
          color: Color(0xFF3730A3),
          fontSize: 10,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _statusBadge(String status, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withAlpha(26),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withAlpha(77)),
      ),
      child: Text(
        status.replaceAll('_', ' '),
        style: TextStyle(
          color: color,
          fontSize: 10,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.inbox_outlined, size: 64, color: Color(0xFFBFDBFE)),
          const SizedBox(height: 16),
          const Text(
            'No Pending Appointments',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Color(0xFF1A237E),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'All appointments have been reviewed.',
            style: TextStyle(color: Colors.grey[600]),
          ),
        ],
      ),
    );
  }
}

// ─── Detail / Action Bottom Sheet ─────────────────────────────────────────────

class _ApproverDetailSheet extends StatefulWidget {
  final _ApproverAppointment appointment;
  final void Function(String remarks) onApprove;
  final void Function(String remarks) onReject;
  final void Function(String date) onReschedule;

  const _ApproverDetailSheet({
    required this.appointment,
    required this.onApprove,
    required this.onReject,
    required this.onReschedule,
  });

  @override
  State<_ApproverDetailSheet> createState() => _ApproverDetailSheetState();
}

class _ApproverDetailSheetState extends State<_ApproverDetailSheet> {
  final _remarksCtrl = TextEditingController();

  @override
  void dispose() {
    _remarksCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appt = widget.appointment;
    final isPending = appt.status == 'PENDING';

    return DraggableScrollableSheet(
      initialChildSize: 0.75,
      minChildSize: 0.4,
      maxChildSize: 0.95,
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
                  color: const Color(0xFFE8EAF6),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  appt.eventType,
                  style: const TextStyle(
                    color: Color(0xFF3730A3),
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                appt.id,
                style: const TextStyle(
                  color: Color(0xFF1E40AF),
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            appt.applicantName,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            '${appt.district} · ${appt.agendaType} · ${appt.location}',
            style: TextStyle(color: Colors.grey[600], fontSize: 13),
          ),
          const SizedBox(height: 16),
          const Divider(),
          const SizedBox(height: 8),
          Text(
            appt.agendaBrief,
            style: TextStyle(color: Colors.grey[700], fontSize: 14),
          ),
          if (appt.shortNotes != null) ...[
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
                          appt.shortNotes!,
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
          if (appt.approverRemarks != null &&
              appt.approverRemarks!.isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFF0FDF4),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFF86EFAC)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.comment_outlined,
                      size: 16, color: Color(0xFF16A34A)),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Approver Remarks',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF065F46),
                            fontSize: 13,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          appt.approverRemarks!,
                          style: const TextStyle(
                            color: Color(0xFF065F46),
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
          if (isPending && appt.appointmentCategory == 'SCHEDULED') ...[
            const SizedBox(height: 20),
            const Divider(),
            const SizedBox(height: 12),
            const Text(
              'Add Remarks',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 14,
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _remarksCtrl,
              maxLines: 3,
              decoration: const InputDecoration(
                hintText: 'Enter routing direction or rejection reason...',
                border: OutlineInputBorder(),
                contentPadding:
                    EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    icon: const Icon(Icons.check_circle_outline),
                    label: const Text('Route to Official'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF2E7D32),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                    onPressed: () {
                      Navigator.pop(context);
                      widget.onApprove(_remarksCtrl.text);
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.cancel_outlined),
                    label: const Text('Reject'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFFC62828),
                      side: const BorderSide(color: Color(0xFFC62828)),
                      padding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                    onPressed: () {
                      Navigator.pop(context);
                      widget.onReject(_remarksCtrl.text);
                    },
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.calendar_today_outlined),
                    label: const Text('Schedule'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF1A237E),
                      side: const BorderSide(color: Color(0xFF1A237E)),
                      padding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                    onPressed: () async {
                      final picked = await showDatePicker(
                        context: context,
                        initialDate:
                            DateTime.now().add(const Duration(days: 1)),
                        firstDate: DateTime.now(),
                        lastDate: DateTime.now().add(const Duration(days: 365)),
                      );
                      if (!context.mounted) return;
                      if (picked != null) {
                        final formatted =
                            '${picked.day}/${picked.month}/${picked.year}';
                        Navigator.pop(context);
                        widget.onReschedule(formatted);
                      }
                    },
                  ),
                ),
              ],
            ),
          ],
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
