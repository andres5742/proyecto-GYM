import { NgStyle } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { ReportPeriodMode, ReportView } from '../../core/models/report.model';
import {
  BILLING_PAYMENT_METHODS,
  PAYMENT_METHODS,
  PaymentMethod,
} from '../../core/models/sale.model';
import { ReportService } from '../../core/services/report.service';
import { httpErrorMessage } from '../../core/utils/http-error-message';
import { toDailyReportView, toMonthlyReportView } from '../../core/utils/report-view';
import { todayIsoDate } from '../../core/utils/today-date';
import { roundCop } from '../../core/utils/money';

interface ChartBar {
  key: PaymentMethod;
  label: string;
  amount: number;
  percent: number;
  color: string;
}

interface MethodGainRow {
  key: PaymentMethod;
  label: string;
  income: number;
  expense: number;
  gain: number;
}

const METHOD_COLORS: Record<PaymentMethod, string> = {
  CASH: '#3d7fb8',
  NEQUI: '#3d8f58',
  BANCOLOMBIA: '#6d5cad',
  AUX: '#6d737e',
  PENDING: '#d4922e',
};

const MONTH_OPTIONS = [
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
] as const;

@Component({
  selector: 'app-reports',
  imports: [FormsModule, CopCurrencyPipe, NgStyle],
  templateUrl: './reports.html',
  styleUrl: './reports.scss',
})
export class ReportsPage implements OnInit {
  private readonly reportService = inject(ReportService);

  protected readonly viewMode = signal<ReportPeriodMode>('daily');
  protected readonly selectedDate = signal(todayIsoDate());
  protected readonly selectedYear = signal(new Date().getFullYear());
  protected readonly selectedMonth = signal(new Date().getMonth() + 1);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly reportView = signal<ReportView | null>(null);

  protected readonly paymentMethods = BILLING_PAYMENT_METHODS;
  protected readonly monthOptions = MONTH_OPTIONS;
  protected readonly yearOptions = computed(() => {
    const current = new Date().getFullYear();
    return [current - 1, current, current + 1];
  });

  protected readonly incomeBars = computed(() => {
    const r = this.reportView();
    if (!r) {
      return [] as ChartBar[];
    }
    return this.buildBars(r.totalIncomeByMethod, r.totalIncome);
  });

  protected readonly billingBars = computed(() => {
    const r = this.reportView();
    if (!r) {
      return [] as ChartBar[];
    }
    return this.buildBars(r.billingIncomeByMethod, r.billingIncomeTotal);
  });

  protected readonly productBars = computed(() => {
    const r = this.reportView();
    if (!r) {
      return [] as ChartBar[];
    }
    return this.buildBars(r.productSales.amountByMethod, r.productSales.totalAmount);
  });

  protected readonly expenseBars = computed(() => {
    const r = this.reportView();
    if (!r) {
      return [] as ChartBar[];
    }
    return this.buildBars(r.expensesByMethod, r.expensesTotal);
  });

  protected readonly methodGains = computed(() => {
    const r = this.reportView();
    if (!r) {
      return [] as MethodGainRow[];
    }
    const targetMethods: PaymentMethod[] = ['CASH', 'NEQUI', 'BANCOLOMBIA'];
    return targetMethods.map((method) => {
      const income = roundCop(r.totalIncomeByMethod[method] ?? 0);
      const expense = roundCop(r.expensesByMethod[method] ?? 0);
      const gain = roundCop(income - expense);
      return {
        key: method,
        label: this.paymentMethodLabel(method),
        income,
        expense,
        gain,
      };
    });
  });

  protected readonly fiadoBars = computed(() => {
    const r = this.reportView();
    if (!r || r.fiadoCollectedTotal <= 0) {
      return [] as ChartBar[];
    }
    return this.buildBars(r.fiadoCollectedByMethod, r.fiadoCollectedTotal);
  });

  protected readonly lowStockCount = computed(() => {
    const r = this.reportView();
    return r ? r.inventory.filter((line) => line.lowStock).length : 0;
  });

  protected readonly billingTypeMethods = BILLING_PAYMENT_METHODS;

  protected readonly incomeDonutStyle = computed(() => {
    const bars = this.incomeBars();
    if (bars.length === 0) {
      return { background: 'conic-gradient(var(--color-surface-muted) 0 100%)' };
    }
    let acc = 0;
    const stops: string[] = [];
    for (const bar of bars) {
      const start = acc;
      acc += bar.percent;
      stops.push(`${bar.color} ${start}% ${acc}%`);
    }
    return { background: `conic-gradient(${stops.join(', ')})` };
  });

  ngOnInit(): void {
    this.load();
  }

  setViewMode(mode: ReportPeriodMode): void {
    if (this.viewMode() === mode) {
      return;
    }
    this.viewMode.set(mode);
    this.reportView.set(null);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    if (this.viewMode() === 'daily') {
      this.reportService.daily(this.selectedDate()).subscribe({
        next: (data) => {
          this.reportView.set(toDailyReportView(data));
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(httpErrorMessage(err, 'No se pudo cargar el reporte del día'));
          this.loading.set(false);
        },
      });
      return;
    }
    this.reportService.monthly(this.selectedYear(), this.selectedMonth()).subscribe({
      next: (data) => {
        this.reportView.set(toMonthlyReportView(data));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'No se pudo cargar el reporte mensual'));
        this.loading.set(false);
      },
    });
  }

  onDateChange(value: string): void {
    this.selectedDate.set(value);
    this.load();
  }

  onMonthChange(value: number): void {
    this.selectedMonth.set(Number(value));
    this.load();
  }

  onYearChange(value: number): void {
    this.selectedYear.set(Number(value));
    this.load();
  }

  printReport(): void {
    this.runPrint('Imprimir reporte');
  }

  downloadPdf(): void {
    this.runPrint('Guardar como PDF');
  }

  paymentMethodLabel(method: PaymentMethod): string {
    return PAYMENT_METHODS.find((pm) => pm.value === method)?.label ?? method;
  }

  paymentBadgeClass(method: PaymentMethod): string {
    return `pay-badge pay-badge--${method.toLowerCase()}`;
  }

  gainClass(value: number): string {
    return value >= 0 ? 'pos' : 'neg';
  }

  methodAmount(totals: Partial<Record<PaymentMethod, number>>, method: PaymentMethod): number {
    return roundCop(totals[method] ?? 0);
  }

  downloadCsv(): void {
    const r = this.reportView();
    if (!r) {
      return;
    }
    const lines: string[] = [
      r.title,
      `Ingresos totales,${r.totalIncome}`,
      `Gastos,${r.expensesTotal}`,
      `${r.resultLabel},${r.netResult}`,
      '',
      'Ingresos por medio de pago',
      'Medio,Monto',
    ];
    for (const bar of this.incomeBars()) {
      lines.push(`${bar.label},${bar.amount}`);
    }
    if (r.digitalAccounts?.length) {
      lines.push(
        '',
        'Cuentas digitales',
        'Cuenta,Apertura,Ingresos,Gastos,Saldo cierre,Acumulado',
      );
      for (const acc of r.digitalAccounts) {
        lines.push(
          `${acc.paymentMethodLabel},${acc.openingBalance},${acc.incomeTotal},${acc.expenseTotal},${acc.closingBalance},${acc.cumulativeBalance}`,
        );
      }
    }
    const b = r.breakdown;
    lines.push(
      '',
      'Facturación por tipo',
      'Tipo,Pagos,Total',
      `Entreno del día,${b.dayWorkout.paymentCount},${b.dayWorkout.totalAmount}`,
      `Bailes deportivos,${b.sportsDance.paymentCount},${b.sportsDance.totalAmount}`,
      `Membresías,${b.membership.paymentCount},${b.membership.totalAmount}`,
      `Otros ingresos,${b.otherIncomes.paymentCount},${b.otherIncomes.totalAmount}`,
      '',
      'Otros ingresos por medio de pago',
      'Medio,Monto',
    );
    for (const bar of this.buildBars(b.otherIncomes.amountByMethod, b.otherIncomes.totalAmount)) {
      lines.push(`${bar.label},${bar.amount}`);
    }
    lines.push(
      '',
      'Membresías por plan',
      'Plan,Pagos,Total,Efectivo,Nequi,Bancolombia',
    );
    for (const plan of b.membershipByPlan) {
      lines.push(
        `"${plan.planName.replace(/"/g, '""')}",${plan.paymentCount},${plan.totalAmount},${this.methodAmount(plan.amountByMethod, 'CASH')},${this.methodAmount(plan.amountByMethod, 'NEQUI')},${this.methodAmount(plan.amountByMethod, 'BANCOLOMBIA')}`,
      );
    }
    lines.push('', 'Ventas por producto y medio de pago', 'Producto,Medio,Unidades,Monto');
    for (const row of b.productSalesByPayment) {
      lines.push(
        `"${row.productName.replace(/"/g, '""')}",${this.paymentMethodLabel(row.paymentMethod)},${row.units},${row.amount}`,
      );
    }
    if (r.fiadoCollectedTotal > 0) {
      lines.push('', 'Abonos de fiado por medio de pago', 'Medio,Monto');
      for (const bar of this.fiadoBars()) {
        lines.push(`${bar.label},${bar.amount}`);
      }
      lines.push(`Total fiado,${r.fiadoCollectedTotal}`);
    }
    lines.push(
      '',
      'Inventario',
      `Producto,Categoría,Stock,Mínimo,${r.soldColumnLabel},${r.salesColumnLabel}`,
    );
    for (const line of r.inventory) {
      const cat = line.category ?? '';
      lines.push(
        `"${line.name.replace(/"/g, '""')}",${cat},${line.quantityInStock},${line.minStock},${line.unitsSoldToday},${line.salesAmountToday}`,
      );
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${r.exportBaseName}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  private runPrint(documentTitle: string): void {
    const r = this.reportView();
    if (!r) {
      return;
    }
    const previousTitle = document.title;
    document.title = `${r.exportBaseName} — Sport Gym`;
    window.print();
    window.setTimeout(() => {
      document.title = previousTitle || documentTitle;
    }, 300);
  }

  private buildBars(totals: Partial<Record<PaymentMethod, number>>, maxTotal: number): ChartBar[] {
    const total = maxTotal > 0 ? maxTotal : 0;
    return this.paymentMethods
      .map((pm) => {
        const amount = roundCop(totals[pm.value] ?? 0);
        const percent = total > 0 ? Math.round((amount / total) * 100) : 0;
        return {
          key: pm.value,
          label: pm.label,
          amount,
          percent,
          color: METHOD_COLORS[pm.value],
        };
      })
      .filter((bar) => bar.amount > 0);
  }
}
