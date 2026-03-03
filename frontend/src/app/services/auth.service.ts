import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { UserRole } from '../models';

export interface AuthUser {
  username: string;
  fullName: string;
  role: UserRole;
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

  constructor(private router: Router, private http: HttpClient) {
    const stored = sessionStorage.getItem('megha_user');
    if (stored) this._user.set(JSON.parse(stored));
  }

  login(username: string, password: string): Observable<boolean> {
    return this.http.post<LoginResponse>('/api/v1/auth/login', { username, password }).pipe(
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

  isLoggedIn() { return !!this._user(); }
  hasRole(...roles: UserRole[]) { const u = this._user(); return u ? roles.includes(u.role) : false; }
}
