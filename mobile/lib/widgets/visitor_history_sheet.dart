import 'package:flutter/material.dart';

import '../services/api_service.dart';
import 'authenticated_photo.dart';

class VisitorHistorySheet extends StatefulWidget {
  final int citizenId;
  final String fallbackName;
  final String? fallbackPhotoUrl;

  const VisitorHistorySheet({
    super.key,
    required this.citizenId,
    required this.fallbackName,
    this.fallbackPhotoUrl,
  });

  static Future<void> show(
    BuildContext context, {
    required int citizenId,
    required String fallbackName,
    String? fallbackPhotoUrl,
  }) =>
      showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        useSafeArea: true,
        builder: (_) => VisitorHistorySheet(
          citizenId: citizenId,
          fallbackName: fallbackName,
          fallbackPhotoUrl: fallbackPhotoUrl,
        ),
      );

  @override
  State<VisitorHistorySheet> createState() => _VisitorHistorySheetState();
}

class _VisitorHistorySheetState extends State<VisitorHistorySheet> {
  late Future<Map<String, dynamic>?> _history;

  @override
  void initState() {
    super.initState();
    _history = ApiService.getPublicIdentificationHistory(widget.citizenId);
  }

  @override
  Widget build(BuildContext context) => FractionallySizedBox(
        heightFactor: .92,
        child: Scaffold(
          appBar: AppBar(
            title: const Text('Visitor History'),
            leading: IconButton(
              icon: const Icon(Icons.close),
              onPressed: () => Navigator.pop(context),
            ),
          ),
          body: FutureBuilder<Map<String, dynamic>?>(
            future: _history,
            builder: (_, snapshot) {
              if (snapshot.connectionState != ConnectionState.done) {
                return const Center(child: CircularProgressIndicator());
              }
              final history = snapshot.data;
              if (history == null) return _errorState();
              return _content(history);
            },
          ),
        ),
      );

  Widget _content(Map<String, dynamic> history) {
    final appointments = _maps(history['appointments']);
    final schemes = _maps(history['schemes']);
    final directions = _maps(history['directions'] ?? history['followUps']);
    final photo = _first([history['photoUrl'], widget.fallbackPhotoUrl]);
    return RefreshIndicator(
      onRefresh: () async {
        setState(() => _history =
            ApiService.getPublicIdentificationHistory(widget.citizenId));
        await _history;
      },
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Row(children: [
                ClipOval(
                  child: SizedBox(
                    width: 64,
                    height: 64,
                    child: AuthenticatedPhoto(
                      source: photo,
                      fallback: const ColoredBox(
                        color: Color(0xFFE8EAF6),
                        child: Icon(Icons.person, color: Color(0xFF1A237E)),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                          _first([
                            history['citizenName'],
                            history['name'],
                            widget.fallbackName
                          ]),
                          style: const TextStyle(
                              fontSize: 18, fontWeight: FontWeight.w700)),
                      Text('EPIC: ${_first([
                            history['epicNumber'],
                            history['epic']
                          ], '—')}'),
                      Text('Mobile: ${_first([
                            history['mobile'],
                            history['phoneNumber']
                          ], '—')}'),
                      Text(
                          '${history['visitCount'] ?? appointments.length} visit(s) · Last: ${_date(history['lastVisitedAt'])}'),
                    ],
                  ),
                ),
              ]),
            ),
          ),
          _heading('Previous Appointments'),
          if (appointments.isEmpty) _empty('No previous appointments found.'),
          for (final item in appointments)
            _record(
              _first([item['applicationId'], item['appointmentId']],
                  'Appointment'),
              [
                _date(item['dateTime'] ??
                    item['appointmentDate'] ??
                    item['requestedAt']),
                _first([item['appointmentType'], item['type']]),
                _first([item['department']]),
                _first([item['purpose'], item['agenda']]),
                _first([item['status']]),
                _first([item['remarks']]),
              ],
            ),
          _heading('Schemes'),
          if (schemes.isEmpty) _empty('No scheme applications found.'),
          for (final item in schemes)
            _record(_first([item['schemeName'], item['scheme']], 'Scheme'), [
              _first([item['projectName'], item['project']]),
              _date(item['appliedDate'] ?? item['applicationDate']),
              _first([item['status']]),
              _first([item['amount']]),
              _first([item['remarks']]),
            ]),
          if (directions.isNotEmpty) ...[
            _heading('Directions / Follow-up'),
            for (final item in directions)
              _record(
                  _first([item['directionId'], item['title']], 'Direction'), [
                _first([item['direction'], item['remarks']]),
                _first([item['department']]),
                _first([item['status'], item['followUpStatus']]),
              ]),
          ],
        ],
      ),
    );
  }

  Widget _errorState() => Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Text('Unable to load visitor history.'),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => setState(() => _history =
                ApiService.getPublicIdentificationHistory(widget.citizenId)),
            child: const Text('Retry'),
          ),
        ]),
      );

  Widget _heading(String value) => Padding(
        padding: const EdgeInsets.fromLTRB(4, 20, 4, 6),
        child: Text(value,
            style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
      );

  Widget _empty(String value) => Padding(
        padding: const EdgeInsets.all(12),
        child: Text(value, style: const TextStyle(color: Colors.black54)),
      );

  Widget _record(String title, List<String> values) => Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
            const SizedBox(height: 5),
            Text(values
                .where((value) => value.isNotEmpty && value != '—')
                .join(' · ')),
          ]),
        ),
      );
}

List<Map<String, dynamic>> _maps(dynamic value) => value is List
    ? value
        .whereType<Map>()
        .map((item) => Map<String, dynamic>.from(item))
        .toList()
    : const [];

String _first(List<dynamic> values, [String fallback = '']) {
  for (final value in values) {
    final text = value?.toString().trim() ?? '';
    if (text.isNotEmpty && text.toLowerCase() != 'null') return text;
  }
  return fallback;
}

String _date(dynamic value) {
  final text = _first([value]);
  if (text.isEmpty) return '—';
  final parsed = DateTime.tryParse(text)?.toLocal();
  return parsed == null
      ? text
      : '${parsed.day.toString().padLeft(2, '0')}/${parsed.month.toString().padLeft(2, '0')}/${parsed.year}';
}
