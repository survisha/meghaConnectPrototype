import 'api_service.dart';

class CachedAiNotes {
  final int appointmentId;
  final List<Map<String, dynamic>> notes;
  final DateTime generatedAt;
  final String status;
  final String? error;

  const CachedAiNotes({
    required this.appointmentId,
    required this.notes,
    required this.generatedAt,
    required this.status,
    this.error,
  });

  bool get isExpired =>
      DateTime.now().difference(generatedAt) > const Duration(minutes: 10);
}

class AiNotesCacheService {
  AiNotesCacheService._();

  static final AiNotesCacheService instance = AiNotesCacheService._();

  final Map<int, CachedAiNotes> _cache = {};
  final Map<int, Future<CachedAiNotes>> _inFlight = {};

  CachedAiNotes? get(int appointmentId, {bool allowExpired = false}) {
    final cached = _cache[appointmentId];
    if (cached == null) return null;
    if (!allowExpired && cached.isExpired) return null;
    return cached;
  }

  Future<CachedAiNotes> getOrLoad(int appointmentId, {bool force = false}) {
    if (!force) {
      final cached = get(appointmentId);
      if (cached != null) return Future.value(cached);
      final loading = _inFlight[appointmentId];
      if (loading != null) return loading;
    } else {
      _cache.remove(appointmentId);
    }

    final future = _load(appointmentId);
    _inFlight[appointmentId] = future;
    return future.whenComplete(() => _inFlight.remove(appointmentId));
  }

  Future<CachedAiNotes> _load(int appointmentId) async {
    try {
      final notes = await ApiService.getAiNotesByAppointment(appointmentId);
      final status = _deriveStatus(notes);
      final cached = CachedAiNotes(
        appointmentId: appointmentId,
        notes: notes,
        generatedAt: DateTime.now(),
        status: status,
      );
      _cache[appointmentId] = cached;
      return cached;
    } catch (_) {
      final cached = CachedAiNotes(
        appointmentId: appointmentId,
        notes: const [],
        generatedAt: DateTime.now(),
        status: 'FAILED',
        error: 'Failed to load AI notes',
      );
      _cache[appointmentId] = cached;
      return cached;
    }
  }

  void put(int appointmentId, List<Map<String, dynamic>> notes) {
    _cache[appointmentId] = CachedAiNotes(
      appointmentId: appointmentId,
      notes: notes,
      generatedAt: DateTime.now(),
      status: _deriveStatus(notes),
    );
  }

  void invalidate(int appointmentId) {
    _cache.remove(appointmentId);
    _inFlight.remove(appointmentId);
  }

  void clear() {
    _cache.clear();
    _inFlight.clear();
  }

  String _deriveStatus(List<Map<String, dynamic>> notes) {
    if (notes.isEmpty) return 'NONE';
    if (notes.any((note) => _text(note['status']) == 'FAILED')) {
      return 'FAILED';
    }
    if (notes.any((note) {
      final status = _text(note['status']);
      return status == 'PROCESSING' || status == 'PENDING';
    })) {
      return 'PROCESSING';
    }
    return 'COMPLETED';
  }

  String _text(dynamic value) => (value?.toString() ?? '').toUpperCase().trim();
}
