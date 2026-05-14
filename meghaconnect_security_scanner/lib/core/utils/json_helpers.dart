Map<String, dynamic> asMap(Object? value) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  if (value is Map) {
    return value.map((key, mapValue) => MapEntry(key.toString(), mapValue));
  }
  return <String, dynamic>{};
}

Map<String, dynamic> unwrapData(Object? value) {
  final map = asMap(value);
  final data = map['data'] ?? map['result'] ?? map['payload'];
  if (data is Map) {
    return asMap(data);
  }
  return map;
}

List<dynamic> unwrapList(Object? value) {
  if (value is List) {
    return value;
  }

  final map = asMap(value);
  final data = map['data'] ?? map['items'] ?? map['content'];
  if (data is List) {
    return data;
  }
  return const <dynamic>[];
}

String? readString(Map<String, dynamic> map, List<String> keys) {
  for (final key in keys) {
    final value = map[key];
    if (value != null && value.toString().trim().isNotEmpty) {
      return value.toString().trim();
    }
  }
  return null;
}
