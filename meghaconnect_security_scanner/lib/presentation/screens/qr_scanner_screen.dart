import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../state/app_state.dart';
import 'visitor_details_screen.dart';

class QrScannerScreen extends StatefulWidget {
  const QrScannerScreen({super.key});

  @override
  State<QrScannerScreen> createState() => _QrScannerScreenState();
}

class _QrScannerScreenState extends State<QrScannerScreen> {
  late final MobileScannerController _controller;
  bool _processing = false;

  @override
  void initState() {
    super.initState();
    _controller = MobileScannerController(
      detectionSpeed: DetectionSpeed.noDuplicates,
      formats: const [BarcodeFormat.qrCode],
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final gateName = appState.session?.gateName;

    return Scaffold(
      appBar: AppBar(
        title: const Text('QR Code Scanner'),
        actions: [
          IconButton(
            tooltip: 'Manual token',
            onPressed: _processing ? null : _showManualTokenDialog,
            icon: const Icon(Icons.keyboard_alt_outlined),
          ),
          IconButton(
            tooltip: 'Torch',
            onPressed: _processing ? null : _toggleTorch,
            icon: const Icon(Icons.flash_on_outlined),
          ),
          IconButton(
            tooltip: 'Switch camera',
            onPressed: _processing ? null : _switchCamera,
            icon: const Icon(Icons.cameraswitch_outlined),
          ),
        ],
      ),
      body: Stack(
        fit: StackFit.expand,
        children: [
          MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
            errorBuilder: (context, error, child) {
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.no_photography_outlined,
                        size: 48,
                        color: Colors.redAccent,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        _scannerErrorMessage(error),
                        textAlign: TextAlign.center,
                        style: const TextStyle(fontWeight: FontWeight.w700),
                      ),
                      const SizedBox(height: 12),
                      OutlinedButton.icon(
                        onPressed: _safeStartScanner,
                        icon: const Icon(Icons.refresh),
                        label: const Text('Try again'),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          Center(
            child: Container(
              width: 260,
              height: 260,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.tealAccent.shade400, width: 3),
                borderRadius: BorderRadius.circular(8),
              ),
            ),
          ),
          Positioned(
            left: 20,
            right: 20,
            bottom: 24,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.72),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    const Icon(Icons.fmd_good_outlined, color: Colors.white),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        gateName ?? 'Gate not assigned',
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    if (_processing)
                      const SizedBox.square(
                        dimension: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _onDetect(BarcodeCapture capture) {
    final token = _firstToken(capture);
    if (token == null || _processing) {
      return;
    }
    _validateToken(token);
  }

  String? _firstToken(BarcodeCapture capture) {
    for (final barcode in capture.barcodes) {
      final value = _normalizeQrToken(barcode.rawValue);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  String? _normalizeQrToken(String? rawValue) {
    final raw = rawValue?.trim();
    if (raw == null || raw.isEmpty) return null;

    final fromJson = _tokenFromJson(raw);
    if (fromJson != null) return fromJson;

    final uri = Uri.tryParse(raw);
    if (uri != null) {
      final queryToken = _firstNonEmpty([
        uri.queryParameters['qrToken'],
        uri.queryParameters['qrData'],
        uri.queryParameters['token'],
        uri.queryParameters['passToken'],
      ]);
      if (queryToken != null) return queryToken;

      final pathToken =
          uri.pathSegments.isNotEmpty ? uri.pathSegments.last : '';
      if (uri.hasScheme && pathToken.trim().isNotEmpty) {
        return Uri.decodeComponent(pathToken.trim());
      }
    }

    return raw;
  }

  String? _tokenFromJson(String raw) {
    if (!raw.startsWith('{')) return null;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! Map) return null;
      return _firstNonEmpty([
        decoded['qrToken']?.toString(),
        decoded['qrData']?.toString(),
        decoded['token']?.toString(),
        decoded['passToken']?.toString(),
      ]);
    } catch (_) {
      return null;
    }
  }

  String? _firstNonEmpty(Iterable<String?> values) {
    for (final value in values) {
      final trimmed = value?.trim();
      if (trimmed != null && trimmed.isNotEmpty) return trimmed;
    }
    return null;
  }

  Future<void> _validateToken(String token) async {
    final appState = context.read<AppState>();
    setState(() => _processing = true);
    await _safeStopScanner();

    try {
      await appState.validateQr(token);
      if (!mounted) {
        return;
      }
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => const VisitorDetailsScreen(),
        ),
      );
    } on ApiException catch (error) {
      if (!mounted) {
        return;
      }
      _showMessage(error.message);
    } catch (_) {
      if (!mounted) {
        return;
      }
      _showMessage('Unable to validate this QR token. Please try again.');
    } finally {
      if (mounted) {
        setState(() => _processing = false);
        await _safeStartScanner();
      }
    }
  }

  Future<void> _showManualTokenDialog() async {
    final controller = TextEditingController();
    final token = await showDialog<String>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Manual token'),
          content: TextField(
            controller: controller,
            autofocus: true,
            decoration: const InputDecoration(labelText: 'QR token'),
            textInputAction: TextInputAction.done,
            onSubmitted: (value) => Navigator.of(context).pop(value),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(controller.text),
              child: const Text('Validate'),
            ),
          ],
        );
      },
    );
    controller.dispose();

    final trimmed = _normalizeQrToken(token);
    if (trimmed != null && trimmed.isNotEmpty) {
      await _validateToken(trimmed);
    }
  }

  Future<void> _safeStopScanner() async {
    try {
      await _controller.stop();
    } catch (_) {
      // The controller can be uninitialized for a moment while permissions open.
    }
  }

  Future<void> _safeStartScanner() async {
    try {
      await _controller.start();
    } catch (_) {
      // The scanner screen remains usable through manual token entry.
    }
  }

  Future<void> _toggleTorch() async {
    try {
      await _controller.toggleTorch();
    } catch (_) {
      _showMessage('Torch is not available.');
    }
  }

  Future<void> _switchCamera() async {
    try {
      await _controller.switchCamera();
    } catch (_) {
      _showMessage('Camera switch is not available.');
    }
  }

  String _scannerErrorMessage(MobileScannerException error) {
    final message = error.errorDetails?.message?.trim();
    if (message != null && message.isNotEmpty) {
      final lower = message.toLowerCase();
      if (lower.contains('permission') || lower.contains('denied')) {
        return 'Camera permission denied. Please allow camera access from app settings.';
      }
      if (lower.contains('in use') || lower.contains('busy')) {
        return 'Camera is busy. Close other camera apps and try again.';
      }
      if (lower.contains('not found') || lower.contains('no camera')) {
        return 'No camera found on this device.';
      }
    }
    return 'Camera is unavailable on this device. You can enter the QR token manually.';
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }
}
