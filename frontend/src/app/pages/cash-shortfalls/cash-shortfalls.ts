import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { CashShortfall } from '../../core/models/cash-shortfall.model';
import { AuthService } from '../../core/services/auth.service';
import { CashShortfallService } from '../../core/services/cash-shortfall.service';

@Component({
  selector: 'app-cash-shortfalls',
  imports: [FormsModule, CopCurrencyPipe, DatePipe],
  templateUrl: './cash-shortfalls.html',
  styleUrl: './cash-shortfalls.scss',
})
export class CashShortfallsPage implements OnInit {
  private readonly shortfallService = inject(CashShortfallService);
  protected readonly auth = inject(AuthService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly records = signal<CashShortfall[]>([]);
  protected readonly summary = signal<
    import('../../core/models/cash-shortfall.model').CashShortfallMonthlySummary[]
  >([]);

  protected readonly selectedYear = signal(new Date().getFullYear());
  protected readonly selectedMonth = signal(new Date().getMonth() + 1);
  protected readonly expandedRecordIds = signal<ReadonlySet<number>>(new Set());
  protected readonly isAdmin = computed(() => this.auth.isAdmin());

  protected readonly monthOptions = [
    { value: 1, label: 'Enero' },
    { value: 2, label: 'Febrero' },
    { value: 3, label: 'Marzo' },
    { value: 4, label: 'Abril' },
    { value: 5, label: 'Mayo' },
    { value: 6, label: 'Junio' },
    { value: 7, label: 'Julio' },
    { value: 8, label: 'Agosto' },
    { value: 9, label: 'Septiembre' },
    { value: 10, label: 'Octubre' },
    { value: 11, label: 'Noviembre' },
    { value: 12, label: 'Diciembre' },
  ];

  protected readonly pendingMonthTotal = computed(() =>
    this.records()
      .filter((r) => r.status === 'PENDING')
      .reduce((s, r) => s + r.shortfallAmount, 0),
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.shortfallService.findForMonth(this.selectedYear(), this.selectedMonth()).subscribe({
      next: (list) => {
        this.expandedRecordIds.set(new Set());
        this.records.set(list);
        if (this.isAdmin()) {
          this.shortfallService
            .monthlySummary(this.selectedYear(), this.selectedMonth())
            .subscribe({
              next: (rows) => this.summary.set(rows),
              error: () => this.summary.set([]),
            });
        } else {
          this.summary.set([]);
        }
        this.loading.set(false);
      },
      error: () => {
        this.message.set('No se pudo cargar los descuadres');
        this.loading.set(false);
      },
    });
  }

  onPeriodChange(): void {
    this.load();
  }

  protected isInventoryShortfall(record: CashShortfall): boolean {
    if (record.kind === 'CASH_REGISTER') {
      return false;
    }
    if (record.kind === 'INVENTORY') {
      return true;
    }
    if ((record.inventoryMissingLines?.length ?? 0) > 0) {
      return true;
    }
    const notes = record.notes?.toLowerCase() ?? '';
    return notes.includes('inventario');
  }

  protected inventorySummary(record: CashShortfall): string {
    const lines = record.inventoryMissingLines ?? [];
    if (lines.length > 0) {
      const units = lines.reduce((s, l) => s + l.missingQuantity, 0);
      return `${lines.length} producto(s), ${units} unidad(es) faltantes`;
    }
    return record.notes ?? 'Faltante de inventario';
  }

  protected isDetailExpanded(recordId: number): boolean {
    return this.expandedRecordIds().has(recordId);
  }

  protected toggleInventoryDetail(record: CashShortfall): void {
    if (!this.isInventoryShortfall(record)) {
      return;
    }
    const next = new Set(this.expandedRecordIds());
    if (next.has(record.id)) {
      next.delete(record.id);
    } else {
      next.add(record.id);
    }
    this.expandedRecordIds.set(next);
  }

  protected canShowInventoryProducts(record: CashShortfall): boolean {
    return (record.inventoryMissingLines?.length ?? 0) > 0;
  }

  settle(record: CashShortfall): void {
    if (!this.isAdmin() || record.status !== 'PENDING') {
      return;
    }
    const note = prompt(
      `¿Marcar como cobrado el faltante de ${record.employeeName} (${record.shortfallAmount})? Nota opcional:`,
      '',
    );
    if (note === null) {
      return;
    }
    this.saving.set(true);
    this.shortfallService.settle(record.id, note || undefined).subscribe({
      next: () => {
        this.message.set('Descuadre marcado como cobrado');
        this.saving.set(false);
        this.load();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo actualizar');
        this.saving.set(false);
      },
    });
  }
}
