import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../utils/photo_url_resolver.dart';
import '../services/api_service.dart';

class AuthenticatedPhoto extends StatelessWidget {
  final String? source;
  final BoxFit fit;
  final Widget fallback;

  const AuthenticatedPhoto({
    super.key,
    required this.source,
    required this.fallback,
    this.fit = BoxFit.cover,
  });

  @override
  Widget build(BuildContext context) {
    final resolved = resolvePhotoUrl(source);
    if (resolved == null) return fallback;
    if (resolved.toLowerCase().startsWith('data:image/')) {
      final bytes = _decodeDataUrl(resolved);
      return bytes == null
          ? fallback
          : Image.memory(bytes,
              fit: fit, errorBuilder: (_, __, ___) => fallback);
    }
    return FutureBuilder<Map<String, String>>(
      future: ApiService.authenticatedMediaHeaders(),
      builder: (_, snapshot) {
        if (!snapshot.hasData) return fallback;
        return Image.network(
          resolved,
          headers: snapshot.data,
          fit: fit,
          errorBuilder: (_, __, ___) => fallback,
        );
      },
    );
  }
}

Uint8List? _decodeDataUrl(String value) {
  try {
    return base64Decode(value.substring(value.indexOf(',') + 1));
  } catch (_) {
    return null;
  }
}
