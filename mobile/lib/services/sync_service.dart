import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api_service.dart';
import 'connectivity_service.dart';
import 'offline_repository.dart';

class SyncService extends ChangeNotifier {
  SyncService({
    required ConnectivityService connectivity,
    OfflineRepository? repository,
  })  : _connectivity = connectivity,
        _repository = repository ?? OfflineRepository() {
    _connectivity.addListener(_onConnectivityChanged);
  }

  final ConnectivityService _connectivity;
  final OfflineRepository _repository;
  bool _syncing = false;
  String? _lastMessage;
  DateTime? _lastSyncAt;

  bool get syncing => _syncing;
  String? get lastMessage => _lastMessage;
  DateTime? get lastSyncAt => _lastSyncAt;

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString('meghaconnect.offline.last_sync_at');
    _lastSyncAt = raw == null ? null : DateTime.tryParse(raw);
  }

  void _onConnectivityChanged() {
    if (_connectivity.isOnline) syncNow();
  }

  Future<void> syncNow() async {
    if (_syncing || _connectivity.isOffline) return;
    _syncing = true;
    _lastMessage = null;
    notifyListeners();
    var failed = 0;
    try {
      await preloadRoleData();
      final queue = await _repository.pendingQueue(includeFailed: true);
      queue.sort((a, b) => _syncRank(a['entityType'].toString())
          .compareTo(_syncRank(b['entityType'].toString())));
      for (final item in queue) {
        if (item['status'] == SyncState.processing) continue;
        final id = item['id'].toString();
        await _repository.markQueueProcessing(id);
        final payload =
            jsonDecode(item['payloadJson'] as String) as Map<String, dynamic>;
        final result = await _uploadQueueItem(
          item['entityType'].toString(),
          item['action'].toString(),
          payload,
        );
        if (result['success'] == true) {
          await _repository.markQueueSynced(id);
        } else {
          final code = result['code']?.toString().toUpperCase() ?? '';
          failed++;
          await _repository.markQueueFailed(
            id,
            result['message']?.toString() ?? 'Sync failed. Please try again.',
            needsReview: code.contains('DUPLICATE') || code.contains('REVIEW'),
          );
        }
      }
      await preloadRoleData();
      _lastSyncAt = DateTime.now();
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(
        'meghaconnect.offline.last_sync_at',
        _lastSyncAt!.toIso8601String(),
      );
      _lastMessage = failed == 0
          ? 'Sync completed successfully.'
          : 'Some records failed to sync. Please try again.';
    } catch (error, stackTrace) {
      debugPrint('SyncService.syncNow failed: $error');
      debugPrint(stackTrace.toString());
      _lastMessage = 'Some records failed to sync. Please try again.';
    } finally {
      _syncing = false;
      notifyListeners();
    }
  }

  Future<void> preloadRoleData() async {
    if (_connectivity.isOffline) return;
    final master = await ApiService.getSyncMasterData();
    if (master['success'] == true) {
      await _repository.cacheMasterData('ROLE_MASTER_DATA', master);
    }
    final preload = await ApiService.getSyncAppointmentPreload();
    final rows = preload['content'];
    if (rows is List) {
      for (final row in rows.whereType<Map>()) {
        await _repository.cacheAppointment(Map<String, dynamic>.from(row));
      }
    }
    final qrCache = await ApiService.getSyncQrCache();
    if (qrCache['success'] == true) {
      await _repository.cacheMasterData('QR_PASS_CACHE', qrCache);
    }
  }

  Future<Map<String, dynamic>> _uploadQueueItem(
    String entityType,
    String action,
    Map<String, dynamic> payload,
  ) {
    switch (entityType) {
      case SyncEntityType.visitor:
        return ApiService.syncVisitor(payload);
      case SyncEntityType.photo:
        return ApiService.syncVisitorPhoto(payload);
      case SyncEntityType.appointment:
        return ApiService.syncAppointment(payload);
      case SyncEntityType.aiNote:
        return ApiService.syncAiNote(payload);
      case SyncEntityType.action:
      case SyncEntityType.qrScan:
        return ApiService.syncStatusUpdate(payload);
      default:
        return Future.value({
          'success': false,
          'message': 'Unsupported sync item. Please contact support.',
        });
    }
  }

  int _syncRank(String entityType) {
    switch (entityType) {
      case SyncEntityType.visitor:
        return 1;
      case SyncEntityType.photo:
        return 2;
      case SyncEntityType.appointment:
        return 3;
      case SyncEntityType.action:
        return 4;
      case SyncEntityType.qrScan:
        return 5;
      case SyncEntityType.aiNote:
        return 6;
      default:
        return 99;
    }
  }

  @override
  void dispose() {
    _connectivity.removeListener(_onConnectivityChanged);
    super.dispose();
  }
}
