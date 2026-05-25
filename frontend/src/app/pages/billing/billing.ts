import { DatePipe, formatCurrency, formatDate, getCurrencySymbol } from '@angular/common';
import { Component, computed, effect, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DataTableComponent } from '../../components/data-table/data-table';
import { DataTableColumn } from '../../components/data-table/data-table.model';
import { MemberSearchSelectComponent } from '../../components/member-search-select/member-search-select';
import { APP_CURRENCY, APP_LOCALE } from '../../core/constants/currency';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import {
  BillingCashRegister,
  BillingCashRegisterExpense,
  DigitalAccountIncomeLine,
  BillingCashRegisterOtherIncome,
  PaymentAccountSettings,
  BillingPayment,
  BillingDailySummary,
  BillingMonthlySummary,
} from '../../core/models/billing.model';
import { Gender, Member } from '../../core/models/member.model';
import {
  AccessOnboardingKind,
  MembershipObligation,
  MembershipOnboardingRequest,
} from '../../core/models/billing.model';
import { roundCop } from '../../core/utils/money';
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
import { AccessService } from '../../core/services/access.service';
import { BillingService } from '../../core/services/billing.service';
import { MemberService } from '../../core/services/member.service';
import { PlanService } from '../../core/services/plan.service';
import { httpErrorMessage } from '../../core/utils/http-error-message';
import { todayIsoDate } from '../../core/utils/today-date';
import Swal from 'sweetalert2';

const PAYMENT_TIME_LOCALE = 'es-CO';
const PAYMENT_TIME_TZ = 'America/Bogota';
const CARD_CAPTURE_POLL_MS = 1000;

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

interface DayIncomeMethodBreakdown {
  method: PaymentMethod;
  methodLabel: string;
  total: number;
  dayOnlyTotal?: number;
  lines: { label: string; amount: number }[];
  cashDrawerTotal?: number;
  carryFromHandover?: number;
  cashMovementsSinceHandover?: number;
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
    RouterLink,
    DatePipe,
    CopCurrencyPipe,
    MemberSearchSelectComponent,
    DataTableComponent,
  ],
  templateUrl: './billing.html',
  styleUrls: ['./billing.scss', './billing-monthly.scss'],
})
export class BillingPage implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly accessService = inject(AccessService);
  private readonly billingService = inject(BillingService);
  private readonly billingContext = inject(BillingContextService);
  private readonly memberService = inject(MemberService);
  private readonly planService = inject(PlanService);

  protected readonly paymentMethods = BILLING_PAYMENT_METHODS;
  /** Medios mostrados en el resumen global de ingresos del día. */
  protected readonly incomeSummaryMethods = BILLING_PAYMENT_METHODS.filter((pm) =>
    ['CASH', 'NEQUI', 'BANCOLOMBIA'].includes(pm.value),
  );
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
  /** Administración y super admin: saldos efectivo total, Nequi y Bancolombia. */
  protected readonly canViewTreasury = () => this.auth.isAdmin();
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
  protected readonly otherIncomesDayModalOpen = signal(false);
  protected readonly openingCashInput = signal(0);
  protected readonly accountSettingsModalOpen = signal(false);
  protected readonly savingAccountSettings = signal(false);
  protected readonly nequiInitialInput = signal(0);
  protected readonly bancolombiaInitialInput = signal(0);
  protected readonly paymentAccountSettings = signal<PaymentAccountSettings | null>(null);
  protected readonly digitalIncomesModalOpen = signal(false);
  protected readonly treasuryIncomeDeleteKind = signal<'digital' | 'cash'>('digital');
  protected readonly digitalIncomes = signal<DigitalAccountIncomeLine[]>([]);
  protected readonly loadingDigitalIncomes = signal(false);
  protected readonly deletingDigitalIncomeKey = signal<string | null>(null);
  protected readonly dayExpenses = signal<BillingCashRegisterExpense[]>([]);
  protected readonly dayOtherIncomes = signal<BillingCashRegisterOtherIncome[]>([]);
  protected readonly savingExpense = signal(false);
  protected readonly savingOtherIncome = signal(false);
  protected readonly expenseAmountInput = signal(0);
  protected readonly expensePaymentMethod = signal<PaymentMethod>('CASH');
  protected readonly expenseObservationInput = signal('');
  protected readonly otherIncomeAmountInput = signal(0);
  protected readonly otherIncomePaymentMethod = signal<PaymentMethod>('CASH');
  protected readonly otherIncomeObservationInput = signal('');

  protected readonly dayExpensesTotal = computed(() =>
    this.dayExpenses().reduce((sum, e) => sum + e.amount, 0),
  );

  protected readonly hasDayExpenses = computed(() => {
    const reg = this.activeRegister();
    return this.dayExpenses().length > 0 || (reg?.sessionExpensesTotal ?? 0) > 0;
  });

  protected dayExpensesDisplayTotal(reg: BillingCashRegister): number {
    return roundCop(Math.max(reg.sessionExpensesTotal ?? 0, this.dayExpensesTotal()));
  }

  protected readonly dayOtherIncomesTotal = computed(() =>
    this.dayOtherIncomes().reduce((sum, i) => sum + i.amount, 0),
  );

  protected readonly hasDayOtherIncomes = computed(() => {
    const reg = this.activeRegister();
    return this.dayOtherIncomes().length > 0 || (reg?.dayOtherIncomesTotal ?? 0) > 0;
  });

  protected dayOtherIncomesDisplayTotal(reg: BillingCashRegister): number {
    return roundCop(Math.max(reg.dayOtherIncomesTotal ?? 0, this.dayOtherIncomesTotal()));
  }

  protected readonly dayOtherIncomesByMethod = computed(() => {
    const map: Partial<Record<PaymentMethod, number>> = {};
    for (const row of this.dayOtherIncomes()) {
      map[row.paymentMethod] = roundCop((map[row.paymentMethod] ?? 0) + row.amount);
    }
    return map;
  });
  protected readonly monthlyModalOpen = signal(false);
  protected readonly incomeByConceptModalOpen = signal(false);
  protected readonly summaryBreakdownModal = signal<{
    title: string;
    total: number;
    map: Record<string, number>;
  } | null>(null);
  protected readonly dayIncomeMethodBreakdownModal = signal<DayIncomeMethodBreakdown | null>(null);
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
  protected readonly cardDeviceId = signal('');
  protected readonly cardDeviceLabel = signal('');
  protected readonly lastCapturedCardPin = signal<string | null>(null);
  protected readonly cardCaptureWaiting = signal(false);
  private cardCapturePollTimer: ReturnType<typeof setInterval> | null = null;
  private cardCaptureSinceIso = new Date().toISOString();
  private lastCardCaptureLogId = 0;
  protected readonly membershipPlanId = signal<number | null>(null);
  protected readonly membershipMonthsPaid = signal(1);
  protected readonly membershipPaymentMethod = signal<PaymentMethod>('CASH');
  protected readonly membershipPaymentMode = signal<'full' | 'partial'>('full');
  protected readonly membershipAmountToday = signal<number | null>(null);
  protected readonly membershipUseSplitPayment = signal(false);
  protected readonly membershipSplitMethod1 = signal<PaymentMethod>('CASH');
  protected readonly membershipSplitMethod2 = signal<PaymentMethod>('NEQUI');
  protected readonly membershipSplitAmount1 = signal<number | null>(null);
  protected readonly membershipSplitAmount2 = signal<number | null>(null);
  protected readonly openMembershipObligation = signal<MembershipObligation | null>(null);

  protected readonly membershipPayingDebt = computed(() => this.openMembershipObligation() != null);
  protected readonly roundCop = roundCop;

  protected readonly membershipAmountToCharge = computed(() => {
    if (this.membershipUseSplitPayment()) {
      const splitSum = this.membershipSplitSum();
      if (splitSum > 0) {
        return splitSum;
      }
    }
    const debt = this.openMembershipObligation();
    if (debt) {
      const amount = this.membershipAmountToday();
      return amount != null && amount > 0 ? roundCop(amount) : roundCop(debt.balance);
    }
    const total = this.membershipChargeTotal();
    if (total == null) {
      return null;
    }
    if (this.membershipPaymentMode() === 'full') {
      return total;
    }
    const amount = this.membershipAmountToday();
    return amount != null && amount > 0 ? roundCop(amount) : null;
  });

  protected readonly membershipBalancePreview = computed(() => {
    const debt = this.openMembershipObligation();
    if (debt) {
      const pay = this.membershipAmountToCharge();
      if (pay == null) {
        return debt.balance;
      }
      return Math.max(0, debt.balance - pay);
    }
    const total = this.membershipChargeTotal();
    const pay = this.membershipAmountToCharge();
    if (total == null || pay == null) {
      return null;
    }
    return Math.max(0, total - pay);
  });

  protected readonly membershipSplitSum = computed(() => {
    const a1 = this.membershipSplitAmount1() ?? 0;
    const a2 = this.membershipSplitAmount2() ?? 0;
    return roundCop(a1 + a2);
  });

  /** Meta fija al dividir (pago completo o deuda con monto definido). En abono parcial el total sale de la suma. */
  protected readonly membershipSplitFixedTarget = computed(() => {
    const obligation = this.openMembershipObligation();
    if (obligation && !this.membershipUseSplitPayment()) {
      const amount = this.membershipAmountToday();
      return amount != null && amount > 0 ? roundCop(amount) : roundCop(obligation.balance);
    }
    if (this.membershipPaymentMode() === 'full') {
      return this.membershipChargeTotal();
    }
    if (obligation && this.membershipUseSplitPayment()) {
      const amount = this.membershipAmountToday();
      if (amount != null && amount > 0) {
        return roundCop(amount);
      }
      return roundCop(obligation.balance);
    }
    return null;
  });

  protected readonly membershipSplitRemaining = computed(() => {
    const target = this.membershipSplitFixedTarget();
    if (target == null) {
      return null;
    }
    return roundCop(target - this.membershipSplitSum());
  });

  /** Muestra la casilla «¿Dividir el pago?» sin exigir monto previo. */
  protected readonly canShowSplitPaymentToggle = computed(
    () => this.membershipChargeTotal() != null || this.membershipPayingDebt(),
  );

  protected paymentMethodLabel(method: PaymentMethod): string {
    return this.paymentMethods.find((pm) => pm.value === method)?.label ?? method;
  }

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
    return roundCop(plan.price * months);
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

  protected dayCollectedTotal(reg: BillingCashRegister): number {
    return roundCop(
      (reg.sessionTotal ?? 0) +
        (reg.dayFiadoCollectedTotal ?? 0) +
        (reg.dayOtherIncomesTotal ?? 0),
    );
  }

  /** Total ingresado hoy (facturación + productos + fiado), sin base inicial. */
  protected dayIncomeTotal(reg: BillingCashRegister): number {
    return roundCop(
      this.incomeSummaryMethods.reduce(
        (sum, pm) => sum + this.dayIncomeByMethodAmount(reg, pm.value),
        0,
      ),
    );
  }

  protected dayIncomeByMethodAmount(reg: BillingCashRegister, method: PaymentMethod): number {
    if (reg.dayIncomeByMethod) {
      return this.methodTotal(reg.dayIncomeByMethod, method);
    }
    return roundCop(
      this.methodTotal(reg.sessionIncomeByMethod, method) +
        this.methodTotal(reg.dayFiadoCollectedByMethod, method) +
        this.methodTotal(reg.dayOtherIncomesByMethod ?? {}, method) +
        (method === 'CASH' ? (reg.dayProductSalesCash ?? 0) : 0),
    );
  }

  protected openDayIncomeMethodBreakdown(reg: BillingCashRegister, method: PaymentMethod): void {
    const methodLabel = this.paymentMethodLabel(method);
    const total = this.dayIncomeByMethodAmount(reg, method);
    const dayOnlyTotal = total;
    const cashDrawerTotal = method === 'CASH' ? this.billingCashInDrawer(reg) : undefined;
    const carryFromHandover =
      method === 'CASH' && reg.lastHandoverCashTotal != null ? roundCop(reg.lastHandoverCashTotal) : undefined;
    const cashMovementsSinceHandover =
      method === 'CASH' && reg.cashSinceLastHandover != null
        ? roundCop(reg.cashSinceLastHandover)
        : undefined;

    const billing = roundCop(this.methodTotal(reg.sessionIncomeByMethod, method));
    const membership = method === 'CASH' ? roundCop(reg.sessionCashMembership ?? 0) : 0;
    const dayWorkout = method === 'CASH' ? roundCop(reg.sessionCashDayWorkout ?? 0) : 0;
    const sportsDance = method === 'CASH' ? roundCop(reg.sessionCashSportsDance ?? 0) : 0;
    const products =
      method === 'CASH'
        ? roundCop(reg.dayProductSalesCash ?? 0)
        : roundCop(Math.max(0, total - billing - this.methodTotal(reg.dayFiadoCollectedByMethod, method) - this.methodTotal(reg.dayOtherIncomesByMethod ?? {}, method)));
    const fiado = roundCop(this.methodTotal(reg.dayFiadoCollectedByMethod, method));
    const otherTotal = roundCop(this.methodTotal(reg.dayOtherIncomesByMethod ?? {}, method));
    const surplus = roundCop(this.methodTotal(reg.dayAutoSurplusByMethod ?? {}, method));
    const otherManual = roundCop(Math.max(0, otherTotal - surplus));

    const lines: { label: string; amount: number }[] = [];
    if (method === 'CASH') {
      lines.push({ label: 'Membresías', amount: membership });
      lines.push({ label: 'Entrenos', amount: dayWorkout });
      lines.push({ label: 'Bailes', amount: sportsDance });
    } else if (billing > 0) {
      lines.push({ label: 'Facturación', amount: billing });
    }
    lines.push({ label: 'Productos vendidos', amount: products });
    if (fiado > 0) {
      lines.push({ label: 'Fiado cobrado', amount: fiado });
    }
    lines.push({ label: 'Sobrantes registrados', amount: surplus });
    lines.push({ label: 'Otros ingresos', amount: otherManual });
    if (method === 'CASH' && (carryFromHandover ?? 0) > 0) {
      lines.push({ label: 'Efectivo arrastrado (turno anterior)', amount: carryFromHandover ?? 0 });
    }

    this.dayIncomeMethodBreakdownModal.set({
      method,
      methodLabel,
      total,
      dayOnlyTotal,
      lines,
      cashDrawerTotal,
      carryFromHandover,
      cashMovementsSinceHandover,
    });
  }

  protected closeDayIncomeMethodBreakdown(): void {
    this.dayIncomeMethodBreakdownModal.set(null);
  }

  protected dayMovementCount(reg: BillingCashRegister): number {
    return (
      (reg.paymentCount ?? 0) +
      (reg.dayFiadoPaymentCount ?? 0) +
      (reg.dayOtherIncomeCount ?? 0)
    );
  }

  protected fiadoCashCollected(reg: BillingCashRegister): number {
    return this.methodTotal(reg.dayFiadoCollectedByMethod, 'CASH');
  }

  protected otherIncomeCashCollected(reg: BillingCashRegister): number {
    return this.methodTotal(reg.dayOtherIncomesByMethod ?? {}, 'CASH');
  }

  /** Efectivo físico en caja (usa el total calculado en el servidor). */
  protected billingCashInDrawer(reg: BillingCashRegister): number {
    if (reg.cashInDrawer != null && !Number.isNaN(reg.cashInDrawer)) {
      return roundCop(reg.cashInDrawer);
    }
    const billingCash = this.methodTotal(reg.sessionIncomeByMethod, 'CASH');
    const cashOut = this.methodTotal(reg.sessionExpensesByMethod, 'CASH');
    const productCash = reg.dayProductSalesCash ?? 0;
    const fiadoCash = this.fiadoCashCollected(reg);
    const otherCash = this.otherIncomeCashCollected(reg);
    return roundCop(reg.openingCashAmount + billingCash + productCash + fiadoCash + otherCash - cashOut);
  }

  protected sessionNonCashIncome(reg: BillingCashRegister): number {
    return roundCop(
      Math.max(0, (reg.sessionTotal ?? 0) - this.methodTotal(reg.sessionIncomeByMethod, 'CASH')),
    );
  }

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
    if (!this.canViewTreasury()) {
      return null;
    }
    const reg = this.activeRegister();
    if (!reg) {
      return null;
    }
    return this.billingCashInDrawer(reg);
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
    this.loadPaymentAccountSettings();

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
    if (this.canViewTreasury()) {
      this.loadPaymentAccountSettings();
    }
    this.loadDayExpenses();
    this.loadDayOtherIncomes();
  }

  loadDayOtherIncomes(): void {
    const reg = this.todayRegister();
    const date = reg?.registerDate?.slice(0, 10) ?? todayIsoDate();
    this.billingService.listCashRegisterOtherIncomes(date).subscribe({
      next: (rows) => this.dayOtherIncomes.set(rows),
      error: () => this.dayOtherIncomes.set([]),
    });
  }

  loadDayExpenses(): void {
    const reg = this.todayRegister();
    const date = reg?.registerDate?.slice(0, 10) ?? todayIsoDate();
    this.billingService.listCashRegisterExpenses(date).subscribe({
      next: (rows) => this.dayExpenses.set(rows),
      error: () => this.dayExpenses.set([]),
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
    this.loadDayOtherIncomes();
  }

  openOpenCajaModal(): void {
    const closed = this.closedRegisterToday();
    this.reopeningCaja.set(closed != null);
    this.openingCashInput.set(closed?.openingCashAmount ?? 0);
    this.openCajaModal.set(true);
  }

  loadPaymentAccountSettings(): void {
    if (!this.canViewTreasury()) {
      return;
    }
    this.billingService.getPaymentAccountSettings().subscribe({
      next: (s) => this.paymentAccountSettings.set(s),
      error: () => this.paymentAccountSettings.set(null),
    });
  }

  openAccountSettingsModal(): void {
    this.billingService.getPaymentAccountSettings().subscribe({
      next: (s) => {
        this.nequiInitialInput.set(s.nequiInitialBalance ?? 0);
        this.bancolombiaInitialInput.set(s.bancolombiaInitialBalance ?? 0);
        this.paymentAccountSettings.set(s);
        this.accountSettingsModalOpen.set(true);
      },
      error: (err) => this.message.set(httpErrorMessage(err)),
    });
  }

  closeAccountSettingsModal(): void {
    if (!this.savingAccountSettings()) {
      this.accountSettingsModalOpen.set(false);
    }
  }

  openDigitalIncomesModal(): void {
    this.openTreasuryIncomesModal('digital');
  }

  openCashIncomesModal(): void {
    this.openTreasuryIncomesModal('cash');
  }

  private openTreasuryIncomesModal(kind: 'digital' | 'cash'): void {
    if (!this.isSuperAdmin()) {
      return;
    }
    this.treasuryIncomeDeleteKind.set(kind);
    this.digitalIncomesModalOpen.set(true);
    this.loadTreasuryIncomes();
  }

  closeDigitalIncomesModal(): void {
    if (!this.deletingDigitalIncomeKey()) {
      this.digitalIncomesModalOpen.set(false);
    }
  }

  loadTreasuryIncomes(): void {
    const kind = this.treasuryIncomeDeleteKind();
    this.loadingDigitalIncomes.set(true);
    const request =
      kind === 'cash'
        ? this.billingService.listCashAccountIncomes()
        : this.billingService.listDigitalAccountIncomes();
    request.subscribe({
      next: (rows) => {
        this.digitalIncomes.set(rows);
        this.loadingDigitalIncomes.set(false);
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.digitalIncomes.set([]);
        this.loadingDigitalIncomes.set(false);
      },
    });
  }

  digitalIncomeKey(row: DigitalAccountIncomeLine): string {
    return `${row.source}-${row.id}`;
  }

  deleteDigitalIncome(row: DigitalAccountIncomeLine, event: Event): void {
    event.stopPropagation();
    const isCash = this.treasuryIncomeDeleteKind() === 'cash';
    const label = `${row.sourceLabel} · ${row.description} · ${row.amount}`;
    if (
      !confirm(
        isCash
          ? `¿Eliminar este ingreso en efectivo?\n${label}\nIncluye días anteriores del mes. Se actualizará la caja.`
          : `¿Eliminar este ingreso de ${row.paymentMethodLabel}?\n${label}\nSe actualizarán los saldos de Nequi/Bancolombia.`,
      )
    ) {
      return;
    }
    const key = this.digitalIncomeKey(row);
    this.deletingDigitalIncomeKey.set(key);
    const deleteRequest = isCash
      ? this.billingService.deleteCashAccountIncome(row.source, row.id)
      : this.billingService.deleteDigitalAccountIncome(row.source, row.id);
    deleteRequest.subscribe({
      next: () => {
        this.message.set('Ingreso eliminado');
        this.deletingDigitalIncomeKey.set(null);
        this.loadTreasuryIncomes();
        this.loadPaymentAccountSettings();
        this.refreshCashRegister();
        this.loadToday();
        this.loadDayOtherIncomes();
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.deletingDigitalIncomeKey.set(null);
      },
    });
  }

  saveAccountSettings(): void {
    const nequi = this.nequiInitialInput();
    const bancolombia = this.bancolombiaInitialInput();
    if (nequi < 0 || bancolombia < 0) {
      this.message.set('Los saldos iniciales no pueden ser negativos');
      return;
    }
    this.savingAccountSettings.set(true);
    this.billingService
      .updatePaymentAccountSettings({
        nequiInitialBalance: nequi,
        bancolombiaInitialBalance: bancolombia,
      })
      .subscribe({
        next: () => {
          this.message.set('Saldos iniciales de Nequi y Bancolombia guardados');
          this.savingAccountSettings.set(false);
          this.accountSettingsModalOpen.set(false);
          this.loadPaymentAccountSettings();
        },
        error: (err) => {
          this.message.set(httpErrorMessage(err));
          this.savingAccountSettings.set(false);
        },
      });
  }

  digitalAccount(reg: BillingCashRegister, method: 'NEQUI' | 'BANCOLOMBIA') {
    return reg.digitalAccounts?.find((a) => a.paymentMethod === method) ?? null;
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

  openOtherIncomesDayModal(): void {
    this.loadDayOtherIncomes();
    this.otherIncomeAmountInput.set(0);
    this.otherIncomePaymentMethod.set('CASH');
    this.otherIncomeObservationInput.set('');
    this.otherIncomesDayModalOpen.set(true);
  }

  closeOtherIncomesDayModal(): void {
    if (!this.savingOtherIncome()) {
      this.otherIncomesDayModalOpen.set(false);
    }
  }

  registerOtherIncome(): void {
    if (!this.canBill()) {
      this.message.set('La caja debe estar abierta para registrar otros ingresos');
      return;
    }
    const amount = this.otherIncomeAmountInput();
    const observation = this.otherIncomeObservationInput().trim();
    if (amount <= 0) {
      this.message.set('Indique un valor de ingreso mayor a cero');
      return;
    }
    if (!observation) {
      this.message.set('Escriba el concepto del ingreso');
      return;
    }
    this.savingOtherIncome.set(true);
    this.billingService
      .addCashRegisterOtherIncome({
        amount,
        paymentMethod: this.otherIncomePaymentMethod(),
        observation,
      })
      .subscribe({
        next: () => {
          this.message.set('Otro ingreso registrado');
          this.savingOtherIncome.set(false);
          this.otherIncomeAmountInput.set(0);
          this.otherIncomePaymentMethod.set('CASH');
          this.otherIncomeObservationInput.set('');
          this.refreshCashRegister();
          this.loadDayOtherIncomes();
        },
        error: (err) => {
          this.message.set(httpErrorMessage(err));
          this.savingOtherIncome.set(false);
        },
      });
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
    this.billingService
      .openCashRegister({
        openingCashAmount: amount,
        openingNequiAmount: 0,
        openingBancolombiaAmount: 0,
      })
      .subscribe({
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
    if (!this.auth.isAdmin()) {
      void Swal.fire({
        icon: 'warning',
        title: 'No puede cerrar caja',
        text: 'Solo la administración puede cerrar la caja del día.',
      });
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
    if (this.membershipPaymentMode() === 'full' && !this.membershipPayingDebt()) {
      const total = this.membershipChargeTotal();
      if (total != null) {
        this.membershipAmountToday.set(total);
        this.syncMembershipSplitFromTotal();
      }
    }
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
      this.startCardCapturePolling();
    } else {
      this.stopCardCapturePolling();
      this.cardDeviceId.set('');
      this.cardDeviceLabel.set('');
      this.lastCapturedCardPin.set(null);
    }
  }

  ngOnDestroy(): void {
    this.stopCardCapturePolling();
  }

  private startCardCapturePolling(): void {
    this.stopCardCapturePolling();
    this.cardCaptureSinceIso = new Date().toISOString();
    this.lastCardCaptureLogId = 0;
    this.lastCapturedCardPin.set(null);
    this.cardCaptureWaiting.set(true);
    void this.pollCardRead();
    this.cardCapturePollTimer = setInterval(() => void this.pollCardRead(), CARD_CAPTURE_POLL_MS);
  }

  private stopCardCapturePolling(): void {
    if (this.cardCapturePollTimer) {
      clearInterval(this.cardCapturePollTimer);
      this.cardCapturePollTimer = null;
    }
    this.cardCaptureWaiting.set(false);
  }

  private pollCardRead(): void {
    this.accessService.lastDeviceRead(this.cardCaptureSinceIso).subscribe({
      next: (read) => {
        if (!read?.pin?.trim() || read.logId <= this.lastCardCaptureLogId) {
          return;
        }
        this.lastCardCaptureLogId = read.logId;
        const pin = read.pin.trim();
        this.lastCapturedCardPin.set(pin);
        this.cardDeviceId.set(pin);
        this.cardCaptureWaiting.set(false);
      },
    });
  }

  onMembershipMemberChange(memberId: number | null): void {
    this.membershipMemberId.set(memberId);
    this.openMembershipObligation.set(null);
    if (!Number.isFinite(memberId)) {
      return;
    }
    this.billingService.findOpenMembershipObligation(memberId!).subscribe({
      next: (obligation) => {
        this.openMembershipObligation.set(obligation);
        if (obligation) {
          this.membershipPlanId.set(obligation.planId);
          this.membershipMonthsPaid.set(obligation.monthsPaid);
          this.membershipPaymentMode.set('partial');
          this.membershipAmountToday.set(roundCop(obligation.balance));
        }
      },
    });
  }

  onMembershipPlanChange(value: string | number | null): void {
    this.membershipPlanId.set(value != null ? +value : null);
    if (this.membershipPaymentMode() === 'full' && !this.membershipPayingDebt()) {
      const plan = this.membershipPlans().find((p) => p.id === +(value ?? 0));
      const months = this.membershipMonthsPaid();
      if (plan && months >= 1) {
        this.membershipAmountToday.set(roundCop(plan.price * months));
        this.syncMembershipSplitFromTotal();
      }
    }
  }

  setMembershipPaymentMode(mode: 'full' | 'partial'): void {
    if (this.membershipPayingDebt()) {
      return;
    }
    this.membershipPaymentMode.set(mode);
    const total = this.membershipChargeTotal();
    if (mode === 'full' && total != null) {
      this.membershipAmountToday.set(total);
      this.syncMembershipSplitFromTotal();
    } else if (mode === 'partial') {
      this.membershipAmountToday.set(null);
    }
  }

  setMembershipUseSplitPayment(enabled: boolean): void {
    this.membershipUseSplitPayment.set(enabled);
    if (enabled) {
      this.syncMembershipSplitFromTotal();
    } else {
      this.membershipSplitAmount1.set(null);
      this.membershipSplitAmount2.set(null);
    }
  }

  onMembershipSplitAmount1Change(raw: string | number | null): void {
    const parsed = raw != null && raw !== '' ? roundCop(+raw) : null;
    this.membershipSplitAmount1.set(parsed);
    const target = this.membershipSplitFixedTarget();
    if (target == null || parsed == null) {
      return;
    }
    const rest = roundCop(target - parsed);
    this.membershipSplitAmount2.set(rest > 0 ? rest : null);
  }

  onMembershipSplitAmount2Change(raw: string | number | null): void {
    const parsed = raw != null && raw !== '' ? roundCop(+raw) : null;
    this.membershipSplitAmount2.set(parsed);
    const target = this.membershipSplitFixedTarget();
    if (target == null || parsed == null) {
      return;
    }
    const rest = roundCop(target - parsed);
    this.membershipSplitAmount1.set(rest > 0 ? rest : null);
  }

  onMembershipAmountTodayChange(raw: string | number | null): void {
    const parsed = raw != null && raw !== '' ? roundCop(+raw) : null;
    this.membershipAmountToday.set(parsed);
    if (this.membershipUseSplitPayment() && parsed != null && parsed > 0) {
      this.syncMembershipSplitFromTotal();
    }
  }

  private syncMembershipSplitFromTotal(): void {
    if (!this.membershipUseSplitPayment()) {
      return;
    }
    const target = this.membershipSplitFixedTarget();
    if (target == null) {
      this.membershipSplitAmount1.set(null);
      this.membershipSplitAmount2.set(null);
      return;
    }
    const half = roundCop(Math.floor(target / 2));
    this.membershipSplitAmount1.set(half);
    this.membershipSplitAmount2.set(roundCop(target - half));
  }

  onRegisterNewMember(query: string): void {
    this.setMembershipFlow('new');
    const trimmed = query.trim();
    if (/^\d[\d.\s-]{4,}$/.test(trimmed)) {
      this.newMemberDocumentId.set(trimmed.replace(/\s+/g, ''));
    }
  }

  private resetMembershipForm(): void {
    this.membershipMemberId.set(null);
    this.membershipFlow.set('existing');
    this.newMemberFirstName.set('');
    this.newMemberLastName.set('');
    this.newMemberDocumentId.set('');
    this.newMemberPhone.set('');
    this.newMemberGender.set('');
    this.cardDeviceId.set('');
    this.cardDeviceLabel.set('');
    this.lastCapturedCardPin.set(null);
    this.stopCardCapturePolling();
    this.membershipPlanId.set(null);
    this.membershipMonthsPaid.set(1);
    this.membershipPaymentMethod.set('CASH');
    this.membershipPaymentMode.set('full');
    this.membershipAmountToday.set(null);
    this.membershipUseSplitPayment.set(false);
    this.membershipSplitMethod1.set('CASH');
    this.membershipSplitMethod2.set('NEQUI');
    this.membershipSplitAmount1.set(null);
    this.membershipSplitAmount2.set(null);
    this.openMembershipObligation.set(null);
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
      this.message.set('Entreno del día y bailes deportivos se registran con F2 y F8');
      this.membershipPlanId.set(null);
      return;
    }
    if (monthsPaid < 1) {
      this.message.set('Indica al menos 1 mes a pagar');
      return;
    }

    const useSplit = this.membershipUseSplitPayment() && this.canShowSplitPaymentToggle();
    let paymentSplits: MembershipOnboardingRequest['paymentSplits'] = null;
    if (useSplit) {
      const a1 = this.membershipSplitAmount1();
      const a2 = this.membershipSplitAmount2();
      const m1 = this.membershipSplitMethod1();
      const m2 = this.membershipSplitMethod2();
      if (a1 == null || a2 == null || a1 < 1 || a2 < 1) {
        this.message.set('Indique el monto de cada medio de pago');
        return;
      }
      if (m1 === m2) {
        this.message.set('Los dos medios de pago deben ser distintos');
        return;
      }
      const splitSum = roundCop(a1 + a2);
      const fixedTarget = this.membershipSplitFixedTarget();
      if (fixedTarget != null && splitSum !== fixedTarget) {
        this.message.set(
          `La suma de los dos medios ($${splitSum.toLocaleString('es-CO')}) debe igualar el monto a cobrar ($${fixedTarget.toLocaleString('es-CO')})`,
        );
        return;
      }
      paymentSplits = [
        { paymentMethod: m1, amount: roundCop(a1) },
        { paymentMethod: m2, amount: roundCop(a2) },
      ];
    }

    const amount = roundCop(this.membershipAmountToCharge());
    if (amount <= 0) {
      this.message.set(
        useSplit
          ? 'Indique el monto de cada medio de pago'
          : 'Indique cuánto va a abonar hoy y el medio de pago (pesos, sin decimales)',
      );
      return;
    }

    const debt = this.openMembershipObligation();
    if (debt && amount > roundCop(debt.balance)) {
      this.message.set(`El abono no puede superar el saldo pendiente ($${roundCop(debt.balance).toLocaleString('es-CO')})`);
      return;
    }
    const total = this.membershipChargeTotal();
    if (!debt && total != null && amount > total) {
      this.message.set(`El monto no puede superar el total de la membresía ($${total.toLocaleString('es-CO')})`);
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
    if (flow === 'new') {
      const deviceUserId = this.cardDeviceId().trim();
      if (!deviceUserId) {
        this.message.set('Pase la tarjeta en el lector o escriba el número de tarjeta / Pin ZKTeco');
        return;
      }
      access = {
        kind: 'CARD' as AccessOnboardingKind,
        deviceUserId,
        deviceLabel: this.cardDeviceLabel().trim() || undefined,
      };
    }

    const request: MembershipOnboardingRequest = {
      memberId: flow === 'existing' ? memberId : null,
      newMember: flow === 'new' ? newMember : null,
      planId,
      paymentMethod: useSplit ? this.membershipSplitMethod1() : this.membershipPaymentMethod(),
      monthsPaid,
      amount,
      obligationId: debt?.id ?? null,
      access,
      paymentSplits,
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
          : 'Puede vincular la tarjeta después en Acceso biométrico.';
        const title = res.paymentMessage || (res.balanceRemaining > 0 ? 'Abono registrado' : 'Membresía al día');
        void Swal.fire({
          icon: 'success',
          title,
          html: `<strong>${res.member.firstName} ${res.member.lastName}</strong><br>${res.paymentMessage}<br>${accessNote}`,
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
