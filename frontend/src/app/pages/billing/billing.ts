import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { MemberSearchSelectComponent } from '../../components/member-search-select/member-search-select';
import { BillingPayment, BillingDailySummary } from '../../core/models/billing.model';
import { Member } from '../../core/models/member.model';
import { MembershipPlan } from '../../core/models/plan.model';
import { SALES_PAYMENT_METHODS, PaymentMethod } from '../../core/models/sale.model';
import { AuthService } from '../../core/services/auth.service';
import { BillingContextService } from '../../core/services/billing-context.service';
import { BillingService } from '../../core/services/billing.service';
import { MemberService } from '../../core/services/member.service';
import { PlanService } from '../../core/services/plan.service';
import { httpErrorMessage } from '../../core/utils/http-error-message';

@Component({
  selector: 'app-billing',
  imports: [FormsModule, DatePipe, CopCurrencyPipe, MemberSearchSelectComponent],
  templateUrl: './billing.html',
  styleUrl: './billing.scss',
})
export class BillingPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly billingService = inject(BillingService);
  private readonly billingContext = inject(BillingContextService);
  private readonly memberService = inject(MemberService);
  private readonly planService = inject(PlanService);

  protected readonly paymentMethods = SALES_PAYMENT_METHODS;
  protected readonly message = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly members = signal<Member[]>([]);
  protected readonly plans = signal<MembershipPlan[]>([]);
  protected readonly payments = signal<BillingPayment[]>([]);
  protected readonly summary = signal<BillingDailySummary | null>(null);
  protected readonly section = signal<'membership' | 'summary'>('summary');
  protected readonly isSuperAdmin = () => this.auth.isSuperAdmin();
  protected readonly deletingPaymentId = signal<number | null>(null);

  protected readonly membershipMemberId = signal<number | null>(null);
  protected readonly membershipPlanId = signal<number | null>(null);
  protected readonly membershipPaymentMethod = signal<PaymentMethod>('CASH');

  protected readonly membershipPlans = computed(() =>
    this.plans().filter((p) => p.durationDays > 1 && p.active),
  );

  constructor() {
    effect(() => {
      if (this.billingContext.paymentsRefreshTick() === 0) {
        return;
      }
      this.section.set('summary');
      this.loadToday();
    });
  }

  ngOnInit(): void {
    this.memberService.findAll().subscribe({
      next: (m) => this.members.set(m),
    });
    this.planService.findAll().subscribe({
      next: (p) => this.plans.set(p),
    });
    this.loadToday();

    const ctxMember = this.billingContext.memberId();
    if (ctxMember != null) {
      this.membershipMemberId.set(ctxMember);
    }
  }

  loadToday(): void {
    this.billingService.dailySummary().subscribe({
      next: (s) => this.summary.set(s),
    });
    this.billingService.listPayments().subscribe({
      next: (p) => this.payments.set(p),
    });
  }

  registerMembership(): void {
    const memberId = this.membershipMemberId();
    const planId = this.membershipPlanId();
    if (memberId == null || planId == null) {
      this.message.set('Selecciona afiliado y plan de membresía');
      return;
    }
    this.saving.set(true);
    this.billingService
      .registerMembershipPayment({
        memberId,
        planId,
        paymentMethod: this.membershipPaymentMethod(),
      })
      .subscribe({
        next: () => {
          this.message.set('Membresía activada y pago registrado');
          this.saving.set(false);
          this.loadToday();
        },
        error: (err) => {
          this.message.set(httpErrorMessage(err));
          this.saving.set(false);
        },
      });
  }

  methodTotal(map: Record<string, number> | undefined, key: string): number {
    return map?.[key as PaymentMethod] ?? 0;
  }

  deletePayment(row: BillingPayment, event: Event): void {
    event.stopPropagation();
    const label = `${row.paymentTypeLabel} · ${row.memberName} · ${row.amount}`;
    if (
      !confirm(
        `¿Eliminar este movimiento de hoy?\n${label}\nEsta acción no se puede deshacer.`,
      )
    ) {
      return;
    }
    this.deletingPaymentId.set(row.id);
    this.billingService.deletePayment(row.id).subscribe({
      next: () => {
        this.message.set('Movimiento eliminado');
        this.deletingPaymentId.set(null);
        this.loadToday();
      },
      error: (err) => {
        this.message.set(httpErrorMessage(err));
        this.deletingPaymentId.set(null);
      },
    });
  }
}
