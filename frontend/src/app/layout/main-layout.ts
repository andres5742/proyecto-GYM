import { Component, computed, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { ModuleService } from '../core/services/module.service';
import { BillingContextService } from '../core/services/billing-context.service';
import { BillingService } from '../core/services/billing.service';
import { PlanService } from '../core/services/plan.service';
import { AccessService } from '../core/services/access.service';
import { BILLING_PAYMENT_METHODS, PaymentMethod } from '../core/models/sale.model';
import { DayWorkoutRegisterRequest } from '../core/models/billing.model';
import { normalizePlanNameKey } from '../core/models/plan.model';
import { CopCurrencyPipe } from '../core/pipes/cop-currency.pipe';
import { roundCop } from '../core/utils/money';
import {
  ensureWelcomeAudioUnlocked,
  isWelcomeAudioSupported,
  isWelcomeAudioUnlocked,
  speakAnnouncement,
} from '../core/utils/access-welcome-audio';
import { httpErrorMessage } from '../core/utils/http-error-message';

type DayPassShortcut = 'workout' | 'sports-dance';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule, CopCurrencyPipe],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly modules = inject(ModuleService);
  private readonly billingService = inject(BillingService);
  private readonly billingContext = inject(BillingContextService);
  private readonly planService = inject(PlanService);
  private readonly accessService = inject(AccessService);

  protected readonly shortcutMessage = signal<string | null>(null);
  protected readonly dayPassModal = signal<DayPassShortcut | null>(null);
  protected readonly dayPassProcessing = signal(false);
  protected readonly dayPassPaymentMethod = signal<PaymentMethod>('CASH');
  protected readonly dayPassUseSplitPayment = signal(false);
  protected readonly dayPassSplitMethod1 = signal<PaymentMethod>('CASH');
  protected readonly dayPassSplitMethod2 = signal<PaymentMethod>('NEQUI');
  protected readonly dayPassSplitAmount1 = signal<number | null>(null);
  protected readonly dayPassSplitAmount2 = signal<number | null>(null);
  protected readonly dayPassWorkoutPrice = signal<number | null>(null);
  protected readonly dayPassSportsDancePrice = signal<number | null>(null);
  protected readonly paymentMethods = BILLING_PAYMENT_METHODS;
  protected readonly roundCop = roundCop;

  private lastTicketAt = 0;
  private shortcutTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly dayPassChargeTotal = computed(() => {
    const kind = this.dayPassModal();
    if (!kind) {
      return null;
    }
    return kind === 'sports-dance' ? this.dayPassSportsDancePrice() : this.dayPassWorkoutPrice();
  });

  protected readonly dayPassSplitSum = computed(() => {
    const a1 = this.dayPassSplitAmount1() ?? 0;
    const a2 = this.dayPassSplitAmount2() ?? 0;
    return roundCop(a1 + a2);
  });

  protected readonly dayPassSplitRemaining = computed(() => {
    const total = this.dayPassChargeTotal();
    if (total == null) {
      return null;
    }
    return roundCop(total - this.dayPassSplitSum());
  });

  protected readonly canConfirmDayPass = computed(() => {
    if (this.dayPassProcessing()) {
      return false;
    }
    if (!this.dayPassUseSplitPayment()) {
      return true;
    }
    const total = this.dayPassChargeTotal();
    const a1 = this.dayPassSplitAmount1();
    const a2 = this.dayPassSplitAmount2();
    if (total == null || a1 == null || a2 == null || a1 < 1 || a2 < 1) {
      return false;
    }
    if (this.dayPassSplitMethod1() === this.dayPassSplitMethod2()) {
      return false;
    }
    return roundCop(a1 + a2) === total;
  });

  ngOnInit(): void {
    this.refreshModules();
    if (this.auth.isLoggedIn()) {
      this.auth.loadProfile().subscribe({
        next: () => {
          this.refreshModules();
          this.refreshOpenCashRegister();
          this.loadDayPassPlanPrices();
        },
        error: () => this.auth.logout(),
      });
    }
  }

  private loadDayPassPlanPrices(): void {
    if (!this.canUseDayPassShortcuts()) {
      return;
    }
    this.planService.findAll().subscribe({
      next: (plans) => {
        for (const plan of plans) {
          const key = normalizePlanNameKey(plan.name);
          if (key === 'entreno dia') {
            this.dayPassWorkoutPrice.set(roundCop(plan.price));
          } else if (key === 'bailes deportivos') {
            this.dayPassSportsDancePrice.set(roundCop(plan.price));
          }
        }
      },
    });
  }

  private refreshOpenCashRegister(): void {
    if (!this.canUseDayPassShortcuts()) {
      return;
    }
    this.billingService.findTodayCashRegister().subscribe({
      next: (today) => {
        const open = today?.status === 'OPEN' ? today : null;
        this.billingContext.setOpenCashRegister(open);
      },
      error: () => this.billingContext.setOpenCashRegister(null),
    });
  }

  private refreshModules(): void {
    this.modules.reloadPanelForUser().subscribe();
  }

  logout(): void {
    this.modules.resetPanel();
    this.auth.logout();
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.dayPassModal()) {
      this.closeDayPassModal();
      return;
    }

    const shortcut = this.shortcutFromKey(event.key);
    if (!shortcut) {
      return;
    }
    if (!this.auth.isLoggedIn() || !this.canUseDayPassShortcuts()) {
      return;
    }
    event.preventDefault();
    this.openDayPassModal(shortcut);
  }

  dayPassModalTitle(): string {
    return this.dayPassModal() === 'sports-dance'
      ? 'Bailes deportivos — invitado'
      : 'Entreno del día — invitado';
  }

  openDayPassModal(kind: DayPassShortcut): void {
    if (!this.billingContext.openCashRegister()) {
      const key = kind === 'sports-dance' ? 'F8' : 'F2';
      this.showShortcutMessage(`Abra la caja del día en Facturación antes de usar ${key}.`);
      return;
    }
    this.resetDayPassPaymentFields();
    this.dayPassPaymentMethod.set(this.billingContext.paymentMethod());
    if (this.dayPassWorkoutPrice() == null || this.dayPassSportsDancePrice() == null) {
      this.loadDayPassPlanPrices();
    }
    this.dayPassModal.set(kind);
  }

  closeDayPassModal(): void {
    if (this.dayPassProcessing()) {
      return;
    }
    this.dayPassModal.set(null);
  }

  setDayPassUseSplitPayment(enabled: boolean): void {
    this.dayPassUseSplitPayment.set(enabled);
    if (enabled) {
      this.syncDayPassSplitFromTotal();
    }
  }

  onDayPassSplitAmount1Change(raw: string | number | null): void {
    const parsed = raw != null && raw !== '' ? roundCop(+raw) : null;
    this.dayPassSplitAmount1.set(parsed);
    const total = this.dayPassChargeTotal();
    if (total == null || parsed == null) {
      this.dayPassSplitAmount2.set(null);
      return;
    }
    const rest = roundCop(total - parsed);
    this.dayPassSplitAmount2.set(rest > 0 ? rest : null);
  }

  onDayPassSplitAmount2Change(raw: string | number | null): void {
    const parsed = raw != null && raw !== '' ? roundCop(+raw) : null;
    this.dayPassSplitAmount2.set(parsed);
    const total = this.dayPassChargeTotal();
    if (total == null || parsed == null) {
      this.dayPassSplitAmount1.set(null);
      return;
    }
    const rest = roundCop(total - parsed);
    this.dayPassSplitAmount1.set(rest > 0 ? rest : null);
  }

  confirmDayPass(): void {
    const kind = this.dayPassModal();
    if (!kind || !this.canConfirmDayPass()) {
      return;
    }

    const now = Date.now();
    if (now - this.lastTicketAt < 1500) {
      return;
    }
    this.lastTicketAt = now;

    if (isWelcomeAudioSupported() && !isWelcomeAudioUnlocked()) {
      ensureWelcomeAudioUnlocked();
    }

    this.dayPassProcessing.set(true);
    const useSplit = this.dayPassUseSplitPayment();
    const paymentMethod = useSplit ? this.dayPassSplitMethod1() : this.dayPassPaymentMethod();
    this.billingContext.paymentMethod.set(paymentMethod);

    const memberId = this.billingContext.memberId();
    let paymentSplits: DayWorkoutRegisterRequest['paymentSplits'] = null;
    if (useSplit) {
      const total = this.dayPassChargeTotal();
      const a1 = this.dayPassSplitAmount1();
      const a2 = this.dayPassSplitAmount2();
      const m1 = this.dayPassSplitMethod1();
      const m2 = this.dayPassSplitMethod2();
      if (total == null || a1 == null || a2 == null) {
        this.dayPassProcessing.set(false);
        return;
      }
      paymentSplits = [
        { paymentMethod: m1, amount: roundCop(a1) },
        { paymentMethod: m2, amount: roundCop(a2) },
      ];
    }

    const payload = {
      memberId: memberId ?? undefined,
      paymentMethod,
      paymentSplits,
    };

    const request$ =
      kind === 'sports-dance'
        ? this.billingService.registerSportsDance(payload)
        : this.billingService.registerDayWorkout(payload);

    request$.subscribe({
      next: (res) => {
        this.syncLocalGateForDayPass(kind);
        if (!res.gateOpened) {
          const reason = kind === 'sports-dance' ? 'sports-dance' : 'workout';
          this.accessService.kioskOpenGate(reason).subscribe({
            next: () => undefined,
            error: () => undefined,
          });
        }
        const announcement =
          res.speechText?.trim() ||
          (kind === 'sports-dance' ? 'Baile deportivo activado.' : 'Entreno activado.');
        speakAnnouncement(announcement);
        this.showShortcutMessage(kind === 'sports-dance' ? 'Baile deportivo activado.' : 'Entreno activado.');
        this.billingContext.notifyBillingPaymentRecorded();
        this.refreshOpenCashRegister();
        this.dayPassProcessing.set(false);
        this.dayPassModal.set(null);
      },
      error: (err) => {
        this.showShortcutMessage(httpErrorMessage(err));
        this.dayPassProcessing.set(false);
      },
    });
  }

  private resetDayPassPaymentFields(): void {
    this.dayPassUseSplitPayment.set(false);
    this.dayPassSplitMethod1.set('CASH');
    this.dayPassSplitMethod2.set('NEQUI');
    this.dayPassSplitAmount1.set(null);
    this.dayPassSplitAmount2.set(null);
  }

  private syncDayPassSplitFromTotal(): void {
    const total = this.dayPassChargeTotal();
    if (total == null || !this.dayPassUseSplitPayment()) {
      return;
    }
    const half = roundCop(Math.floor(total / 2));
    this.dayPassSplitAmount1.set(half);
    this.dayPassSplitAmount2.set(roundCop(total - half));
  }

  private shortcutFromKey(key: string): DayPassShortcut | null {
    if (key === 'F2') {
      return 'workout';
    }
    if (key === 'F8') {
      return 'sports-dance';
    }
    return null;
  }

  private syncLocalGateForDayPass(kind: DayPassShortcut): void {
    const deviceUserId = kind === 'sports-dance' ? `BAILES-BILL-${Date.now()}` : `ENTRENO-BILL-${Date.now()}`;
    const payload = {
      result: 'GRANTED',
      gateOpened: true,
      credentialType: 'CARD',
      deviceUserId,
    };
    window.sportGymDesktop?.syncAccessResult?.(payload);
    if (window.sportGymDesktop?.syncAccessResult) {
      return;
    }
    fetch('http://127.0.0.1:8765/gate/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).catch(() => undefined);
  }

  private canUseDayPassShortcuts(): boolean {
    return this.auth.hasRole('SUPER_ADMIN', 'ADMIN', 'TRAINER');
  }

  private showShortcutMessage(text: string): void {
    this.shortcutMessage.set(text);
    if (this.shortcutTimer) {
      clearTimeout(this.shortcutTimer);
    }
    this.shortcutTimer = setTimeout(() => {
      this.shortcutMessage.set(null);
      this.shortcutTimer = null;
    }, 5000);
  }
}
