import 'package:flutter_test/flutter_test.dart';
import 'package:megha_connect/utils/dashboard_metrics.dart';

void main() {
  final today = DateTime(2026, 9, 1, 12);

  test('uses the same appointment and scheme status rules as Angular', () {
    final metrics = calculateDashboardMetrics([
      {
        'scheduledDateTime': '2026-09-01T10:00:00',
        'status': 'PENDING_APPROVER_REVIEW',
        'isWalkIn': false,
      },
      {
        'scheduledDateTime': '2026-09-02T10:00:00',
        'createdAt': '2026-09-01T09:00:00',
        'status': 'HCM_PENDING',
        'isWalkIn': true,
      },
    ], [
      {'status': 'SUBMITTED'},
      {'status': 'REJECTED'},
    ], now: today);

    expect(metrics["Today's Appointments"], 1);
    expect(metrics['Pending Approvals'], 2);
    expect(metrics['Walk-ins Today'], 1);
    expect(metrics['Active Scheme Apps'], 1);
  });

  test('returns zero instead of demo values for empty API results', () {
    final metrics = calculateDashboardMetrics(const [], const [], now: today);
    expect(metrics.values, everyElement(0));
  });
}
