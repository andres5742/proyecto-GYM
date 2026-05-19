import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AccessLogEntry, FingerprintEnrollResponse } from '../../core/models/access.model';
import { Member } from '../../core/models/member.model';
import { AccessService } from '../../core/services/access.service';
import { MemberService } from '../../core/services/member.service';

@Component({
  selector: 'app-access-control',
  imports: [ReactiveFormsModule, DatePipe, RouterLink],
  templateUrl: './access-control.html',
  styleUrl: './access-control.scss',
})
export class AccessControlPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accessService = inject(AccessService);
  private readonly memberService = inject(MemberService);

  protected readonly message = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly members = signal<Member[]>([]);
  protected readonly enrollments = signal<FingerprintEnrollResponse[]>([]);
  protected readonly logs = signal<AccessLogEntry[]>([]);

  protected readonly enrollForm = this.fb.nonNullable.group({
    memberId: [null as number | null, Validators.required],
    fingerprintUserId: ['', [Validators.required, Validators.maxLength(64)]],
    deviceLabel: [''],
  });

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.memberService.findAll().subscribe({
      next: (m) => this.members.set(m),
    });
    this.accessService.listEnrollments().subscribe({
      next: (e) => this.enrollments.set(e),
    });
    this.accessService.logs().subscribe({
      next: (l) => this.logs.set(l),
    });
  }

  enroll(): void {
    if (this.enrollForm.invalid) {
      this.enrollForm.markAllAsTouched();
      return;
    }
    const raw = this.enrollForm.getRawValue();
    if (raw.memberId == null) {
      return;
    }
    this.saving.set(true);
    this.accessService
      .enroll({
        memberId: raw.memberId,
        fingerprintUserId: raw.fingerprintUserId,
        deviceLabel: raw.deviceLabel || undefined,
      })
      .subscribe({
        next: () => {
          this.message.set('Huella vinculada al afiliado correctamente');
          this.saving.set(false);
          this.enrollForm.reset({ memberId: null, fingerprintUserId: '', deviceLabel: '' });
          this.loadAll();
        },
        error: (err) => {
          this.message.set(err?.error?.message ?? 'No se pudo registrar la huella');
          this.saving.set(false);
        },
      });
  }

  removeEnrollment(memberId: number): void {
    if (!confirm('¿Quitar la huella de este afiliado?')) {
      return;
    }
    this.accessService.removeEnrollment(memberId).subscribe({
      next: () => {
        this.message.set('Huella eliminada');
        this.loadAll();
      },
      error: () => this.message.set('No se pudo eliminar'),
    });
  }

  manualOpen(memberId: number): void {
    this.accessService.manualOpen(memberId).subscribe({
      next: (res) => {
        this.message.set(res.message);
        this.loadAll();
      },
      error: (err) => this.message.set(err?.error?.message ?? 'No se pudo abrir'),
    });
  }
}
