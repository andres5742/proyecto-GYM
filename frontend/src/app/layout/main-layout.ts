import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { ModuleService } from '../core/services/module.service';
import { BillingContextService } from '../core/services/billing-context.service';
import { BillingService } from '../core/services/billing.service';
import { BILLING_PAYMENT_METHODS, PaymentMethod } from '../core/models/sale.model';
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
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly modules = inject(ModuleService);
  private readonly billingService = inject(BillingService);
  private readonly billingContext = inject(BillingContextService);

  protected readonly shortcutMessage = signal<string | null>(null);
  protected readonly dayPassModal = signal<DayPassShortcut | null>(null);
  protected readonly dayPassProcessing = signal(false);
  protected readonly dayPassPaymentMethod = signal<PaymentMethod>('CASH');
  protected readonly paymentMethods = BILLING_PAYMENT_METHODS;

  private lastTicketAt = 0;
  private shortcutTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.refreshModules();
    if (this.auth.isLoggedIn()) {
      this.auth.loadProfile().subscribe({
        next: () => {
          this.refreshModules();
          this.refreshOpenCashRegister();
        },
        error: () => this.auth.logout(),
      });
    }
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
    const target = event.target;
    if (
      target instanceof HTMLInputElement ||
      target instanceof HTMLTextAreaElement ||
      target instanceof HTMLSelectElement
    ) {
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
      const key = kind === 'sports-dance' ? 'F3' : 'F2';
      this.showShortcutMessage(`Abra la caja del día en Facturación antes de usar ${key}.`);
      return;
    }
    this.dayPassPaymentMethod.set(this.billingContext.paymentMethod());
    this.dayPassModal.set(kind);
  }

  closeDayPassModal(): void {
    if (this.dayPassProcessing()) {
      return;
    }
    this.dayPassModal.set(null);
  }

  confirmDayPass(): void {
    const kind = this.dayPassModal();
    if (!kind) {
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
    const paymentMethod = this.dayPassPaymentMethod();
    this.billingContext.paymentMethod.set(paymentMethod);

    const memberId = this.billingContext.memberId();
    const request$ =
      kind === 'sports-dance'
        ? this.billingService.registerSportsDance({
            memberId: memberId ?? undefined,
            paymentMethod,
          })
        : this.billingService.registerDayWorkout({
            memberId: memberId ?? undefined,
            paymentMethod,
          });

    request$.subscribe({
      next: (res) => {
        const announcement =
          res.speechText?.trim() ||
          (kind === 'sports-dance' ? 'Baile deportivo activado.' : 'Entreno registrado.');
        speakAnnouncement(announcement);
        this.showShortcutMessage(res.message);
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

  private shortcutFromKey(key: string): DayPassShortcut | null {
    if (key === 'F2') {
      return 'workout';
    }
    if (key === 'F3') {
      return 'sports-dance';
    }
    return null;
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
