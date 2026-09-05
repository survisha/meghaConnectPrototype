import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:uuid/uuid.dart';

import '../services/api_service.dart';

class VoiceRemarkField extends StatefulWidget {
  final TextEditingController controller;
  final String referenceType;
  final String referenceId;
  final String label;
  final String? hint;
  final int minLines;
  final int maxLines;

  const VoiceRemarkField({super.key, required this.controller, required this.referenceType,
    required this.referenceId, this.label = 'Remarks', this.hint, this.minLines = 3, this.maxLines = 5});

  @override State<VoiceRemarkField> createState() => _VoiceRemarkFieldState();
}

class _VoiceRemarkFieldState extends State<VoiceRemarkField> {
  final AudioRecorder _recorder = AudioRecorder();
  final AudioPlayer _player = AudioPlayer();
  Timer? _clock;
  Timer? _poll;
  DateTime? _started;
  String? _localPath;
  String? _requestId;
  int? _voiceRemarkId;
  int _seconds = 0;
  int _durationMs = 0;
  bool _recording = false;
  bool _uploading = false;
  String _status = '';
  String? _transcript;

  @override void initState() {
    super.initState();
    _loadSavedVoice();
  }

  Future<void> _loadSavedVoice() async {
    final rows = await ApiService.getVoiceRemarks(referenceType: widget.referenceType, referenceId: widget.referenceId);
    if (!mounted || rows.isEmpty) return;
    final latest = rows.first;
    setState(() {
      _voiceRemarkId = (latest['voiceRemarkId'] as num?)?.toInt();
      final state = latest['transcriptionStatus']?.toString();
      _status = state == 'COMPLETED' ? 'Saved voice transcription available.' : 'A saved voice is awaiting transcription.';
    });
    _applyOrOffer(latest['transcript']?.toString());
  }

  @override void dispose() {
    _clock?.cancel(); _poll?.cancel(); _recorder.dispose(); _player.dispose(); super.dispose();
  }

  Future<void> _toggleRecording() async {
    if (_recording) { await _stopAndUpload(); return; }
    if (!await _recorder.hasPermission()) { _setStatus('Microphone permission is required.'); return; }
    final dir = await getTemporaryDirectory();
    _requestId = const Uuid().v4();
    _localPath = '${dir.path}${Platform.pathSeparator}voice_$_requestId.m4a';
    await _recorder.start(const RecordConfig(encoder: AudioEncoder.aacLc, sampleRate: 44100, bitRate: 128000, numChannels: 1), path: _localPath!);
    _started = DateTime.now();
    setState(() { _recording = true; _seconds = 0; _status = 'Recording… Keep the phone close to the speaker.'; });
    _clock = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() => _seconds++);
      if (_seconds >= 60) _stopAndUpload();
    });
  }

  Future<void> _stopAndUpload() async {
    _clock?.cancel();
    final path = _recording ? (await _recorder.stop() ?? _localPath) : _localPath;
    if (_recording) _durationMs = DateTime.now().difference(_started ?? DateTime.now()).inMilliseconds;
    if (!mounted || path == null) return;
    setState(() { _recording = false; _uploading = true; _status = 'Saving voice…'; });
    final result = await ApiService.uploadVoiceRemark(audioPath: path, referenceType: widget.referenceType,
      referenceId: widget.referenceId, requestId: _requestId!, durationMs: _durationMs);
    if (!mounted) return;
    final stored = result['audioStored'] == true;
    setState(() {
      _uploading = false; _voiceRemarkId = (result['voiceRemarkId'] as num?)?.toInt();
      _status = stored ? '✓ Voice saved. Transcribing in the background…' : (result['message']?.toString() ?? 'Upload failed. Tap retry.');
    });
    if (stored) {
      try { await File(path).delete(); } catch (_) {}
      _localPath = null;
      _applyOrOffer(result['transcript']?.toString());
      if (_voiceRemarkId != null && result['transcriptionStatus'] != 'COMPLETED') _startPolling();
    }
  }

  void _startPolling() {
    var attempts = 0;
    _poll?.cancel();
    _poll = Timer.periodic(const Duration(seconds: 5), (timer) async {
      if (++attempts > 12 || !mounted) { timer.cancel(); return; }
      final value = await ApiService.getVoiceRemark(_voiceRemarkId!);
      if (!mounted || value == null) return;
      final state = value['transcriptionStatus']?.toString();
      if (state == 'COMPLETED') {
        timer.cancel();
        setState(() => _status = value['needsReview'] == true ? 'Transcription ready — please review for noise.' : 'Transcription ready for review.');
        _applyOrOffer(value['transcript']?.toString());
      } else if (state == 'FAILED') {
        timer.cancel(); setState(() => _status = 'Voice saved. Transcription will be retried later.');
      }
    });
  }

  void _applyOrOffer(String? value) {
    final text = value?.trim() ?? '';
    if (text.isEmpty || !mounted) return;
    if (widget.controller.text.trim().isEmpty) {
      widget.controller.text = text;
      widget.controller.selection = TextSelection.collapsed(offset: text.length);
    } else { setState(() => _transcript = text); }
  }

  Future<void> _play() async {
    if (_voiceRemarkId == null) return;
    final bytes = await ApiService.downloadVoiceRemark(_voiceRemarkId!);
    if (bytes == null) { _setStatus('Unable to play the saved voice.'); return; }
    await _player.play(BytesSource(Uint8List.fromList(bytes)));
  }

  void _setStatus(String value) { if (mounted) setState(() => _status = value); }

  @override Widget build(BuildContext context) {
    final mm = (_seconds ~/ 60).toString().padLeft(2, '0');
    final ss = (_seconds % 60).toString().padLeft(2, '0');
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      TextField(controller: widget.controller, minLines: widget.minLines, maxLines: widget.maxLines,
        decoration: InputDecoration(labelText: widget.label, hintText: widget.hint, alignLabelWithHint: true,
          border: const OutlineInputBorder(), suffixIcon: IconButton(
            tooltip: _recording ? 'Stop recording' : 'Record voice remark',
            onPressed: _uploading ? null : _toggleRecording,
            icon: Icon(_recording ? Icons.stop_circle : Icons.mic, color: _recording ? Colors.red : const Color(0xFF1A237E)),))),
      if (_status.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 6), child: Text(_recording ? '$_status  $mm:$ss' : _status,
        style: TextStyle(fontSize: 12, color: _status.startsWith('✓') ? Colors.green[700] : Colors.grey[700]))),
      if (!_uploading && _localPath != null) TextButton.icon(onPressed: _stopAndUpload, icon: const Icon(Icons.refresh), label: const Text('Retry upload')),
      if (_voiceRemarkId != null) TextButton.icon(onPressed: _play, icon: const Icon(Icons.play_arrow), label: const Text('Play saved voice')),
      if (_transcript != null) Wrap(spacing: 8, children: [
        OutlinedButton(onPressed: () { widget.controller.text = '${widget.controller.text.trim()}\n${_transcript!}'; setState(() => _transcript = null); }, child: const Text('Add transcript')),
        OutlinedButton(onPressed: () { widget.controller.text = _transcript!; setState(() => _transcript = null); }, child: const Text('Replace with transcript')),
      ]),
    ]);
  }
}
