import { resolvePhotoUrl } from './photo-url.util';

describe('resolvePhotoUrl', () => {
  it('resolves the citizen 584 relative upload without adding api/v1', () => {
    const result = resolvePhotoUrl('/uploads/visitor-photos/2026-08-12/7c1c1158.jpg');
    expect(result).toContain('/uploads/visitor-photos/2026-08-12/7c1c1158.jpg');
    expect(result).not.toContain('/api/v1/uploads/');
  });
  it('preserves absolute URLs and rejects blank values', () => {
    expect(resolvePhotoUrl('https://files.example/photo.jpg')).toBe('https://files.example/photo.jpg');
    expect(resolvePhotoUrl(' ')).toBeNull();
  });
});
