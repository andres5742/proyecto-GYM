import { Component, inject, OnInit, signal } from '@angular/core';
import { AppModuleItem, ModuleCode } from '../../core/models/module.model';
import { ModuleService } from '../../core/services/module.service';

@Component({
  selector: 'app-modules-admin',
  templateUrl: './modules-admin.html',
  styleUrl: './modules-admin.scss',
})
export class ModulesAdminPage implements OnInit {
  private readonly moduleService = inject(ModuleService);

  protected readonly modules = signal<AppModuleItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
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
        this.saving.set(null);
        this.message.set(next ? `«${mod.name}» activado` : `«${mod.name}» desactivado`);
      },
      error: (err) => {
        this.saving.set(null);
        this.message.set(err.error?.message ?? 'No se pudo actualizar');
      },
    });
  }

  panelModules(): AppModuleItem[] {
    return this.modules().filter((m) => m.category === 'PANEL');
  }

  publicModules(): AppModuleItem[] {
    return this.modules().filter((m) => m.category === 'PUBLIC');
  }
}
