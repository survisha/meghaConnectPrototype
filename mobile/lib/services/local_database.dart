import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

class LocalDatabase {
  LocalDatabase._();

  static final LocalDatabase instance = LocalDatabase._();
  Database? _db;

  Future<Database> get database async {
    if (_db != null) return _db!;
    final dir = await getApplicationDocumentsDirectory();
    final path = p.join(dir.path, 'megha_connect_offline.db');
    _db = await openDatabase(path, version: 1, onCreate: _create);
    return _db!;
  }

  Future<void> _create(Database db, int version) async {
    await db.execute('''
      CREATE TABLE user_session (
        localId TEXT PRIMARY KEY,
        serverId TEXT,
        payloadJson TEXT NOT NULL,
        syncStatus TEXT NOT NULL,
        createdOffline INTEGER NOT NULL,
        lastModifiedAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        errorMessage TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE visitors (
        localId TEXT PRIMARY KEY,
        serverId TEXT,
        payloadJson TEXT NOT NULL,
        syncStatus TEXT NOT NULL,
        createdOffline INTEGER NOT NULL,
        lastModifiedAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        errorMessage TEXT,
        localReferenceNumber TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE visitor_photos (
        localId TEXT PRIMARY KEY,
        serverId TEXT,
        visitorLocalId TEXT NOT NULL,
        filePath TEXT,
        payloadJson TEXT NOT NULL,
        syncStatus TEXT NOT NULL,
        createdOffline INTEGER NOT NULL,
        lastModifiedAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        errorMessage TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE appointments (
        localId TEXT PRIMARY KEY,
        serverId TEXT,
        visitorLocalId TEXT,
        payloadJson TEXT NOT NULL,
        syncStatus TEXT NOT NULL,
        createdOffline INTEGER NOT NULL,
        lastModifiedAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        errorMessage TEXT,
        localAppointmentNumber TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE appointment_notes (
        localId TEXT PRIMARY KEY,
        serverId TEXT,
        appointmentLocalId TEXT,
        payloadJson TEXT NOT NULL,
        noteText TEXT NOT NULL,
        locallyGenerated INTEGER NOT NULL,
        syncStatus TEXT NOT NULL,
        createdOffline INTEGER NOT NULL,
        lastModifiedAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        errorMessage TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE master_data (
        localId TEXT PRIMARY KEY,
        serverId TEXT,
        dataType TEXT NOT NULL,
        payloadJson TEXT NOT NULL,
        syncStatus TEXT NOT NULL,
        createdOffline INTEGER NOT NULL,
        lastModifiedAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        errorMessage TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE qr_pass_cache (
        localId TEXT PRIMARY KEY,
        serverId TEXT,
        payloadJson TEXT NOT NULL,
        syncStatus TEXT NOT NULL,
        createdOffline INTEGER NOT NULL,
        lastModifiedAt TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        errorMessage TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE sync_queue (
        id TEXT PRIMARY KEY,
        entityType TEXT NOT NULL,
        localEntityId TEXT NOT NULL,
        action TEXT NOT NULL,
        payloadJson TEXT NOT NULL,
        status TEXT NOT NULL,
        retryCount INTEGER NOT NULL DEFAULT 0,
        lastAttemptAt TEXT,
        errorMessage TEXT,
        createdAt TEXT NOT NULL
      )
    ''');
  }
}
