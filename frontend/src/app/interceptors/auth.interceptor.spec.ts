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
});
