export type PaymentMethod = 'CASH' | 'NEQUI' | 'BANCOLOMBIA' | 'PENDING';

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
  { value: 'PENDING', label: 'Pendiente de pago' },
];
