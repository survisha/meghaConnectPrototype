import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';
import 'package:uuid/uuid.dart';

import 'local_database.dart';

class SyncState {
  static const pending = 'PENDING';
  static const processing = 'PROCESSING';
  static const synced = 'SYNCED';
  static const failed = 'FAILED';
  static const needsReview = 'NEEDS_REVIEW';
}

class SyncEntityType {
  static const visitor = 'VISITOR';
  static const photo = 'PHOTO';
  static const appointment = 'APPOINTMENT';
  static const aiNote = 'AI_NOTE';
  static const action = 'ACTION';
  static const qrScan = 'QR_SCAN';
}

class OfflineSaveResult {
  final String localId;
  final String referenceNumber;
  final Map<String, dynamic> payload;

  const OfflineSaveResult({
    required this.localId,
    required this.referenceNumber,
    required this.payload,
  });
}

class OfflineRepository {
  OfflineRepository({LocalDatabase? database})
      : _database = database ?? LocalDatabase.instance;

  final LocalDatabase _database;
  final _uuid = const Uuid();

  Future<Database> get _db => _database.database;

  String _now() => DateTime.now().toUtc().toIso8601String();

  Future<void> saveSession(Map<String, dynamic> userJson) async {
    final db = await _db;
    await db.insert(
      'user_session',
      {
        'localId': 'current',
        'serverId': userJson['username']?.toString(),
        'payloadJson': jsonEncode(userJson),
        'syncStatus': SyncState.synced,
        'createdOffline': 0,
        'lastModifiedAt': _now(),
        'retryCount': 0,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<OfflineSaveResult> saveVisitorOffline(
    Map<String, dynamic> payload, {
    String? photoDataUri,
    String? localId,
    bool queue = true,
  }) async {
    final db = await _db;
    final id = localId ?? _uuid.v4();
    final reference = 'LOCAL-VIS-${DateTime.now().millisecondsSinceEpoch}';
    final enriched = {
      ...payload,
      'localId': id,
      'localReferenceNumber': reference,
      'syncStatus': SyncState.pending,
      'createdOffline': true,
    };
    await db.insert(
      'visitors',
      {
        'localId': id,
        'serverId': payload['serverId']?.toString(),
        'payloadJson': jsonEncode(enriched),
        'syncStatus': SyncState.pending,
        'createdOffline': 1,
        'lastModifiedAt': _now(),
        'retryCount': 0,
        'localReferenceNumber': reference,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
    if (queue) {
      await enqueue(
        entityType: SyncEntityType.visitor,
        localEntityId: id,
        action: 'CREATE',
        payload: enriched,
      );
    }
    if (photoDataUri != null && photoDataUri.isNotEmpty) {
      await saveVisitorPhotoOffline(
        visitorLocalId: id,
        photoDataUri: photoDataUri,
        queue: queue,
      );
    }
    return OfflineSaveResult(
      localId: id,
      referenceNumber: reference,
      payload: enriched,
    );
  }

  Future<OfflineSaveResult> saveVisitorPhotoOffline({
    required String visitorLocalId,
    required String photoDataUri,
    bool queue = true,
  }) async {
    final db = await _db;
    final id = _uuid.v4();
    final dir = await getApplicationDocumentsDirectory();
    final photoDir = Directory(p.join(dir.path, 'visitor_photos'));
    if (!await photoDir.exists()) await photoDir.create(recursive: true);
    final filePath = p.join(photoDir.path, '$id.jpg');
    final raw = photoDataUri.contains(',')
        ? photoDataUri.split(',').last
        : photoDataUri;
    await File(filePath).writeAsBytes(base64Decode(raw));
    final payload = {
      'localId': id,
      'visitorLocalId': visitorLocalId,
      'filePath': filePath,
      'photoBase64': photoDataUri,
      'syncStatus': SyncState.pending,
      'createdOffline': true,
    };
    await db.insert(
      'visitor_photos',
      {
        'localId': id,
        'visitorLocalId': visitorLocalId,
        'filePath': filePath,
        'payloadJson': jsonEncode(payload),
        'syncStatus': SyncState.pending,
        'createdOffline': 1,
        'lastModifiedAt': _now(),
        'retryCount': 0,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
    if (queue) {
      await enqueue(
        entityType: SyncEntityType.photo,
        localEntityId: id,
        action: 'UPLOAD',
        payload: payload,
      );
    }
    return OfflineSaveResult(
        localId: id, referenceNumber: id, payload: payload);
  }

  Future<OfflineSaveResult> saveAppointmentOffline(
    Map<String, dynamic> payload, {
    String? visitorLocalId,
    bool queue = true,
  }) async {
    final db = await _db;
    final id = _uuid.v4();
    final reference = 'LOCAL-APT-${DateTime.now().millisecondsSinceEpoch}';
    final enriched = {
      ...payload,
      'localId': id,
      'visitorLocalId': visitorLocalId,
      'localAppointmentNumber': reference,
      'syncStatus': SyncState.pending,
      'createdOffline': true,
    };
    await db.insert(
      'appointments',
      {
        'localId': id,
        'visitorLocalId': visitorLocalId,
        'payloadJson': jsonEncode(enriched),
        'syncStatus': SyncState.pending,
        'createdOffline': 1,
        'lastModifiedAt': _now(),
        'retryCount': 0,
        'localAppointmentNumber': reference,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
    if (queue) {
      await enqueue(
        entityType: SyncEntityType.appointment,
        localEntityId: id,
        action: 'CREATE',
        payload: enriched,
      );
    }
    return OfflineSaveResult(
      localId: id,
      referenceNumber: reference,
      payload: enriched,
    );
  }

  Future<OfflineSaveResult> saveAiNoteOffline({
    String? appointmentLocalId,
    required String noteText,
    required Map<String, dynamic> payload,
    bool queue = true,
  }) async {
    final db = await _db;
    final id = _uuid.v4();
    final enriched = {
      ...payload,
      'localId': id,
      'appointmentLocalId': appointmentLocalId,
      'noteText': noteText,
      'locallyGenerated': true,
      'syncStatus': SyncState.pending,
      'createdOffline': true,
    };
    await db.insert(
      'appointment_notes',
      {
        'localId': id,
        'appointmentLocalId': appointmentLocalId,
        'payloadJson': jsonEncode(enriched),
        'noteText': noteText,
        'locallyGenerated': 1,
        'syncStatus': SyncState.pending,
        'createdOffline': 1,
        'lastModifiedAt': _now(),
        'retryCount': 0,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
    if (queue) {
      await enqueue(
        entityType: SyncEntityType.aiNote,
        localEntityId: id,
        action: 'CREATE',
        payload: enriched,
      );
    }
    return OfflineSaveResult(
        localId: id, referenceNumber: id, payload: enriched);
  }

  Future<void> cacheMasterData(String type, dynamic payload) async {
    final db = await _db;
    await db.insert(
      'master_data',
      {
        'localId': type,
        'dataType': type,
        'payloadJson': jsonEncode(payload),
        'syncStatus': SyncState.synced,
        'createdOffline': 0,
        'lastModifiedAt': _now(),
        'retryCount': 0,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> cacheAppointment(Map<String, dynamic> appointment) async {
    final db = await _db;
    final serverId = appointment['id']?.toString() ??
        appointment['applicationId']?.toString() ??
        _uuid.v4();
    await db.insert(
      'appointments',
      {
        'localId': 'server-$serverId',
        'serverId': serverId,
        'payloadJson': jsonEncode(appointment),
        'syncStatus': SyncState.synced,
        'createdOffline': 0,
        'lastModifiedAt': _now(),
        'retryCount': 0,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> enqueue({
    required String entityType,
    required String localEntityId,
    required String action,
    required Map<String, dynamic> payload,
  }) async {
    final db = await _db;
    await db.insert(
      'sync_queue',
      {
        'id': _uuid.v4(),
        'entityType': entityType,
        'localEntityId': localEntityId,
        'action': action,
        'payloadJson': jsonEncode(payload),
        'status': SyncState.pending,
        'retryCount': 0,
        'createdAt': _now(),
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<Map<String, dynamic>>> pendingQueue(
      {bool includeFailed = true}) async {
    final db = await _db;
    return db.query(
      'sync_queue',
      where: includeFailed ? 'status != ?' : 'status = ?',
      whereArgs: includeFailed ? [SyncState.synced] : [SyncState.pending],
      orderBy: 'createdAt ASC',
    );
  }

  Future<Map<String, int>> pendingCounts() async {
    final rows = await pendingQueue();
    final counts = <String, int>{};
    for (final row in rows) {
      final key = row['entityType']?.toString() ?? 'UNKNOWN';
      counts[key] = (counts[key] ?? 0) + 1;
    }
    counts['FAILED'] = rows
        .where((row) =>
            row['status'] == SyncState.failed ||
            row['status'] == SyncState.needsReview)
        .length;
    counts['TOTAL'] = rows.length;
    return counts;
  }

  Future<void> markQueueProcessing(String id) async {
    final db = await _db;
    await db.update(
      'sync_queue',
      {'status': SyncState.processing, 'lastAttemptAt': _now()},
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<void> markQueueSynced(String id) async {
    final db = await _db;
    await db.update(
      'sync_queue',
      {'status': SyncState.synced, 'errorMessage': null},
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<void> markQueueFailed(String id, String message,
      {bool needsReview = false}) async {
    final db = await _db;
    await db.rawUpdate(
      '''
      UPDATE sync_queue
      SET status = ?, retryCount = retryCount + 1, errorMessage = ?, lastAttemptAt = ?
      WHERE id = ?
      ''',
      [
        needsReview ? SyncState.needsReview : SyncState.failed,
        message,
        _now(),
        id
      ],
    );
  }

  Future<List<Map<String, dynamic>>> cachedAppointments() async {
    final db = await _db;
    final rows = await db.query('appointments', orderBy: 'lastModifiedAt DESC');
    return rows.map((row) {
      final payload =
          jsonDecode(row['payloadJson'] as String) as Map<String, dynamic>;
      return {
        ...payload,
        'localId': row['localId'],
        'syncStatus': row['syncStatus'],
        'localAppointmentNumber': row['localAppointmentNumber'],
      };
    }).toList();
  }
}
