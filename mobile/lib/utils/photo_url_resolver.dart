import '../core/config/app_config.dart';

String? resolvePhotoUrl(String? value) {
  final source = value?.trim() ?? '';
  if (source.isEmpty) return null;
  if (source.toLowerCase().startsWith('data:image/') ||
      source.startsWith('blob:')) {
    return source;
  }
  if (_looksLikeBase64Image(source)) {
    final mimeType = source.startsWith('iVBOR') ? 'image/png' : 'image/jpeg';
    return 'data:$mimeType;base64,$source';
  }
  final parsed = Uri.tryParse(source);
  if (parsed != null && (parsed.scheme == 'http' || parsed.scheme == 'https')) {
    return source;
  }

  final api = Uri.parse(AppConfig.apiV1BaseUrl);
  final origin = api.replace(path: '', query: null, fragment: null);
  final relativePath = source.replaceFirst(RegExp(r'^/+'), '');
  final uploadPath = relativePath.startsWith('uploads/')
      ? relativePath
      : 'uploads/$relativePath';
  return origin.resolve('/$uploadPath').toString();
}

bool _looksLikeBase64Image(String value) {
  if (value.length < 32 || value.length % 4 != 0) return false;
  if (!(value.startsWith('/9j/') || value.startsWith('iVBOR'))) return false;
  return RegExp(r'^[A-Za-z0-9+/]+={0,2}$').hasMatch(value);
}
