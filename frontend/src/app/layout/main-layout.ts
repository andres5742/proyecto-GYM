import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { ModuleService } from '../core/services/module.service';
import { BillingContextService } from '../core/services/billing-context.service';
import { BillingService } from '../core/services/billing.service';
import { SALES_PAYMENT_METHODS, PaymentMethod } from '../core/models/sale.model';
import {
  ensureWelcomeAudioUnlocked,
  isWelcomeAudioSupported,
  isWelcomeAudioUnlocked,
  speakAnnouncement,
} from '../core/utils/access-welcome-audio';
import { httpErrorMessage } from '../core/utils/http-error-message';

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
  protected readonly f2ModalOpen = signal(false);
  protected readonly f2Processing = signal(false);
  protected readonly f2PaymentMethod = signal<PaymentMethod>('CASH');
  protected readonly paymentMethods = SALES_PAYMENT_METHODS;

  private lastTicketAt = 0;
  private shortcutTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.refreshModules();
    if (this.auth.isLoggedIn()) {
      this.auth.loadProfile().subscribe({
        next: () => this.refreshModules(),
        error: () => this.auth.logout(),
      });
    }
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
    if (event.key === 'Escape' && this.f2ModalOpen()) {
      this.closeF2Modal();
      return;
    }
    if (event.key !== 'F2') {
      return;
    }
    if (!this.auth.isLoggedIn() || !this.canUseDayWorkoutF2()) {
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
    this.openF2Modal();
  }

  openF2Modal(): void {
    this.f2PaymentMethod.set(this.billingContext.paymentMethod());
    this.f2ModalOpen.set(true);
  }

  closeF2Modal(): void {
    if (this.f2Processing()) {
      return;
    }
    this.f2ModalOpen.set(false);
  }

  confirmF2GuestWorkout(): void {
    const now = Date.now();
    if (now - this.lastTicketAt < 1500) {
      return;
    }
    this.lastTicketAt = now;

    if (isWelcomeAudioSupported() && !isWelcomeAudioUnlocked()) {
      ensureWelcomeAudioUnlocked();
    }

    this.f2Processing.set(true);
    const paymentMethod = this.f2PaymentMethod();
    this.billingContext.paymentMethod.set(paymentMethod);

    const memberId = this.billingContext.memberId();
    this.billingService
      .registerDayWorkout({
        memberId: memberId ?? undefined,
        paymentMethod,
      })
      .subscribe({
        next: (res) => {
          speakAnnouncement(res.speechText);
          this.showShortcutMessage(res.message);
          this.billingContext.notifyDayWorkoutRecorded();
          this.f2Processing.set(false);
          this.f2ModalOpen.set(false);
        },
        error: (err) => {
          this.showShortcutMessage(httpErrorMessage(err));
          this.f2Processing.set(false);
        },
      });
  }

  private canUseDayWorkoutF2(): boolean {
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
