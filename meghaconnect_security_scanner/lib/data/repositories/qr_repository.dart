import '../../core/config/app_environment.dart';
import '../../core/utils/device_id_provider.dart';
import '../api/qr_api.dart';
import '../models/qr_action_result.dart';
import '../models/qr_payloads.dart';
import '../models/recent_scan.dart';
import '../models/visitor_details.dart';
import 'recent_scan_store.dart';

class QrRepository {
  QrRepository({
    required QrApi qrApi,
    required DeviceIdProvider deviceIdProvider,
    required RecentScanStore recentScanStore,
    required AppEnvironment environment,
  })  : _qrApi = qrApi,
        _deviceIdProvider = deviceIdProvider,
        _recentScanStore = recentScanStore,
        _environment = environment;

  final QrApi _qrApi;
  final DeviceIdProvider _deviceIdProvider;
  final RecentScanStore _recentScanStore;
  final AppEnvironment _environment;

  List<RecentScan> get localRecentScans => _recentScanStore.items;

  Future<VisitorDetails> validateQr({
    required String qrToken,
    String? gateName,
    String? location,
  }) async {
    final deviceId = await _deviceIdProvider.getDeviceId();
    final details = await _qrApi.validate(
      QrValidationPayload(
        qrToken: qrToken.trim(),
        deviceId: deviceId,
        gateName: gateName,
        location: location,
      ),
    );
    _recentScanStore.recordValidation(details);
    return details;
  }

  Future<QrActionResult> checkIn({
    required VisitorDetails visitor,
    String? gateName,
    String? location,
  }) async {
    final result = await _qrApi.checkIn(
      QrActionPayload(
        qrToken: visitor.qrToken,
        deviceId: await _deviceIdProvider.getDeviceId(),
        gateName: gateName,
        location: location,
      ),
    );
    _recentScanStore.recordAction(visitor, result);
    return result;
  }

  Future<QrActionResult> checkOut({
    required VisitorDetails visitor,
    String? gateName,
    String? location,
  }) async {
    final result = await _qrApi.checkOut(
      QrActionPayload(
        qrToken: visitor.qrToken,
        deviceId: await _deviceIdProvider.getDeviceId(),
        gateName: gateName,
        location: location,
      ),
    );
    _recentScanStore.recordAction(visitor, result);
    return result;
  }

  Future<List<RecentScan>> recentScans() async {
    if (!_environment.useBackendRecentScans) {
      return _recentScanStore.items;
    }

    final scans = await _qrApi.recentScans();
    _recentScanStore.replaceFromBackend(scans);
    return _recentScanStore.items;
  }

  void clearLocalState() {
    _recentScanStore.clear();
  }
}
