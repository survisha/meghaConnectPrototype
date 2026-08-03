import 'package:flutter_test/flutter_test.dart';
import 'package:megha_connect/services/api_service.dart';

void main() {
  test('normalizes JPEG and PNG data URLs for JSON face search', () {
    expect(ApiService.normalizeFacePhoto('data:image/jpeg;base64,/9j/AA=='), '/9j/AA==');
    expect(ApiService.normalizeFacePhoto('data:image/png;base64,iVBORw0KGgo='), 'iVBORw0KGgo=');
    expect(ApiService.normalizeFacePhoto('/9j/AA=='), '/9j/AA==');
  });

  test('rejects blank and unsupported face images', () {
    expect(() => ApiService.normalizeFacePhoto(' '), throwsFormatException);
    expect(() => ApiService.normalizeFacePhoto('data:image/webp;base64,AAAA'), throwsFormatException);
  });
}
