import { PaymentMethod } from './sale.model';
import { ProductSalesRow, ShiftDetail } from './shift.model';

export interface CashDenomination {
  key: keyof ShiftHandoverCashForm;
  label: string;
  value: number;
  type: 'bill' | 'coin';
}

export interface ShiftHandoverCashForm {
  bill2000: number;
  bill5000: number;
  bill10000: number;
  bill20000: number;
  bill50000: number;
  bill100000: number;
  coin1000: number;
  coin500: number;
  coin200: number;
  coin100: number;
  coin50: number;
}

export interface ShiftHandoverExpenseLine {
  description: string;
  amount: number;
}

export interface ShiftHandoverPriorPaymentLine {
  description: string;
  amount: number;
  paymentMethod: PaymentMethod;
  notes?: string;
}

export interface ShiftHandoverRequest extends ShiftHandoverCashForm {
  workShiftId: number;
  auxAmount: number;
  nequiAmount: number;
  bankAmount: number;
  notes?: string;
  expenses: ShiftHandoverExpenseLine[];
  priorPayments: ShiftHandoverPriorPaymentLine[];
}

export interface ShiftHandoverComparison {
  label: string;
  declared: number;
  expected: number;
  difference: number;
}

export interface ShiftHandover {
  id?: number;
  workShiftId: number;
  workShiftName: string;
  employeeId: number;
  employeeName: string;
  submittedAt?: string;
  bill2000?: number;
  bill5000?: number;
  bill10000?: number;
  bill20000?: number;
  bill50000?: number;
  bill100000?: number;
  coin1000?: number;
  coin500?: number;
  coin200?: number;
  coin100?: number;
  coin50?: number;
  cashCountedTotal: number;
  auxAmount: number;
  nequiAmount: number;
  bankAmount: number;
  expensesTotal: number;
  priorPaymentsTotal: number;
  declaredGrandTotal: number;
  notes?: string;
  expenses: { id?: number; description: string; amount: number }[];
  priorPayments: {
    id?: number;
    description: string;
    amount: number;
    paymentMethod: PaymentMethod;
    paymentMethodLabel: string;
    notes?: string;
  }[];
  shiftDetail: ShiftDetail;
  comparisons: ShiftHandoverComparison[];
}

export const CASH_DENOMINATIONS: CashDenomination[] = [
  { key: 'bill2000', label: '$ 2.000', value: 2000, type: 'bill' },
  { key: 'bill5000', label: '$ 5.000', value: 5000, type: 'bill' },
  { key: 'bill10000', label: '$ 10.000', value: 10000, type: 'bill' },
  { key: 'bill20000', label: '$ 20.000', value: 20000, type: 'bill' },
  { key: 'bill50000', label: '$ 50.000', value: 50000, type: 'bill' },
  { key: 'bill100000', label: '$ 100.000', value: 100000, type: 'bill' },
  { key: 'coin1000', label: '$ 1.000', value: 1000, type: 'coin' },
  { key: 'coin500', label: '$ 500', value: 500, type: 'coin' },
  { key: 'coin200', label: '$ 200', value: 200, type: 'coin' },
  { key: 'coin100', label: '$ 100', value: 100, type: 'coin' },
  { key: 'coin50', label: '$ 50', value: 50, type: 'coin' },
];

export function emptyCashForm(): ShiftHandoverCashForm {
  return {
    bill2000: 0,
    bill5000: 0,
    bill10000: 0,
    bill20000: 0,
    bill50000: 0,
    bill100000: 0,
    coin1000: 0,
    coin500: 0,
    coin200: 0,
    coin100: 0,
    coin50: 0,
  };
}

export function computeCashTotal(cash: ShiftHandoverCashForm): number {
  return CASH_DENOMINATIONS.reduce((sum, d) => sum + (cash[d.key] || 0) * d.value, 0);
}
