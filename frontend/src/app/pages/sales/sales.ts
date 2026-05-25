import { DatePipe } from '@angular/common';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Member } from '../../core/models/member.model';
import { Product } from '../../core/models/product.model';
import {
  BatchSaleLine,
  SALES_PAYMENT_METHODS,
  PaymentMethod,
  SalesSummary,
} from '../../core/models/sale.model';
import {
  ProductInventoryCountItem,
  ProductInventoryLine,
  ProductSalesRow,
  ShiftDetail,
  ShiftOpenInventoryPreview,
  WorkShift,
} from '../../core/models/shift.model';
import {
  CASH_DENOMINATIONS,
  computeCashTotal,
  emptyCashForm,
  ShiftHandoverCashForm,
} from '../../core/models/shift-handover.model';
import { AuthService } from '../../core/services/auth.service';
import { MemberService } from '../../core/services/member.service';
import { ProductService } from '../../core/services/product.service';
import { SaleService } from '../../core/services/sale.service';
import { ShiftService } from '../../core/services/shift.service';
import { filterMembersByQuery, sortMembersByName } from '../../core/utils/member-search';

type QtyMatrix = Record<number, Partial<Record<PaymentMethod, number>>>;

/** Reparto de unidades fiadas entre varios afiliados (suma = columna Pendiente). */
interface PendingFiadoAlloc {
  key: string;
  memberId: number | null;
  quantity: number;
  searchText: string;
}

@Component({
  selector: 'app-sales',
  imports: [FormsModule, CopCurrencyPipe, DatePipe, RouterLink],
  templateUrl: './sales.html',
  styleUrl: './sales.scss',
})
export class Sales implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly saleService = inject(SaleService);
  private readonly shiftService = inject(ShiftService);
  private readonly productService = inject(ProductService);
  private readonly memberService = inject(MemberService);

  protected readonly products = signal<Product[]>([]);
  protected readonly members = signal<Member[]>([]);
  /** Reparto de fiado por producto: varios afiliados, cantidades que deben sumar el pendiente. */
  protected readonly pendingAllocations = signal<Record<number, PendingFiadoAlloc[]>>({});
  protected readonly cartProductIds = signal<number[]>([]);
  protected readonly pickerProductId = signal<number | null>(null);

  protected readonly openShift = signal<WorkShift | null>(null);
  protected readonly shiftHistory = signal<WorkShift[]>([]);
  protected readonly historyDetail = signal<ShiftDetail | null>(null);
  protected readonly summary = signal<SalesSummary | null>(null);
  protected readonly soldRows = signal<ProductSalesRow[]>([]);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly openingShift = signal(false);
  protected readonly inventoryModalOpen = signal(false);
  protected readonly inventoryPreview = signal<ShiftOpenInventoryPreview | null>(null);
  protected readonly inventoryCounts = signal<Record<number, number>>({});
  protected readonly openShiftCash = signal<ShiftHandoverCashForm>(emptyCashForm());
  protected readonly billDenominations = CASH_DENOMINATIONS.filter((d) => d.type === 'bill').sort(
    (a, b) => b.value - a.value,
  );
  protected readonly coinDenominations = CASH_DENOMINATIONS.filter((d) => d.type === 'coin').sort(
    (a, b) => b.value - a.value,
  );
  protected readonly pendingShiftName = signal('Mañana');
  protected readonly loadingInventoryPreview = signal(false);
  protected readonly loadingHistory = signal(false);
  protected readonly deletingShiftId = signal<number | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly shiftNameInput = signal('Mañana');
  protected readonly qtyMatrix = signal<QtyMatrix>({});
  /** Fila de búsqueda de afiliado con lista abierta (productId:allocKey). */
  protected readonly activeAllocSuggest = signal<string | null>(null);

  protected readonly paymentMethods = SALES_PAYMENT_METHODS;
  protected readonly shiftPresets = ['Mañana', 'Tarde', 'Noche'];
  protected readonly isAdmin = computed(() => this.auth.isAdmin());
  protected readonly isSuperAdmin = computed(() => this.auth.isSuperAdmin());

  protected readonly currentUserName = computed(
    () => this.auth.currentUser()?.fullName ?? 'Usuario',
  );

  protected readonly cartProducts = computed(() => {
    const ids = new Set(this.cartProductIds());
    return this.products().filter((p) => ids.has(p.id));
  });

  protected readonly productsForPicker = computed(() => {
    const inCart = new Set(this.cartProductIds());
    return this.products().filter((p) => !inCart.has(p.id));
  });

  ngOnInit(): void {
    this.productService.findAll().subscribe({
      next: (products) => this.products.set(products.filter((p) => p.active)),
    });
    this.memberService.findAll().subscribe({
      next: (list) => this.members.set(sortMembersByName(list)),
      error: () => this.members.set([]),
    });
    this.refreshShift();
    if (this.auth.isAdmin()) {
      this.loadShiftHistory();
    }
  }

  private ensureProductRow(productId: number): void {
    this.qtyMatrix.update((matrix) => {
      if (matrix[productId]) {
        return matrix;
      }
      const row: Partial<Record<PaymentMethod, number>> = {};
      for (const pm of SALES_PAYMENT_METHODS) {
        row[pm.value] = 0;
      }
      return { ...matrix, [productId]: row };
    });
  }

  addProductToCart(): void {
    const id = this.pickerProductId();
    if (id == null) {
      return;
    }
    if (this.cartProductIds().includes(id)) {
      this.message.set('Ese producto ya está en la lista');
      return;
    }
    this.cartProductIds.update((ids) => [...ids, id]);
    this.ensureProductRow(id);
    this.pickerProductId.set(null);
    this.message.set(null);
  }

  removeFromCart(productId: number): void {
    this.cartProductIds.update((ids) => ids.filter((i) => i !== productId));
    this.qtyMatrix.update((matrix) => {
      const next = { ...matrix };
      delete next[productId];
      return next;
    });
    this.pendingAllocations.update((m) => {
      const next = { ...m };
      delete next[productId];
      return next;
    });
  }

  private newPendingAlloc(): PendingFiadoAlloc {
    return {
      key: `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
      memberId: null,
      quantity: 0,
      searchText: '',
    };
  }

  getPendingAllocations(productId: number): PendingFiadoAlloc[] {
    return this.pendingAllocations()[productId] ?? [];
  }

  private syncPendingAllocations(productId: number): void {
    const pending = this.pendingQty(productId);
    if (pending <= 0) {
      this.pendingAllocations.update((m) => {
        const next = { ...m };
        delete next[productId];
        return next;
      });
      return;
    }
    const current = this.pendingAllocations()[productId];
    if (!current?.length) {
      this.pendingAllocations.update((m) => ({
        ...m,
        [productId]: [this.newPendingAlloc()],
      }));
    }
  }

  addPendingAllocation(productId: number): void {
    this.pendingAllocations.update((m) => ({
      ...m,
      [productId]: [...(m[productId] ?? []), this.newPendingAlloc()],
    }));
  }

  removePendingAllocation(productId: number, key: string): void {
    this.pendingAllocations.update((m) => {
      const list = (m[productId] ?? []).filter((a) => a.key !== key);
      if (list.length === 0 && this.pendingQty(productId) > 0) {
        return { ...m, [productId]: [this.newPendingAlloc()] };
      }
      return { ...m, [productId]: list };
    });
  }

  setPendingAllocQty(productId: number, key: string, value: string): void {
    const qty = Math.max(0, parseInt(value, 10) || 0);
    this.pendingAllocations.update((m) => ({
      ...m,
      [productId]: (m[productId] ?? []).map((a) => (a.key === key ? { ...a, quantity: qty } : a)),
    }));
  }

  setPendingAllocSearch(productId: number, key: string, text: string): void {
    this.activeAllocSuggest.set(this.allocSuggestKey(productId, key));
    this.pendingAllocations.update((m) => ({
      ...m,
      [productId]: (m[productId] ?? []).map((a) =>
        a.key === key ? { ...a, searchText: text, memberId: null } : a,
      ),
    }));
  }

  selectPendingAllocMember(productId: number, key: string, memberId: number): void {
    const m = this.members().find((x) => x.id === memberId);
    this.activeAllocSuggest.set(null);
    this.pendingAllocations.update((map) => ({
      ...map,
      [productId]: (map[productId] ?? []).map((a) =>
        a.key === key
          ? {
              ...a,
              memberId,
              searchText: m ? `${m.firstName} ${m.lastName}` : a.searchText,
            }
          : a,
      ),
    }));
  }

  clearPendingAllocMember(productId: number, key: string): void {
    this.activeAllocSuggest.set(this.allocSuggestKey(productId, key));
    this.pendingAllocations.update((m) => ({
      ...m,
      [productId]: (m[productId] ?? []).map((a) =>
        a.key === key ? { ...a, memberId: null, searchText: '' } : a,
      ),
    }));
  }

  allocSuggestKey(productId: number, key: string): string {
    return `${productId}:${key}`;
  }

  isAllocSuggestOpen(productId: number, key: string): boolean {
    return this.activeAllocSuggest() === this.allocSuggestKey(productId, key);
  }

  openAllocSuggest(productId: number, key: string): void {
    this.activeAllocSuggest.set(this.allocSuggestKey(productId, key));
  }

  pendingAllocStatusText(productId: number): string {
    const pending = this.pendingQty(productId);
    const sum = this.pendingAllocSum(productId);
    const diff = pending - sum;
    if (sum === pending && this.pendingAllocComplete(productId)) {
      return `Completo · ${sum} de ${pending} u.`;
    }
    if (diff > 0) {
      return `Faltan ${diff} u. · llevas ${sum} de ${pending}`;
    }
    if (diff < 0) {
      return `Sobran ${-diff} u. · llevas ${sum} de ${pending}`;
    }
    return `Repartido ${sum} de ${pending} u.`;
  }

  pendingAllocSum(productId: number): number {
    return this.getPendingAllocations(productId).reduce((s, a) => s + (a.quantity || 0), 0);
  }

  pendingAllocRemaining(productId: number): number {
    return this.pendingQty(productId) - this.pendingAllocSum(productId);
  }

  pendingAllocComplete(productId: number): boolean {
    const pending = this.pendingQty(productId);
    if (pending <= 0) {
      return true;
    }
    const allocs = this.getPendingAllocations(productId);
    if (!allocs.length) {
      return false;
    }
    if (this.pendingAllocSum(productId) !== pending) {
      return false;
    }
    return allocs.every((a) => a.memberId != null && a.quantity > 0);
  }

  filteredMembersForAlloc(productId: number, key: string): Member[] {
    const alloc = this.getPendingAllocations(productId).find((a) => a.key === key);
    return filterMembersByQuery(this.members(), alloc?.searchText ?? '', 25);
  }

  memberNameById(memberId: number | null): string | null {
    if (memberId == null) {
      return null;
    }
    const m = this.members().find((x) => x.id === memberId);
    return m ? `${m.firstName} ${m.lastName}` : null;
  }

  refreshShift(): void {
    this.shiftService.findOpen().subscribe({
      next: (shift) => {
        this.openShift.set(shift);
        if (shift) {
          this.loadShiftDetail(shift.id);
        } else {
          this.summary.set(null);
          this.soldRows.set([]);
          this.cartProductIds.set([]);
          this.qtyMatrix.set({});
          this.loading.set(false);
        }
      },
      error: () => {
        this.openShift.set(null);
        this.loading.set(false);
      },
    });
  }

  loadShiftDetail(shiftId: number): void {
    this.loading.set(true);
    this.shiftService.getDetail(shiftId).subscribe({
      next: (detail) => {
        this.summary.set(detail.summary);
        this.soldRows.set(detail.productRows);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Error al cargar el turno');
        this.loading.set(false);
      },
    });
  }

  loadShiftHistory(): void {
    this.loadingHistory.set(true);
    this.shiftService.findAll().subscribe({
      next: (shifts) => {
        this.shiftHistory.set(shifts);
        this.loadingHistory.set(false);
      },
      error: () => this.loadingHistory.set(false),
    });
  }

  openShiftAction(name?: string): void {
    const shiftName = name ?? this.shiftNameInput();
    const user = this.auth.currentUser();
    if (!user) {
      return;
    }
    this.pendingShiftName.set(shiftName);
    this.loadingInventoryPreview.set(true);
    this.shiftService.openInventoryPreview().subscribe({
      next: (preview) => {
        this.loadingInventoryPreview.set(false);
        if (!preview.inventoryCheckRequired) {
          this.submitOpenShift(shiftName, user.employeeId ?? undefined);
          return;
        }
        const counts: Record<number, number> = {};
        for (const line of preview.products) {
          counts[line.productId] = line.expectedQuantity;
        }
        this.inventoryCounts.set(counts);
        this.openShiftCash.set(emptyCashForm());
        this.inventoryPreview.set(preview);
        this.inventoryModalOpen.set(true);
      },
      error: (err) => {
        this.loadingInventoryPreview.set(false);
        this.message.set(err?.error?.message ?? 'No se pudo validar el inventario');
      },
    });
  }

  protected inventoryLine(productId: number): ProductInventoryLine | undefined {
    return this.inventoryPreview()?.products.find((p) => p.productId === productId);
  }

  protected inventoryMissingQty(productId: number): number {
    const line = this.inventoryLine(productId);
    if (!line) {
      return 0;
    }
    const counted = this.inventoryCounts()[productId] ?? 0;
    return Math.max(0, line.expectedQuantity - counted);
  }

  protected hasAnyInventoryMissing(): boolean {
    const preview = this.inventoryPreview();
    if (!preview) {
      return false;
    }
    return preview.products.some((p) => this.inventoryMissingQty(p.productId) > 0);
  }

  protected setInventoryCount(productId: number, value: string): void {
    const qty = Math.max(0, Math.floor(Number(value) || 0));
    this.inventoryCounts.update((m) => ({ ...m, [productId]: qty }));
  }

  protected fillInventoryExpected(): void {
    const preview = this.inventoryPreview();
    if (!preview) {
      return;
    }
    const counts: Record<number, number> = {};
    for (const line of preview.products) {
      counts[line.productId] = line.expectedQuantity;
    }
    this.inventoryCounts.set(counts);
  }

  protected readonly openCashTotal = computed(() => computeCashTotal(this.openShiftCash()));

  protected readonly openCashExpected = computed(
    () => this.inventoryPreview()?.cash?.expectedCashTotal ?? 0,
  );

  protected readonly openCashDiff = computed(() => this.openCashTotal() - this.openCashExpected());

  protected readonly openCashMatches = computed(() => this.openCashDiff() === 0);

  protected readonly openBillTotal = computed(() =>
    this.billDenominations.reduce((s, d) => s + (this.openShiftCash()[d.key] || 0) * d.value, 0),
  );

  protected readonly openCoinTotal = computed(() =>
    this.coinDenominations.reduce((s, d) => s + (this.openShiftCash()[d.key] || 0) * d.value, 0),
  );

  protected billingCashConceptTotal(
    cash: NonNullable<ShiftOpenInventoryPreview['cash']>,
  ): number {
    return cash.cashMembership + cash.cashDayWorkout + cash.cashSportsDance;
  }

  protected updateOpenCash(key: keyof ShiftHandoverCashForm, value: string | number): void {
    const n = Math.max(0, Math.floor(Number(value) || 0));
    this.openShiftCash.update((c) => ({ ...c, [key]: n }));
  }

  closeInventoryModal(): void {
    this.inventoryModalOpen.set(false);
    this.inventoryPreview.set(null);
    this.openShiftCash.set(emptyCashForm());
  }

  confirmInventoryAndOpenShift(): void {
    const preview = this.inventoryPreview();
    const user = this.auth.currentUser();
    if (!preview || !user) {
      return;
    }
    const inventoryCounts: ProductInventoryCountItem[] = preview.products.map((p) => ({
      productId: p.productId,
      countedQuantity: this.inventoryCounts()[p.productId] ?? 0,
    }));
    this.submitOpenShift(
      this.pendingShiftName(),
      user.employeeId ?? undefined,
      inventoryCounts,
      { ...this.openShiftCash() },
    );
  }

  private submitOpenShift(
    shiftName: string,
    employeeId?: number,
    inventoryCounts?: ProductInventoryCountItem[],
    cashCount?: ShiftHandoverCashForm,
  ): void {
    const user = this.auth.currentUser();
    if (!user) {
      return;
    }
    this.openingShift.set(true);
    this.shiftService
      .open({
        name: shiftName,
        employeeId,
        inventoryCounts,
        cashCount,
      })
      .subscribe({
        next: (result) => {
          const shift = result.shift;
          const fmtCop = (n: number) =>
            new Intl.NumberFormat('es-CO', {
              style: 'currency',
              currency: 'COP',
              maximumFractionDigits: 0,
            }).format(n);
          let msg = `Turno "${shift.name}" abierto · Vendedor: ${shift.employeeName ?? user.fullName}`;
          if (result.inventoryShortfallRegistered) {
            const prev = this.inventoryPreview()?.previousEmployeeName ?? 'turno anterior';
            msg += `. Descuadre por inventario (${fmtCop(result.inventoryShortfallAmount)}) a cargo de ${prev}.`;
          } else if (result.inventoryAdjusted) {
            msg += '. Inventario actualizado con el conteo.';
          }
          if (result.cashShortfallRegistered) {
            const opener =
              this.inventoryPreview()?.cash?.cashRegisterOpenedByName ?? 'quien abrió la caja';
            msg += `. Descuadre por efectivo (${fmtCop(result.cashShortfallAmount)}) a cargo de ${opener}.`;
          }
          if (!result.cashShortfallRegistered && this.openCashDiff() > 0) {
            msg += `. Sobrante en efectivo (${fmtCop(this.openCashDiff())}) queda en caja.`;
          }
          this.message.set(msg);
          this.openingShift.set(false);
          this.closeInventoryModal();
          this.cartProductIds.set([]);
          this.qtyMatrix.set({});
          this.pickerProductId.set(null);
          this.productService.findAll().subscribe({
            next: (products) => this.products.set(products.filter((p) => p.active)),
          });
          this.refreshShift();
        },
        error: (err) => {
          this.message.set(err?.error?.message ?? 'No se pudo abrir el turno');
          this.openingShift.set(false);
          if (typeof err?.error?.message === 'string' && err.error.message.toLowerCase().includes('turno abierto')) {
            this.refreshShift();
          }
        },
      });
  }

  getQty(productId: number, method: PaymentMethod): number {
    return this.qtyMatrix()[productId]?.[method] ?? 0;
  }

  setQty(productId: number, method: PaymentMethod, value: string): void {
    const parsed = Math.max(0, parseInt(value, 10) || 0);
    this.qtyMatrix.update((matrix) => ({
      ...matrix,
      [productId]: { ...matrix[productId], [method]: parsed },
    }));
    if (method === 'PENDING') {
      this.syncPendingAllocations(productId);
    }
  }

  pendingQty(productId: number): number {
    return this.getQty(productId, 'PENDING');
  }

  matrixColspan(): number {
    return 3 + this.paymentMethods.length + 2;
  }

  rowTotalUnits(productId: number): number {
    const row = this.qtyMatrix()[productId];
    if (!row) {
      return 0;
    }
    return SALES_PAYMENT_METHODS.reduce((sum, pm) => sum + (row[pm.value] ?? 0), 0);
  }

  rowTotalAmount(product: Product): number {
    return product.unitPrice * this.rowTotalUnits(product.id);
  }

  hasLinesToSave(): boolean {
    return this.cartProducts().some((p) => this.rowTotalUnits(p.id) > 0);
  }

  saveMatrix(): void {
    const shift = this.openShift();
    if (!shift) {
      this.message.set('Debe abrir un turno antes de registrar ventas');
      return;
    }

    const fiadoErrors: string[] = [];
    for (const product of this.cartProducts()) {
      const pending = this.pendingQty(product.id);
      if (pending <= 0) {
        continue;
      }
      const sum = this.pendingAllocSum(product.id);
      const allocs = this.getPendingAllocations(product.id);
      if (!allocs.length || allocs.some((a) => !a.memberId || a.quantity <= 0)) {
        fiadoErrors.push(`${product.name}: indique afiliado y cantidad en cada fila`);
      } else if (sum !== pending) {
        fiadoErrors.push(
          `${product.name}: reparte exactamente ${pending} u. entre afiliados (llevas ${sum})`,
        );
      }
    }
    if (fiadoErrors.length) {
      this.message.set(fiadoErrors.join(' · '));
      return;
    }

    const lines: BatchSaleLine[] = [];
    for (const product of this.cartProducts()) {
      for (const pm of SALES_PAYMENT_METHODS) {
        if (pm.value === 'PENDING') {
          continue;
        }
        const qty = this.getQty(product.id, pm.value);
        if (qty > 0) {
          lines.push({
            productId: product.id,
            paymentMethod: pm.value,
            quantity: qty,
          });
        }
      }
      const pending = this.pendingQty(product.id);
      if (pending > 0) {
        for (const alloc of this.getPendingAllocations(product.id)) {
          if (alloc.quantity > 0 && alloc.memberId != null) {
            lines.push({
              productId: product.id,
              paymentMethod: 'PENDING',
              quantity: alloc.quantity,
              memberId: alloc.memberId,
            });
          }
        }
      }
    }

    if (lines.length === 0) {
      this.message.set('Ingresa al menos una cantidad');
      return;
    }

    const stockErrors: string[] = [];
    for (const product of this.cartProducts()) {
      const units = this.rowTotalUnits(product.id);
      if (units > product.quantity) {
        stockErrors.push(`${product.name}: máx. ${product.quantity}`);
      }
    }
    if (stockErrors.length) {
      this.message.set('Stock insuficiente — ' + stockErrors.join(', '));
      return;
    }

    this.saving.set(true);
    this.saleService.createBatch({ workShiftId: shift.id, lines }).subscribe({
      next: () => {
        this.message.set('Ventas registradas');
        this.saving.set(false);
        this.cartProductIds.set([]);
        this.qtyMatrix.set({});
        this.pendingAllocations.set({});
        this.pickerProductId.set(null);
        this.reloadProducts();
        this.loadShiftDetail(shift.id);
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudieron registrar las ventas');
        this.saving.set(false);
      },
    });
  }

  private reloadProducts(): void {
    this.productService.findAll().subscribe({
      next: (products) => this.products.set(products.filter((p) => p.active)),
    });
  }

  soldQty(productId: number, method: PaymentMethod): number {
    const row = this.soldRows().find((r) => r.productId === productId);
    return row?.byPaymentMethod?.[method]?.quantity ?? 0;
  }

  summaryAmount(method: PaymentMethod): number {
    return this.summary()?.amountByPaymentMethod?.[method] ?? 0;
  }

  viewHistoryShift(shiftId: number): void {
    this.shiftService.getDetail(shiftId).subscribe({
      next: (detail) => this.historyDetail.set(detail),
      error: () => this.message.set('No se pudo cargar el detalle del turno'),
    });
  }

  closeHistoryDetail(): void {
    this.historyDetail.set(null);
  }

  deleteHistoryShift(shift: WorkShift, event: Event): void {
    event.stopPropagation();
    if (shift.status === 'OPEN') {
      this.message.set('Cierra el turno antes de eliminarlo');
      return;
    }
    const label = `${shift.name} · ${shift.employeeName ?? '—'} · ${shift.shiftDate}`;
    if (
      !confirm(
        `¿Eliminar del historial el turno "${label}"?\nSe borrarán sus ventas y se restaurará el inventario.`,
      )
    ) {
      return;
    }
    this.deletingShiftId.set(shift.id);
    this.shiftService.delete(shift.id).subscribe({
      next: () => {
        this.message.set('Turno eliminado del historial');
        this.deletingShiftId.set(null);
        if (this.historyDetail()?.shift.id === shift.id) {
          this.historyDetail.set(null);
        }
        this.loadShiftHistory();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo eliminar el turno');
        this.deletingShiftId.set(null);
      },
    });
  }

  historySoldQty(row: ProductSalesRow, method: PaymentMethod): number {
    return row.byPaymentMethod?.[method]?.quantity ?? 0;
  }
}
