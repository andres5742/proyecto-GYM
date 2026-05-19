import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AppModuleItem, ModuleCode, ModuleFlags } from '../models/module.model';
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

  loadPublic(): Observable<ModuleFlags> {
    return this.http.get<ModuleFlags>(`${environment.apiUrl}/modules/public`).pipe(
      tap((flags) => {
        this.publicFlags.set(flags);
        this.loadedPublic.set(true);
      }),
    );
  }

  loadPanel(): Observable<ModuleFlags> {
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

  isEnabled(code: ModuleCode, scope: 'panel' | 'public' = 'panel'): boolean {
    if (this.auth.hasRole('SUPER_ADMIN')) {
      return true;
    }
    const flags = scope === 'public' ? this.publicFlags() : this.panelFlags();
    return flags[code] ?? true;
  }

  resetPanel(): void {
    this.panelFlags.set({});
    this.loadedPanel.set(false);
  }
}
