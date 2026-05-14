import '../models/qr_action_result.dart';
import '../models/qr_payloads.dart';
import '../models/recent_scan.dart';
import '../models/visitor_details.dart';

abstract class QrApi {
  Future<VisitorDetails> validate(QrValidationPayload payload);

  Future<QrActionResult> checkIn(QrActionPayload payload);

  Future<QrActionResult> checkOut(QrActionPayload payload);

  Future<List<RecentScan>> recentScans();
}
