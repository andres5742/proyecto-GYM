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

export interface WorkShiftRequest {
  name: string;
  shiftDate?: string;
  employeeId?: number;
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
