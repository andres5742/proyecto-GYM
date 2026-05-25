import { PaymentMethod, Sale, SalesSummary } from './sale.model';
import { ShiftHandoverCashForm } from './shift-handover.model';

export type ShiftStatus = 'OPEN' | 'CLOSED';

export interface WorkShift {
  id: number;
  shiftDate: string;
  name: string;
  employeeId?: number;
  employeeName?: string;
  openedAt: string;
  closedAt?: string;
  status: ShiftStatus;
  totalAmount: number;
  totalSales: number;
  createdAt: string;
}

export interface ProductInventoryCountItem {
  productId: number;
  countedQuantity: number;
}

export interface ProductInventoryLine {
  productId: number;
  productName: string;
  category: string;
  expectedQuantity: number;
  unitPrice: number;
}

export interface ShiftOpenCashPreview {
  cashRegisterId: number;
  cashRegisterOpenedByName: string;
  openingCashAmount: number;
  cashExpenses: number;
  productCashTotal: number;
  /** Efectivo contado en la última entrega de turno del día. */
  lastHandoverCashTotal: number;
  /** Movimientos en efectivo en Facturación después de esa entrega. */
  cashSinceLastHandover: number;
  /** Ventas en efectivo de turnos cerrados hoy (igual que entrega de turno). */
  closedShiftsCashNet: number;
  fiadoCashTotal: number;
  cashMembership: number;
  cashDayWorkout: number;
  cashSportsDance: number;
  otherIncomesCash: number;
  /** Total según sistema (antes de descontar faltantes ya registrados). */
  systemCashTotal: number;
  /** Faltantes de caja ya cargados en el día (entrega, cierre, etc.). */
  cashShortfallsDeducted: number;
  expectedCashTotal: number;
}

export interface ShiftOpenInventoryPreview {
  inventoryCheckRequired: boolean;
  previousShiftId: number | null;
  previousShiftName: string | null;
  previousEmployeeName: string | null;
  products: ProductInventoryLine[];
  cash: ShiftOpenCashPreview | null;
}

export interface WorkShiftRequest {
  name: string;
  shiftDate?: string;
  employeeId?: number;
  inventoryCounts?: ProductInventoryCountItem[];
  cashCount?: ShiftHandoverCashForm;
}

export interface WorkShiftOpenResult {
  shift: WorkShift;
  inventoryAdjusted: boolean;
  inventoryShortfallRegistered: boolean;
  inventoryShortfallAmount: number;
  inventoryShortfallNotes: string | null;
  cashShortfallRegistered: boolean;
  cashShortfallAmount: number;
  cashShortfallNotes: string | null;
  cashSurplusRegistered: boolean;
  cashSurplusAmount: number;
  cashSurplusBillingObservation: string | null;
}

export interface PaymentMethodTotals {
  quantity: number;
  amount: number;
}

export interface ProductSalesRow {
  productId: number;
  productName: string;
  unitPrice: number;
  byPaymentMethod: Partial<Record<PaymentMethod, PaymentMethodTotals>>;
  totalQuantity: number;
  totalAmount: number;
}

export interface ShiftDetail {
  shift: WorkShift;
  summary: SalesSummary;
  productRows: ProductSalesRow[];
  sales: Sale[];
}
