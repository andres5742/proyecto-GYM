export type PaymentMethod = 'CASH' | 'NEQUI' | 'BANCOLOMBIA' | 'AUX' | 'PENDING';

export interface Sale {
  id: number;
  workShiftId?: number;
  workShiftName?: string;
  employeeId: number;
  employeeName: string;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  paymentMethod: PaymentMethod;
  paymentMethodLabel: string;
  saleDate: string;
  notes?: string;
  createdAt: string;
}

export interface SaleRequest {
  workShiftId: number;
  employeeId: number;
  productId: number;
  quantity: number;
  paymentMethod: PaymentMethod;
  saleDate?: string;
  notes?: string;
}

export interface BatchSaleLine {
  productId: number;
  paymentMethod: PaymentMethod;
  quantity: number;
  /** Obligatorio si paymentMethod es PENDING: afiliado fiado. */
  memberId?: number;
}

export interface BatchSaleRequest {
  workShiftId: number;
  lines: BatchSaleLine[];
}

export interface SalesSummary {
  totalSales: number;
  totalUnits: number;
  totalAmount: number;
  amountByPaymentMethod: Record<PaymentMethod, number>;
}

export const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Efectivo' },
  { value: 'NEQUI', label: 'Nequi' },
  { value: 'BANCOLOMBIA', label: 'Bancolombia' },
  { value: 'AUX', label: 'Sistema AUX' },
  { value: 'PENDING', label: 'Pendiente / deuda' },
];

/** Medios registrables en ventas (sin AUX). */
export const SALES_PAYMENT_METHODS = PAYMENT_METHODS.filter((pm) => pm.value !== 'AUX');

/** Medios en facturación (membresías y entreno del día; sin pendiente/deuda ni AUX). */
export const BILLING_PAYMENT_METHODS = PAYMENT_METHODS.filter(
  (pm) => pm.value !== 'AUX' && pm.value !== 'PENDING',
);
