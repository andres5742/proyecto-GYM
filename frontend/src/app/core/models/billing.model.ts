import { PaymentMethod } from './sale.model';

export type BillingPaymentType = 'DAY_WORKOUT' | 'MEMBERSHIP';

export interface BillingPayment {
  id: number;
  paymentType: BillingPaymentType;
  paymentTypeLabel: string;
  memberId: number;
  memberName: string;
  planId?: number | null;
  planName?: string | null;
  saleId?: number | null;
  paymentMethod: PaymentMethod;
  paymentMethodLabel: string;
  amount: number;
  paymentDate: string;
  membershipStart?: string | null;
  membershipEnd?: string | null;
  createdAt: string;
}

export interface DayWorkoutRegisterRequest {
  memberId?: number | null;
  paymentMethod: PaymentMethod;
}

export interface DayWorkoutRegisterResponse {
  gateOpened: boolean;
  message: string;
  speechText: string;
  payment: BillingPayment;
}

export interface MembershipPaymentRequest {
  memberId: number;
  planId: number;
  paymentMethod: PaymentMethod;
}

export interface BillingDailySummary {
  date: string;
  dayWorkoutCount: number;
  dayWorkoutTotal: number;
  dayWorkoutByMethod: Record<PaymentMethod, number>;
  membershipCount: number;
  membershipTotal: number;
  membershipByMethod: Record<PaymentMethod, number>;
  grandTotal: number;
}
