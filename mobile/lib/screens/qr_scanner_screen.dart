import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:provider/provider.dart';

import '../models/user.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/notification_service.dart';

class QrScannerScreen extends StatefulWidget {
  const QrScannerScreen({super.key});

  @override
  State<QrScannerScreen> createState() => _QrScannerScreenState();
}

class _QrScannerScreenState extends State<QrScannerScreen>
    with WidgetsBindingObserver {
  late final MobileScannerController _controller;
  bool _processing = false;
  Map<String, dynamic>? _result;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _controller = MobileScannerController(
      detectionSpeed: DetectionSpeed.noDuplicates,
      formats: const [BarcodeFormat.qrCode],
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _result == null) {
      _safeStart();
    } else if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      _safeStop();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthService>().user;
    if (user?.role != UserRole.DEO) {
      return const Center(
        child: Text('You are not authorized to access the QR scanner.'),
      );
    }

    if (_result != null) return _buildResult(_result!);
    return Stack(
      fit: StackFit.expand,
      children: [
        MobileScanner(
          controller: _controller,
          onDetect: _onDetect,
          errorBuilder: (_, __, ___) => const Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: Text(
                'Camera is unavailable. Check camera permission and try again.',
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ),
        Center(
          child: Container(
            width: 260,
            height: 260,
            decoration: BoxDecoration(
              border: Border.all(color: Colors.white, width: 3),
              borderRadius: BorderRadius.circular(18),
            ),
          ),
        ),
        Positioned(
          left: 20,
          right: 20,
          bottom: 28,
          child: Card(
            color: Colors.black87,
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Text(
                _processing
                    ? 'Validating pass…'
                    : 'Point the camera at the visitor pass QR code.',
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white),
              ),
            ),
          ),
        ),
      ],
    );
  }

  void _onDetect(BarcodeCapture capture) {
    if (_processing || _result != null) return;
    for (final barcode in capture.barcodes) {
      final token = _normalizeToken(barcode.rawValue);
      if (token != null) {
        _validate(token);
        return;
      }
    }
  }

  Future<void> _validate(String token) async {
    if (_processing) return;
    final username = context.read<AuthService>().user?.username ?? 'deo';
    setState(() => _processing = true);
    await _safeStop();
    final result = await ApiService.validateQr(
      qrToken: token,
      deviceId: 'meghaconnect-mobile-$username',
    );
    if (!mounted) return;
    if (result['success'] == true && result['valid'] == true) {
      setState(() {
        _processing = false;
        _result = result;
      });
      AppNotificationService.success('Scan Successful');
      return;
    }
    setState(() => _processing = false);
    AppNotificationService.error(
      result['message']?.toString() ?? 'This QR code is invalid or expired.',
    );
    await _safeStart();
  }

  Widget _buildResult(Map<String, dynamic> result) {
    final fields = <(String, String)>[
      ('Applicant Name', _value(result['applicantName'])),
      ('Application ID', _value(result['applicationId'])),
      (
        'Appointment Date/Time',
        _value(result['scheduledDateTime'] ?? result['appointmentDateTime'])
      ),
      ('Status', _value(result['status'])),
      ('Department', _value(result['department'])),
      ('Person to Meet', _value(result['personToMeet'])),
      (
        'Visit Status',
        _value(result['entryExitStatus'] ?? result['movementStatus'])
      ),
    ].where((field) => field.$2.isNotEmpty).toList();
    return SingleChildScrollView(
      padding: const EdgeInsets.all(18),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Icon(Icons.check_circle,
                  color: Color(0xFF059669), size: 60),
              const SizedBox(height: 10),
              const Text(
                'Scan Successful',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 18),
              for (final field in fields)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(
                        width: 130,
                        child: Text(field.$1,
                            style:
                                const TextStyle(fontWeight: FontWeight.w700)),
                      ),
                      Expanded(child: Text(field.$2)),
                    ],
                  ),
                ),
              const SizedBox(height: 10),
              ElevatedButton.icon(
                onPressed: _reset,
                icon: const Icon(Icons.qr_code_scanner),
                label: const Text('Scan Another'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _reset() async {
    setState(() => _result = null);
    await _safeStart();
  }

  String? _normalizeToken(String? rawValue) {
    final raw = rawValue?.trim();
    if (raw == null || raw.isEmpty) return null;
    if (raw.startsWith('{')) {
      try {
        final decoded = jsonDecode(raw);
        if (decoded is Map) {
          for (final key in ['qrToken', 'qrData', 'token', 'passToken']) {
            final value = decoded[key]?.toString().trim();
            if (value != null && value.isNotEmpty) return value;
          }
        }
      } catch (_) {
        return null;
      }
    }
    final uri = Uri.tryParse(raw);
    if (uri != null) {
      for (final key in ['qrToken', 'qrData', 'token', 'passToken']) {
        final value = uri.queryParameters[key]?.trim();
        if (value != null && value.isNotEmpty) return value;
      }
      if (uri.hasScheme && uri.pathSegments.isNotEmpty) {
        return Uri.decodeComponent(uri.pathSegments.last);
      }
    }
    return raw;
  }

  String _value(dynamic value) => value?.toString().trim() ?? '';

  Future<void> _safeStop() async {
    try {
      await _controller.stop();
    } catch (_) {}
  }

  Future<void> _safeStart() async {
    if (!mounted || _processing || _result != null) return;
    try {
      await _controller.start();
    } catch (_) {}
  }
}
