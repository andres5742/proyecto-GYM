import { DatePipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { FaceWebcamCaptureComponent } from '../../components/face-webcam-capture/face-webcam-capture';
import { AccessVerifyResponse } from '../../core/models/access.model';
import { AccessService } from '../../core/services/access.service';
import { firstNameFromFullName, playAccessWelcome } from '../../core/utils/access-welcome-audio';

@Component({
  selector: 'app-access-kiosk',
  imports: [DatePipe, FaceWebcamCaptureComponent],
  templateUrl: './access-kiosk.html',
  styleUrl: './access-kiosk.scss',
})
export class AccessKiosk implements OnInit, OnDestroy {
  private readonly accessService = inject(AccessService);
  private readonly faceCapture = viewChild(FaceWebcamCaptureComponent);

  private clockTimer: ReturnType<typeof setInterval> | null = null;
  private scanRafId: number | null = null;
  private lastDetectAt = 0;
  private consecutiveFaces = 0;
  private verifyInFlight = false;
  private cooldownUntil = 0;

  private readonly detectIntervalMs = 100;
  private readonly stableFramesRequired = 2;
  private readonly grantedCooldownMs = 5000;
  private readonly deniedCooldownMs = 3000;

  protected readonly deviceUserId = signal('');
  protected readonly scanning = signal(false);
  protected readonly facePresent = signal(false);
  protected readonly lastResult = signal<AccessVerifyResponse | null>(null);
  protected readonly clock = signal(new Date());
  protected readonly statusLine = signal('Iniciando lectores…');

  ngOnInit(): void {
    this.clockTimer = setInterval(() => this.clock.set(new Date()), 1000);
    requestAnimationFrame(() => this.startAccessLoop());
  }

  ngOnDestroy(): void {
    if (this.clockTimer) {
      clearInterval(this.clockTimer);
    }
    this.stopAccessLoop();
  }

  onFaceStatus(message: string): void {
    if (!this.verifyInFlight && !this.lastResult() && !this.facePresent()) {
      this.statusLine.set(message);
    }
  }

  onFaceDetected(detected: boolean): void {
    this.facePresent.set(detected);
    if (detected) {
      if (!this.verifyInFlight && !this.lastResult()) {
        this.statusLine.set('Rostro detectado, mantén la mirada a la cámara…');
      }
      return;
    }
    this.consecutiveFaces = 0;
    this.tryReleaseForNextPerson();
  }

  private tryReleaseForNextPerson(): void {
    if (this.canAcceptNextPerson()) {
      this.clearResultAndWait();
    } else if (this.lastResult()) {
      this.statusLine.set('Retírate de la cámara cuando hayas pasado…');
    } else if (!this.verifyInFlight) {
      this.statusLine.set('Esperando al siguiente afiliado… Coloca huella o mira la cámara.');
    }
  }

  simulateScan(): void {
    const id = this.deviceUserId().trim();
    if (!id || this.verifyInFlight || !this.canAcceptNextPerson()) {
      return;
    }
    this.verifyFingerprint(id);
  }

  /** Llamable desde integración con lector de huella físico. */
  verifyFingerprint(deviceUserId: string): void {
    const id = deviceUserId.trim();
    if (!id || this.verifyInFlight) {
      return;
    }
    this.verifyInFlight = true;
    this.scanning.set(true);
    this.statusLine.set('Verificando huella…');
    this.stopAccessLoop();

    this.accessService.verify(id, 'FINGERPRINT').subscribe({
      next: (res) => {
        this.verifyInFlight = false;
        this.scanning.set(false);
        this.deviceUserId.set('');
        this.handleVerifyResult(res);
        this.resumeAccessLoop();
      },
      error: (err) => {
        this.verifyInFlight = false;
        this.scanning.set(false);
        this.handleVerifyError(err, id, 'FINGERPRINT');
        this.resumeAccessLoop();
      },
    });
  }

  private startAccessLoop(): void {
    this.stopAccessLoop();
    const loop = (now: number) => {
      this.scanRafId = requestAnimationFrame(loop);
      if (now - this.lastDetectAt < this.detectIntervalMs) {
        return;
      }
      if (this.verifyInFlight || !this.canAcceptNextPerson()) {
        return;
      }
      void this.tickFaceScan(now);
    };
    this.scanRafId = requestAnimationFrame(loop);
  }

  private stopAccessLoop(): void {
    if (this.scanRafId !== null) {
      cancelAnimationFrame(this.scanRafId);
      this.scanRafId = null;
    }
  }

  private resumeAccessLoop(): void {
    requestAnimationFrame(() => this.startAccessLoop());
  }

  private canAcceptNextPerson(): boolean {
    return Date.now() >= this.cooldownUntil;
  }

  private async tickFaceScan(now: number): Promise<void> {
    this.lastDetectAt = now;
    const capture = this.faceCapture();
    if (!capture) {
      return;
    }

    const descriptor = await capture.detectForAccess();
    if (!descriptor) {
      this.consecutiveFaces = 0;
      if (!this.facePresent() && this.canAcceptNextPerson() && this.lastResult()) {
        this.clearResultAndWait();
      } else if (!this.facePresent() && !this.lastResult() && !this.verifyInFlight) {
        this.statusLine.set('Esperando al siguiente afiliado…');
      }
      return;
    }

    this.facePresent.set(true);
    this.consecutiveFaces++;
    if (this.consecutiveFaces < this.stableFramesRequired) {
      this.statusLine.set('Rostro detectado, verificando…');
      return;
    }

    this.consecutiveFaces = 0;
    this.verifyInFlight = true;
    this.scanning.set(true);
    this.statusLine.set('Verificando rostro…');
    this.stopAccessLoop();

    this.accessService.verifyWebcam(descriptor).subscribe({
      next: (res) => {
        this.verifyInFlight = false;
        this.scanning.set(false);
        this.handleVerifyResult(res);
        this.resumeAccessLoop();
      },
      error: (err) => {
        this.verifyInFlight = false;
        this.scanning.set(false);
        this.handleVerifyError(err, 'BIO', 'FACE');
        this.resumeAccessLoop();
      },
    });
  }

  private handleVerifyResult(res: AccessVerifyResponse): void {
    this.lastResult.set(res);
    const cooldown = res.result === 'GRANTED' ? this.grantedCooldownMs : this.deniedCooldownMs;
    this.cooldownUntil = Date.now() + cooldown;

    if (res.result === 'GRANTED') {
      playAccessWelcome(firstNameFromFullName(res.memberName));
      this.statusLine.set('¡Ingreso autorizado! Pasa al siguiente cuando salgas del encuadre.');
    } else {
      this.statusLine.set(res.message);
    }
  }

  private handleVerifyError(
    err: { error?: { message?: string } },
    id: string,
    credentialType: 'FINGERPRINT' | 'FACE',
  ): void {
    const message = err?.error?.message ?? 'Error de conexión con el servidor';
    this.lastResult.set({
      result: 'DENIED',
      gateOpened: false,
      message,
      deviceUserId: id,
      credentialType,
    });
    this.cooldownUntil = Date.now() + this.deniedCooldownMs;
    this.statusLine.set(message);
  }

  private clearResultAndWait(): void {
    this.lastResult.set(null);
    this.consecutiveFaces = 0;
    this.statusLine.set('Esperando al siguiente afiliado… Coloca huella o mira la cámara.');
  }
}
