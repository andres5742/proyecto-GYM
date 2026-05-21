import { PaymentMethod, Sale, SalesSummary } from './sale.model';

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

export interface ShiftOpenInventoryPreview {
  inventoryCheckRequired: boolean;
  previousShiftId: number | null;
  previousShiftName: string | null;
  previousEmployeeName: string | null;
  products: ProductInventoryLine[];
}

export interface WorkShiftRequest {
  name: string;
  shiftDate?: string;
  employeeId?: number;
  inventoryCounts?: ProductInventoryCountItem[];
}

export interface WorkShiftOpenResult {
  shift: WorkShift;
  inventoryAdjusted: boolean;
  inventoryShortfallRegistered: boolean;
  inventoryShortfallAmount: number;
  inventoryShortfallNotes: string | null;
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
