import { DatePipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { AccessVerifyResponse } from '../../core/models/access.model';
import { AccessService } from '../../core/services/access.service';

@Component({
  selector: 'app-access-kiosk',
  imports: [DatePipe],
  templateUrl: './access-kiosk.html',
  styleUrl: './access-kiosk.scss',
})
export class AccessKiosk implements OnInit, OnDestroy {
  private readonly accessService = inject(AccessService);
  private clockTimer: ReturnType<typeof setInterval> | null = null;

  protected readonly fingerprintId = signal('');
  protected readonly scanning = signal(false);
  protected readonly lastResult = signal<AccessVerifyResponse | null>(null);
  protected readonly clock = signal(new Date());

  ngOnInit(): void {
    this.clockTimer = setInterval(() => this.clock.set(new Date()), 1000);
  }

  ngOnDestroy(): void {
    if (this.clockTimer) {
      clearInterval(this.clockTimer);
    }
  }

  simulateScan(): void {
    const id = this.fingerprintId().trim();
    if (!id) {
      return;
    }
    this.scanning.set(true);
    this.accessService.verifyFingerprint(id).subscribe({
      next: (res) => {
        this.lastResult.set(res);
        this.scanning.set(false);
        if (res.result === 'GRANTED') {
          this.fingerprintId.set('');
        }
      },
      error: (err) => {
        this.scanning.set(false);
        this.lastResult.set({
          result: 'DENIED',
          gateOpened: false,
          message: err?.error?.message ?? 'Error de conexión con el servidor',
          fingerprintUserId: id,
        });
      },
    });
  }
}
