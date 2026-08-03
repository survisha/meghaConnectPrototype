import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FaceRecognitionService, normalizeFacePhoto } from './face-recognition.service';
import { environment } from '../../environments/environment';

describe('FaceRecognitionService', () => {
  let service: FaceRecognitionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(FaceRecognitionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends one JSON DTO and normalizes a mobile JPEG data URL', () => {
    service.search('data:image/jpeg;base64,/9j/AA==', 25.57, 91.88).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/face-recognition/search`);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Content-Type')).toBeNull();
    expect(typeof request.request.body).toBe('object');
    expect(request.request.body).toEqual({
      photo: '/9j/AA==', latitude: 25.57, longitude: 91.88, includeMatchedPhoto: false
    });
    request.flush({ success: true, matched: false, message: 'No matching visitor found.' });
  });

  it('normalizes PNG data URLs and preserves raw Base64', () => {
    expect(normalizeFacePhoto('data:image/png;base64,iVBORw0KGgo=')).toBe('iVBORw0KGgo=');
    expect(normalizeFacePhoto('/9j/AA==')).toBe('/9j/AA==');
  });

  it('rejects blank and unsupported data URLs before HTTP', () => {
    expect(() => service.search('   ')).toThrowError('Face photo is required.');
    expect(() => service.search('data:image/webp;base64,AAAA')).toThrowError(
      'Only JPEG or PNG face images are supported.');
    http.expectNone(`${environment.apiUrl}/face-recognition/search`);
  });
});
