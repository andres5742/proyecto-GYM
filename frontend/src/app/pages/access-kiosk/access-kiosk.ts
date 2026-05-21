import { DatePipe } from '@angular/common';
import { Component, HostListener, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AccessVerifyResponse, KioskAccessEvent } from '../../core/models/access.model';
import { HttpErrorResponse } from '@angular/common/http';
import { AccessService } from '../../core/services/access.service';
import {
  firstNameFromFullName,
  isWelcomeAudioSupported,
  isWelcomeAudioUnlocked,
  playAccessWelcome,
  prepareWelcomeSpeech,
  unlockWelcomeAudio,
  welcomeHeadline,
} from '../../core/utils/access-welcome-audio';

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
/** Arranque automático al abrir la página (sin botón Activar). */
const AUTO_START_MS = 400;

@Component({
  selector: 'app-access-kiosk',
  imports: [DatePipe],
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
  private autoStartTimer: ReturnType<typeof setTimeout> | null = null;
  private audioUnlockAttempted = false;

  protected readonly deviceUserId = signal('');
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
    this.lastResult.set(null);
    this.welcomeTitle.set(null);
    this.statusLine.set('Pase su tarjeta o coloque su huella en el lector…');
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
    playAccessWelcome(firstName, gender);
    this.showWelcomeAudioBtn.set(false);
  }

  onCedulaInput(value: string): void {
    this.deviceUserId.set(value);
  }

  protected cedulaDigitCount(value: string): number {
    return value.replace(/\D/g, '').length;
  }

  simulateScan(): void {
    const id = this.deviceUserId().trim();
    if (!id || !this.kioskReady()) {
      return;
    }
    this.accessService.zktEvent(id).subscribe({
      next: (res) => {
        this.applyVerifyResponse(res);
      },
      error: (err: HttpErrorResponse) => {
        const msg =
          typeof err.error === 'object' && err.error?.message
            ? String(err.error.message)
            : 'No se pudo validar el acceso. Revise la cédula o la conexión.';
        this.statusLine.set(msg);
        this.lastResult.set({
          result: 'DENIED',
          gateOpened: false,
          message: msg,
          deviceUserId: id,
          credentialType: 'CARD',
        });
        this.welcomeTitle.set(null);
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
      memberName: event.memberName,
      deviceUserId: event.deviceUserId,
      credentialType: event.credentialType,
      gender: event.gender ?? null,
      documentId: event.documentId ?? null,
    });
  }

  private applyVerifyResponse(res: AccessVerifyResponse): void {
    if (this.releaseTimer) {
      clearTimeout(this.releaseTimer);
      this.releaseTimer = null;
    }

    this.lastResult.set(res);
    if (res.accessLogId && res.accessLogId > this.lastProcessedLogId) {
      this.lastProcessedLogId = res.accessLogId;
    }

    if (res.result === 'GRANTED') {
      const firstName = firstNameFromFullName(res.memberName);
      const gender = res.gender ?? null;
      this.welcomeTitle.set(welcomeHeadline(firstName, gender));
      prepareWelcomeSpeech();
      if (isWelcomeAudioUnlocked()) {
        const spoke = playAccessWelcome(firstName, gender);
        this.showWelcomeAudioBtn.set(!spoke && this.audioSupported);
      } else {
        this.showWelcomeAudioBtn.set(this.audioSupported);
      }
      const cedula = this.displayCedula(res);
      this.statusLine.set(
        firstName
          ? `${welcomeHeadline(firstName, gender)}${cedula ? ` · Cédula ${cedula}` : ''} Pasa al torniquete.`
          : cedula
            ? `¡Ingreso autorizado! Cédula ${cedula}. Pasa al torniquete.`
            : '¡Ingreso autorizado! Pasa al torniquete.',
      );
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

  private scheduleRelease(ms: number): void {
    this.releaseTimer = setTimeout(() => {
      this.releaseTimer = null;
      this.lastResult.set(null);
      this.welcomeTitle.set(null);
      this.showWelcomeAudioBtn.set(false);
      this.statusLine.set('Pase su tarjeta o coloque su huella en el lector…');
    }, ms);
  }
}
