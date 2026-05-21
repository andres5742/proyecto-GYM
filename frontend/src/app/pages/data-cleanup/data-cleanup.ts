import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  DataCleanupScope,
  DataCleanupScopeCode,
  DataCleanupService,
} from '../../core/services/data-cleanup.service';
import { httpErrorMessage } from '../../core/utils/http-error-message';

@Component({
  selector: 'app-data-cleanup',
  imports: [FormsModule],
  templateUrl: './data-cleanup.html',
  styleUrl: './data-cleanup.scss',
})
export class DataCleanupPage implements OnInit {
  private readonly cleanupService = inject(DataCleanupService);

  protected readonly scopes = signal<DataCleanupScope[]>([]);
  protected readonly loading = signal(true);
  protected readonly running = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedScope = signal<DataCleanupScopeCode | null>(null);
  protected readonly confirmInput = signal('');
  protected readonly lastResult = signal<{ total: number; details: Record<string, number> } | null>(
    null,
  );

  protected readonly requiredPhrase = DataCleanupService.CONFIRM_PHRASE;

  ngOnInit(): void {
    this.cleanupService.listScopes().subscribe({
      next: (list) => {
        this.scopes.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'No se pudieron cargar los ámbitos de limpieza'));
        this.loading.set(false);
      },
    });
  }

  selectScope(code: DataCleanupScopeCode): void {
    this.selectedScope.set(code);
    this.confirmInput.set('');
    this.message.set(null);
    this.error.set(null);
    this.lastResult.set(null);
  }

  canRun(): boolean {
    const scope = this.selectedScope();
    return (
      scope != null &&
      !this.running() &&
      this.confirmInput().trim() === this.requiredPhrase
    );
  }

  protected detailRows(details: Record<string, number>): { key: string; value: number }[] {
    return Object.entries(details).map(([key, value]) => ({ key, value }));
  }

  runCleanup(): void {
    const scope = this.selectedScope();
    if (!scope || !this.canRun()) {
      return;
    }
    const label = this.scopes().find((s) => s.code === scope)?.label ?? scope;
    if (
      !confirm(
        `¿Borrar permanentemente los datos de «${label}»? Esta acción no se puede deshacer.`,
      )
    ) {
      return;
    }
    this.running.set(true);
    this.message.set(null);
    this.error.set(null);
    this.cleanupService.cleanup(scope, this.confirmInput().trim()).subscribe({
      next: (res) => {
        this.lastResult.set({ total: res.totalDeleted, details: res.details });
        this.message.set(`Se eliminaron ${res.totalDeleted} registro(s).`);
        this.confirmInput.set('');
        this.running.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'No se pudo completar la limpieza'));
        this.running.set(false);
      },
    });
  }
}
