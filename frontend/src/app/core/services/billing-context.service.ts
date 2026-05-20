import { Injectable, signal } from '@angular/core';
import { PaymentMethod } from '../models/sale.model';

/** Afiliado y medio de pago activos para F2 (entreno del día / Facturación). */
@Injectable({ providedIn: 'root' })
export class BillingContextService {
  readonly memberId = signal<number | null>(null);
  readonly paymentMethod = signal<PaymentMethod>('CASH');
  /** Incrementa tras F2 para refrescar la tabla en Facturación. */
  readonly paymentsRefreshTick = signal(0);

  notifyDayWorkoutRecorded(): void {
    this.paymentsRefreshTick.update((n) => n + 1);
  }
}
