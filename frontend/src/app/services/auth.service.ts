import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { UserRole } from '../models';

export interface AuthUser {
  username: string;
  fullName: string;
  role: UserRole;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _user = signal<AuthUser | null>(null);
  readonly user = this._user.asReadonly();

  readonly DEMO_USERS: { username: string; password: string; fullName: string; role: UserRole }[] = [
    { username: 'hcm',       password: 'hcm123',    fullName: 'Hon. Chief Minister', role: 'HCM' },
    { username: 'admin',     password: 'admin123',  fullName: 'System Admin',         role: 'ADMIN' },
    { username: 'saidul',    password: 'osd123',    fullName: 'Saidul OSD',           role: 'SAIDUL_OSD' },
    { username: 'jtsecy',    password: 'jts123',    fullName: 'Joint Secretary',       role: 'APPROVER_JT_SECY' },
    { username: 'cmo',       password: 'cmo123',    fullName: 'CMO Officer',           role: 'CMO_OFFICER' },
    { username: 'deo1',      password: 'deo123',    fullName: 'Data Entry Operator 1', role: 'DATA_ENTRY_OPERATOR' },
    { username: '9876543210',password: '123456',    fullName: 'Public User',           role: 'PUBLIC' },
  ];

  constructor(private router: Router) {
    const stored = sessionStorage.getItem('megha_user');
    if (stored) this._user.set(JSON.parse(stored));
  }

  login(username: string, password: string): boolean {
    const u = this.DEMO_USERS.find(d => d.username === username && d.password === password);
    if (u) {
      const auth: AuthUser = { username: u.username, fullName: u.fullName, role: u.role };
      this._user.set(auth);
      sessionStorage.setItem('megha_user', JSON.stringify(auth));
      return true;
    }
    return false;
  }

  logout() {
    this._user.set(null);
    sessionStorage.removeItem('megha_user');
    this.router.navigate(['/login']);
  }

  isLoggedIn() { return !!this._user(); }
  hasRole(...roles: UserRole[]) { const u = this._user(); return u ? roles.includes(u.role) : false; }
}
