Map<String, int> calculateDashboardMetrics(
  List<Map<String, dynamic>> appointments,
  List<Map<String, dynamic>> schemes, {
  DateTime? now,
}) {
  final today = (now ?? DateTime.now()).toLocal();
  const pendingStatuses = {
    'PENDING_APPROVER_REVIEW',
    'APPROVER_REVIEW',
    'HCM_PENDING',
  };
  const inactiveSchemeStatuses = {
    'REJECTED',
    'HCM_REJECTED',
    'CANCELLED',
    'CANCELED',
    'COMPLETED',
    'CLOSED',
  };

  bool isToday(dynamic value) {
    final date = DateTime.tryParse('${value ?? ''}')?.toLocal();
    return date != null &&
        date.year == today.year &&
        date.month == today.month &&
        date.day == today.day;
  }

  return {
    "Today's Appointments":
        appointments.where((row) => isToday(row['scheduledDateTime'])).length,
    'Pending Approvals': appointments
        .where((row) => pendingStatuses.contains('${row['status']}'))
        .length,
    'Walk-ins Today': appointments.where((row) {
      return row['isWalkIn'] == true &&
          isToday(row['createdAt'] ??
              row['submittedAt'] ??
              row['scheduledDateTime']);
    }).length,
    'Active Scheme Apps': schemes.where((row) {
      return !inactiveSchemeStatuses
          .contains('${row['status']}'.trim().toUpperCase());
    }).length,
  };
}
