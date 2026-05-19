import { Injectable } from '@angular/core';
import { AuthUser } from '../models/auth.model';

const TOKEN_KEY = 'gym_auth_token';
const USER_KEY = 'gym_auth_user';

/** Persistencia de sesión JWT (SRP — separado de AuthService). */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }

  save(token: string | null, user: AuthUser): void {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    }
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
