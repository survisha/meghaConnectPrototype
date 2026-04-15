import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { UserRole } from '../models';
import { environment } from '../../environments/environment';

export interface AuthUser {
  username: string;
  fullName: string;
  role: UserRole;
  visitorId?: number;
}

interface LoginResponse {
  token: string;
  username: string;
  fullName: string;
  role: string;
  expiresIn: number;
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

  constructor(private router: Router, private http: HttpClient) {
    const stored = sessionStorage.getItem('megha_user');
    if (stored) this._user.set(JSON.parse(stored));
  }

  login(username: string, password: string): Observable<boolean> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password }).pipe(
      tap(res => {
        // Strip Spring Security "ROLE_" prefix to match frontend UserRole type
        const role = (res.role ?? '').replace(/^ROLE_/, '') as UserRole;
        const auth: AuthUser = { username: res.username, fullName: res.fullName ?? username, role };
        this._user.set(auth);
        sessionStorage.setItem('megha_user', JSON.stringify(auth));
        sessionStorage.setItem('megha_token', res.token);
      }),
      map(() => true),
      catchError(() => of(false))
    );
  }

  logout() {
    this._user.set(null);
    sessionStorage.removeItem('megha_user');
    sessionStorage.removeItem('megha_token');
    this.router.navigate(['/login']);
  }

  setVisitorSession(username: string, fullName: string, token: string, visitorId?: number) {
    const auth: AuthUser = { username, fullName, role: 'PUBLIC', visitorId };
    this._user.set(auth);
    sessionStorage.setItem('megha_user', JSON.stringify(auth));
    sessionStorage.setItem('megha_token', token);
    if (visitorId) sessionStorage.setItem('megha_visitor_id', String(visitorId));
  }

  isLoggedIn() { return !!this._user(); }
  hasRole(...roles: UserRole[]) { const u = this._user(); return u ? roles.includes(u.role) : false; }
}
