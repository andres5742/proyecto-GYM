import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Product, ProductRequest } from '../../core/models/product.model';
import { ProductService } from '../../core/services/product.service';

@Component({
  selector: 'app-inventory',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './inventory.html',
  styleUrl: './inventory.scss',
})
export class Inventory implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly productService = inject(ProductService);

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly totalUnits = computed(() =>
    this.products().reduce((sum, p) => sum + p.quantity, 0),
  );

  protected readonly totalValue = computed(() =>
    this.products().reduce((sum, p) => sum + p.stockValue, 0),
  );

  protected readonly lowStockCount = computed(() =>
    this.products().filter((p) => p.lowStock).length,
  );

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    category: [''],
    quantity: [0, [Validators.required, Validators.min(0)]],
    unitPrice: [0, [Validators.required, Validators.min(0)]],
    minStock: [0, [Validators.min(0)]],
    active: [true],
  });

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.productService.findAll().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Error al cargar inventario');
        this.loading.set(false);
      },
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      name: '',
      description: '',
      category: '',
      quantity: 0,
      unitPrice: 0,
      minStock: 0,
      active: true,
    });
  }

  startEdit(product: Product): void {
    this.editingId.set(product.id);
    this.form.patchValue({
      name: product.name,
      description: product.description ?? '',
      category: product.category ?? '',
      quantity: product.quantity,
      unitPrice: product.unitPrice,
      minStock: product.minStock,
      active: product.active,
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: ProductRequest = {
      name: raw.name,
      description: raw.description || undefined,
      category: raw.category || undefined,
      quantity: raw.quantity,
      unitPrice: raw.unitPrice,
      minStock: raw.minStock,
      active: raw.active,
    };

    this.saving.set(true);
    const id = this.editingId();
    const action = id
      ? this.productService.update(id, request)
      : this.productService.create(request);

    action.subscribe({
      next: () => {
        this.message.set(id ? 'Producto actualizado' : 'Producto registrado');
        this.saving.set(false);
        this.startCreate();
        this.loadProducts();
      },
      error: () => {
        this.message.set('No se pudo guardar el producto');
        this.saving.set(false);
      },
    });
  }

  applyStockAdjustment(product: Product, delta: number): void {
    if (!delta) {
      return;
    }
    this.productService.adjustStock(product.id, { delta }).subscribe({
      next: () => {
        this.message.set(
          delta > 0 ? `Entrada de ${delta} unidades` : `Salida de ${Math.abs(delta)} unidades`,
        );
        this.loadProducts();
      },
      error: () => this.message.set('No se pudo ajustar el stock'),
    });
  }

  remove(id: number): void {
    if (!confirm('¿Eliminar este producto del inventario?')) {
      return;
    }
    this.productService.delete(id).subscribe({
      next: () => {
        this.message.set('Producto eliminado');
        this.loadProducts();
      },
      error: () => this.message.set('No se pudo eliminar el producto'),
    });
  }
}
