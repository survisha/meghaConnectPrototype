import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:camera/camera.dart';
import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';
import 'package:image/image.dart' as img;

import '../core/config/app_config.dart';
import '../services/api_service.dart';
import '../widgets/megha_ui.dart';

class _VisitorProfile {
  final int id;
  final String fullName;
  final String phoneNumber;
  final String epicNumber;
  final String designation;
  final String district;
  final String constituency;
  final String booth;
  final String village;
  final String kycStatus;
  final String briefProfile;
  final String photoSource;
  final Map<String, dynamic> raw;

  const _VisitorProfile({
    required this.id,
    required this.fullName,
    required this.phoneNumber,
    required this.epicNumber,
    required this.designation,
    required this.district,
    required this.constituency,
    required this.booth,
    required this.village,
    required this.kycStatus,
    required this.briefProfile,
    required this.photoSource,
    required this.raw,
  });

  factory _VisitorProfile.fromJson(Map<String, dynamic> raw) {
    return _VisitorProfile(
      id: _asInt(raw['id']) ?? 0,
      fullName: _firstText([raw['fullName'], raw['name']], '-'),
      phoneNumber: _text(raw['phoneNumber']),
      epicNumber: _text(raw['epicNumber']),
      designation: _text(raw['designation']),
      district: _text(raw['district']),
      constituency: _text(raw['constituency']),
      booth: _text(raw['booth']),
      village: _text(raw['village']),
      kycStatus: _text(raw['kycStatus']),
      briefProfile: _text(raw['briefProfile']),
      photoSource: _photoSource(raw),
      raw: raw,
    );
  }
}

class _PendingFace {
  final String trackingId;
  final String photo;
  final int session;

  const _PendingFace(this.trackingId, this.photo, this.session);
}

enum _FaceResultStatus {
  queued,
  searching,
  matched,
  notRegistered,
  timeout,
  unavailable
}

class _FaceRecognitionResult {
  final String trackingId;
  final String capturedImage;
  _FaceResultStatus status;
  _VisitorProfile? visitor;
  String enrollmentId = '';
  double? matchScore;
  DateTime recognitionTime;
  String message = '';

  _FaceRecognitionResult({
    required this.trackingId,
    required this.capturedImage,
    required this.status,
    required this.recognitionTime,
  });
}

class _MobileFaceTrack {
  final int id;
  int? detectorId;
  Offset center;
  DateTime lastSeen;
  bool active = true;
  bool captured = false;

  _MobileFaceTrack(
      {required this.id,
      required this.detectorId,
      required this.center,
      required this.lastSeen});
}

class _CitizenHistory {
  final int visitCount;
  final String lastVisitedAt;
  final String photoUrl;
  final List<Map<String, dynamic>> schemes;
  final List<Map<String, dynamic>> appointments;

  const _CitizenHistory({
    required this.visitCount,
    required this.lastVisitedAt,
    required this.photoUrl,
    required this.schemes,
    required this.appointments,
  });

  factory _CitizenHistory.fromJson(Map<String, dynamic> raw) {
    return _CitizenHistory(
      visitCount: _asInt(raw['visitCount']) ?? 0,
      lastVisitedAt: _text(raw['lastVisitedAt']),
      photoUrl: _text(raw['photoUrl']),
      schemes: _listOfMaps(raw['schemes']),
      appointments: _listOfMaps(raw['appointments']),
    );
  }
}

class PublicIdentificationScreen extends StatefulWidget {
  const PublicIdentificationScreen({super.key});

  @override
  State<PublicIdentificationScreen> createState() =>
      _PublicIdentificationScreenState();
}

class _PublicIdentificationScreenState
    extends State<PublicIdentificationScreen> {
  final _phoneCtrl = TextEditingController();
  final _epicCtrl = TextEditingController();
  final _nameCtrl = TextEditingController();
  CameraController? _faceCamera;
  final FaceDetector _faceDetector = FaceDetector(
      options: FaceDetectorOptions(
    enableTracking: true,
    performanceMode: FaceDetectorMode.fast,
  ));
  Timer? _faceTimer;
  bool _detectingFaces = false;
  int _activeFaceSearches = 0;
  final List<_PendingFace> _faceQueue = [];
  final Map<int, _MobileFaceTrack> _faceTracks = {};
  final List<_FaceRecognitionResult> _faceResults = [];
  int _nextFaceTrackId = 1;
  int _faceSession = 0;
  static const _faceRetryTimeout = Duration(seconds: 15);
  static const _faceDisappearGrace = Duration(milliseconds: 2400);
  String _district = '';

  List<_VisitorProfile> _results = [];
  _VisitorProfile? _selected;
  _CitizenHistory? _history;
  bool _searched = false;
  bool _searching = false;
  bool _historyLoading = false;
  bool _fullHistoryOpen = false;
  String? _error;
  String? _historyError;

  Future<void> _identifyByFace() async {
    if (_faceCamera != null) {
      await _stopFaceIdentification();
      return;
    }
    try {
      final cameras = await availableCameras();
      final selected = cameras.firstWhere(
        (camera) => camera.lensDirection == CameraLensDirection.front,
        orElse: () => cameras.first,
      );
      final controller = CameraController(selected, ResolutionPreset.medium,
          enableAudio: false);
      await controller.initialize();
      if (!mounted) return controller.dispose();
      setState(() {
        _resetFaceSession();
        _faceCamera = controller;
        _searched = true;
        _error = null;
      });
      _scheduleFaceDetection();
    } catch (_) {
      if (mounted) {
        setState(() =>
            _error = 'Unable to open camera. Please check camera permission.');
      }
    }
  }

  void _scheduleFaceDetection() {
    _faceTimer?.cancel();
    _faceTimer = Timer(const Duration(milliseconds: 800), _detectAndQueueFaces);
  }

  Future<void> _detectAndQueueFaces() async {
    final controller = _faceCamera;
    if (controller == null ||
        !controller.value.isInitialized ||
        _detectingFaces) return;
    _detectingFaces = true;
    try {
      final capture = await controller.takePicture();
      final faces = await _faceDetector
          .processImage(InputImage.fromFilePath(capture.path));
      final bytes = await capture.readAsBytes();
      final source = img.decodeImage(bytes);
      if (source != null) {
        final now = DateTime.now();
        final seenTrackIds = <int>{};
        for (final face in faces) {
          final center = face.boundingBox.center;
          _MobileFaceTrack? track;
          for (final candidate in _faceTracks.values) {
            final detectorMatch = face.trackingId != null &&
                candidate.detectorId == face.trackingId;
            final spatialDistance = (candidate.center - center).distance;
            if (candidate.active &&
                (detectorMatch || spatialDistance < source.width * .14)) {
              track = candidate;
              break;
            }
          }
          if (track == null) {
            for (final candidate in _faceTracks.values) {
              if (!candidate.active &&
                  now.difference(candidate.lastSeen) < _faceRetryTimeout &&
                  (candidate.center - center).distance < source.width * .1) {
                track = candidate;
                break;
              }
            }
          }
          track ??= _MobileFaceTrack(
              id: _nextFaceTrackId++,
              detectorId: face.trackingId,
              center: center,
              lastSeen: now);
          _faceTracks[track.id] = track;
          track.detectorId = face.trackingId ?? track.detectorId;
          track.center = center;
          track.lastSeen = now;
          track.active = true;
          seenTrackIds.add(track.id);
          final qualityOk = face.boundingBox.width >= source.width * .12 &&
              face.boundingBox.height >= source.height * .16 &&
              center.dx >= source.width * .06 &&
              center.dx <= source.width * .94 &&
              center.dy >= source.height * .08 &&
              center.dy <= source.height * .92 &&
              (face.headEulerAngleY ?? 0).abs() <= 18 &&
              (face.headEulerAngleZ ?? 0).abs() <= 12;
          if (!qualityOk || track.captured) continue;
          track.captured = true;
          final crop = _cropFace(source, face.boundingBox);
          final trackingId = 'Face ${track.id}';
          final photo =
              'data:image/jpeg;base64,${base64Encode(img.encodeJpg(crop, quality: 85))}';
          _faceResults.add(_FaceRecognitionResult(
              trackingId: trackingId,
              capturedImage: photo,
              status: _FaceResultStatus.queued,
              recognitionTime: now));
          _faceQueue.add(_PendingFace(trackingId, photo, _faceSession));
        }
        for (final entry in _faceTracks.entries.toList()) {
          final track = entry.value;
          if (!seenTrackIds.contains(track.id) &&
              now.difference(track.lastSeen) > _faceDisappearGrace) {
            track.active = false;
          }
          if (!track.active &&
              now.difference(track.lastSeen) >= _faceRetryTimeout) {
            _faceTracks.remove(entry.key);
          }
        }
        _drainFaceQueue();
      }
      final temporaryCapture = File(capture.path);
      if (await temporaryCapture.exists()) {
        await temporaryCapture.delete();
      }
    } catch (_) {
      if (mounted) {
        setState(() =>
            _error = 'Automatic face detection is temporarily unavailable.');
      }
    } finally {
      _detectingFaces = false;
      if (_faceCamera != null) _scheduleFaceDetection();
    }
  }

  img.Image _cropFace(img.Image source, Rect box) {
    final padX = box.width * .18;
    final padY = box.height * .18;
    final x = math.max(0, (box.left - padX).round());
    final y = math.max(0, (box.top - padY).round());
    final width = math.min(source.width - x, (box.width + padX * 2).round());
    final height = math.min(source.height - y, (box.height + padY * 2).round());
    return img.copyCrop(source, x: x, y: y, width: width, height: height);
  }

  void _drainFaceQueue() {
    while (_activeFaceSearches < 5 && _faceQueue.isNotEmpty) {
      final pending = _faceQueue.removeAt(0);
      _activeFaceSearches++;
      if (mounted) {
        setState(() => _faceResult(pending.trackingId)?.status =
            _FaceResultStatus.searching);
      }
      _searchQueuedFace(pending);
    }
  }

  Future<void> _searchQueuedFace(_PendingFace pending) async {
    try {
      final response = await ApiService.searchVisitorByFace(pending.photo);
      final visitor = response['visitor'];
      if (!mounted || pending.session != _faceSession) return;
      setState(() {
        final result = _faceResult(pending.trackingId);
        if (result == null) return;
        result.recognitionTime = DateTime.now();
        result.enrollmentId = _text(response['enrollmentId']);
        result.matchScore = _asDouble(response['score']);
        result.message = _text(response['message']);
        if (response['success'] != true) {
          result.status = result.message.toLowerCase().contains('timeout')
              ? _FaceResultStatus.timeout
              : _FaceResultStatus.unavailable;
        } else if (response['matched'] == true && visitor is Map) {
          final profile =
              _VisitorProfile.fromJson(Map<String, dynamic>.from(visitor));
          result.status = _FaceResultStatus.matched;
          result.visitor = profile;
          if (!_results.any((item) => item.id == profile.id)) {
            _results.add(profile);
          }
        } else {
          result.status = _FaceResultStatus.notRegistered;
          result.message = result.message.isEmpty
              ? 'Visitor not found in system'
              : result.message;
        }
      });
    } on TimeoutException {
      if (mounted && pending.session == _faceSession) {
        setState(() {
          final result = _faceResult(pending.trackingId);
          result?.status = _FaceResultStatus.timeout;
          result?.message = 'Search timed out.';
        });
      }
    } catch (_) {
      if (mounted && pending.session == _faceSession) {
        setState(() {
          final result = _faceResult(pending.trackingId);
          result?.status = _FaceResultStatus.unavailable;
          result?.message = 'Face recognition service is unavailable.';
        });
      }
    } finally {
      if (pending.session == _faceSession) {
        _activeFaceSearches = math.max(0, _activeFaceSearches - 1);
        _drainFaceQueue();
      }
    }
  }

  Future<void> _stopFaceIdentification() async {
    _faceTimer?.cancel();
    _faceTimer = null;
    final controller = _faceCamera;
    _faceCamera = null;
    _resetFaceSession();
    await controller?.dispose();
    if (mounted) setState(() {});
  }

  void _resetFaceSession() {
    _faceSession++;
    _faceQueue.clear();
    _faceTracks.clear();
    _faceResults.clear();
    _activeFaceSearches = 0;
    _nextFaceTrackId = 1;
  }

  _FaceRecognitionResult? _faceResult(String trackingId) {
    for (final result in _faceResults) {
      if (result.trackingId == trackingId) return result;
    }
    return null;
  }

  static const _districts = [
    'East Khasi Hills',
    'West Khasi Hills',
    'Ri Bhoi',
    'East Jaintia Hills',
    'West Jaintia Hills',
    'East Garo Hills',
    'West Garo Hills',
    'South Garo Hills',
    'North Garo Hills',
  ];

  @override
  void dispose() {
    _faceTimer?.cancel();
    _faceCamera?.dispose();
    _faceDetector.close();
    _phoneCtrl.dispose();
    _epicCtrl.dispose();
    _nameCtrl.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    final phone = _phoneCtrl.text.trim();
    final epic = _epicCtrl.text.trim().toUpperCase();
    final name = _nameCtrl.text.trim();
    final district = _district.trim();

    if (phone.isEmpty && epic.isEmpty && name.isEmpty && district.isEmpty) {
      setState(() {
        _searched = true;
        _error = 'Enter at least one search criteria.';
        _results = [];
        _selected = null;
        _history = null;
      });
      return;
    }
    if (phone.isNotEmpty && phone.length != 10) {
      setState(() => _error = 'Enter valid mobile number.');
      return;
    }

    setState(() {
      _searching = true;
      _searched = true;
      _error = null;
      _historyError = null;
      _selected = null;
      _history = null;
      _results = [];
    });

    try {
      final rows = await _searchRows(
        phone: phone,
        epic: epic,
        name: name,
        district: district,
      );
      final mapped = rows
          .map(_VisitorProfile.fromJson)
          .where((visitor) => _matchesCriteria(
                visitor,
                phone: phone,
                epic: epic,
                name: name,
                district: district,
              ))
          .toList();
      if (!mounted) return;
      setState(() {
        _results = mapped;
        _searching = false;
      });
      if (mapped.isNotEmpty) {
        await _selectVisitor(mapped.first);
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _searching = false;
        _error = 'Failed to search visitor.';
      });
    }
  }

  Future<List<Map<String, dynamic>>> _searchRows({
    required String phone,
    required String epic,
    required String name,
    required String district,
  }) async {
    if (phone.isNotEmpty) {
      return ApiService.searchVisitorsByPhone(phone);
    }
    if (epic.isNotEmpty) {
      final visitor = await ApiService.searchPersonByEpic(epic);
      return visitor == null ? [] : [visitor];
    }
    if (name.isNotEmpty) {
      return (await ApiService.searchPersonsByName(name))
          .whereType<Map>()
          .map((row) => Map<String, dynamic>.from(row))
          .toList();
    }
    return (await ApiService.searchPersonsByDistrict(district))
        .whereType<Map>()
        .map((row) => Map<String, dynamic>.from(row))
        .toList();
  }

  bool _matchesCriteria(
    _VisitorProfile visitor, {
    required String phone,
    required String epic,
    required String name,
    required String district,
  }) {
    if (phone.isNotEmpty && !visitor.phoneNumber.contains(phone)) return false;
    if (epic.isNotEmpty &&
        !visitor.epicNumber.toUpperCase().contains(epic.toUpperCase())) {
      return false;
    }
    if (name.isNotEmpty &&
        !visitor.fullName.toLowerCase().contains(name.toLowerCase())) {
      return false;
    }
    if (district.isNotEmpty &&
        visitor.district.toLowerCase() != district.toLowerCase()) {
      return false;
    }
    return true;
  }

  Future<void> _selectVisitor(_VisitorProfile visitor) async {
    setState(() {
      _selected = visitor;
      _history = null;
      _historyError = null;
      _historyLoading = true;
      _fullHistoryOpen = false;
    });

    if (visitor.id <= 0) {
      setState(() {
        _historyLoading = false;
        _historyError = 'Failed to load visitor history.';
      });
      return;
    }

    final raw = await ApiService.getPublicIdentificationHistory(visitor.id);
    if (!mounted || _selected?.id != visitor.id) return;
    setState(() {
      _historyLoading = false;
      if (raw == null) {
        _historyError = 'Failed to load visitor history.';
      } else {
        _history = _CitizenHistory.fromJson(raw);
      }
    });
  }

  void _clear() {
    _phoneCtrl.clear();
    _epicCtrl.clear();
    _nameCtrl.clear();
    setState(() {
      _district = '';
      _results = [];
      _selected = null;
      _history = null;
      _searched = false;
      _searching = false;
      _historyLoading = false;
      _fullHistoryOpen = false;
      _error = null;
      _historyError = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Row(
          children: [
            Icon(Icons.person_search_outlined, color: MeghaColors.primary),
            SizedBox(width: 8),
            Expanded(
              child: Text(
                'Public Identification',
                style: TextStyle(
                  color: MeghaColors.primary,
                  fontSize: 21,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        const Text(
          'Search for a visitor by phone, EPIC, name, or district.',
          style:
              TextStyle(color: MeghaColors.muted, fontSize: 13, height: 1.35),
        ),
        const SizedBox(height: 16),
        _buildSearchCard(),
        const SizedBox(height: 16),
        _buildResultsCard(),
        if (_faceResults.isNotEmpty) ...[
          const SizedBox(height: 16),
          _buildFaceProfileResults(),
        ],
        if (_selected != null && _faceResults.isEmpty) ...[
          const SizedBox(height: 16),
          _buildProfileDetails(_selected!),
        ],
      ],
    );
  }

  Widget _buildSearchCard() {
    return MeghaSectionCard(
      title: 'Search',
      icon: Icons.search,
      child: Column(
        children: [
          TextField(
            controller: _phoneCtrl,
            keyboardType: TextInputType.phone,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(10),
            ],
            decoration: const InputDecoration(
              labelText: 'Phone Number',
              prefixIcon: Icon(Icons.phone_outlined),
              hintText: 'Mobile number',
            ),
            onSubmitted: (_) => _search(),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _epicCtrl,
            textCapitalization: TextCapitalization.characters,
            inputFormatters: [
              FilteringTextInputFormatter.allow(RegExp('[A-Za-z0-9]')),
            ],
            decoration: const InputDecoration(
              labelText: 'EPIC / Voter ID',
              prefixIcon: Icon(Icons.badge_outlined),
              hintText: 'EPIC number',
            ),
            onSubmitted: (_) => _search(),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _nameCtrl,
            textCapitalization: TextCapitalization.words,
            decoration: const InputDecoration(
              labelText: 'Name',
              prefixIcon: Icon(Icons.person_outline),
              hintText: 'Full or partial name',
            ),
            onSubmitted: (_) => _search(),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            isExpanded: true,
            value: _district,
            decoration: const InputDecoration(
              labelText: 'District',
              prefixIcon: Icon(Icons.place_outlined),
            ),
            items: [
              const DropdownMenuItem(value: '', child: Text('-- Clear --')),
              for (final d in _districts)
                DropdownMenuItem(
                  value: d,
                  child: Text(d, maxLines: 1, overflow: TextOverflow.ellipsis),
                ),
            ],
            onChanged: (value) {
              setState(() {
                _district = value ?? '';
                _results = [];
                _selected = null;
                _history = null;
                _searched = false;
                _error = null;
              });
            },
          ),
          if (_error != null) ...[
            const SizedBox(height: 10),
            _InlineError(_error!),
          ],
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _searching ? null : _search,
                  icon: _searching
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.search),
                  label: Text(_searching ? 'Searching...' : 'Search'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _clear,
                  icon: const Icon(Icons.close),
                  label: const Text('Clear'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          OutlinedButton.icon(
            onPressed: _searching ? null : _identifyByFace,
            icon: const Icon(Icons.camera_alt_outlined),
            label:
                Text(_faceCamera == null ? 'Identify by Face' : 'Close Camera'),
          ),
          if (_faceCamera != null) ...[
            const SizedBox(height: 10),
            AspectRatio(
              aspectRatio: _faceCamera!.value.aspectRatio,
              child: CameraPreview(_faceCamera!),
            ),
            const SizedBox(height: 8),
            const Text('Automatic detection active — look toward the camera.'),
          ],
          if (_faceResults.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
                '${_faceResults.length} face${_faceResults.length == 1 ? '' : 's'} detected',
                style: const TextStyle(color: MeghaColors.muted)),
          ],
        ],
      ),
    );
  }

  Widget _buildResultsCard() {
    return MeghaSectionCard(
      title: 'Results (${_results.length})',
      icon: Icons.list_alt_outlined,
      child: _buildResultsBody(),
    );
  }

  Widget _buildFaceProfileResults() {
    return MeghaSectionCard(
      title: 'Profile Detail (${_faceResults.length})',
      icon: Icons.badge_outlined,
      child: ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: _faceResults.length,
        separatorBuilder: (_, __) => const SizedBox(height: 12),
        itemBuilder: (_, index) =>
            _FaceRecognitionResultCard(result: _faceResults[index]),
      ),
    );
  }

  Widget _buildResultsBody() {
    if (_searching) {
      return const Padding(
        padding: EdgeInsets.all(28),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (!_searched) {
      return _emptyState(
          Icons.info_outline, 'Enter search criteria and click Search.');
    }
    if (_results.isEmpty && _error == null) {
      return _emptyState(
          Icons.search_off_outlined, 'No matching visitor found.');
    }
    if (_results.isEmpty) return const SizedBox.shrink();

    return Column(
      children: [
        for (final person in _results)
          _PersonResultTile(
            person: person,
            selected: _selected?.id == person.id,
            onTap: () => _selectVisitor(person),
          ),
      ],
    );
  }

  Widget _emptyState(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 28, horizontal: 12),
      child: Column(
        children: [
          Icon(icon, size: 52, color: const Color(0xFFD1D5DB)),
          const SizedBox(height: 8),
          Text(
            text,
            textAlign: TextAlign.center,
            style: const TextStyle(color: MeghaColors.muted, fontSize: 13),
          ),
        ],
      ),
    );
  }

  Widget _buildProfileDetails(_VisitorProfile person) {
    final history = _history;
    final schemes = history?.schemes ?? const <Map<String, dynamic>>[];
    final meetings = history?.appointments ?? const <Map<String, dynamic>>[];
    final lastVisited = _text(history?.lastVisitedAt).isNotEmpty
        ? {'dateTime': history!.lastVisitedAt}
        : _lastVisited(meetings);
    final upcomingAppointment = _upcomingAppointment(meetings);
    final photoSource = _firstText([history?.photoUrl, person.photoSource]);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        MeghaSectionCard(
          title: 'Person Profile',
          icon: Icons.badge_outlined,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _VisitorPhoto(
                    name: person.fullName,
                    source: photoSource,
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _display(person.fullName),
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w800,
                            color: MeghaColors.text,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(_display(person.designation),
                            style: const TextStyle(color: MeghaColors.muted)),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 6,
                          runSpacing: 6,
                          children: [
                            if (person.kycStatus.isNotEmpty)
                              _StatusPill(
                                _statusLabel(person.kycStatus),
                                const Color(0xFF065F46),
                              ),
                            if (person.epicNumber.isNotEmpty)
                              const _StatusPill(
                                  'EPIC Verified', Color(0xFF1E40AF)),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const Divider(height: 26),
              _InfoRow('Phone', person.phoneNumber),
              _InfoRow('EPIC', person.epicNumber),
              _InfoRow('District', person.district),
              _InfoRow('Constituency', person.constituency),
              _InfoRow('Booth', person.booth),
              _InfoRow('Village', person.village),
              if (person.briefProfile.trim().isNotEmpty) ...[
                const SizedBox(height: 10),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: MeghaColors.panelBg,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    person.briefProfile,
                    style:
                        const TextStyle(color: MeghaColors.text, fontSize: 13),
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 14),
        MeghaSectionCard(
          title: 'Scheme & Meeting History',
          icon: Icons.history,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_historyLoading)
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Center(child: CircularProgressIndicator()),
                )
              else if (_historyError != null)
                _InlineError(_historyError!)
              else if (history == null ||
                  (schemes.isEmpty &&
                      meetings.isEmpty &&
                      history.lastVisitedAt.isEmpty))
                const Padding(
                  padding: EdgeInsets.all(12),
                  child: Text(
                    'No scheme or meeting history found for this citizen.',
                    style: TextStyle(color: MeghaColors.muted),
                  ),
                )
              else ...[
                _HistorySummary(
                  history,
                  lastVisited: lastVisited,
                  upcomingAppointment: upcomingAppointment,
                ),
                const SizedBox(height: 14),
                _HistorySection(
                  title: 'Scheme History',
                  emptyText: 'No scheme history found for this citizen.',
                  children: [
                    for (final item in schemes.take(3))
                      _SchemeHistoryCard(item),
                  ],
                ),
                const Divider(height: 24),
                _HistorySection(
                  title: 'Meeting History',
                  emptyText: 'No meeting history found for this citizen.',
                  children: [
                    for (final item in meetings.take(3))
                      _MeetingHistoryCard(item),
                  ],
                ),
                if (schemes.length > 3 || meetings.length > 3) ...[
                  const SizedBox(height: 10),
                  OutlinedButton.icon(
                    onPressed: () =>
                        setState(() => _fullHistoryOpen = !_fullHistoryOpen),
                    icon: Icon(_fullHistoryOpen
                        ? Icons.expand_less
                        : Icons.expand_more),
                    label: Text(_fullHistoryOpen
                        ? 'Hide Full Citizen History'
                        : 'View Full Citizen History'),
                  ),
                ],
                if (_fullHistoryOpen) ...[
                  const Divider(height: 24),
                  const Text('Full Citizen History',
                      style: TextStyle(fontWeight: FontWeight.w900)),
                  const SizedBox(height: 10),
                  for (final item in schemes) _SchemeHistoryCard(item),
                  for (final item in meetings) _MeetingHistoryCard(item),
                ],
              ],
            ],
          ),
        ),
      ],
    );
  }
}

class _PersonResultTile extends StatelessWidget {
  final _VisitorProfile person;
  final bool selected;
  final VoidCallback onTap;

  const _PersonResultTile({
    required this.person,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          border: Border(
            bottom: const BorderSide(color: Color(0xFFF3F4F6)),
            left: BorderSide(
              color: selected ? MeghaColors.primary : Colors.transparent,
              width: 3,
            ),
          ),
        ),
        child: Row(
          children: [
            _MiniAvatar(name: person.fullName, source: person.photoSource),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_display(person.fullName),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          color: MeghaColors.text,
                          fontWeight: FontWeight.w700)),
                  Text(_display(person.designation),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          color: MeghaColors.muted, fontSize: 12)),
                  Text(
                    '${_display(person.constituency)}, ${_display(person.district)}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style:
                        const TextStyle(color: Color(0xFF9CA3AF), fontSize: 11),
                  ),
                  if (person.phoneNumber.isNotEmpty)
                    Text(person.phoneNumber,
                        style: const TextStyle(
                            color: Color(0xFF9CA3AF), fontSize: 11)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: Color(0xFF9CA3AF)),
          ],
        ),
      ),
    );
  }
}

class _FaceRecognitionResultCard extends StatelessWidget {
  final _FaceRecognitionResult result;

  const _FaceRecognitionResultCard({required this.result});

  @override
  Widget build(BuildContext context) {
    final visitor = result.visitor;
    final loading = result.status == _FaceResultStatus.queued ||
        result.status == _FaceResultStatus.searching;
    final title = switch (result.status) {
      _FaceResultStatus.queued || _FaceResultStatus.searching => 'Searching…',
      _FaceResultStatus.matched => visitor?.fullName ?? 'Matched',
      _FaceResultStatus.notRegistered => 'Not Registered',
      _FaceResultStatus.timeout => 'Search Timeout',
      _FaceResultStatus.unavailable => 'Service Unavailable',
    };
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE5E7EB)),
        borderRadius: BorderRadius.circular(10),
        color: Colors.white,
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Column(children: [
            const Text('Captured Face',
                style: TextStyle(fontSize: 11, color: MeghaColors.muted)),
            const SizedBox(height: 4),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.memory(_dataUriBytes(result.capturedImage),
                  width: 72, height: 72, fit: BoxFit.cover),
            ),
            if (result.status == _FaceResultStatus.notRegistered) ...[
              const SizedBox(height: 4),
              const SizedBox(
                width: 72,
                child: Text('Not Registered',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                        color: Color(0xFFB91C1C),
                        fontSize: 11,
                        fontWeight: FontWeight.w700)),
              ),
            ],
          ]),
          if (visitor != null) ...[
            const SizedBox(width: 10),
            Column(children: [
              const Text('Visitor Photo',
                  style: TextStyle(fontSize: 11, color: MeghaColors.muted)),
              const SizedBox(height: 4),
              _VisitorPhoto(
                  name: visitor.fullName, source: visitor.photoSource),
            ]),
          ],
          const SizedBox(width: 12),
          Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                Text(result.trackingId,
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 4),
                Row(children: [
                  if (loading)
                    const SizedBox(
                        width: 15,
                        height: 15,
                        child: CircularProgressIndicator(strokeWidth: 2))
                  else
                    Icon(
                        result.status == _FaceResultStatus.matched
                            ? Icons.check_circle
                            : Icons.info_outline,
                        size: 18,
                        color: result.status == _FaceResultStatus.matched
                            ? Colors.green
                            : MeghaColors.muted),
                  const SizedBox(width: 6),
                  Expanded(child: Text(title)),
                ]),
              ])),
        ]),
        if (visitor != null) ...[
          const Divider(height: 24),
          _InfoRow('Enrollment ID', result.enrollmentId),
          _InfoRow('EPIC', visitor.epicNumber),
          _InfoRow('Mobile', visitor.phoneNumber),
          _InfoRow('Department',
              _firstText([visitor.raw['department'], visitor.designation])),
          _InfoRow(
              'Appointment Status', _text(visitor.raw['appointmentStatus'])),
          _InfoRow(
              'Face Match Score', result.matchScore?.toStringAsFixed(2) ?? ''),
          _InfoRow(
              'Recognition Time', result.recognitionTime.toLocal().toString()),
        ] else if (!loading) ...[
          const SizedBox(height: 10),
          Text(
              result.message.isEmpty
                  ? 'Visitor not found in system'
                  : result.message,
              style: const TextStyle(color: MeghaColors.muted)),
        ],
      ]),
    );
  }
}

Uint8List _dataUriBytes(String value) {
  final comma = value.indexOf(',');
  return base64Decode(comma >= 0 ? value.substring(comma + 1) : value);
}

double? _asDouble(dynamic value) {
  if (value is num) return value.toDouble();
  return double.tryParse(value?.toString() ?? '');
}

class _VisitorPhoto extends StatelessWidget {
  final String name;
  final String source;

  const _VisitorPhoto({required this.name, required this.source});

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(10),
      child: SizedBox(
        width: 78,
        height: 88,
        child: _photoWidget(name: name, source: source),
      ),
    );
  }
}

class _MiniAvatar extends StatelessWidget {
  final String name;
  final String source;

  const _MiniAvatar({required this.name, required this.source});

  @override
  Widget build(BuildContext context) {
    return ClipOval(
      child: SizedBox(
        width: 42,
        height: 42,
        child: _photoWidget(name: name, source: source),
      ),
    );
  }
}

Widget _photoWidget({required String name, required String source}) {
  if (source.startsWith('data:image/') || _looksBase64(source)) {
    try {
      final raw = source.contains(',') ? source.split(',').last : source;
      return Image.memory(base64Decode(raw), fit: BoxFit.cover);
    } catch (_) {}
  }
  if (source.startsWith('http')) {
    return Image.network(
      source,
      fit: BoxFit.cover,
      errorBuilder: (_, __, ___) => _AvatarFallback(name),
    );
  }
  return _AvatarFallback(name);
}

class _AvatarFallback extends StatelessWidget {
  final String name;
  const _AvatarFallback(this.name);

  @override
  Widget build(BuildContext context) {
    return Container(
      color: MeghaColors.primary,
      alignment: Alignment.center,
      child: Text(
        (name.trim().isEmpty ? '?' : name.trim()[0]).toUpperCase(),
        style: const TextStyle(
          color: Colors.white,
          fontSize: 22,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _HistorySummary extends StatelessWidget {
  final _CitizenHistory history;
  final Map<String, dynamic>? lastVisited;
  final Map<String, dynamic>? upcomingAppointment;

  const _HistorySummary(
    this.history, {
    required this.lastVisited,
    required this.upcomingAppointment,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _SummaryBox('Total visits', '${history.visitCount}'),
        const SizedBox(height: 8),
        _AppointmentSummaryCard(
          title: 'Last Visited',
          icon: Icons.history,
          item: lastVisited,
          emptyText: 'No previous visit found.',
        ),
        const SizedBox(height: 8),
        _AppointmentSummaryCard(
          title: 'Upcoming Appointment',
          icon: Icons.event_available_outlined,
          item: upcomingAppointment,
          emptyText: 'No upcoming appointment scheduled.',
        ),
      ],
    );
  }
}

class _SummaryBox extends StatelessWidget {
  final String label;
  final String value;
  const _SummaryBox(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: MeghaColors.panelBg,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label,
              style: const TextStyle(color: MeghaColors.muted, fontSize: 11)),
          const SizedBox(height: 4),
          Text(value,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

class _AppointmentSummaryCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final Map<String, dynamic>? item;
  final String emptyText;

  const _AppointmentSummaryCard({
    required this.title,
    required this.icon,
    required this.item,
    required this.emptyText,
  });

  @override
  Widget build(BuildContext context) {
    final record = item;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: const Color(0xFFE5E7EB)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 18, color: MeghaColors.primary),
              const SizedBox(width: 6),
              Text(
                title,
                style: const TextStyle(
                  color: MeghaColors.text,
                  fontSize: 12,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          if (record == null)
            Text(emptyText, style: const TextStyle(color: MeghaColors.muted))
          else ...[
            Text(
              _fmtDateTime(_appointmentDateTime(record)),
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 4),
            if (_text(record['purpose']).isNotEmpty)
              Text(
                _text(record['purpose']),
                style: const TextStyle(color: MeghaColors.text, fontSize: 13),
              ),
            if (_text(record['department']).isNotEmpty) ...[
              const SizedBox(height: 3),
              Text(
                [
                  _text(record['department']),
                  _text(record['officerName']),
                ].where((value) => value.isNotEmpty).join(' / '),
                style: const TextStyle(color: MeghaColors.muted, fontSize: 12),
              ),
            ],
            if (_text(record['status']).isNotEmpty) ...[
              const SizedBox(height: 6),
              _StatusPill(_statusLabel(_text(record['status'])),
                  _statusColor(_text(record['status']))),
            ],
          ],
        ],
      ),
    );
  }
}

class _HistorySection extends StatelessWidget {
  final String title;
  final String emptyText;
  final List<Widget> children;

  const _HistorySection({
    required this.title,
    required this.emptyText,
    required this.children,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
        const SizedBox(height: 8),
        if (children.isEmpty)
          Text(emptyText, style: const TextStyle(color: MeghaColors.muted))
        else
          ...children,
      ],
    );
  }
}

class _SchemeHistoryCard extends StatelessWidget {
  final Map<String, dynamic> item;
  const _SchemeHistoryCard(this.item);

  @override
  Widget build(BuildContext context) {
    return _HistoryCard(
      title: _text(item['schemeName'], '-'),
      subtitle: _text(item['projectName']),
      meta: [
        _fmtDateTime(item['appliedDate']),
        _currency(item['amount']),
      ].where((value) => value != '-').join(' / '),
      status: _text(item['status']),
      remarks: _text(item['remarks']),
    );
  }
}

class _MeetingHistoryCard extends StatelessWidget {
  final Map<String, dynamic> item;
  const _MeetingHistoryCard(this.item);

  @override
  Widget build(BuildContext context) {
    final groupMembers = _listOfMaps(item['groupMembers']);
    return _HistoryCard(
      title: _text(item['purpose'], '-'),
      subtitle: [
        _text(item['department']),
        _text(item['officerName']),
      ].where((value) => value.isNotEmpty).join(' / '),
      meta: [
        _fmtDateTime(_appointmentDateTime(item)),
        _text(item['role']) == 'ASSOCIATE'
            ? 'Associate Visitor'
            : 'Primary Visitor',
      ].join(' / '),
      status: _text(item['status']),
      remarks: [
        _text(item['remarks']),
        if (groupMembers.isNotEmpty)
          'Group: ${groupMembers.map((m) => _text(m['fullName'])).where((v) => v.isNotEmpty).join(', ')}',
      ].where((value) => value.isNotEmpty).join('\n'),
    );
  }
}

class _HistoryCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final String meta;
  final String status;
  final String remarks;

  const _HistoryCard({
    required this.title,
    required this.subtitle,
    required this.meta,
    required this.status,
    required this.remarks,
  });

  @override
  Widget build(BuildContext context) {
    final color = _statusColor(status);
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE5E7EB)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  _display(title),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
              _StatusPill(_statusLabel(status), color),
            ],
          ),
          if (subtitle.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(subtitle, style: const TextStyle(color: MeghaColors.muted)),
          ],
          if (meta.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(meta,
                style: const TextStyle(color: Color(0xFF9CA3AF), fontSize: 12)),
          ],
          if (remarks.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(remarks, style: const TextStyle(fontSize: 12)),
          ],
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 104,
            child: Text(label,
                style: const TextStyle(
                    color: MeghaColors.muted,
                    fontSize: 12,
                    fontWeight: FontWeight.w700)),
          ),
          Expanded(
            child: Text(
              _display(value),
              style: const TextStyle(
                color: MeghaColors.text,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  final String label;
  final Color color;

  const _StatusPill(this.label, this.color);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withAlpha(31),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style:
            TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700),
      ),
    );
  }
}

class _InlineError extends StatelessWidget {
  final String message;
  const _InlineError(this.message);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFEE2E2),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: Color(0xFF991B1B), size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(color: Color(0xFF991B1B), fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}

int? _asInt(dynamic value) {
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '');
}

String _text(dynamic value, [String fallback = '']) {
  final text = value?.toString().trim() ?? '';
  return text.isEmpty ? fallback : text;
}

String _firstText(List<dynamic> values, [String fallback = '']) {
  for (final value in values) {
    final text = _text(value);
    if (text.isNotEmpty) return text;
  }
  return fallback;
}

String _display(String value) => value.trim().isEmpty ? '-' : value.trim();

List<Map<String, dynamic>> _listOfMaps(dynamic value) {
  if (value is! List) return [];
  return value
      .whereType<Map>()
      .map((row) => Map<String, dynamic>.from(row))
      .toList();
}

String _fmtDateTime(dynamic value) {
  final raw = value?.toString().trim() ?? '';
  if (raw.isEmpty) return '-';
  final date = DateTime.tryParse(raw);
  if (date == null) return raw;
  final local = date.toLocal();
  return '${local.day.toString().padLeft(2, '0')}-${_month(local.month)}-${local.year} '
      '${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
}

String _month(int month) {
  const labels = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
  ];
  return labels[month - 1];
}

String _currency(dynamic value) {
  final amount = value is num ? value : num.tryParse(value?.toString() ?? '');
  if (amount == null) return '-';
  return 'Rs. ${amount.toStringAsFixed(0)}';
}

String _statusLabel(String status) {
  final text = status.replaceAll('_', ' ').toLowerCase().trim();
  if (text.isEmpty) return '-';
  return text
      .split(RegExp(r'\s+'))
      .map((word) =>
          word.isEmpty ? word : '${word[0].toUpperCase()}${word.substring(1)}')
      .join(' ');
}

Map<String, dynamic>? _lastVisited(List<Map<String, dynamic>> records) {
  final now = DateTime.now();
  final past = records
      .map((record) =>
          MapEntry(record, _parseHistoryDate(_appointmentDateTime(record))))
      .where((entry) =>
          entry.value != null &&
          entry.value!.isBefore(now) &&
          _isPastVisitStatus(_text(entry.key['status'])))
      .toList()
    ..sort((a, b) => b.value!.compareTo(a.value!));
  return past.isEmpty ? null : past.first.key;
}

Map<String, dynamic>? _upcomingAppointment(List<Map<String, dynamic>> records) {
  final now = DateTime.now();
  final upcoming = records
      .map((record) =>
          MapEntry(record, _parseHistoryDate(_appointmentDateTime(record))))
      .where((entry) =>
          entry.value != null &&
          !entry.value!.isBefore(now) &&
          _text(entry.key['status']).toUpperCase() == 'SCHEDULED')
      .toList()
    ..sort((a, b) => a.value!.compareTo(b.value!));
  return upcoming.isEmpty ? null : upcoming.first.key;
}

String _appointmentDateTime(Map<String, dynamic> item) {
  final dateOnly = _firstText([
    item['appointmentDate'],
    item['visitDate'],
    item['eventDate'],
    item['meetingDate'],
  ]);
  final timeOnly = _text(item['startTime']);
  final combined = dateOnly.isNotEmpty && timeOnly.isNotEmpty
      ? '${dateOnly}T$timeOnly'
      : dateOnly;
  return _firstText([
    item['scheduledAt'],
    item['appointmentDateTime'],
    item['dateTime'],
    combined,
  ]);
}

DateTime? _parseHistoryDate(dynamic value) {
  final raw = value?.toString().trim() ?? '';
  if (raw.isEmpty) return null;
  return DateTime.tryParse(raw)?.toLocal();
}

bool _isPastVisitStatus(String status) {
  final normalized = status.toUpperCase();
  if (normalized.isEmpty) return true;
  return {
    'COMPLETED',
    'VISITED',
    'CLOSED',
    'EXITED',
    'RESOLVED',
  }.contains(normalized);
}

Color _statusColor(String status) {
  final normalized = status.toUpperCase();
  if (['APPROVED', 'COMPLETED', 'RESOLVED', 'HCM_ACCEPTED']
      .contains(normalized)) {
    return const Color(0xFF065F46);
  }
  if (['REJECTED', 'CANCELLED', 'HCM_REJECTED'].contains(normalized)) {
    return const Color(0xFF991B1B);
  }
  if ([
    'PENDING',
    'SUBMITTED',
    'CMO_REVIEW',
    'APPROVER_REVIEW',
    'HCM_PENDING',
    'SCHEDULED'
  ].contains(normalized)) {
    return const Color(0xFFB45309);
  }
  return const Color(0xFF1E40AF);
}

String _photoSource(Map<String, dynamic> raw) {
  final canonical = _firstText([
    raw['photoUrl'],
    raw['photoStoragePath'],
    raw['photoPath'],
  ]);
  if (canonical.isNotEmpty) return _normalizePhotoSource(canonical);
  final fallback = _firstText([
    raw['livePhotoPath'],
    raw['livePhotoBase64'],
    raw['photoBase64'],
  ]);
  return fallback.isEmpty ? '' : _normalizePhotoSource(fallback);
}

String _normalizePhotoSource(String value) {
  final source = value.trim();
  if (source.startsWith('data:image/') ||
      source.startsWith('blob:') ||
      source.startsWith('http') ||
      _looksBase64(source)) {
    return source;
  }
  final origin = AppConfig.apiV1BaseUrl.replaceFirst(RegExp(r'/api/v1/?$'), '');
  final path = source.replaceFirst(RegExp(r'^/+'), '');
  return '$origin/${path.startsWith('uploads/') ? path : 'uploads/$path'}';
}

bool _looksBase64(String value) {
  final text = value.trim();
  if (text.isEmpty || text.startsWith('http')) return false;
  return RegExp(r'^[A-Za-z0-9+/=\r\n]+$').hasMatch(text) && text.length > 80;
}
