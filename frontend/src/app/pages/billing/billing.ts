import { DatePipe, formatCurrency, formatDate, getCurrencySymbol } from '@angular/common';
import { Component, computed, effect, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DataTableComponent } from '../../components/data-table/data-table';
import { DataTableColumn } from '../../components/data-table/data-table.model';
import { FaceWebcamCaptureComponent } from '../../components/face-webcam-capture/face-webcam-capture';
import { MemberSearchSelectComponent } from '../../components/member-search-select/member-search-select';
import { APP_CURRENCY, APP_LOCALE } from '../../core/constants/currency';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import {
  BillingCashRegister,
  BillingCashRegisterExpense,
  BillingPayment,
  BillingDailySummary,
  BillingMonthlySummary,
} from '../../core/models/billing.model';
import { Gender, Member } from '../../core/models/member.model';
import { AccessOnboardingKind, MembershipOnboardingRequest } from '../../core/models/billing.model';
import {
  isBillingDayPassPlan,
  isTiqueteraPlan,
  MembershipPlan,
} from '../../core/models/plan.model';
import {
  BillingCashRegisterClosePreview,
  ProductInventoryLine,
} from '../../core/models/billing-close.model';
import {
  CASH_DENOMINATIONS,
  emptyCashForm,
  ShiftHandoverCashForm,
} from '../../core/models/shift-handover.model';
import { BILLING_PAYMENT_METHODS, PaymentMethod } from '../../core/models/sale.model';
import { AuthService } from '../../core/services/auth.service';
import { BillingContextService } from '../../core/services/billing-context.service';
import { BillingService } from '../../core/services/billing.service';
import { MemberService } from '../../core/services/member.service';
import { PlanService } from '../../core/services/plan.service';
import { httpErrorMessage } from '../../core/utils/http-error-message';
import Swal from 'sweetalert2';

const PAYMENT_TIME_LOCALE = 'es-CO';
const PAYMENT_TIME_TZ = 'America/Bogota';

function formatPaymentTime(value: string): string {
  try {
    return formatDate(value, 'HH:mm', PAYMENT_TIME_LOCALE, PAYMENT_TIME_TZ);
  } catch {
    return '—';
  }
}

function formatPaymentAmount(value: number): string {
  return formatCurrency(
    value,
    APP_LOCALE,
    getCurrencySymbol(APP_CURRENCY, 'narrow'),
    APP_CURRENCY,
    '1.0-0',
  );
}

interface MonthlyMethodRow {
  methodLabel: string;
  total: number;
  dayWorkout: number;
  sportsDance: number;
  membership: number;
  expenses: number;
}

const MONTH_LABELS = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
] as const;

const MONTH_SHORT = [
  'Ene',
  'Feb',
  'Mar',
  'Abr',
  'May',
  'Jun',
  'Jul',
  'Ago',
  'Sep',
  'Oct',
  'Nov',
  'Dic',
] as const;

@Component({
  selector: 'app-billing',
  imports: [
    FormsModule,
    DatePipe,
    CopCurrencyPipe,
    MemberSearchSelectComponent,
    FaceWebcamCaptureComponent,
    DataTableComponent,
  ],
  templateUrl: './billing.html',
  styleUrls: ['./billing.scss', './billing-monthly.scss'],
})
export class BillingPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly billingService = inject(BillingService);
  private readonly billingContext = inject(BillingContextService);
  private readonly memberService = inject(MemberService);
  private readonly planService = inject(PlanService);

  protected readonly paymentMethods = BILLING_PAYMENT_METHODS;
  protected readonly monthOptions = MONTH_LABELS.map((label, i) => ({
    value: i + 1,
    label,
  }));

  protected readonly monthPickerItems = MONTH_LABELS.map((label, i) => ({
    value: i + 1,
    label,
    short: MONTH_SHORT[i],
  }));

  protected readonly currentCalendarYear = new Date().getFullYear();
  protected readonly currentCalendarMonth = new Date().getMonth() + 1;

  protected readonly message = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly openingCaja = signal(false);
  protected readonly closingCaja = signal(false);
  protected readonly members = signal<Member[]>([]);
  protected readonly plans = signal<MembershipPlan[]>([]);
  protected readonly payments = signal<BillingPayment[]>([]);
  protected readonly summary = signal<BillingDailySummary | null>(null);
  protected readonly todayRegister = signal<BillingCashRegister | null>(null);
  protected readonly openRegister = signal<BillingCashRegister | null>(null);
  protected readonly section = signal<'membership' | 'summary'>('summary');
  protected readonly tablePageSizes = [10, 20, 30] as const;
  protected readonly isSuperAdmin = () => this.auth.isSuperAdmin();
  protected readonly deletingPaymentId = signal<number | null>(null);

  protected readonly openCajaModal = signal(false);
  protected readonly closeCajaModal = signal(false);
  protected readonly closeCajaPreview = signal<BillingCashRegisterClosePreview | null>(null);
  protected readonly closeCash = signal<ShiftHandoverCashForm>(emptyCashForm());
  protected readonly closeInventoryCounts = signal<Record<number, number>>({});
  protected readonly billDenominations = CASH_DENOMINATIONS.filter((d) => d.type === 'bill').sort(
    (a, b) => b.value - a.value,
  );
  protected readonly coinDenominations = CASH_DENOMINATIONS.filter((d) => d.type === 'coin').sort(
    (a, b) => b.value - a.value,
  );
  /** true = reabrir caja cerrada hoy (solo super admin) */
  protected readonly reopeningCaja = signal(false);
  protected readonly expensesDayModalOpen = signal(false);
  protected readonly openingCashInput = signal(0);
  protected readonly dayExpenses = signal<BillingCashRegisterExpense[]>([]);
  protected readonly savingExpense = signal(false);
  protected readonly expenseAmountInput = signal(0);
  protected readonly expensePaymentMethod = signal<PaymentMethod>('CASH');
  protected readonly expenseObservationInput = signal('');

  protected readonly dayExpensesTotal = computed(() =>
    this.dayExpenses().reduce((sum, e) => sum + e.amount, 0),
  );
  protected readonly monthlyModalOpen = signal(false);
  protected readonly incomeByConceptModalOpen = signal(false);
  protected readonly summaryBreakdownModal = signal<{
    title: string;
    total: number;
    map: Record<string, number>;
  } | null>(null);
  protected readonly monthlyLoading = signal(false);
  protected readonly monthlySummary = signal<BillingMonthlySummary | null>(null);
  protected readonly selectedYear = signal(new Date().getFullYear());
  protected readonly selectedMonth = signal(new Date().getMonth() + 1);

  protected readonly membershipMemberId = signal<number | null>(null);
  protected readonly membershipFlow = signal<'existing' | 'new'>('existing');
  protected readonly newMemberFirstName = signal('');
  protected readonly newMemberLastName = signal('');
  protected readonly newMemberDocumentId = signal('');
  protected readonly newMemberPhone = signal('');
  protected readonly newMemberGender = signal<Gender | ''>('');
  protected readonly accessKind = signal<'FINGERPRINT' | 'FACE' | 'SKIP'>('SKIP');
  protected readonly fingerprintDeviceId = signal('');
  protected readonly fingerprintDeviceLabel = signal('');
  protected readonly billingFaceStatus = signal('Opcional: registra huella o rostro para el ingreso.');
  protected readonly membershipPlanId = signal<number | null>(null);
  protected readonly membershipMonthsPaid = signal(1);
  protected readonly membershipPaymentMethod = signal<PaymentMethod>('CASH');

  private readonly billingFaceCapture = viewChild('billingFaceCapture', {
    read: FaceWebcamCaptureComponent,
  });

  protected readonly genders: { value: Gender | ''; label: string }[] = [
    { value: '', label: 'Sin especificar' },
    { value: 'MALE', label: 'Masculino' },
    { value: 'FEMALE', label: 'Femenino' },
    { value: 'OTHER', label: 'Otro' },
  ];

  protected readonly currentUserName = computed(
    () => this.auth.currentUser()?.fullName ?? 'Usuario',
  );

  protected readonly canBill = computed(() => this.openRegister() != null);
  protected readonly cajaCerradaHoy = computed(
    () => this.todayRegister()?.status === 'CLOSED',
  );
  protected readonly closedRegisterToday = computed(() => {
    const r = this.todayRegister();
    return r?.status === 'CLOSED' ? r : null;
  });
  protected readonly puedeAbrirCaja = computed(
    () => this.todayRegister() == null && !this.openingCaja(),
  );
  protected readonly puedeReabrirCaja = computed(
    () =>
      this.isSuperAdmin() &&
      this.cajaCerradaHoy() &&
      !this.openingCaja(),
  );

  protected readonly membershipPlans = computed(() =>
    this.plans()
      .filter((p) => p.active && !isBillingDayPassPlan(p))
      .sort((a, b) => a.name.localeCompare(b.name, 'es')),
  );

  protected readonly selectedMembershipPlan = computed(() => {
    const planId = this.membershipPlanId();
    if (planId == null) {
      return null;
    }
    return this.membershipPlans().find((p) => p.id === planId) ?? null;
  });

  protected readonly membershipChargeTotal = computed(() => {
    const plan = this.selectedMembershipPlan();
    const months = this.membershipMonthsPaid();
    if (!plan || months < 1) {
      return null;
    }
    return plan.price * months;
  });

  protected readonly membershipEndDatePreview = computed(() => {
    const plan = this.selectedMembershipPlan();
    const months = this.membershipMonthsPaid();
    if (!plan || months < 1) {
      return null;
    }
    const end = new Date();
    end.setDate(end.getDate() + plan.durationDays * months);
    return end.toLocaleDateString('es-CO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  });

  protected readonly monthlyMethodRows = computed((): MonthlyMethodRow[] => {
    const ms = this.monthlySummary();
    if (!ms) {
      return [];
    }
    return this.paymentMethods
      .filter(
        (pm) =>
          this.methodTotal(ms.byMethod, pm.value) > 0 ||
          this.methodTotal(ms.expensesByMethod, pm.value) > 0,
      )
      .map((pm) => ({
        methodLabel: pm.label,
        total: this.methodTotal(ms.byMethod, pm.value),
        dayWorkout: this.methodTotal(ms.dayWorkoutByMethod, pm.value),
        sportsDance: this.methodTotal(ms.sportsDanceByMethod, pm.value),
        membership: this.methodTotal(ms.membershipByMethod, pm.value),
        expenses: this.methodTotal(ms.expensesByMethod, pm.value),
      }));
  });

  protected readonly monthlyMethodColumns: DataTableColumn<MonthlyMethodRow>[] = [
    {
      id: 'method',
      header: 'Medio',
      sortable: true,
      sortValue: (r) => r.methodLabel,
      cell: (r) => r.methodLabel,
    },
    {
      id: 'total',
      header: 'Total',
      sortable: true,
      sortValue: (r) => r.total,
      cell: (r) => formatPaymentAmount(r.total),
      headerClass: 'col-amount',
      cellClass: () => 'col-amount',
    },
    {
      id: 'dayWorkout',
      header: 'Entrenos',
      sortable: true,
      sortValue: (r) => r.dayWorkout,
      cell: (r) => formatPaymentAmount(r.dayWorkout),
      headerClass: 'col-amount',
      cellClass: () => 'col-amount',
    },
    {
      id: 'sportsDance',
      header: 'Bailes',
      sortable: true,
      sortValue: (r) => r.sportsDance,
      cell: (r) => formatPaymentAmount(r.sportsDance),
      headerClass: 'col-amount',
      cellClass: () => 'col-amount',
    },
    {
      id: 'membership',
      header: 'Membresías',
      sortable: true,
      sortValue: (r) => r.membership,
      cell: (r) => formatPaymentAmount(r.membership),
      headerClass: 'col-amount',
      cellClass: () => 'col-amount',
    },
    {
      id: 'expenses',
      header: 'Gastos',
      sortable: true,
      sortValue: (r) => r.expenses,
      cell: (r) => formatPaymentAmount(r.expenses),
      headerClass: 'col-amount col-amount--expense',
      cellClass: () => 'col-amount col-amount--expense',
    },
  ];

  protected readonly paymentsDayTotal = computed(() =>
    this.payments().reduce((sum, p) => sum + p.amount, 0),
  );

  protected readonly movementsPageSize = signal(10);
  protected readonly movementsPage = signal(1);

  protected readonly sortedDayPayments = computed(() =>
    [...this.payments()].sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
  );

  protected readonly movementsTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.sortedDayPayments().length / this.movementsPageSize())),
  );

  protected readonly paginatedDayPayments = computed(() => {
    const page = Math.min(this.movementsPage(), this.movementsTotalPages());
    const start = (page - 1) * this.movementsPageSize();
    return this.sortedDayPayments().slice(start, start + this.movementsPageSize());
  });

  protected readonly movementsPageStart = computed(() =>
    this.sortedDayPayments().length === 0
      ? 0
      : (Math.min(this.movementsPage(), this.movementsTotalPages()) - 1) *
          this.movementsPageSize() +
        1,
  );

  protected readonly movementsPageEnd = computed(() =>
    Math.min(this.movementsPageStart() + this.movementsPageSize() - 1, this.sortedDayPayments().length),
  );

  protected readonly activeRegister = computed(
    () => this.openRegister() ?? this.closedRegisterToday(),
  );

  protected readonly sessionCashBillingTotal = (reg: BillingCashRegister): number =>
    (reg.sessionCashMembership ?? 0) +
    (reg.sessionCashDayWorkout ?? 0) +
    (reg.sessionCashSportsDance ?? 0);

  protected readonly closeBillTotal = computed(() =>
    this.billDenominations.reduce((sum, d) => sum + (this.closeCash()[d.key] || 0) * d.value, 0),
  );
  protected readonly closeCoinTotal = computed(() =>
    this.coinDenominations.reduce((sum, d) => sum + (this.closeCash()[d.key] || 0) * d.value, 0),
  );
  protected readonly closeCashTotal = computed(() => this.closeBillTotal() + this.closeCoinTotal());
  protected readonly closeExpectedCashRounded = computed(() =>
    Math.round(this.closeCajaPreview()?.expectedCashTotal ?? 0),
  );

  protected readonly closeCashDiff = computed(() => {
    return this.closeCashTotal() - this.closeExpectedCashRounded();
  });

  protected readonly closeCashMatches = computed(() => this.closeCashDiff() === 0);

  protected closeRegisterCashExpenses(reg: BillingCashRegister): number {
    return this.methodTotal(reg.sessionExpensesByMethod, 'CASH');
  }

  protected readonly efectivoEnCaja = computed(() => {
    const reg = this.activeRegister();
    if (!reg) {
      return null;
    }
    if (reg.cashInDrawer != null && reg.cashInDrawer !== undefined) {
      return reg.cashInDrawer;
    }
    const billingCash = this.methodTotal(reg.sessionIncomeByMethod, 'CASH');
    const cashOut = this.methodTotal(reg.sessionExpensesByMethod, 'CASH');
    return reg.openingCashAmount + (reg.dayProductSalesCash ?? 0) + billingCash - cashOut;
  });

  protected readonly dayExpensesByMethod = computed(() => {
    const totals: Partial<Record<PaymentMethod, number>> = {};
    for (const exp of this.dayExpenses()) {
      totals[exp.paymentMethod] = (totals[exp.paymentMethod] ?? 0) + exp.amount;
    }
    return totals;
  });

  constructor() {
    effect(() => {
      if (this.billingContext.paymentsRefreshTick() === 0) {
        return;
      }
      this.section.set('summary');
      this.refreshCashRegister();
      this.loadToday();
    });
    effect(() => {
      const planId = this.membershipPlanId();
      if (planId == null) {
        return;
      }
      if (!this.membershipPlans().some((p) => p.id === planId)) {
        this.membershipPlanId.set(null);
      }
    });
  }

  ngOnInit(): void {
    this.memberService.findAll().subscribe({
      next: (m) => this.members.set(m),
      error: (err) => this.message.set(httpErrorMessage(err)),
    });
    this.planService.findAll().subscribe({
      next: (p) => this.plans.set(p),
      error: (err) => this.message.set(httpErrorMessage(err)),
    });
    this.refreshCashRegister();
    this.loadToday();

    const ctxMember = this.billingContext.memberId();
    if (ctxMember != null) {
      this.membershipMemberId.set(ctxMember);
      this.section.set('membership');
    }
  }

  refreshCashRegister(): void {
    this.billingService.findTodayCashRegister().subscribe({
      next: (today) => {
        this.todayRegister.set(today);
        const open = today?.status === 'OPEN' ? today : null;
        this.openRegister.set(open);
        this.billingContext.setOpenCashRegister(open);
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.todayRegister.set(null);
        this.openRegister.set(null);
        this.billingContext.setOpenCashRegister(null);
      },
    });
    this.loadDayExpenses();
  }

  loadDayExpenses(): void {
    const today = new Date().toISOString().slice(0, 10);
    this.billingService.listCashRegisterExpenses(today).subscribe({
      next: (rows) => this.dayExpenses.set(rows),
    });
  }

  loadToday(): void {
    this.billingService.dailySummary().subscribe({
      next: (s) =>
        this.summary.set({
          ...s,
          incomeByMethod: s.incomeByMethod ?? {},
          expenseCount: s.expenseCount ?? 0,
          expensesTotal: s.expensesTotal ?? 0,
          expensesByMethod: s.expensesByMethod ?? {},
        }),
      error: (err) => this.message.set(httpErrorMessage(err)),
    });
    this.billingService.listPayments().subscribe({
      next: (p) => {
        this.payments.set(p);
        this.movementsPage.set(1);
      },
      error: (err) => this.message.set(httpErrorMessage(err)),
    });
    this.loadDayExpenses();
  }

  openOpenCajaModal(): void {
    const closed = this.closedRegisterToday();
    this.reopeningCaja.set(closed != null);
    this.openingCashInput.set(closed?.openingCashAmount ?? 0);
    this.openCajaModal.set(true);
  }

  closeOpenCajaModal(): void {
    if (!this.openingCaja()) {
      this.openCajaModal.set(false);
      this.reopeningCaja.set(false);
    }
  }

  openExpensesDayModal(): void {
    this.loadDayExpenses();
    this.expenseAmountInput.set(0);
    this.expensePaymentMethod.set('CASH');
    this.expenseObservationInput.set('');
    this.expensesDayModalOpen.set(true);
  }

  closeExpensesDayModal(): void {
    if (!this.savingExpense()) {
      this.expensesDayModalOpen.set(false);
    }
  }

  registerExpense(): void {
    if (!this.canBill()) {
      this.message.set('La caja debe estar abierta para registrar gastos');
      return;
    }
    const amount = this.expenseAmountInput();
    const observation = this.expenseObservationInput().trim();
    if (amount <= 0) {
      this.message.set('Indique un valor de gasto mayor a cero');
      return;
    }
    if (!observation) {
      this.message.set('Escriba una observación del gasto');
      return;
    }
    this.savingExpense.set(true);
    this.billingService
      .addCashRegisterExpense({
        amount,
        paymentMethod: this.expensePaymentMethod(),
        observation,
      })
      .subscribe({
      next: () => {
        this.message.set('Gasto registrado');
        this.savingExpense.set(false);
        this.expenseAmountInput.set(0);
        this.expensePaymentMethod.set('CASH');
        this.expenseObservationInput.set('');
        this.refreshCashRegister();
        this.loadDayExpenses();
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.savingExpense.set(false);
      },
    });
  }

  confirmOpenCaja(): void {
    const amount = this.openingCashInput();
    if (amount < 0) {
      this.message.set('El efectivo inicial no puede ser negativo');
      return;
    }
    this.openingCaja.set(true);
    this.billingService.openCashRegister({ openingCashAmount: amount }).subscribe({
      next: (reg) => {
        this.message.set(this.reopeningCaja() ? 'Caja reabierta' : 'Caja del día abierta');
        this.openingCaja.set(false);
        this.openCajaModal.set(false);
        this.reopeningCaja.set(false);
        this.todayRegister.set(reg);
        this.openRegister.set(reg);
        this.billingContext.setOpenCashRegister(reg);
        this.refreshCashRegister();
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.openingCaja.set(false);
      },
    });
  }

  closeCaja(): void {
    const reg = this.openRegister();
    if (!reg) {
      return;
    }
    this.closingCaja.set(true);
    this.billingService.closeCashRegisterPreview(reg.id).subscribe({
      next: (preview) => {
        this.closeCajaPreview.set(preview);
        this.closeCash.set(emptyCashForm());
        const counts: Record<number, number> = {};
        for (const p of preview.products) {
          counts[p.productId] = p.expectedQuantity;
        }
        this.closeInventoryCounts.set(counts);
        this.closeCajaModal.set(true);
        this.closingCaja.set(false);
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.closingCaja.set(false);
      },
    });
  }

  closeCloseCajaModal(): void {
    if (!this.closingCaja()) {
      this.closeCajaModal.set(false);
      this.closeCajaPreview.set(null);
    }
  }

  protected updateCloseCash(key: keyof ShiftHandoverCashForm, value: string | number): void {
    const n = Math.max(0, Math.floor(Number(value) || 0));
    this.closeCash.update((c) => ({ ...c, [key]: n }));
  }

  protected setCloseInventoryCount(productId: number, value: string | number): void {
    const n = Math.max(0, Math.floor(Number(value) || 0));
    this.closeInventoryCounts.update((m) => ({ ...m, [productId]: n }));
  }

  protected closeInventoryMissingQty(line: ProductInventoryLine): number {
    const counted = this.closeInventoryCounts()[line.productId] ?? 0;
    return Math.max(0, line.expectedQuantity - counted);
  }

  protected fillCloseInventoryExpected(): void {
    const preview = this.closeCajaPreview();
    if (!preview) {
      return;
    }
    const counts: Record<number, number> = {};
    for (const p of preview.products) {
      counts[p.productId] = p.expectedQuantity;
    }
    this.closeInventoryCounts.set(counts);
  }

  protected hasCloseInventoryMissing(): boolean {
    const preview = this.closeCajaPreview();
    if (!preview) {
      return false;
    }
    return preview.products.some((p) => this.closeInventoryMissingQty(p) > 0);
  }

  confirmCloseCaja(): void {
    const reg = this.openRegister();
    const preview = this.closeCajaPreview();
    if (!reg || !preview) {
      return;
    }
    if (preview.products.length > 0 && preview.products.some((p) => this.closeInventoryCounts()[p.productId] == null)) {
      this.message.set('Confirme el conteo de todos los productos');
      return;
    }
    this.closingCaja.set(true);
    const cash = this.closeCash();
    this.billingService
      .closeCashRegister(reg.id, {
        cashCount: { ...cash },
        inventoryCounts: preview.products.map((p) => ({
          productId: p.productId,
          countedQuantity: this.closeInventoryCounts()[p.productId] ?? 0,
        })),
      })
      .subscribe({
        next: (result) => {
          let msg = 'Caja del día cerrada';
          if (result.cashShortfall) {
            msg += `. Descuadre de efectivo: ${result.cashShortfall.shortfallAmount}`;
          }
          if (result.inventoryShortfall) {
            msg += `. Descuadre de inventario: ${result.inventoryShortfall.shortfallAmount}`;
          }
          this.message.set(msg);
          this.closingCaja.set(false);
          this.closeCajaModal.set(false);
          this.closeCajaPreview.set(null);
          this.openRegister.set(null);
          this.todayRegister.set(result.register);
          this.billingContext.setOpenCashRegister(null);
          this.refreshCashRegister();
        },
        error: (err) => {
          this.message.set(httpErrorMessage(err));
          this.closingCaja.set(false);
        },
      });
  }

  setMembershipMonthsPaid(value: number | string): void {
    const parsed = Math.floor(Number(value));
    if (!Number.isFinite(parsed)) {
      this.membershipMonthsPaid.set(1);
      return;
    }
    this.membershipMonthsPaid.set(Math.min(36, Math.max(1, parsed)));
  }

  protected membershipPlanOptionSuffix(plan: MembershipPlan): string {
    if (isTiqueteraPlan(plan)) {
      const entrenos = plan.monthlyEntryLimit ?? '?';
      return ` · ${entrenos} entrenos / ${plan.durationDays} días`;
    }
    return ` / ${plan.durationDays} días`;
  }

  setMembershipFlow(flow: 'existing' | 'new'): void {
    this.membershipFlow.set(flow);
    if (flow === 'new') {
      this.membershipMemberId.set(null);
    } else {
      this.accessKind.set('SKIP');
      this.fingerprintDeviceId.set('');
      this.fingerprintDeviceLabel.set('');
    }
  }

  onRegisterNewMember(query: string): void {
    this.setMembershipFlow('new');
    const trimmed = query.trim();
    if (/^\d[\d.\s-]{4,}$/.test(trimmed)) {
      this.newMemberDocumentId.set(trimmed.replace(/\s+/g, ''));
    }
  }

  onBillingFaceStatus(status: string): void {
    this.billingFaceStatus.set(status);
  }

  private resetMembershipForm(): void {
    this.membershipMemberId.set(null);
    this.membershipFlow.set('existing');
    this.newMemberFirstName.set('');
    this.newMemberLastName.set('');
    this.newMemberDocumentId.set('');
    this.newMemberPhone.set('');
    this.newMemberGender.set('');
    this.accessKind.set('SKIP');
    this.fingerprintDeviceId.set('');
    this.fingerprintDeviceLabel.set('');
    this.billingFaceStatus.set('Opcional: registra huella o rostro para el ingreso.');
    this.membershipPlanId.set(null);
    this.membershipMonthsPaid.set(1);
    this.membershipPaymentMethod.set('CASH');
  }

  async registerMembership(): Promise<void> {
    if (!this.canBill()) {
      this.message.set('Abra la caja del día para registrar membresías');
      return;
    }
    const planId = Number(this.membershipPlanId());
    const monthsPaid = Number(this.membershipMonthsPaid());
    const plan = this.selectedMembershipPlan();
    if (!Number.isFinite(planId) || plan == null) {
      this.message.set('Selecciona un plan de membresía válido');
      return;
    }
    if (isBillingDayPassPlan(plan)) {
      this.message.set('Entreno del día y bailes deportivos se registran con F2 y F3');
      this.membershipPlanId.set(null);
      return;
    }
    if (monthsPaid < 1) {
      this.message.set('Indica al menos 1 mes a pagar');
      return;
    }

    const flow = this.membershipFlow();
    const memberId = this.membershipMemberId();
    let newMember: MembershipOnboardingRequest['newMember'];

    if (flow === 'existing') {
      if (!Number.isFinite(memberId)) {
        this.message.set('Busca y selecciona el afiliado, o regístralo como nuevo');
        return;
      }
    } else {
      const firstName = this.newMemberFirstName().trim();
      const lastName = this.newMemberLastName().trim();
      const documentId = this.newMemberDocumentId().trim();
      if (!firstName || !lastName || !documentId) {
        this.message.set('Nombre, apellido y documento son obligatorios para el afiliado nuevo');
        return;
      }
      newMember = {
        firstName,
        lastName,
        documentId,
        phone: this.newMemberPhone().trim() || undefined,
        gender: this.newMemberGender() || null,
      };
    }

    let access: MembershipOnboardingRequest['access'] = null;
    if (flow !== 'new') {
      access = null;
    } else {
    const accessKind = this.accessKind();
    if (accessKind === 'FINGERPRINT') {
      const deviceUserId = this.fingerprintDeviceId().trim();
      if (!deviceUserId) {
        this.message.set('Indica el ID de huella del lector biométrico');
        return;
      }
      access = {
        kind: 'FINGERPRINT' as AccessOnboardingKind,
        deviceUserId,
        deviceLabel: this.fingerprintDeviceLabel().trim() || undefined,
      };
    } else if (accessKind === 'FACE') {
      const capture = this.billingFaceCapture();
      if (!capture) {
        this.message.set('Prepara la cámara para capturar el rostro');
        return;
      }
      const descriptor = await capture.captureDescriptor();
      if (!descriptor || descriptor.length !== 128) {
        this.message.set('Mira la cámara hasta que el rostro quede encuadrado y vuelve a intentar');
        return;
      }
      access = { kind: 'FACE' as AccessOnboardingKind, faceDescriptor: descriptor };
    }
    }

    const request: MembershipOnboardingRequest = {
      memberId: flow === 'existing' ? memberId : null,
      newMember: flow === 'new' ? newMember : null,
      planId,
      paymentMethod: this.membershipPaymentMethod(),
      monthsPaid,
      access,
    };

    this.saving.set(true);
    this.billingService.registerMembershipOnboarding(request).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.resetMembershipForm();
        this.memberService.findAll().subscribe({
          next: (m) => this.members.set(m),
        });
        this.refreshCashRegister();
        this.loadToday();
        this.section.set('summary');
        const accessNote = res.accessRegistered
          ? res.accessMessage
          : 'Puede registrar huella o rostro después en Acceso biométrico.';
        void Swal.fire({
          icon: 'success',
          title: 'Pago registrado',
          html: `Membresía activada para <strong>${res.member.firstName} ${res.member.lastName}</strong>.<br>${accessNote}`,
          confirmButtonText: 'Aceptar',
          confirmButtonColor: '#d4623a',
        });
      },
      error: (err) => {
        const msg = httpErrorMessage(err);
        this.message.set(msg);
        this.saving.set(false);
        void Swal.fire({
          icon: 'error',
          title: 'No se pudo registrar el pago',
          text: msg,
          confirmButtonColor: '#d4623a',
        });
      },
    });
  }

  openMonthlyModal(): void {
    this.monthlyModalOpen.set(true);
    this.loadMonthlySummary();
  }

  closeMonthlyModal(): void {
    this.monthlyModalOpen.set(false);
  }

  loadMonthlySummary(): void {
    this.monthlyLoading.set(true);
    this.billingService.monthlySummary(this.selectedYear(), this.selectedMonth()).subscribe({
      next: (s) => {
        this.monthlySummary.set({
          ...s,
          totalExpenses: s.totalExpenses ?? 0,
          expenseCount: s.expenseCount ?? 0,
          expensesByMethod: s.expensesByMethod ?? {},
        });
        this.monthlyLoading.set(false);
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.monthlyLoading.set(false);
      },
    });
  }

  methodTotal(map: Record<string, number> | undefined, key: string): number {
    return map?.[key as PaymentMethod] ?? 0;
  }

  hasMethodAmount(map: Record<string, number> | undefined, key: PaymentMethod): boolean {
    return this.methodTotal(map, key) > 0;
  }

  hasAnyMethodAmount(map: Record<string, number> | undefined): boolean {
    return this.paymentMethods.some((pm) => this.hasMethodAmount(map, pm.value));
  }

  protected hasConceptActivity(total: number, count: number): boolean {
    return total > 0 || count > 0;
  }

  protected movementCountLabel(count: number): string {
    return count === 1 ? '1 movimiento' : `${count} movimientos`;
  }

  openIncomeByConceptModal(): void {
    this.incomeByConceptModalOpen.set(true);
  }

  closeIncomeByConceptModal(): void {
    this.incomeByConceptModalOpen.set(false);
  }

  openSummaryBreakdown(
    title: string,
    map: Record<string, number> | undefined,
    total: number,
  ): void {
    if (!map || !this.hasAnyMethodAmount(map)) {
      return;
    }
    this.summaryBreakdownModal.set({ title, total, map });
  }

  closeSummaryBreakdown(): void {
    this.summaryBreakdownModal.set(null);
  }

  methodChipClass(method: PaymentMethod): string {
    switch (method) {
      case 'CASH':
        return 'method-chip method-chip--cash';
      case 'NEQUI':
        return 'method-chip method-chip--nequi';
      case 'BANCOLOMBIA':
        return 'method-chip method-chip--bancolombia';
      default:
        return 'method-chip';
    }
  }

  sumMethodMap(map: Record<string, number> | undefined): number {
    if (!map) {
      return 0;
    }
    return this.paymentMethods.reduce(
      (sum, pm) => sum + this.methodTotal(map, pm.value),
      0,
    );
  }

  monthlyNetBalance(ms: BillingMonthlySummary): number {
    return (ms.grandTotal ?? 0) - (ms.totalExpenses ?? 0);
  }

  selectedMonthLabel(): string {
    return this.monthOptions.find((m) => m.value === this.selectedMonth())?.label ?? '';
  }

  selectMonth(month: number): void {
    if (this.isFutureMonth(month) || this.selectedMonth() === month) {
      return;
    }
    this.selectedMonth.set(month);
    this.loadMonthlySummary();
  }

  changeYear(delta: number): void {
    const nextYear = this.selectedYear() + delta;
    if (nextYear < 2020 || nextYear > this.currentCalendarYear) {
      return;
    }
    this.selectedYear.set(nextYear);
    if (this.isFutureMonth(this.selectedMonth())) {
      this.selectedMonth.set(this.currentCalendarMonth);
    }
    this.loadMonthlySummary();
  }

  isSelectedMonth(month: number): boolean {
    return this.selectedMonth() === month;
  }

  isCurrentMonth(month: number): boolean {
    return (
      this.selectedYear() === this.currentCalendarYear &&
      month === this.currentCalendarMonth
    );
  }

  isFutureMonth(month: number): boolean {
    const year = this.selectedYear();
    if (year > this.currentCalendarYear) {
      return true;
    }
    if (year < this.currentCalendarYear) {
      return false;
    }
    return month > this.currentCalendarMonth;
  }

  canGoToPreviousYear(): boolean {
    return this.selectedYear() > 2020;
  }

  canGoToNextYear(): boolean {
    return this.selectedYear() < this.currentCalendarYear;
  }

  hasMonthlyBreakdownRows(ms: BillingMonthlySummary): boolean {
    return (
      this.hasAnyMethodAmount(ms.byMethod) || this.hasAnyMethodAmount(ms.expensesByMethod)
    );
  }

  protected onMovementsPageSizeChange(value: string): void {
    this.movementsPageSize.set(Number(value));
    this.movementsPage.set(1);
  }

  protected goToMovementsPage(next: number): void {
    this.movementsPage.set(Math.max(1, Math.min(next, this.movementsTotalPages())));
  }

  protected formatPaymentTime(value: string): string {
    return formatPaymentTime(value);
  }

  protected formatPaymentAmount(value: number): string {
    return formatPaymentAmount(value);
  }

  protected paymentTypeBadgeClass(type: BillingPayment['paymentType']): string {
    switch (type) {
      case 'DAY_WORKOUT':
        return 'type-badge type-badge--workout';
      case 'SPORTS_DANCE':
        return 'type-badge type-badge--dance';
      default:
        return 'type-badge';
    }
  }

  deletePayment(row: BillingPayment, event: Event): void {
    event.stopPropagation();
    const label = `${row.paymentTypeLabel} · ${row.memberName} · ${row.amount}`;
    if (
      !confirm(
        `¿Eliminar este movimiento de hoy?\n${label}\nEsta acción no se puede deshacer.`,
      )
    ) {
      return;
    }
    this.deletingPaymentId.set(row.id);
    this.billingService.deletePayment(row.id).subscribe({
      next: () => {
        this.message.set('Movimiento eliminado');
        this.deletingPaymentId.set(null);
        this.refreshCashRegister();
        this.loadToday();
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.deletingPaymentId.set(null);
      },
    });
  }
}
