import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, AuthUser, LoginRequest, ROLE_LABELS } from '../models/auth.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storage = inject(TokenStorageService);

  private readonly currentUserSignal = signal<AuthUser | null>(this.storage.getUser());

  readonly currentUser = this.currentUserSignal.asReadonly();

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((response) => this.persistSession(response)),
    );
  }

  loadProfile(): Observable<AuthResponse> {
    return this.http.get<AuthResponse>(`${environment.apiUrl}/auth/me`).pipe(
      tap((response) => this.persistSession(response)),
    );
  }

  logout(): void {
    this.storage.clear();
    this.currentUserSignal.set(null);
    this.router.navigate(['/']);
  }

  getToken(): string | null {
    return this.storage.getToken();
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasRole(...roles: AuthUser['role'][]): boolean {
    const user = this.currentUserSignal();
    return !!user && roles.includes(user.role);
  }

  isAffiliate(): boolean {
    return this.hasRole('AFFILIATE');
  }

  isStaff(): boolean {
    return this.isLoggedIn() && !this.isAffiliate();
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN', 'SUPER_ADMIN');
  }

  isSuperAdmin(): boolean {
    return this.hasRole('SUPER_ADMIN');
  }

  homeRoute(): string {
    return this.isAffiliate() ? '/mi-cuenta' : '/panel';
  }

  private persistSession(response: AuthResponse): void {
    const user: AuthUser = {
      token: response.token,
      employeeId: response.employeeId ?? null,
      memberId: response.memberId ?? null,
      fullName: response.fullName,
      username: response.username,
      role: response.role,
      roleLabel: response.roleLabel || ROLE_LABELS[response.role],
    };
    this.storage.save(response.token, user);
    this.currentUserSignal.set(user);
  }
}
