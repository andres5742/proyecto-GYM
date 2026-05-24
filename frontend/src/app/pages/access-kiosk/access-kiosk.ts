import { Component, HostListener, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AccessVerifyResponse, CardSelectionCandidate, isStaffPerson, KioskAccessEvent } from '../../core/models/access.model';
import { HttpErrorResponse } from '@angular/common/http';
import { AccessService } from '../../core/services/access.service';
import {
  firstNameFromFullName,
  isWelcomeAudioSupported,
  isWelcomeAudioUnlocked,
  accessWelcomeHintsFromResponse,
  playAccessWelcome,
  playStaffAccessWelcome,
  prepareWelcomeSpeech,
  resolveMemberWelcomeText,
  resolveStaffWelcomeText,
  unlockWelcomeAudio,
} from '../../core/utils/access-welcome-audio';
import { zktKeypadSelectionIndex } from '../../core/utils/zkt-keypad';

const KIOSK_MOTIVATIONAL_PHRASES = [
  'Cada día es una nueva oportunidad para ser más fuerte.',
  'Tu único límite eres tú. ¡A entrenar!',
  'La disciplina convierte los sueños en logros.',
  'Hoy entrenas, mañana agradeces.',
  'El esfuerzo de hoy es la fuerza de mañana.',
  'Pequeños pasos, grandes resultados.',
  'Constancia y actitud: esa es la fórmula.',
  'En Sport Gym R.10 crecemos juntos.',
];

const POLL_MS = 700;
const GRANTED_DISPLAY_MS = 8000;
const DENIED_DISPLAY_MS = 5000;
const SELECT_MEMBER_DISPLAY_MS = 45000;
/** Arranque automático al abrir la página (sin botón Activar). */
const AUTO_START_MS = 400;

@Component({
  selector: 'app-access-kiosk',
  templateUrl: './access-kiosk.html',
  styleUrl: './access-kiosk.scss',
})
export class AccessKiosk implements OnInit, OnDestroy {
  private readonly accessService = inject(AccessService);

  private clockTimer: ReturnType<typeof setInterval> | null = null;
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private releaseTimer: ReturnType<typeof setTimeout> | null = null;
  private lastProcessedLogId = 0;
  private pollSinceIso = new Date().toISOString();
  private polling = false;
  private pollBootstrapped = false;
  private autoStartTimer: ReturnType<typeof setTimeout> | null = null;
  private audioUnlockAttempted = false;
  private lastWelcomedLogId = 0;
  private lastWelcomedAt = 0;
  private lastWelcomedKey = '';

  protected readonly lastResult = signal<AccessVerifyResponse | null>(null);
  protected readonly clock = signal(new Date());
  protected readonly statusLine = signal('Iniciando pantalla de acceso…');
  protected readonly welcomeTitle = signal<string | null>(null);
  protected readonly audioUnlocked = signal(false);
  protected readonly kioskReady = signal(false);
  protected readonly showWelcomeAudioBtn = signal(false);
  protected readonly audioSupported = isWelcomeAudioSupported();
  protected readonly motivationalPhrase = signal(KIOSK_MOTIVATIONAL_PHRASES[0]);
  protected readonly configError = signal<string | null>(null);
  protected readonly cardSelection = signal<{
    pin: string;
    candidates: CardSelectionCandidate[];
  } | null>(null);
  protected footerDateLabel(): string {
    return this.clock()
      .toLocaleDateString('es-CO', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      })
      .toUpperCase();
  }

  protected footerTimeLabel(): string {
    return this.clock()
      .toLocaleTimeString('es-CO', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true,
      })
      .toUpperCase();
  }

  protected closeApp(): void {
    if (window.sportGymDesktop?.requestClose) {
      window.sportGymDesktop.requestClose();
      return;
    }
    const ua = typeof navigator !== 'undefined' ? navigator.userAgent : '';
    if (ua.includes('Electron')) {
      window.close();
      return;
    }
    if (confirm('¿Cerrar la pantalla de acceso?')) {
      window.close();
    }
  }

  ngOnInit(): void {
    if (!document.querySelector('link[rel="manifest"][href*="manifest-acceso"]')) {
      const manifest = document.createElement('link');
      manifest.rel = 'manifest';
      manifest.href = '/manifest-acceso.webmanifest';
      document.head.appendChild(manifest);
    }
    const dayIndex = new Date().getDate() % KIOSK_MOTIVATIONAL_PHRASES.length;
    this.motivationalPhrase.set(KIOSK_MOTIVATIONAL_PHRASES[dayIndex]);
    this.clockTimer = setInterval(() => this.clock.set(new Date()), 1000);
    prepareWelcomeSpeech();
    if (!environment.accessDeviceKey?.trim() || environment.accessDeviceKey === '__ACCESS_DEVICE_KEY__') {
      const msg =
        'Clave del torniquete no configurada. Define ACCESS_DEVICE_KEY y reconstruye el frontend.';
      this.configError.set(msg);
      this.statusLine.set(msg);
      return;
    }
    this.autoStartTimer = setTimeout(() => this.startKiosk(), AUTO_START_MS);
  }

  ngOnDestroy(): void {
    if (this.clockTimer) {
      clearInterval(this.clockTimer);
    }
    if (this.autoStartTimer) {
      clearTimeout(this.autoStartTimer);
    }
    this.stopPolling();
    if (this.releaseTimer) {
      clearTimeout(this.releaseTimer);
    }
  }

  /** Primer toque en pantalla: desbloquea voz del navegador (sin botón Activar). */
  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (!this.kioskReady() || this.configError()) {
      return;
    }
    const selection = this.cardSelection();
    if (selection) {
      const choice = this.parseSelectionKey(event);
      if (choice != null) {
        event.preventDefault();
        this.submitCardSelection(selection.pin, choice);
      }
      return;
    }
    if (event.key === 'F2') {
      event.preventDefault();
      this.triggerShortcutGate('workout');
      return;
    }
    if (event.key === 'F8') {
      event.preventDefault();
      this.triggerShortcutGate('sports-dance');
    }
  }

  @HostListener('pointerdown')
  onKioskPointerDown(): void {
    if (this.audioUnlockAttempted || !this.audioSupported) {
      return;
    }
    this.audioUnlockAttempted = true;
    const ok = unlockWelcomeAudio();
    this.audioUnlocked.set(ok);
  }

  private startKiosk(): void {
    if (this.configError()) {
      return;
    }
    if (this.audioSupported) {
      const ok = unlockWelcomeAudio();
      this.audioUnlocked.set(ok);
    }
    this.kioskReady.set(true);
    this.pollSinceIso = new Date().toISOString();
    this.lastProcessedLogId = 0;
    this.pollBootstrapped = false;
    this.lastWelcomedLogId = 0;
    this.lastWelcomedAt = 0;
    this.lastWelcomedKey = '';
    this.lastResult.set(null);
    this.welcomeTitle.set(null);
    this.statusLine.set('Sistema activado. Pase su tarjeta o coloque su huella en el lector…');
    this.startPolling();
  }

  playWelcomeNow(): void {
    const last = this.lastResult();
    const firstName = firstNameFromFullName(last?.memberName);
    const gender = last?.gender ?? null;
    if (!isWelcomeAudioUnlocked()) {
      const ok = unlockWelcomeAudio();
      this.audioUnlocked.set(ok);
      if (!ok) {
        return;
      }
    }
    if (last && isStaffPerson(last)) {
      playStaffAccessWelcome(gender, last?.message);
    } else {
      playAccessWelcome(
        firstName,
        gender,
        last ? accessWelcomeHintsFromResponse(last) : null,
        last?.message,
      );
    }
    this.showWelcomeAudioBtn.set(false);
  }

  private triggerShortcutGate(reason: 'workout' | 'sports-dance'): void {
    const label = reason === 'sports-dance' ? 'F8 Bailes' : 'F2 Entreno';
    this.statusLine.set(`Abriendo torniquete (${label})…`);
    this.accessService.kioskOpenGate(reason).subscribe({
      next: (res) => this.applyVerifyResponse(res),
      error: (err: HttpErrorResponse) => {
        const msg =
          typeof err.error === 'object' && err.error?.message
            ? String(err.error.message)
            : 'No se pudo abrir el torniquete.';
        this.statusLine.set(msg);
        this.lastResult.set({
          result: 'DENIED',
          gateOpened: false,
          message: msg,
          deviceUserId: reason === 'sports-dance' ? 'F8' : 'F2',
          credentialType: 'CARD',
        });
        this.scheduleRelease(DENIED_DISPLAY_MS);
      },
    });
  }

  private startPolling(): void {
    this.stopPolling();
    void this.pollEvents();
    this.pollTimer = setInterval(() => void this.pollEvents(), POLL_MS);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  private async pollEvents(): Promise<void> {
    if (this.polling || !this.kioskReady() || this.configError()) {
      return;
    }
    this.polling = true;
    this.accessService.kioskEventsSince(this.pollSinceIso, this.lastProcessedLogId).subscribe({
      next: (events) => {
        this.polling = false;
        if (!this.pollBootstrapped) {
          this.pollBootstrapped = true;
          for (const event of events) {
            this.lastProcessedLogId = Math.max(this.lastProcessedLogId, event.id);
          }
          this.pollSinceIso = new Date().toISOString();
          return;
        }
        for (const event of events) {
          if (event.id <= this.lastProcessedLogId) {
            continue;
          }
          this.lastProcessedLogId = Math.max(this.lastProcessedLogId, event.id);
          this.applyKioskEvent(event);
          break;
        }
      },
      error: () => {
        this.polling = false;
      },
    });
  }

  private applyKioskEvent(event: KioskAccessEvent): void {
    this.applyVerifyResponse({
      result: event.result,
      gateOpened: event.gateOpened,
      message: event.message,
      memberId: event.memberId,
      employeeId: event.employeeId,
      personType: event.personType,
      memberName: event.memberName,
      deviceUserId: event.deviceUserId,
      credentialType: event.credentialType,
      gender: event.gender ?? null,
      documentId: event.documentId ?? null,
      accessLogId: event.id,
      membershipDaysRemaining: event.membershipDaysRemaining ?? null,
      tiqueteraEntriesRemainingAfter: event.tiqueteraEntriesRemainingAfter ?? null,
      tiqueteraPlan: event.tiqueteraPlan ?? null,
      cardSelectionCandidates: event.cardSelectionCandidates ?? [],
    });
  }

  private parseSelectionKey(event: KeyboardEvent): CardSelectionCandidate | null {
    const index = zktKeypadSelectionIndex(event);
    if (index == null) {
      return null;
    }
    const candidates = this.cardSelection()?.candidates ?? [];
    return candidates.find((c) => c.index === index) ?? null;
  }

  private submitCardSelection(pin: string, candidate: CardSelectionCandidate): void {
    this.statusLine.set(`Verificando a ${candidate.memberName}…`);
    this.accessService.zktSelectMember(pin, candidate.memberId).subscribe({
      next: (res) => {
        this.cardSelection.set(null);
        this.applyVerifyResponse(res);
      },
      error: (err: HttpErrorResponse) => {
        const msg =
          typeof err.error === 'object' && err.error?.message
            ? String(err.error.message)
            : 'No se pudo validar la tarjeta.';
        this.cardSelection.set(null);
        this.applyVerifyResponse({
          result: 'DENIED',
          gateOpened: false,
          message: msg,
          deviceUserId: pin,
          credentialType: 'CARD',
        });
      },
    });
  }

  private applyVerifyResponse(res: AccessVerifyResponse): void {
    if (this.releaseTimer) {
      clearTimeout(this.releaseTimer);
      this.releaseTimer = null;
    }

    window.sportGymDesktop?.syncAccessResult?.({
      result: res.result,
      gateOpened: Boolean(res.gateOpened),
      deviceUserId: res.deviceUserId,
      credentialType: res.credentialType,
    });
    if (res.credentialType !== 'CARD') {
      this.syncLocalGate(res);
    }

    this.lastResult.set(res);
    if (res.accessLogId && res.accessLogId > this.lastProcessedLogId) {
      this.lastProcessedLogId = res.accessLogId;
    }

    if (res.result === 'SELECT_MEMBER') {
      const candidates = res.cardSelectionCandidates ?? [];
      if (candidates.length > 0) {
        this.cardSelection.set({
          pin: res.deviceUserId,
          candidates,
        });
      } else {
        this.cardSelection.set(null);
      }
      this.welcomeTitle.set(null);
      this.showWelcomeAudioBtn.set(false);
      this.statusLine.set(
        candidates.length > 0
          ? res.message
          : `${res.message} Actualice el servidor (git pull + actualizar-acceso-tarjetas.sh).`,
      );
      this.scheduleRelease(SELECT_MEMBER_DISPLAY_MS);
      return;
    }

    this.cardSelection.set(null);

    if (res.result === 'GRANTED') {
      const staff = isStaffPerson(res);
      const firstName = firstNameFromFullName(res.memberName);
      const gender = res.gender ?? null;
      const welcomeText = staff
        ? resolveStaffWelcomeText(res.message, gender)
        : resolveMemberWelcomeText(res.message, firstName, gender);
      this.welcomeTitle.set(welcomeText);
      prepareWelcomeSpeech();
      if (this.shouldSpeakWelcome(res)) {
        if (isWelcomeAudioUnlocked()) {
          const spoke = staff
            ? playStaffAccessWelcome(gender, res.message)
            : playAccessWelcome(firstName, gender, accessWelcomeHintsFromResponse(res), res.message);
          this.showWelcomeAudioBtn.set(!spoke && this.audioSupported);
        } else {
          this.showWelcomeAudioBtn.set(this.audioSupported);
        }
        this.markWelcomeSpoken(res);
      } else {
        this.showWelcomeAudioBtn.set(false);
      }
      if (staff) {
        this.statusLine.set(`${welcomeText}. Pasa al torniquete.`);
      } else {
        const cedula = this.displayCedula(res);
        this.statusLine.set(
          cedula
            ? `${welcomeText} · Cédula ${cedula}. Pasa al torniquete.`
            : `${welcomeText}. Pasa al torniquete.`,
        );
      }
      this.scheduleRelease(GRANTED_DISPLAY_MS);
    } else {
      this.welcomeTitle.set(null);
      this.showWelcomeAudioBtn.set(false);
      const cedula = this.displayCedula(res);
      this.statusLine.set(cedula ? `${res.message} (Cédula ${cedula})` : res.message);
      this.scheduleRelease(DENIED_DISPLAY_MS);
    }
  }

  protected displayCedula(res: AccessVerifyResponse | null): string | null {
    if (!res) {
      return null;
    }
    const doc = res.documentId?.trim();
    if (doc) {
      return doc;
    }
    const pin = res.deviceUserId?.trim();
    if (pin && /^\d{5,}$/.test(pin.replace(/\D/g, ''))) {
      return pin;
    }
    return null;
  }

  private syncLocalGate(res: AccessVerifyResponse): void {
    if (window.sportGymDesktop?.syncAccessResult) {
      return;
    }
    const body = JSON.stringify({
      result: res.result,
      gateOpened: Boolean(res.gateOpened),
      credentialType: res.credentialType,
      deviceUserId: res.deviceUserId,
      accessLogId: res.accessLogId ?? null,
    });
    fetch('http://127.0.0.1:8765/gate/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
    }).catch(() => undefined);
  }

  private shouldSpeakWelcome(res: AccessVerifyResponse): boolean {
    const logId = res.accessLogId ?? null;
    if (logId && logId > 0) {
      return logId !== this.lastWelcomedLogId;
    }
    const key = this.welcomeDedupKey(res);
    const now = Date.now();
    return !(key === this.lastWelcomedKey && now - this.lastWelcomedAt < 7000);
  }

  private markWelcomeSpoken(res: AccessVerifyResponse): void {
    this.lastWelcomedAt = Date.now();
    this.lastWelcomedKey = this.welcomeDedupKey(res);
    const logId = res.accessLogId ?? null;
    if (logId && logId > 0) {
      this.lastWelcomedLogId = logId;
    }
  }

  private welcomeDedupKey(res: AccessVerifyResponse): string {
    const person =
      res.memberId ??
      res.employeeId ??
      res.documentId ??
      res.deviceUserId ??
      '';
    return `${res.result}|${person}|${res.message ?? ''}`;
  }

  private scheduleRelease(ms: number): void {
    this.releaseTimer = setTimeout(() => {
      this.releaseTimer = null;
      this.cardSelection.set(null);
      this.lastResult.set(null);
      this.welcomeTitle.set(null);
      this.showWelcomeAudioBtn.set(false);
      this.statusLine.set('Pase su tarjeta o coloque su huella en el lector…');
    }, ms);
  }
}
