import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { DataTableComponent } from '../../components/data-table/data-table';
import { DataTableColumn } from '../../components/data-table/data-table.model';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { Member } from '../../core/models/member.model';
import { Product } from '../../core/models/product.model';
import {
  FIADO_PAYMENT_METHODS,
  MemberFiadoGroup,
  ProductCredit,
  ProductCreditDebtorType,
  ProductCreditStatus,
} from '../../core/models/product-credit.model';
import { BillingContextService } from '../../core/services/billing-context.service';
import { EmployeeService } from '../../core/services/employee.service';
import { MemberService } from '../../core/services/member.service';
import { ProductCreditService } from '../../core/services/product-credit.service';
import { ProductService } from '../../core/services/product.service';
import { ShiftService } from '../../core/services/shift.service';
import { filterMembersByQuery, sortMembersByName } from '../../core/utils/member-search';
import { WorkShift } from '../../core/models/shift.model';
import { Employee } from '../../core/models/employee.model';

@Component({
  selector: 'app-product-credits',
  imports: [FormsModule, CopCurrencyPipe, DatePipe, DataTableComponent],
  templateUrl: './product-credits.html',
  styleUrl: './product-credits.scss',
})
export class ProductCreditsPage implements OnInit {
  private readonly creditService = inject(ProductCreditService);
  private readonly billingContext = inject(BillingContextService);
  private readonly memberService = inject(MemberService);
  private readonly employeeService = inject(EmployeeService);
  private readonly productService = inject(ProductService);
  private readonly shiftService = inject(ShiftService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly credits = signal<ProductCredit[]>([]);
  protected readonly members = signal<Member[]>([]);
  protected readonly trainers = signal<Employee[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly openShift = signal<WorkShift | null>(null);
  protected readonly statusFilter = signal<'ALL' | ProductCreditStatus>('OPEN');

  protected readonly selectedGroup = signal<MemberFiadoGroup | null>(null);
  protected readonly payAllTarget = signal<MemberFiadoGroup | null>(null);
  protected readonly detailCredits = signal<ProductCredit[]>([]);
  protected readonly detailLoading = signal(false);
  protected readonly payingCreditId = signal<number | null>(null);
  protected readonly payAmount = signal<number | null>(null);
  protected readonly payMethod = signal(FIADO_PAYMENT_METHODS[0].value);
  protected readonly payNotes = signal('');

  protected readonly memberSearch = signal('');
  protected readonly debtorType = signal<ProductCreditDebtorType>('MEMBER');
  protected readonly selectedMemberId = signal<number | null>(null);
  protected readonly selectedTrainerId = signal<number | null>(null);
  protected readonly debtMode = signal<'PRODUCT' | 'PRIOR_DEBT'>('PRODUCT');
  protected readonly selectedProductId = signal<number | null>(null);
  protected readonly quantity = signal(1);
  protected readonly priorDebtAmount = signal<number | null>(null);
  protected readonly priorDebtConcept = signal('');
  protected readonly creditNotes = signal('');

  protected readonly paymentMethods = FIADO_PAYMENT_METHODS;

  protected memberInitial(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return (parts[0]?.[0] ?? '?').toUpperCase();
  }

  protected paymentMethodIcon(method: string): string {
    switch (method) {
      case 'CASH':
        return '💵';
      case 'NEQUI':
        return '📱';
      case 'BANCOLOMBIA':
        return '🏦';
      default:
        return '💳';
    }
  }
  protected readonly tablePageSizes = [10, 20, 30, 50] as const;

  protected readonly filteredMembers = computed(() =>
    filterMembersByQuery(this.members(), this.memberSearch(), 30),
  );
  protected readonly filteredTrainers = computed(() => {
    const q = this.memberSearch().trim().toLowerCase();
    if (!q) {
      return this.trainers().slice(0, 30);
    }
    return this.trainers()
      .filter((t) => `${t.firstName} ${t.lastName}`.toLowerCase().includes(q))
      .slice(0, 30);
  });

  protected readonly selectedProduct = computed(() =>
    this.products().find((p) => p.id === this.selectedProductId()) ?? null,
  );

  protected readonly creditLineTotal = computed(() => {
    if (this.debtMode() === 'PRIOR_DEBT') {
      return Math.max(0, this.priorDebtAmount() ?? 0);
    }
    const p = this.selectedProduct();
    if (!p) {
      return 0;
    }
    return p.unitPrice * Math.max(1, this.quantity());
  });

  protected readonly displayCredits = computed(() => {
    const f = this.statusFilter();
    const list = this.credits();
    if (f === 'ALL') {
      return list;
    }
    return list.filter((c) => c.status === f);
  });

  protected readonly memberGroups = computed((): MemberFiadoGroup[] => {
    const map = new Map<number, MemberFiadoGroup>();
    for (const c of this.displayCredits()) {
      const key = this.groupKey(c);
      let group = map.get(key);
      if (!group) {
        group = {
          debtorType: c.debtorType,
          memberId: c.memberId ?? null,
          debtorEmployeeId: c.debtorEmployeeId ?? null,
          memberName: c.debtorName,
          memberDocumentId: c.memberDocumentId ?? null,
          openBalance: 0,
          openItemsCount: 0,
          credits: [],
          lastCreditedAt: c.creditedAt,
        };
        map.set(key, group);
      }
      group.credits.push(c);
      if (c.creditedAt > group.lastCreditedAt) {
        group.lastCreditedAt = c.creditedAt;
      }
      if (c.status === 'OPEN') {
        group.openBalance += c.balance;
        group.openItemsCount += 1;
      }
    }
    return Array.from(map.values()).sort((a, b) =>
      a.memberName.localeCompare(b.memberName, 'es'),
    );
  });

  protected readonly openBalanceTotal = computed(() =>
    this.credits()
      .filter((c) => c.status === 'OPEN')
      .reduce((s, c) => s + c.balance, 0),
  );

  protected readonly memberTableColumns: DataTableColumn<MemberFiadoGroup>[] = [
    {
      id: 'member',
      header: 'Deudor',
      sortable: true,
      sortValue: (r) => r.memberName,
      cell: (r) => r.memberName,
    },
    {
      id: 'document',
      header: 'Documento',
      sortable: true,
      sortValue: (r) => r.memberDocumentId ?? '',
      cell: (r) => r.memberDocumentId ?? '—',
      cellClass: () => 'col-muted',
    },
    {
      id: 'items',
      header: 'Productos fiados',
      sortable: true,
      sortValue: (r) => r.openItemsCount || r.credits.length,
      cell: (r) => this.itemsLabel(r),
    },
    {
      id: 'balance',
      header: 'Debe',
      sortable: true,
      sortValue: (r) => r.openBalance,
      cell: (r) => this.balanceLabel(r),
      headerClass: 'col-amount',
      cellClass: () => 'col-amount',
      cellInnerClass: (r) => (r.openBalance > 0 ? 'amount-warn' : null),
    },
    {
      id: 'date',
      header: 'Último fiado',
      sortable: true,
      sortValue: (r) => r.lastCreditedAt,
      cell: (r) => this.formatDateShort(r.lastCreditedAt),
      cellClass: () => 'col-muted',
    },
  ];

  protected readonly trackMemberGroup = (row: MemberFiadoGroup) => this.groupKey(row);

  protected readonly payingCredit = computed(() => {
    const id = this.payingCreditId();
    if (id == null) {
      return null;
    }
    return this.detailCredits().find((c) => c.id === id) ?? null;
  });

  ngOnInit(): void {
    this.memberService.findAll().subscribe({
      next: (list) => this.members.set(sortMembersByName(list)),
      error: () => this.members.set([]),
    });
    this.employeeService.findActive().subscribe({
      next: (list) =>
        this.trainers.set(
          list.filter((e) => (e.role ?? '').toUpperCase() === 'TRAINER').sort((a, b) => a.fullName.localeCompare(b.fullName, 'es')),
        ),
      error: () => this.trainers.set([]),
    });
    this.productService.findAll().subscribe({
      next: (list) => this.products.set(list.filter((p) => p.active)),
      error: () => this.products.set([]),
    });
    this.shiftService.findOpen().subscribe({
      next: (shift) => this.openShift.set(shift),
    });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const f = this.statusFilter();
    const status: ProductCreditStatus | undefined = f === 'ALL' ? undefined : f;
    this.creditService.findAll(status).subscribe({
      next: (list) => {
        this.credits.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('No se pudo cargar el fiado');
        this.loading.set(false);
      },
    });
  }

  onFilterChange(): void {
    this.load();
  }

  selectMember(id: number): void {
    this.debtorType.set('MEMBER');
    this.selectedTrainerId.set(null);
    this.selectedMemberId.set(id);
    const m = this.members().find((x) => x.id === id);
    if (m) {
      this.memberSearch.set(`${m.firstName} ${m.lastName}`);
    }
  }

  selectTrainer(id: number): void {
    this.debtorType.set('STAFF');
    this.selectedMemberId.set(null);
    this.selectedTrainerId.set(id);
    const t = this.trainers().find((x) => x.id === id);
    if (t) {
      this.memberSearch.set(`${t.firstName} ${t.lastName}`);
    }
  }

  registerCredit(): void {
    const shift = this.openShift();
    const memberId = this.selectedMemberId();
    const trainerId = this.selectedTrainerId();
    const productId = this.selectedProductId();
    if (!shift) {
      this.message.set('Abra un turno antes de registrar fiado');
      return;
    }
    if (this.debtorType() === 'MEMBER' && !memberId) {
      this.message.set('Seleccione afiliado');
      return;
    }
    if (this.debtorType() === 'STAFF' && !trainerId) {
      this.message.set('Seleccione entrenador');
      return;
    }
    if (this.debtMode() === 'PRODUCT' && !productId) {
      this.message.set('Seleccione el producto');
      return;
    }
    if (this.debtMode() === 'PRIOR_DEBT' && (!this.priorDebtAmount() || this.priorDebtAmount()! <= 0)) {
      this.message.set('Indique el monto de la deuda anterior');
      return;
    }
    const qty = Math.max(1, this.quantity());
    this.saving.set(true);
    this.creditService
      .create({
        memberId: this.debtorType() === 'MEMBER' ? memberId ?? undefined : undefined,
        employeeDebtorId: this.debtorType() === 'STAFF' ? trainerId ?? undefined : undefined,
        productId: this.debtMode() === 'PRODUCT' ? productId ?? undefined : undefined,
        quantity: this.debtMode() === 'PRODUCT' ? qty : 1,
        priorDebt: this.debtMode() === 'PRIOR_DEBT',
        manualAmount: this.debtMode() === 'PRIOR_DEBT' ? this.priorDebtAmount() ?? undefined : undefined,
        concept: this.debtMode() === 'PRIOR_DEBT' ? this.priorDebtConcept() || undefined : undefined,
        workShiftId: shift.id,
        notes: this.creditNotes() || undefined,
      })
      .subscribe({
        next: () => {
          this.message.set('Fiado registrado');
          this.selectedMemberId.set(null);
          this.selectedTrainerId.set(null);
          this.selectedProductId.set(null);
          this.quantity.set(1);
          this.priorDebtAmount.set(null);
          this.priorDebtConcept.set('');
          this.creditNotes.set('');
          this.memberSearch.set('');
          this.saving.set(false);
          this.load();
        },
        error: (err) => {
          this.message.set(err.error?.message ?? 'No se pudo registrar el fiado');
          this.saving.set(false);
        },
      });
  }

  viewMember(group: MemberFiadoGroup): void {
    this.payAllTarget.set(null);
    this.openMemberModal(group);
  }

  payMember(group: MemberFiadoGroup): void {
    if (group.openBalance <= 0 || !this.openShift()) {
      if (!this.openShift()) {
        this.message.set('Abra un turno para cobrar el fiado');
      }
      return;
    }
    this.selectedGroup.set(null);
    this.payAllTarget.set(group);
    this.payMethod.set(FIADO_PAYMENT_METHODS[0].value);
    this.payNotes.set('');
  }

  closeModal(): void {
    this.selectedGroup.set(null);
    this.detailCredits.set([]);
    this.payingCreditId.set(null);
    this.payAmount.set(null);
    this.payNotes.set('');
  }

  closePayAllModal(): void {
    this.payAllTarget.set(null);
    this.payNotes.set('');
  }

  openMemberModal(group: MemberFiadoGroup): void {
    this.selectedGroup.set(group);
    this.detailLoading.set(true);
    this.payingCreditId.set(null);
    this.payAmount.set(null);

    const toLoad = group.credits.filter((c) => c.status === 'OPEN');
    const source$ =
      toLoad.length > 0
        ? forkJoin(toLoad.map((c) => this.creditService.findById(c.id)))
        : of(group.credits);

    source$.subscribe({
      next: (details) => {
        this.detailCredits.set(details);
        this.detailLoading.set(false);
      },
      error: () => {
        this.detailCredits.set(group.credits);
        this.detailLoading.set(false);
        this.message.set('No se pudo cargar el detalle');
      },
    });
  }

  confirmPayAll(): void {
    const group = this.payAllTarget();
    const shift = this.openShift();
    if (!group || !shift) {
      return;
    }
    const items =
      group.openItemsCount === 1 ? '1 producto' : `${group.openItemsCount} productos`;
    if (
      !confirm(
        `¿Cobrar todo el fiado de ${group.memberName}?\n${items} · Total: ${group.openBalance.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })}`,
      )
    ) {
      return;
    }
    this.saving.set(true);
    const payAll$ =
      group.debtorType === 'STAFF'
        ? this.creditService.payAllForEmployee(group.debtorEmployeeId!, {
            paymentMethod: this.payMethod(),
            workShiftId: shift.id,
            notes: this.payNotes() || undefined,
          })
        : this.creditService.payAllForMember(group.memberId!, {
            paymentMethod: this.payMethod(),
            workShiftId: shift.id,
            notes: this.payNotes() || undefined,
          });
    payAll$.subscribe({
        next: (res) => {
          this.message.set(
            `Cobro registrado: ${res.creditsPaid} producto(s) · ${res.totalAmount.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })}`,
          );
          this.saving.set(false);
          this.billingContext.notifyBillingPaymentRecorded();
          this.closePayAllModal();
          this.load();
        },
        error: (err) => {
          this.message.set(err.error?.message ?? 'No se pudo registrar el cobro total');
          this.saving.set(false);
        },
      });
  }

  startPayCredit(credit: ProductCredit): void {
    if (credit.status !== 'OPEN' || !this.openShift()) {
      return;
    }
    this.payingCreditId.set(credit.id);
    this.payAmount.set(credit.balance);
    this.payNotes.set('');
  }

  cancelPayLine(): void {
    this.payingCreditId.set(null);
    this.payAmount.set(null);
  }

  registerPayment(credit: ProductCredit): void {
    const shift = this.openShift();
    const amount = this.payAmount();
    if (!shift) {
      this.message.set('Abra un turno para registrar el cobro');
      return;
    }
    if (!amount || amount <= 0) {
      this.message.set('Indique un monto válido');
      return;
    }
    if (amount > credit.balance) {
      this.message.set('El monto supera el saldo pendiente');
      return;
    }
    this.saving.set(true);
    this.creditService
      .registerPayment(credit.id, {
        amount,
        paymentMethod: this.payMethod(),
        workShiftId: shift.id,
        notes: this.payNotes() || undefined,
      })
      .subscribe({
        next: () => {
          this.message.set('Abono registrado');
          this.saving.set(false);
          this.billingContext.notifyBillingPaymentRecorded();
          this.reloadAndRefreshModal();
        },
        error: (err) => {
          this.message.set(err.error?.message ?? 'No se pudo registrar el abono');
          this.saving.set(false);
        },
      });
  }

  private itemsLabel(group: MemberFiadoGroup): string {
    if (this.statusFilter() === 'OPEN' || group.openItemsCount > 0) {
      const n = group.openItemsCount || group.credits.length;
      return n === 1 ? '1 producto' : `${n} productos`;
    }
    const n = group.credits.length;
    return n === 1 ? '1 registro' : `${n} registros`;
  }

  private balanceLabel(group: MemberFiadoGroup): string {
    if (group.openBalance > 0) {
      return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        maximumFractionDigits: 0,
      }).format(group.openBalance);
    }
    if (this.statusFilter() === 'PAID') {
      const total = group.credits.reduce((s, c) => s + c.totalAmount, 0);
      return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        maximumFractionDigits: 0,
      }).format(total);
    }
    return '—';
  }

  private reloadAndRefreshModal(): void {
    const selectedKey = this.selectedGroup() ? this.groupKey(this.selectedGroup()!) : null;
    const f = this.statusFilter();
    const status: ProductCreditStatus | undefined = f === 'ALL' ? undefined : f;
    this.creditService.findAll(status).subscribe({
      next: (list) => {
        this.credits.set(list);
        if (selectedKey == null) {
          this.closeModal();
          return;
        }
        const group = this.memberGroups().find((g) => this.groupKey(g) === selectedKey);
        if (group && group.openBalance > 0) {
          this.openMemberModal(group);
        } else {
          this.closeModal();
        }
      },
      error: () => this.load(),
    });
  }

  private formatDateShort(iso: string): string {
    try {
      return new Date(iso).toLocaleString('es-CO', {
        day: '2-digit',
        month: '2-digit',
        year: '2-digit',
        hour: 'numeric',
        minute: '2-digit',
      });
    } catch {
      return iso;
    }
  }

  protected readonly debtorTypeLabel = computed(() =>
    this.debtorType() === 'STAFF' ? 'Entrenador' : 'Afiliado',
  );

  private groupKey(item: Pick<MemberFiadoGroup, 'debtorType' | 'memberId' | 'debtorEmployeeId'>): number {
    return item.debtorType === 'STAFF'
      ? -(item.debtorEmployeeId ?? 0)
      : (item.memberId ?? 0);
  }
}
