import '../core/config/app_config.dart';

String? resolvePhotoUrl(String? value) {
  final source = value?.trim() ?? '';
  if (source.isEmpty) return null;
  if (source.startsWith('data:image/') || source.startsWith('blob:')) {
    return source;
  }
  final parsed = Uri.tryParse(source);
  if (parsed != null && (parsed.scheme == 'http' || parsed.scheme == 'https')) {
    return source;
  }

  final api = Uri.parse(AppConfig.apiV1BaseUrl);
  final origin = api.replace(path: '', query: null, fragment: null);
  return origin
      .resolve('/${source.replaceFirst(RegExp(r'^/+'), '')}')
      .toString();
}
