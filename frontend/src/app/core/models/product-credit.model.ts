import { PaymentMethod } from './sale.model';

export type ProductCreditStatus = 'OPEN' | 'PAID' | 'CANCELLED';
export type ProductCreditDebtorType = 'MEMBER' | 'STAFF';

export interface ProductCreditPayment {
  id: number;
  creditId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentMethodLabel: string;
  workShiftId: number;
  workShiftName: string;
  employeeId: number;
  employeeName: string;
  paidAt: string;
  notes?: string;
  createdAt: string;
}

export interface ProductCredit {
  id: number;
  debtorType: ProductCreditDebtorType;
  memberId?: number | null;
  debtorEmployeeId?: number | null;
  debtorName: string;
  memberDocumentId?: string | null;
  productId?: number | null;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  balance: number;
  paidAmount: number;
  status: ProductCreditStatus;
  statusLabel: string;
  workShiftId: number;
  workShiftName: string;
  employeeId: number;
  employeeName: string;
  creditedAt: string;
  priorDebt: boolean;
  concept?: string | null;
  notes?: string;
  createdAt: string;
  payments: ProductCreditPayment[];
}

export interface ProductCreditRequest {
  memberId?: number;
  employeeDebtorId?: number;
  productId?: number;
  quantity?: number;
  manualAmount?: number;
  priorDebt?: boolean;
  concept?: string;
  workShiftId?: number;
  notes?: string;
}

export interface ProductCreditPaymentRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  workShiftId?: number;
  notes?: string;
}

export interface ProductCreditPayAllRequest {
  paymentMethod: PaymentMethod;
  workShiftId?: number;
  notes?: string;
}

export interface ProductCreditPayAllResponse {
  creditsPaid: number;
  totalAmount: number;
  payments: ProductCreditPayment[];
}

/** Deuda consolidada por afiliado (varias líneas de fiado en una fila). */
export interface MemberFiadoGroup {
  debtorType: ProductCreditDebtorType;
  memberId?: number | null;
  debtorEmployeeId?: number | null;
  memberName: string;
  memberDocumentId: string | null;
  /** Suma de saldos OPEN del afiliado. */
  openBalance: number;
  /** Cantidad de productos / líneas con deuda abierta. */
  openItemsCount: number;
  credits: ProductCredit[];
  /** Fecha del fiado más reciente (texto ISO para ordenar). */
  lastCreditedAt: string;
}

/** Medios para abonar fiado (sin pendiente ni AUX). */
export const FIADO_PAYMENT_METHODS = [
  { value: 'CASH' as PaymentMethod, label: 'Efectivo' },
  { value: 'NEQUI' as PaymentMethod, label: 'Nequi' },
  { value: 'BANCOLOMBIA' as PaymentMethod, label: 'Bancolombia' },
];
