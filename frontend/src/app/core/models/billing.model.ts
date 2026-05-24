import { Member } from './member.model';
import { PaymentMethod } from './sale.model';

export type BillingPaymentType = 'DAY_WORKOUT' | 'SPORTS_DANCE' | 'MEMBERSHIP';

export type MembershipPaymentKind = 'FULL' | 'PARTIAL';

export interface MembershipObligation {
  id: number;
  memberId: number;
  memberName: string;
  planId: number;
  planName: string;
  monthsPaid: number;
  totalAmount: number;
  amountPaid: number;
  balance: number;
  status: 'OPEN' | 'PAID';
  plannedMembershipStart: string;
  plannedMembershipEnd: string;
}

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
  membershipPaymentKind?: MembershipPaymentKind | null;
  membershipPaymentKindLabel?: string | null;
  recordedByEmployeeId?: number | null;
  recordedByEmployeeName: string;
  createdAt: string;
}

export interface DayWorkoutRegisterRequest {
  memberId?: number | null;
  paymentMethod: PaymentMethod;
  paymentSplits?: PaymentSplitLine[] | null;
}

export interface DayWorkoutRegisterResponse {
  gateOpened: boolean;
  message: string;
  speechText: string;
  payment: BillingPayment;
}

export interface PaymentSplitLine {
  paymentMethod: PaymentMethod;
  amount: number;
}

export interface MembershipPaymentRequest {
  memberId: number;
  planId: number;
  paymentMethod: PaymentMethod;
  monthsPaid: number;
  amount: number;
  obligationId?: number | null;
  paymentSplits?: PaymentSplitLine[] | null;
}

export interface MembershipPaymentOutcome {
  payment: BillingPayment;
  obligation: MembershipObligation | null;
  membershipActivated: boolean;
  balanceRemaining: number; // pesos enteros
  message: string;
}

export type AccessOnboardingKind = 'FINGERPRINT' | 'FACE' | 'CARD';

export interface NewMemberOnboardingData {
  firstName: string;
  lastName: string;
  documentId: string;
  phone?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER' | null;
}

export interface AccessOnboardingData {
  kind: AccessOnboardingKind;
  deviceUserId?: string;
  deviceLabel?: string;
  faceDescriptor?: number[];
}

export interface MembershipOnboardingRequest {
  memberId?: number | null;
  newMember?: NewMemberOnboardingData | null;
  planId: number;
  paymentMethod: PaymentMethod;
  monthsPaid: number;
  amount: number;
  obligationId?: number | null;
  access?: AccessOnboardingData | null;
  paymentSplits?: PaymentSplitLine[] | null;
}

export interface MembershipOnboardingResponse {
  member: Member;
  payment: BillingPayment;
  openObligation: MembershipObligation | null;
  membershipActivated: boolean;
  balanceRemaining: number; // pesos enteros
  paymentMessage: string;
  accessRegistered: boolean;
  accessMessage: string;
}

export interface BillingDailySummary {
  date: string;
  dayWorkoutCount: number;
  dayWorkoutTotal: number;
  dayWorkoutByMethod: Record<PaymentMethod, number>;
  sportsDanceCount: number;
  sportsDanceTotal: number;
  sportsDanceByMethod: Record<PaymentMethod, number>;
  membershipCount: number;
  membershipTotal: number;
  membershipByMethod: Record<PaymentMethod, number>;
  incomeByMethod: Record<PaymentMethod, number>;
  expenseCount: number;
  expensesTotal: number;
  expensesByMethod: Record<PaymentMethod, number>;
  grandTotal: number;
}

export type BillingCashRegisterStatus = 'OPEN' | 'CLOSED';

export interface BillingCashRegister {
  id: number;
  registerDate: string;
  openedByEmployeeId: number;
  openedByEmployeeName: string;
  openingCashAmount: number;
  openingNequiAmount: number;
  openingBancolombiaAmount: number;
  openedAt: string;
  closedAt?: string | null;
  status: BillingCashRegisterStatus;
  sessionTotal: number;
  sessionIncomeByMethod: Record<PaymentMethod, number>;
  paymentCount: number;
  sessionExpensesTotal: number;
  sessionExpensesByMethod: Record<PaymentMethod, number>;
  expenseCount: number;
  dayProductSalesTotal: number;
  dayProductSalesCount: number;
  dayProductSalesShiftCount: number;
  dayProductUnitsSold: number;
  dayProductSalesCash: number;
  sessionCashMembership: number;
  sessionCashDayWorkout: number;
  sessionCashSportsDance: number;
  /** Abonos a productos fiados cobrados hoy (todos los medios). */
  dayFiadoCollectedTotal: number;
  dayFiadoCollectedByMethod: Record<PaymentMethod, number>;
  dayFiadoPaymentCount: number;
  dayOtherIncomesTotal: number;
  dayOtherIncomesByMethod: Record<PaymentMethod, number>;
  dayOtherIncomeCount: number;
  /** Facturación + productos + fiado + otros ingresos del día, por medio (sin base inicial). */
  dayIncomeByMethod?: Record<PaymentMethod, number>;
  dayIncomeTotal?: number;
  /** Inicio + facturación efectivo + productos efectivo + fiado efectivo − gastos. */
  cashInDrawer: number;
  digitalAccounts?: DigitalAccountBalanceLine[];
}

export interface DigitalAccountBalanceLine {
  paymentMethod: PaymentMethod;
  paymentMethodLabel: string;
  openingBalance: number;
  incomeTotal: number;
  expenseTotal: number;
  closingBalance: number;
  cumulativeBalance: number;
}

export interface PaymentAccountSettings {
  nequiInitialBalance: number;
  bancolombiaInitialBalance: number;
}

export interface BillingCashRegisterExpense {
  id: number;
  cashRegisterId: number;
  registerDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentMethodLabel: string;
  observation: string;
  recordedByEmployeeId: number;
  recordedByEmployeeName: string;
  createdAt: string;
}

export interface BillingCashRegisterExpenseRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  observation: string;
}

export interface BillingCashRegisterOtherIncome {
  id: number;
  cashRegisterId: number;
  registerDate: string;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentMethodLabel: string;
  observation: string;
  recordedByEmployeeId: number;
  recordedByEmployeeName: string;
  createdAt: string;
}

export interface BillingCashRegisterOtherIncomeRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  observation: string;
}

export interface OpenBillingCashRegisterRequest {
  openingCashAmount: number;
  openingNequiAmount: number;
  openingBancolombiaAmount: number;
}

export interface BillingMonthlySummary {
  year: number;
  month: number;
  totalPayments: number;
  grandTotal: number;
  totalExpenses: number;
  expenseCount: number;
  byMethod: Record<PaymentMethod, number>;
  dayWorkoutByMethod: Record<PaymentMethod, number>;
  sportsDanceByMethod: Record<PaymentMethod, number>;
  membershipByMethod: Record<PaymentMethod, number>;
  expensesByMethod: Record<PaymentMethod, number>;
}
