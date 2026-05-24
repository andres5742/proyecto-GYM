import { PaymentMethod } from './sale.model';

export type PaymentMethodTotals = Partial<Record<PaymentMethod, number>>;
export type ReportPeriodMode = 'daily' | 'monthly';

export interface ProductInventoryReportLine {
  productId: number;
  name: string;
  category: string | null;
  quantityInStock: number;
  minStock: number;
  lowStock: boolean;
  unitsSoldToday: number;
  salesAmountToday: number;
}

export interface ProductSalesReportSection {
  saleCount: number;
  unitsSold: number;
  totalAmount: number;
  amountByMethod: PaymentMethodTotals;
}

export interface BillingTypeReportSection {
  paymentCount: number;
  totalAmount: number;
  amountByMethod: PaymentMethodTotals;
}

export interface MembershipPlanReportLine {
  planId: number | null;
  planName: string;
  paymentCount: number;
  totalAmount: number;
  amountByMethod: PaymentMethodTotals;
}

export interface ProductSaleByPaymentLine {
  productId: number;
  productName: string;
  paymentMethod: PaymentMethod;
  units: number;
  amount: number;
}

export interface BusinessReportBreakdown {
  dayWorkout: BillingTypeReportSection;
  sportsDance: BillingTypeReportSection;
  membership: BillingTypeReportSection;
  otherIncomes: BillingTypeReportSection;
  membershipByPlan: MembershipPlanReportLine[];
  productSalesByPayment: ProductSaleByPaymentLine[];
}

export interface BusinessReportCore {
  billingPaymentCount: number;
  billingIncomeTotal: number;
  billingIncomeByMethod: PaymentMethodTotals;
  productSales: ProductSalesReportSection;
  fiadoCollectedTotal: number;
  fiadoCollectedByMethod: PaymentMethodTotals;
  expenseCount: number;
  expensesTotal: number;
  expensesByMethod: PaymentMethodTotals;
  totalIncome: number;
  totalIncomeByMethod: PaymentMethodTotals;
  netResult: number;
  inventory: ProductInventoryReportLine[];
  breakdown: BusinessReportBreakdown;
}

export interface DailyBusinessReport extends BusinessReportCore {
  date: string;
  cashRegisterStatus: string;
  openingCashAmount: number;
}

export interface MonthlyBusinessReport extends BusinessReportCore {
  year: number;
  month: number;
  startDate: string;
  endDate: string;
  cashRegisterDays: number;
}

export interface ReportView extends BusinessReportCore {
  period: ReportPeriodMode;
  title: string;
  printTitle: string;
  incomeLabel: string;
  resultLabel: string;
  inventoryTitle: string;
  inventoryHint: string;
  soldColumnLabel: string;
  salesColumnLabel: string;
  cajaLabel: string;
  cajaMeta: string;
  openingCashAmount?: number;
  emptyIncomeHint: string;
  exportBaseName: string;
}
