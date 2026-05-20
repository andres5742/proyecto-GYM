import { Injectable, signal } from '@angular/core';
import { BillingCashRegister } from '../models/billing.model';
import { PaymentMethod } from '../models/sale.model';

/** Afiliado y medio de pago activos para F2/F3 (pases del día / Facturación). */
@Injectable({ providedIn: 'root' })
export class BillingContextService {
  readonly memberId = signal<number | null>(null);
  readonly paymentMethod = signal<PaymentMethod>('CASH');
  /** Caja de facturación abierta (compartida con F2). */
  readonly openCashRegister = signal<BillingCashRegister | null>(null);
  /** Incrementa tras F2/F3 para refrescar la tabla en Facturación. */
  readonly paymentsRefreshTick = signal(0);

  setOpenCashRegister(register: BillingCashRegister | null): void {
    this.openCashRegister.set(register);
  }

  notifyBillingPaymentRecorded(): void {
    this.paymentsRefreshTick.update((n) => n + 1);
  }

  /** @deprecated Use notifyBillingPaymentRecorded */
  notifyDayWorkoutRecorded(): void {
    this.notifyBillingPaymentRecorded();
  }
}
