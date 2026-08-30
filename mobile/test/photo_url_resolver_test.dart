import 'package:flutter_test/flutter_test.dart';
import 'package:megha_connect/utils/photo_url_resolver.dart';

void main() {
  test('resolves citizen 584 upload against configured server origin', () {
    final resolved =
        resolvePhotoUrl('/uploads/visitor-photos/2026-08-12/7c1c1158.jpg');
    expect(
        resolved, contains('/uploads/visitor-photos/2026-08-12/7c1c1158.jpg'));
    expect(resolved, isNot(contains('/api/v1/uploads/')));
  });
  test('preserves absolute URL and handles blank', () {
    expect(resolvePhotoUrl('https://files.example/photo.jpg'),
        'https://files.example/photo.jpg');
    expect(resolvePhotoUrl(' '), isNull);
  });

  test('normalizes raw JPEG and PNG base64 as in-memory data images', () {
    const jpeg = '/9j/4AAQSkZJRgABAQAAAQABAAD/2w==';
    const png = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB';
    expect(resolvePhotoUrl(jpeg), 'data:image/jpeg;base64,$jpeg');
    expect(resolvePhotoUrl(png), 'data:image/png;base64,$png');
  });

  test('preserves data image values and treats malformed values as URLs', () {
    const dataImage = 'data:image/png;base64,iVBORw0KGgo=';
    expect(resolvePhotoUrl(dataImage), dataImage);
    expect(resolvePhotoUrl('not-base64'), contains('/not-base64'));
  });
}
