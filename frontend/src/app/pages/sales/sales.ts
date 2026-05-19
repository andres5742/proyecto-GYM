import { DatePipe } from '@angular/common';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product } from '../../core/models/product.model';
import {
  BatchSaleLine,
  SALES_PAYMENT_METHODS,
  PaymentMethod,
  SalesSummary,
} from '../../core/models/sale.model';
import { ProductSalesRow, ShiftDetail, WorkShift } from '../../core/models/shift.model';
import { AuthService } from '../../core/services/auth.service';
import { ProductService } from '../../core/services/product.service';
import { SaleService } from '../../core/services/sale.service';
import { ShiftService } from '../../core/services/shift.service';

type QtyMatrix = Record<number, Partial<Record<PaymentMethod, number>>>;

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

  protected readonly products = signal<Product[]>([]);
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
  protected readonly loadingHistory = signal(false);
  protected readonly deletingShiftId = signal<number | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly shiftNameInput = signal('Mañana');
  protected readonly qtyMatrix = signal<QtyMatrix>({});

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
    this.openingShift.set(true);
    this.shiftService
      .open({
        name: shiftName,
        employeeId: user.employeeId ?? undefined,
      })
      .subscribe({
        next: (shift) => {
          this.message.set(`Turno "${shift.name}" abierto · Vendedor: ${shift.employeeName ?? user.fullName}`);
          this.openingShift.set(false);
          this.cartProductIds.set([]);
          this.qtyMatrix.set({});
          this.pickerProductId.set(null);
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

  closeShiftAction(): void {
    const shift = this.openShift();
    if (!shift || !confirm(`¿Cerrar el turno "${shift.name}"?`)) {
      return;
    }
    this.shiftService.close(shift.id).subscribe({
      next: () => {
        this.message.set('Turno cerrado');
        this.historyDetail.set(null);
        this.cartProductIds.set([]);
        this.qtyMatrix.set({});
        this.refreshShift();
        if (this.auth.isAdmin()) {
          this.loadShiftHistory();
        }
      },
      error: () => this.message.set('No se pudo cerrar el turno'),
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

    const lines: BatchSaleLine[] = [];
    for (const product of this.cartProducts()) {
      for (const pm of SALES_PAYMENT_METHODS) {
        const qty = this.getQty(product.id, pm.value);
        if (qty > 0) {
          lines.push({ productId: product.id, paymentMethod: pm.value, quantity: qty });
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
