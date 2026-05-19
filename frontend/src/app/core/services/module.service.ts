import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  AppModuleItem,
  ConfigurableRole,
  ModuleCode,
  ModuleFlags,
  RoleModulePermission,
} from '../models/module.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class ModuleService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly publicFlags = signal<ModuleFlags>({});
  private readonly panelFlags = signal<ModuleFlags>({});
  private readonly loadedPublic = signal(false);
  private readonly loadedPanel = signal(false);

  readonly publicModules = this.publicFlags.asReadonly();
  readonly panelModules = this.panelFlags.asReadonly();
  readonly isPanelLoaded = this.loadedPanel.asReadonly();

  loadPublic(): Observable<ModuleFlags> {
    return this.http.get<ModuleFlags>(`${environment.apiUrl}/modules/public`).pipe(
      tap((flags) => {
        this.publicFlags.set(flags);
        this.loadedPublic.set(true);
      }),
    );
  }

  loadPanel(): Observable<ModuleFlags> {
    if (this.auth.isAffiliate()) {
      this.loadedPanel.set(true);
      return of({});
    }
    return this.http.get<ModuleFlags>(`${environment.apiUrl}/modules`).pipe(
      tap((flags) => {
        this.panelFlags.set(flags);
        this.loadedPanel.set(true);
      }),
    );
  }

  findAllForManagement(): Observable<AppModuleItem[]> {
    return this.http.get<AppModuleItem[]>(`${environment.apiUrl}/modules/manage`);
  }

  setEnabled(code: ModuleCode, enabled: boolean): Observable<AppModuleItem> {
    return this.http.patch<AppModuleItem>(`${environment.apiUrl}/modules/${code}`, { enabled });
  }

  findRolePermissions(role: ConfigurableRole): Observable<RoleModulePermission[]> {
    return this.http.get<RoleModulePermission[]>(`${environment.apiUrl}/modules/roles/${role}/permissions`);
  }

  setRolePermission(
    role: ConfigurableRole,
    code: ModuleCode,
    allowed: boolean,
  ): Observable<RoleModulePermission> {
    return this.http.patch<RoleModulePermission>(
      `${environment.apiUrl}/modules/roles/${role}/permissions/${code}`,
      { enabled: allowed },
    );
  }

  ensurePanelLoaded(): Observable<void> {
    if (this.loadedPanel() || !this.auth.isLoggedIn() || this.auth.isAffiliate()) {
      return of(undefined);
    }
    return this.loadPanel().pipe(map(() => undefined));
  }

  isEnabled(code: ModuleCode, scope: 'panel' | 'public' = 'panel'): boolean {
    if (this.auth.hasRole('SUPER_ADMIN')) {
      return true;
    }
    if (scope === 'panel') {
      if (!this.loadedPanel()) {
        return false;
      }
      return this.panelFlags()[code] === true;
    }
    if (!this.loadedPublic()) {
      return false;
    }
    return this.publicFlags()[code] === true;
  }

  resetPanel(): void {
    this.panelFlags.set({});
    this.loadedPanel.set(false);
  }

  reloadPanelForUser(): Observable<ModuleFlags> {
    this.resetPanel();
    if (!this.auth.isLoggedIn() || this.auth.isAffiliate()) {
      return of({});
    }
    return this.loadPanel();
  }
}
