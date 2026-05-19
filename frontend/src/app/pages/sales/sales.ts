import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Employee } from '../../core/models/employee.model';
import { Product } from '../../core/models/product.model';
import { PAYMENT_METHODS, PaymentMethod, Sale, SalesSummary } from '../../core/models/sale.model';
import { WorkShift } from '../../core/models/shift.model';
import { EmployeeService } from '../../core/services/employee.service';
import { ProductService } from '../../core/services/product.service';
import { SaleService } from '../../core/services/sale.service';
import { ShiftService } from '../../core/services/shift.service';

@Component({
  selector: 'app-sales',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './sales.html',
  styleUrl: './sales.scss',
})
export class Sales implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly saleService = inject(SaleService);
  private readonly shiftService = inject(ShiftService);
  private readonly employeeService = inject(EmployeeService);
  private readonly productService = inject(ProductService);

  protected readonly employees = signal<Employee[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly sales = signal<Sale[]>([]);
  protected readonly summary = signal<SalesSummary | null>(null);
  protected readonly openShift = signal<WorkShift | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly openingShift = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly filterEmployeeId = signal<number | null>(null);
  protected readonly shiftNameInput = signal('Mañana');

  protected readonly paymentMethods = PAYMENT_METHODS;
  protected readonly shiftPresets = ['Mañana', 'Tarde', 'Noche'];

  protected readonly form = this.fb.nonNullable.group({
    employeeId: [null as number | null, Validators.required],
    productId: [null as number | null, Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    paymentMethod: ['CASH' as PaymentMethod, Validators.required],
    notes: [''],
  });

  ngOnInit(): void {
    this.employeeService.findActive().subscribe({
      next: (employees) => this.employees.set(employees),
    });
    this.productService.findAll().subscribe({
      next: (products) => this.products.set(products.filter((p) => p.active)),
    });
    this.refreshShift();
  }

  refreshShift(): void {
    this.shiftService.findOpen().subscribe({
      next: (shift) => {
        this.openShift.set(shift);
        if (shift) {
          this.loadShiftData(shift.id);
        } else {
          this.sales.set([]);
          this.summary.set(null);
          this.loading.set(false);
        }
      },
      error: () => {
        this.openShift.set(null);
        this.loading.set(false);
      },
    });
  }

  loadShiftData(shiftId: number): void {
    this.loading.set(true);
    this.shiftService.getSalesSummary(shiftId).subscribe({
      next: (summary) => this.summary.set(summary),
    });
    this.saleService.findAll(this.filterEmployeeId(), shiftId).subscribe({
      next: (sales) => {
        this.sales.set(sales);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Error al cargar ventas del turno');
        this.loading.set(false);
      },
    });
  }

  openShiftAction(name?: string): void {
    const shiftName = name ?? this.shiftNameInput();
    this.openingShift.set(true);
    this.shiftService.open({ name: shiftName }).subscribe({
      next: (shift) => {
        this.message.set(`Turno "${shift.name}" abierto`);
        this.openingShift.set(false);
        this.refreshShift();
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'No se pudo abrir el turno';
        this.message.set(msg);
        this.openingShift.set(false);
        if (typeof msg === 'string' && msg.toLowerCase().includes('turno abierto')) {
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
        this.refreshShift();
      },
      error: () => this.message.set('No se pudo cerrar el turno'),
    });
  }

  onFilterChange(employeeId: string): void {
    this.filterEmployeeId.set(employeeId ? Number(employeeId) : null);
    const shift = this.openShift();
    if (shift) {
      this.loadShiftData(shift.id);
    }
  }

  selectedProduct(): Product | null {
    const productId = this.form.getRawValue().productId;
    return this.products().find((p) => p.id === productId) ?? null;
  }

  save(): void {
    const shift = this.openShift();
    if (!shift) {
      this.message.set('Debe abrir un turno antes de registrar ventas');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    if (raw.employeeId == null || raw.productId == null) {
      return;
    }

    const product = this.selectedProduct();
    if (product && raw.quantity > product.quantity) {
      this.message.set(`Stock insuficiente. Disponible: ${product.quantity}`);
      return;
    }

    this.saving.set(true);
    this.saleService
      .create({
        workShiftId: shift.id,
        employeeId: raw.employeeId,
        productId: raw.productId,
        quantity: raw.quantity,
        paymentMethod: raw.paymentMethod,
        notes: raw.notes || undefined,
      })
      .subscribe({
        next: () => {
          this.message.set('Venta registrada en el turno actual');
          this.saving.set(false);
          this.form.patchValue({ quantity: 1, notes: '' });
          this.productService.findAll().subscribe({
            next: (products) => this.products.set(products.filter((p) => p.active)),
          });
          this.loadShiftData(shift.id);
        },
        error: (err) => {
          this.message.set(err?.error?.message ?? 'No se pudo registrar la venta');
          this.saving.set(false);
        },
      });
  }

  remove(id: number): void {
    const shift = this.openShift();
    if (!shift || !confirm('¿Anular esta venta? El stock se restaurará.')) {
      return;
    }
    this.saleService.delete(id).subscribe({
      next: () => {
        this.message.set('Venta anulada');
        this.productService.findAll().subscribe({
          next: (products) => this.products.set(products.filter((p) => p.active)),
        });
        this.loadShiftData(shift.id);
      },
      error: () => this.message.set('No se pudo anular la venta'),
    });
  }

  summaryAmount(method: PaymentMethod): number {
    return this.summary()?.amountByPaymentMethod?.[method] ?? 0;
  }
}
