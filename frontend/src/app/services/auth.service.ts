import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { UserRole } from '../models';
import { environment } from '../../environments/environment';
import { AuthSessionService } from './auth-session.service';

export interface AuthUser {
  username: string;
  fullName: string;
  role: UserRole;
  userId?: number;
  departmentId?: number;
  departmentCode?: string;
  departmentName?: string;
  passwordChangeRequired?: boolean;
  visitorId?: number;
}

interface LoginResponse {
  token?: string;
  accessToken?: string;
  access_token?: string;
  jwt?: string;
  jwtToken?: string;
  tokenType?: string;
  username: string;
  fullName: string;
  role: string;
  userId?: number;
  departmentId?: number;
  departmentCode?: string;
  departmentName?: string;
  passwordChangeRequired?: boolean;
  expiresIn: number;
}

export interface ValidateOtpRequest {
  phoneNumber: string;
  otp: string;
  epicNumber?: string;
  visitorId?: number;
  purpose?: 'LOGIN' | 'REGISTRATION';
  registrationFlow?: boolean;
}

export interface ValidateOtpResponse {
  success: boolean;
  code?: string;
  token?: string;
  fullName?: string;
  visitorId?: number;
  role?: string;
  message: string;
  requiresEpic?: boolean;
  kycStatus?: string;
  kycPending?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _user = signal<AuthUser | null>(null);
  readonly user = this._user.asReadonly();

  // Demo users for user management (matches backend seeded data)
  DEMO_USERS = [
    { username: 'hcm', password: 'hcm123', fullName: 'HCM User', role: 'HCM' as UserRole },
    { username: 'admin', password: 'admin123', fullName: 'Admin User', role: 'ADMIN' as UserRole },
    { username: 'saidul', password: 'osd123', fullName: 'OSD', role: 'OSD' as UserRole },
    { username: 'jtsecy', password: 'jts123', fullName: 'Approver', role: 'APPROVER' as UserRole },
    { username: 'cmo', password: 'cmo123', fullName: 'CMO Officer', role: 'CMO_OFFICER' as UserRole },
    { username: 'deo1', password: 'deo123', fullName: 'Data Entry Operator', role: 'DATA_ENTRY_OPERATOR' as UserRole },
    { username: 'public1', password: 'public123', fullName: 'Public User', role: 'PUBLIC' as UserRole },
  ];

  constructor(
    private router: Router,
    private http: HttpClient,
    private authSession: AuthSessionService,
  ) {
    const parsed = this.authSession.getUser();
    if (parsed) {
      parsed.role = this.normalizeRole(parsed.role);
      this._user.set(parsed);
      this.authSession.updateUser(parsed);
    }
  }

  login(username: string, password: string): Observable<boolean> {
    const normalizedUsername = username.trim();
    return this.http.post<LoginResponse | { data: LoginResponse }>(`${environment.apiUrl}/auth/login`, { username: normalizedUsername, password }).pipe(
      tap(response => {
        this.debugLoginResponse(response);
        const res = this.unwrapLoginResponse(response);
        const token = this.extractAccessToken(res);
        if (!token) {
          throw new Error('Login response did not include an access token.');
        }
        // Strip Spring Security "ROLE_" prefix to match frontend UserRole type
        const role = this.normalizeRole((res.role ?? '').replace(/^ROLE_/, ''));
        const auth: AuthUser = {
          username: res.username ?? normalizedUsername,
          fullName: res.fullName ?? normalizedUsername,
          role,
          userId: res.userId,
          departmentId: res.departmentId,
          departmentCode: res.departmentCode,
          departmentName: res.departmentName,
          passwordChangeRequired: res.passwordChangeRequired,
        };
        this._user.set(auth);
        this.authSession.setSession(auth, token);
      }),
      map(() => true),
      catchError(err => throwError(() => err))
    );
  }

  validateOtp(request: ValidateOtpRequest): Observable<ValidateOtpResponse> {
    return this.http.post<ValidateOtpResponse>(`${environment.apiUrl}/auth/validate-otp`, request);
  }

  logout() {
    this._user.set(null);
    this.authSession.clear();
    this.router.navigate(['/login']);
  }

  setVisitorSession(username: string, fullName: string, token: string, visitorId?: number) {
    const auth: AuthUser = { username, fullName, role: 'PUBLIC', visitorId };
    this._user.set(auth);
    this.authSession.setSession(auth, token);
    if (visitorId) sessionStorage.setItem('megha_visitor_id', String(visitorId));
  }

  isLoggedIn() { return !!this._user(); }
  hasRole(...roles: UserRole[]) { const u = this._user(); return u ? roles.includes(u.role) : false; }

  private normalizeRole(role: string): UserRole {
    const normalized = (role || '').replace(/^ROLE_/, '').trim().toUpperCase();
    return (normalized === 'CMO' ? 'CMO_OFFICER' : normalized) as UserRole;
  }

  private unwrapLoginResponse(response: LoginResponse | { data: LoginResponse }): LoginResponse {
    return response && 'data' in response ? response.data : response;
  }

  private extractAccessToken(response: LoginResponse): string {
    const raw = response.accessToken || response.access_token || response.jwt || response.jwtToken || response.token || '';
    return raw.replace(/^Bearer\s+/i, '').trim();
  }

  private debugLoginResponse(response: LoginResponse | { data: LoginResponse }): void {
    const res = this.unwrapLoginResponse(response);
    const token = this.extractAccessToken(res);
    console.debug('Login response keys:', Object.keys(response ?? {}));
    console.debug('Login response data keys:', Object.keys(res ?? {}));
    console.debug('Access token exists:', !!token);
    console.debug('Access token length:', token.length);
    console.debug('Access token prefix:', token.substring(0, 10));
    console.debug('Logged-in username:', res.username);
    console.debug('Logged-in role:', res.role);
    console.debug('Logged-in departmentId:', res.departmentId);
  }
}
