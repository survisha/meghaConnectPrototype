import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('stores a clean accessToken and Super Admin profile after login', () => {
    let result = false;

    service.login('  superadmin  ', 'Megha@TW26').subscribe(success => result = success);

    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.body.username).toBe('superadmin');
    req.flush({
      accessToken: 'Bearer jwt-token',
      tokenType: 'Bearer',
      username: 'superadmin',
      fullName: 'Super Admin',
      role: 'ROLE_SUPER_ADMIN',
      userId: 1,
      departmentId: null,
      departmentName: null,
      expiresIn: 86400,
    });

    expect(result).toBeTrue();
    expect(sessionStorage.getItem('megha_token')).toBe('jwt-token');
    expect(service.user()?.role).toBe('SUPER_ADMIN');
    expect(service.user()?.departmentId).toBeNull();
  });

  it('also accepts legacy token field from login response', () => {
    service.login('admin', 'admin123').subscribe();

    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush({
      token: 'legacy-token',
      username: 'admin',
      fullName: 'Admin',
      role: 'ADMIN',
      expiresIn: 86400,
    });

    expect(sessionStorage.getItem('megha_token')).toBe('legacy-token');
  });

  it('accepts snake-case and jwt token aliases from login response', () => {
    service.login('admin', 'admin123').subscribe();

    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush({
      access_token: 'Bearer alias-token',
      username: 'admin',
      fullName: 'Admin',
      role: 'ADMIN',
      expiresIn: 86400,
    });

    expect(sessionStorage.getItem('megha_token')).toBe('alias-token');
  });
});
