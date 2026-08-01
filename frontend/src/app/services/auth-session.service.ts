import { Injectable } from '@angular/core';
import type { AuthUser } from './auth.service';

const USER_STORAGE_KEY = 'megha_user';
const TOKEN_STORAGE_KEY = 'megha_token';

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private handlingUnauthorized = false;

  getUser(): AuthUser | null {
    const stored = sessionStorage.getItem(USER_STORAGE_KEY);
    if (!stored) {
      return null;
    }

    try {
      return JSON.parse(stored) as AuthUser;
    } catch {
      this.clear();
      return null;
    }
  }

  getAccessToken(): string {
    return this.getToken() ?? '';
  }

  getToken(): string | null {
    const token = this.cleanToken(sessionStorage.getItem(TOKEN_STORAGE_KEY));
    return token || null;
  }

  setSession(user: AuthUser, accessToken: string): void {
    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    this.saveToken(accessToken);
    this.handlingUnauthorized = false;
  }

  saveToken(token: string): void {
    const clean = this.cleanToken(token);
    if (!clean) {
      throw new Error('Refusing to store an empty access token.');
    }
    sessionStorage.setItem(TOKEN_STORAGE_KEY, clean);
  }

  updateUser(user: AuthUser): void {
    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
  }

  clear(): void {
    sessionStorage.removeItem(USER_STORAGE_KEY);
    this.removeToken();
  }

  removeToken(): void {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  }

  beginUnauthorizedHandling(): boolean {
    if (this.handlingUnauthorized) {
      return false;
    }
    this.handlingUnauthorized = true;
    return true;
  }

  private cleanToken(token: string | null): string {
    const clean = (token || '').replace(/^Bearer\s+/i, '').trim();
    if (!clean || clean === 'undefined' || clean === 'null' || clean === '[object Object]') {
      return '';
    }
    if ((clean.startsWith('"') && clean.endsWith('"')) || (clean.startsWith("'") && clean.endsWith("'"))) {
      return clean.slice(1, -1).trim();
    }
    return clean;
  }

}
