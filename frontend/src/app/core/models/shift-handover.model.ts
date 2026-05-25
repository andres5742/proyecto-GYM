import { PaymentMethod } from './sale.model';
import { ProductInventoryCountItem } from './billing-close.model';
import { ProductInventoryLine, ProductSalesRow, ShiftDetail } from './shift.model';

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

export interface ShiftHandoverPriorPaymentLine {
  description: string;
  amount: number;
  paymentMethod: PaymentMethod;
  notes?: string;
}

export interface ShiftHandoverRequest extends ShiftHandoverCashForm {
  workShiftId: number;
  notes?: string;
  /** Siempre vacío; los gastos del turno ya no se registran aquí. */
  expenses?: { description: string; amount: number }[];
  priorPayments: ShiftHandoverPriorPaymentLine[];
  inventoryCounts?: ProductInventoryCountItem[];
}

export interface HandoverDeliveredProductLine {
  productId: number;
  productName: string;
  category: string;
  /** Stock en sistema antes del conteo (registros nuevos). */
  expectedInSystem?: number | null;
  /** Unidades que quedaron en bodega al entregar. */
  stockRemaining: number;
  /** Registros antiguos solo tenían este campo. */
  countedQuantity?: number;
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
  shiftDate?: string;
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
  /** Facturación del día: apertura + cobros efectivo − gastos + otros ingresos en efectivo. */
  billingCashExpected: number;
  /** Apertura + facturación en efectivo − gastos (sin otros ingresos). */
  billingCashBase: number;
  /** Otros ingresos del día en efectivo (facturación). */
  billingOtherIncomesCash: number;
  /** Ventas en efectivo de turnos anteriores del día, ya descontados los faltantes registrados. */
  previousShiftSalesCash: number;
  /** Faltantes ya cargados a otro empleado en turnos anteriores (no se exigen otra vez). */
  previousShiftShortfallsDeducted: number;
  /** Nombres de turnos separados por coma, ej. «Mañana, Tarde». */
  previousShiftName?: string | null;
  /** Ventas en efectivo del turno que se entrega. */
  salesCashExpected: number;
  /** Cobros de fiado en efectivo de turnos anteriores del día. */
  previousShiftCreditPaymentsCash: number;
  /** Cobros de fiado en efectivo del turno de entrega. */
  creditPaymentsCashExpected: number;
  /** Igual que «Efectivo en caja» en Facturación. */
  expectedCashTotal: number;
  /** Desglose Facturación: efectivo entregado en turno anterior. */
  lastHandoverCashTotal?: number | null;
  /** Desglose Facturación: movimientos en caja desde la última entrega. */
  cashSinceLastHandover?: number | null;
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
  inventoryProducts?: ProductInventoryLine[];
  inventoryUnitsDelivered?: number;
  inventoryProductKindsDelivered?: number;
  deliveredInventory?: HandoverDeliveredProductLine[];
  pendingInventoryShortfallTotal?: number;
  comparisons: ShiftHandoverComparison[];
  registeredShortfallAmount?: number | null;
  cashShortfallId?: number | null;
  /** true si el sobrante de efectivo coincidió con inventario faltante y se ajustó stock */
  inventorySurplusResolved?: boolean;
  inventorySurplusResolutionNote?: string | null;
  cashSurplusRegistered?: boolean;
  registeredSurplusAmount?: number | null;
  cashSurplusOtherIncomeId?: number | null;
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
