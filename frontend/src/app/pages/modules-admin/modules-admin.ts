import { Component, inject, OnInit, signal } from '@angular/core';
import { AppModuleItem, CONFIGURABLE_ROLES, ConfigurableRole, ModuleCode, RoleModulePermission } from '../../core/models/module.model';
import { ModuleService } from '../../core/services/module.service';
import {
  moduleDisplayDescription,
  moduleDisplayName,
} from '../../core/utils/member-labels';

@Component({
  selector: 'app-modules-admin',
  templateUrl: './modules-admin.html',
  styleUrl: './modules-admin.scss',
})
export class ModulesAdminPage implements OnInit {
  private readonly moduleService = inject(ModuleService);

  protected readonly configurableRoles = CONFIGURABLE_ROLES;
  protected readonly selectedRole = signal<ConfigurableRole>('TRAINER');

  protected readonly modules = signal<AppModuleItem[]>([]);
  protected readonly rolePermissions = signal<RoleModulePermission[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadingRole = signal(false);
  protected readonly saving = signal<string | null>(null);
  protected readonly savingRole = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);

  protected displayModuleName(code: string, name: string): string {
    return moduleDisplayName(code, name);
  }

  protected displayModuleDescription(code: string, description: string | undefined): string | undefined {
    return moduleDisplayDescription(code, description);
  }

  ngOnInit(): void {
    this.load();
    this.loadRolePermissions();
  }

  load(): void {
    this.loading.set(true);
    this.moduleService.findAllForManagement().subscribe({
      next: (list) => {
        this.modules.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('No se pudieron cargar los módulos');
        this.loading.set(false);
      },
    });
  }

  selectRole(role: ConfigurableRole): void {
    this.selectedRole.set(role);
    this.loadRolePermissions();
  }

  loadRolePermissions(): void {
    this.loadingRole.set(true);
    this.moduleService.findRolePermissions(this.selectedRole()).subscribe({
      next: (list) => {
        this.rolePermissions.set(list);
        this.loadingRole.set(false);
      },
      error: () => {
        this.message.set('No se pudieron cargar los permisos del rol');
        this.loadingRole.set(false);
      },
    });
  }

  toggle(mod: AppModuleItem): void {
    if (mod.code === 'MODULOS_SISTEMA') {
      return;
    }
    const next = !mod.enabled;
    this.saving.set(mod.code);
    this.message.set(null);
    this.moduleService.setEnabled(mod.code as ModuleCode, next).subscribe({
      next: (updated) => {
        this.modules.update((list) => list.map((m) => (m.code === updated.code ? updated : m)));
        this.moduleService.loadPanel().subscribe();
        this.moduleService.loadPublic().subscribe();
        this.loadRolePermissions();
        this.saving.set(null);
        const label = moduleDisplayName(mod.code, mod.name);
        this.message.set(next ? `«${label}» activado en el sistema` : `«${label}» desactivado en el sistema`);
      },
      error: (err) => {
        this.saving.set(null);
        this.message.set(err.error?.message ?? 'No se pudo actualizar');
      },
    });
  }

  toggleRolePermission(perm: RoleModulePermission): void {
    if (!perm.globallyEnabled) {
      return;
    }
    const next = !perm.allowed;
    const role = this.selectedRole();
    this.savingRole.set(perm.moduleCode);
    this.message.set(null);
    this.moduleService.setRolePermission(role, perm.moduleCode, next).subscribe({
      next: (updated) => {
        this.rolePermissions.update((list) =>
          list.map((p) => (p.moduleCode === updated.moduleCode ? updated : p)),
        );
        this.moduleService.loadPanel().subscribe();
        this.savingRole.set(null);
        const roleLabel = this.configurableRoles.find((r) => r.value === role)?.label ?? role;
        this.message.set(
          next
            ? `«${perm.moduleName}» habilitado para ${roleLabel}`
            : `«${perm.moduleName}» deshabilitado para ${roleLabel}`,
        );
      },
      error: (err) => {
        this.savingRole.set(null);
        this.message.set(err.error?.message ?? 'No se pudo actualizar el permiso');
      },
    });
  }

  panelModules(): AppModuleItem[] {
    return this.modules().filter((m) => m.category === 'PANEL');
  }

  publicModules(): AppModuleItem[] {
    return this.modules().filter((m) => m.category === 'PUBLIC');
  }

  selectedRoleLabel(): string {
    return this.configurableRoles.find((r) => r.value === this.selectedRole())?.label ?? '';
  }
}
