import 'dart:convert';

class JwtDecoder {
  const JwtDecoder._();

  static DateTime? expiresAt(String token) {
    final parts = token.split('.');
    if (parts.length < 2) {
      return null;
    }

    try {
      final payload =
          utf8.decode(base64Url.decode(base64Url.normalize(parts[1])));
      final json = jsonDecode(payload);
      if (json is! Map<String, dynamic>) {
        return null;
      }

      final exp = json['exp'];
      final seconds = switch (exp) {
        int value => value,
        num value => value.toInt(),
        String value => int.tryParse(value),
        _ => null,
      };

      if (seconds == null) {
        return null;
      }

      return DateTime.fromMillisecondsSinceEpoch(
        seconds * 1000,
        isUtc: true,
      ).toLocal();
    } catch (_) {
      return null;
    }
  }
}
