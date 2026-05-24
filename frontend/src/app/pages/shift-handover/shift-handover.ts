import { DatePipe } from '@angular/common';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  CASH_DENOMINATIONS,
  computeCashTotal,
  emptyCashForm,
  ShiftHandover,
  ShiftHandoverCashForm,
  ShiftHandoverComparison,
} from '../../core/models/shift-handover.model';
import { PAYMENT_METHODS } from '../../core/models/sale.model';
import { WorkShift } from '../../core/models/shift.model';
import { AuthService } from '../../core/services/auth.service';
import { ShiftHandoverService } from '../../core/services/shift-handover.service';
import { ShiftService } from '../../core/services/shift.service';
import { httpErrorMessage } from '../../core/utils/http-error-message';

@Component({
  selector: 'app-shift-handover',
  imports: [FormsModule, CopCurrencyPipe, DatePipe, RouterLink],
  templateUrl: './shift-handover.html',
  styleUrl: './shift-handover.scss',
})
export class ShiftHandoverPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly shiftService = inject(ShiftService);
  private readonly handoverService = inject(ShiftHandoverService);

  protected readonly authService = this.auth;
  protected readonly isSuperAdmin = computed(() => this.auth.isSuperAdmin());
  protected readonly billDenominations = CASH_DENOMINATIONS.filter((d) => d.type === 'bill').sort(
    (a, b) => b.value - a.value,
  );
  protected readonly coinDenominations = CASH_DENOMINATIONS.filter((d) => d.type === 'coin').sort(
    (a, b) => b.value - a.value,
  );
  protected readonly paymentMethods = PAYMENT_METHODS.filter(
    (p) => p.value !== 'PENDING' && p.value !== 'AUX',
  );
  protected readonly computeCash = computeCashTotal;

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly openShift = signal<WorkShift | null>(null);
  protected readonly preview = signal<ShiftHandover | null>(null);
  protected readonly history = signal<ShiftHandover[]>([]);
  protected readonly selectedHistoryId = signal<number | null>(null);

  protected readonly cash = signal<ShiftHandoverCashForm>(emptyCashForm());
  protected readonly notes = signal('');
  protected readonly inventoryCounts = signal<Record<number, number>>({});
  /** Por defecto mostrar todos los productos para revisar en pantalla y en el modal. */
  protected readonly showOnlyInventoryDiff = signal(false);
  protected readonly confirmModalOpen = signal(false);

  protected readonly inventoryProducts = computed(
    () => this.preview()?.inventoryProducts ?? [],
  );
  protected readonly pendingInventoryDebt = computed(
    () => this.preview()?.pendingInventoryShortfallTotal ?? 0,
  );
  protected readonly handoverMissingInventoryValue = computed(() => {
    let total = 0;
    for (const line of this.inventoryProducts()) {
      const missing = this.inventoryMissingQty(line.productId);
      if (missing > 0) {
        total += missing * line.unitPrice;
      }
    }
    return Math.round(total);
  });
  protected readonly handoverSurplusInventoryValue = computed(() => {
    let total = 0;
    for (const line of this.inventoryProducts()) {
      const surplus = this.inventorySurplusQty(line.productId, line.expectedQuantity);
      if (surplus > 0) {
        total += surplus * line.unitPrice;
      }
    }
    return Math.round(total);
  });
  protected readonly cashSurplusForInventory = computed(() => {
    const diff = this.cashTotal() - this.expectedCashInDrawer();
    return diff > 0 ? Math.round(diff) : 0;
  });
  protected readonly inventoryCreditPreview = computed(() =>
    Math.round(this.cashSurplusForInventory() + this.handoverSurplusInventoryValue()),
  );
  protected readonly inventoryCrossPreview = computed(() => {
    const credit = this.inventoryCreditPreview();
    const pending =
      this.pendingInventoryDebt() + this.handoverMissingInventoryValue();
    return {
      credit,
      pending,
      applied: Math.min(credit, pending),
      remainingDebt: Math.max(0, pending - credit),
      remainingCash: Math.max(0, credit - pending),
    };
  });
  protected readonly hasInventoryVariance = computed(() => {
    for (const line of this.inventoryProducts()) {
      if (this.inventoryMissingQty(line.productId) > 0) {
        return true;
      }
      if (this.inventorySurplusQty(line.productId, line.expectedQuantity) > 0) {
        return true;
      }
    }
    return false;
  });

  protected readonly inventorySummary = computed(() => {
    let missingUnits = 0;
    let surplusUnits = 0;
    let missingValue = 0;
    let surplusValue = 0;
    let diffProducts = 0;
    for (const line of this.inventoryProducts()) {
      const miss = this.inventoryMissingQty(line.productId);
      const sur = this.inventorySurplusQty(line.productId, line.expectedQuantity);
      if (miss > 0 || sur > 0) {
        diffProducts++;
      }
      missingUnits += miss;
      surplusUnits += sur;
      missingValue += miss * line.unitPrice;
      surplusValue += sur * line.unitPrice;
    }
    return {
      totalProducts: this.inventoryProducts().length,
      diffProducts,
      missingUnits,
      surplusUnits,
      missingValue: Math.round(missingValue),
      surplusValue: Math.round(surplusValue),
      allMatch: diffProducts === 0,
    };
  });

  protected readonly inventoryLinesToShow = computed(() => {
    const lines = this.inventoryProducts();
    if (!this.showOnlyInventoryDiff()) {
      return lines;
    }
    return lines.filter(
      (line) =>
        this.inventoryMissingQty(line.productId) > 0 ||
        this.inventorySurplusQty(line.productId, line.expectedQuantity) > 0,
    );
  });

  protected readonly cashDiff = computed(
    () => this.cashTotal() - this.expectedCashInDrawer(),
  );

  protected readonly alreadySubmitted = computed(() => !!this.preview()?.id);
  protected readonly billTotal = computed(() =>
    this.billDenominations.reduce((sum, d) => sum + (this.cash()[d.key] || 0) * d.value, 0),
  );
  protected readonly coinTotal = computed(() =>
    this.coinDenominations.reduce((sum, d) => sum + (this.cash()[d.key] || 0) * d.value, 0),
  );
  protected readonly cashTotal = computed(() => this.billTotal() + this.coinTotal());

  protected readonly cashLinesForConfirm = computed(() => {
    const lines: { label: string; qty: number; subtotal: number }[] = [];
    for (const d of [...this.billDenominations, ...this.coinDenominations]) {
      const qty = this.cash()[d.key] || 0;
      if (qty > 0) {
        lines.push({ label: d.label, qty, subtotal: qty * d.value });
      }
    }
    return lines;
  });

  protected readonly confirmInventoryLines = computed(() =>
    this.inventoryProducts().map((line) => {
      const counted = this.inventoryCounts()[line.productId] ?? 0;
      const missing = this.inventoryMissingQty(line.productId);
      const surplus = this.inventorySurplusQty(line.productId, line.expectedQuantity);
      return {
        ...line,
        counted,
        missing,
        surplus,
        matches: counted === line.expectedQuantity,
        lineValue:
          missing > 0
            ? Math.round(missing * line.unitPrice)
            : surplus > 0
              ? Math.round(surplus * line.unitPrice)
              : 0,
      };
    }),
  );

  protected readonly confirmInventoryDiffCount = computed(
    () => this.confirmInventoryLines().filter((l) => !l.matches).length,
  );

  protected readonly billingCashExpected = computed(
    () => this.preview()?.billingCashExpected ?? 0,
  );
  protected readonly billingCashBase = computed(
    () => this.preview()?.billingCashBase ?? this.preview()?.billingCashExpected ?? 0,
  );
  protected readonly billingOtherIncomesCash = computed(
    () => this.preview()?.billingOtherIncomesCash ?? 0,
  );
  protected readonly previousShiftSalesCash = computed(
    () => this.preview()?.previousShiftSalesCash ?? 0,
  );
  protected readonly previousShiftShortfallsDeducted = computed(
    () => this.preview()?.previousShiftShortfallsDeducted ?? 0,
  );
  protected readonly previousShiftName = computed(() => this.preview()?.previousShiftName ?? null);
  protected readonly salesCashExpected = computed(
    () => this.preview()?.salesCashExpected ?? 0,
  );
  protected readonly previousShiftCreditPaymentsCash = computed(
    () => this.preview()?.previousShiftCreditPaymentsCash ?? 0,
  );
  protected readonly creditPaymentsCashExpected = computed(
    () => this.preview()?.creditPaymentsCashExpected ?? 0,
  );
  protected readonly expectedCashInDrawer = computed(
    () =>
      this.preview()?.expectedCashTotal ??
      this.billingCashExpected() +
        this.previousShiftSalesCash() +
        this.previousShiftCreditPaymentsCash() +
        this.salesCashExpected() +
        this.creditPaymentsCashExpected(),
  );

  protected readonly cashMatchesDeclared = computed(
    () => this.cashTotal() === this.expectedCashInDrawer(),
  );

  protected readonly liveComparisons = computed((): ShiftHandoverComparison[] => {
    if (!this.preview()?.shiftDetail?.summary) {
      return [];
    }
    const expectedCash = this.expectedCashInDrawer();
    return [this.cmp('Dinero contado (billetes + monedas)', this.cashTotal(), expectedCash)];
  });

  protected readonly selectedHistory = computed(() => {
    const id = this.selectedHistoryId();
    return this.history().find((h) => h.id === id) ?? null;
  });

  ngOnInit(): void {
    this.shiftService.findOpen().subscribe({
      next: (shift) => {
        this.openShift.set(shift);
        if (shift) {
          this.loadPreview(shift.id);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.message.set('No se pudo cargar el turno activo');
        this.loading.set(false);
      },
    });
    this.handoverService.findAll().subscribe({
      next: (list) => {
        this.history.set(list.filter((h) => h.id != null));
        if (list.length > 0 && list[0].id) {
          this.selectedHistoryId.set(list[0].id);
        }
      },
    });
  }

  loadPreview(shiftId: number): void {
    this.loading.set(true);
    this.handoverService.previewForShift(shiftId).subscribe({
      next: (data) => {
        this.preview.set(data);
        if (data.id) {
          this.patchFromHandover(data);
        } else {
          this.initInventoryCounts(data);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.message.set(
          httpErrorMessage(err, 'No se pudo cargar la entrega del turno'),
        );
        this.loading.set(false);
      },
    });
  }

  patchFromHandover(data: ShiftHandover): void {
    this.cash.set({
      bill2000: data.bill2000 ?? 0,
      bill5000: data.bill5000 ?? 0,
      bill10000: data.bill10000 ?? 0,
      bill20000: data.bill20000 ?? 0,
      bill50000: data.bill50000 ?? 0,
      bill100000: data.bill100000 ?? 0,
      coin1000: data.coin1000 ?? 0,
      coin500: data.coin500 ?? 0,
      coin200: data.coin200 ?? 0,
      coin100: data.coin100 ?? 0,
      coin50: data.coin50 ?? 0,
    });
    this.notes.set(data.notes ?? '');
  }

  updateCash(key: keyof ShiftHandoverCashForm, value: number): void {
    this.cash.update((c) => ({ ...c, [key]: Math.max(0, value || 0) }));
  }

  protected inventoryMissingQty(productId: number): number {
    const line = this.inventoryProducts().find((p) => p.productId === productId);
    if (!line) {
      return 0;
    }
    const counted = this.inventoryCounts()[productId] ?? 0;
    return Math.max(0, line.expectedQuantity - counted);
  }

  protected inventorySurplusQty(productId: number, expected: number): number {
    const counted = this.inventoryCounts()[productId] ?? 0;
    return Math.max(0, counted - expected);
  }

  protected setInventoryCount(productId: number, raw: string): void {
    const qty = Math.max(0, parseInt(String(raw), 10) || 0);
    this.inventoryCounts.update((m) => ({ ...m, [productId]: qty }));
  }

  protected fillInventoryExpected(): void {
    const counts: Record<number, number> = {};
    for (const line of this.inventoryProducts()) {
      counts[line.productId] = line.expectedQuantity;
    }
    this.inventoryCounts.set(counts);
  }

  protected toggleInventoryFilter(): void {
    this.showOnlyInventoryDiff.update((v) => !v);
  }

  private initInventoryCounts(data: ShiftHandover): void {
    const counts: Record<number, number> = {};
    for (const line of data.inventoryProducts ?? []) {
      counts[line.productId] = line.expectedQuantity;
    }
    this.inventoryCounts.set(counts);
  }

  protected onFormSubmit(event: Event): void {
    event.preventDefault();
    this.openConfirmModal();
  }

  protected openConfirmModal(): void {
    if (this.alreadySubmitted()) {
      return;
    }
    const products = this.inventoryProducts();
    if (products.length > 0) {
      for (const p of products) {
        if (this.inventoryCounts()[p.productId] === undefined) {
          this.message.set('Indique cuántos hay de cada producto en bodega.');
          return;
        }
      }
    }
    this.message.set(null);
    this.confirmModalOpen.set(true);
  }

  protected readonly canConfirmHandover = computed(
    () => this.cashTotal() > 0 && !this.saving(),
  );

  protected closeConfirmModal(): void {
    this.confirmModalOpen.set(false);
  }

  protected confirmAndSubmit(): void {
    if (this.cashTotal() <= 0) {
      return;
    }
    this.closeConfirmModal();
    this.submit();
  }

  submit(): void {
    const shift = this.openShift();
    if (!shift || this.alreadySubmitted()) {
      return;
    }

    const products = this.inventoryProducts();
    const inventoryCounts =
      products.length > 0
        ? products.map((p) => ({
            productId: p.productId,
            countedQuantity: this.inventoryCounts()[p.productId] ?? 0,
          }))
        : undefined;

    this.saving.set(true);
    this.handoverService
      .submit({
        workShiftId: shift.id,
        ...this.cash(),
        notes: this.notes() || undefined,
        expenses: [],
        priorPayments: [],
        inventoryCounts,
      })
      .subscribe({
        next: (result) => {
          let msg = 'Entrega de turno registrada. El turno fue cerrado.';
          if (result.inventorySurplusResolutionNote) {
            msg += ' ' + result.inventorySurplusResolutionNote;
          } else if (result.registeredShortfallAmount && result.registeredShortfallAmount > 0) {
            msg +=
              ` Se registró un faltante de ${result.registeredShortfallAmount} en Descuadres de caja para cobro al fin de mes.`;
          }
          this.message.set(msg);
          this.preview.set(result);
          this.openShift.set(null);
          this.saving.set(false);
          this.handoverService.findAll().subscribe({
            next: (list) => this.history.set(list.filter((h) => h.id != null)),
          });
        },
        error: (err) => {
          const body = err?.error;
          const validation =
            body?.errors && typeof body.errors === 'object'
              ? Object.values(body.errors).join('. ')
              : null;
          this.message.set(validation ?? body?.message ?? 'No se pudo registrar la entrega');
          this.saving.set(false);
        },
      });
  }

  deleteHistoryItem(h: ShiftHandover): void {
    if (!h.id || !this.isSuperAdmin()) {
      return;
    }
    const label = `${h.workShiftName} · ${h.employeeName}`;
    if (!confirm(`¿Eliminar la entrega de turno "${label}"? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.handoverService.delete(h.id).subscribe({
      next: () => {
        const remaining = this.history().filter((item) => item.id !== h.id);
        this.history.set(remaining);
        if (this.selectedHistoryId() === h.id) {
          this.selectedHistoryId.set(remaining[0]?.id ?? null);
        }
        this.message.set('Entrega eliminada del historial.');
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo eliminar la entrega');
      },
    });
  }

  diffClass(diff: number): string {
    if (diff === 0) {
      return 'ok';
    }
    return diff > 0 ? 'over' : 'under';
  }

  private cmp(label: string, declared: number, expected: number): ShiftHandoverComparison {
    return { label, declared, expected, difference: declared - expected };
  }
}
