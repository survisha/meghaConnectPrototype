import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: { navigate: jasmine.Spy };

  beforeEach(() => {
    sessionStorage.clear();
    router = { navigate: jasmine.createSpy('navigate') };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('adds exactly one Bearer authorization header to protected requests', () => {
    sessionStorage.setItem('megha_token', 'Bearer jwt-token');

    http.get('/api/v1/users').subscribe();

    const req = httpMock.expectOne('/api/v1/users');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
  });

  it('skips public auth endpoints', () => {
    sessionStorage.setItem('megha_token', 'jwt-token');

    http.post('/api/v1/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('keeps a valid session on 403 authorization failures', done => {
    sessionStorage.setItem('megha_token', 'jwt-token');
    sessionStorage.setItem('megha_user', JSON.stringify({ username: 'admin', role: 'ADMIN', fullName: 'Admin' }));

    http.get('/api/v1/admin/departments').subscribe({
      error: () => {
        expect(sessionStorage.getItem('megha_token')).toBe('jwt-token');
        expect(router.navigate).not.toHaveBeenCalled();
        done();
      },
    });

    const req = httpMock.expectOne('/api/v1/admin/departments');
    req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
  });

  it('clears authentication and redirects on 401 for protected requests', done => {
    sessionStorage.setItem('megha_token', 'jwt-token');
    sessionStorage.setItem('megha_user', JSON.stringify({ username: 'admin', role: 'ADMIN', fullName: 'Admin' }));

    http.get('/api/v1/users').subscribe({
      error: () => {
        expect(sessionStorage.getItem('megha_token')).toBeNull();
        expect(sessionStorage.getItem('megha_user')).toBeNull();
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
        done();
      },
    });

    const req = httpMock.expectOne('/api/v1/users');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
  });

  it('handles concurrent 401 responses with a single logout navigation', done => {
    sessionStorage.setItem('megha_token', 'jwt-token');
    sessionStorage.setItem('megha_user', JSON.stringify({ username: 'admin', role: 'ADMIN', fullName: 'Admin' }));
    let errors = 0;

    const onError = () => {
      errors += 1;
      if (errors === 2) {
        expect(sessionStorage.getItem('megha_token')).toBeNull();
        expect(sessionStorage.getItem('megha_user')).toBeNull();
        expect(router.navigate).toHaveBeenCalledTimes(1);
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
        done();
      }
    };

    http.get('/api/v1/appointments').subscribe({ error: onError });
    http.get('/api/v1/schedule').subscribe({ error: onError });

    const requests = httpMock.match(req => req.url === '/api/v1/appointments' || req.url === '/api/v1/schedule');
    expect(requests.length).toBe(2);
    requests[0].flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    requests[1].flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
  });
});
