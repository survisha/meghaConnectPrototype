import 'package:dio/dio.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/json_helpers.dart';
import '../models/qr_action_result.dart';
import '../models/qr_payloads.dart';
import '../models/recent_scan.dart';
import '../models/visitor_details.dart';
import 'qr_api.dart';

class RestQrApi implements QrApi {
  RestQrApi(this._dio);

  final Dio _dio;

  @override
  Future<VisitorDetails> validate(QrValidationPayload payload) async {
    try {
      final response = await _dio.post(
        '/api/v1/qr/validate',
        data: payload.toJson(),
      );
      return VisitorDetails.fromJson(
        unwrapData(response.data),
        qrToken: payload.qrToken,
      );
    } on DioException catch (error) {
      throw ApiException.fromDio(error);
    }
  }

  @override
  Future<QrActionResult> checkIn(QrActionPayload payload) async {
    return _postAction(
      '/api/v1/qr/check-in',
      payload,
      fallbackAction: 'CHECK_IN',
    );
  }

  @override
  Future<QrActionResult> checkOut(QrActionPayload payload) async {
    return _postAction(
      '/api/v1/qr/check-out',
      payload,
      fallbackAction: 'CHECK_OUT',
    );
  }

  @override
  Future<List<RecentScan>> recentScans() async {
    try {
      final response = await _dio.get('/api/v1/qr/recent-scans');
      return unwrapList(response.data)
          .map((item) => RecentScan.fromJson(asMap(item)))
          .toList(growable: false);
    } on DioException catch (error) {
      if (error.response?.statusCode == 404) {
        return const <RecentScan>[];
      }
      throw ApiException.fromDio(error);
    }
  }

  Future<QrActionResult> _postAction(
    String path,
    QrActionPayload payload, {
    required String fallbackAction,
  }) async {
    try {
      final response = await _dio.post(path, data: payload.toJson());
      return QrActionResult.fromJson(
        unwrapData(response.data),
        fallbackAction: fallbackAction,
      );
    } on DioException catch (error) {
      throw ApiException.fromDio(error);
    }
  }
}
