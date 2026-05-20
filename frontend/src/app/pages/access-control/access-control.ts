import { formatDate } from '@angular/common';
import { Component, computed, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DataTableComponent } from '../../components/data-table/data-table';
import { DataTableColumn } from '../../components/data-table/data-table.model';
import { FaceWebcamCaptureComponent } from '../../components/face-webcam-capture/face-webcam-capture';
import { MemberSearchSelectComponent } from '../../components/member-search-select/member-search-select';
import {
  AccessLogEntry,
  BIOMETRIC_TYPE_LABELS,
  BiometricCredentialType,
  BiometricEnrollResponse,
  FaceWebcamEnrollResponse,
} from '../../core/models/access.model';
import { Member } from '../../core/models/member.model';
import { AccessService } from '../../core/services/access.service';
import { AuthService } from '../../core/services/auth.service';
import { MemberService } from '../../core/services/member.service';
import { buildMemberAccessMap } from '../../core/utils/member-access-status';

const ACCESS_DATE_LOCALE = 'es-CO';
const ACCESS_DATE_TZ = 'America/Bogota';

function formatAccessDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  try {
    return formatDate(value, 'dd/MM/yy, h:mm a', ACCESS_DATE_LOCALE, ACCESS_DATE_TZ);
  } catch {
    return '—';
  }
}

@Component({
  selector: 'app-access-control',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    FaceWebcamCaptureComponent,
    MemberSearchSelectComponent,
    DataTableComponent,
  ],
  templateUrl: './access-control.html',
  styleUrl: './access-control.scss',
})
export class AccessControlPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accessService = inject(AccessService);
  private readonly memberService = inject(MemberService);
  private readonly auth = inject(AuthService);

  protected readonly typeLabels = BIOMETRIC_TYPE_LABELS;
  protected readonly isSuperAdmin = () => this.auth.isSuperAdmin();
  protected readonly message = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly clearingLogs = signal(false);
  protected readonly members = signal<Member[]>([]);
  protected readonly enrollments = signal<BiometricEnrollResponse[]>([]);
  protected readonly faceEnrollments = signal<FaceWebcamEnrollResponse[]>([]);
  protected readonly logs = signal<AccessLogEntry[]>([]);
  protected readonly faceMemberId = signal<number | null>(null);
  protected readonly faceStatus = signal('Busca un afiliado por nombre o cédula y mira la cámara del lector biométrico.');
  protected readonly section = signal<'register' | 'registered' | 'logs'>('register');

  private readonly faceCapture = viewChild(FaceWebcamCaptureComponent);

  protected readonly fingerprintCount = computed(() => this.fingerprintEnrollments().length);
  protected readonly faceCount = computed(() => this.faceEnrollments().length);
  protected readonly logsCount = computed(() => this.logs().length);

  protected readonly fingerprintEnrollments = computed(() =>
    this.enrollments().filter((e) => e.credentialType === 'FINGERPRINT'),
  );

  protected readonly selectedFaceMember = computed(() => {
    const id = this.faceMemberId();
    if (id == null) {
      return undefined;
    }
    return this.members().find((m) => m.id === id);
  });

  protected readonly accessByMemberId = computed(() =>
    buildMemberAccessMap(this.enrollments(), this.faceEnrollments()),
  );

  protected readonly fingerprintColumns: DataTableColumn<BiometricEnrollResponse>[] = [
    {
      id: 'name',
      header: 'Afiliado',
      sortable: true,
      sortValue: (r) => r.memberName,
      cell: (r) => r.memberName,
    },
    {
      id: 'device',
      header: 'ID lector',
      sortable: true,
      sortValue: (r) => r.deviceUserId,
      cell: (r) => r.deviceUserId,
    },
    {
      id: 'label',
      header: 'Nota',
      cell: (r) => r.deviceLabel ?? '—',
    },
    {
      id: 'date',
      header: 'Vinculado',
      sortable: true,
      sortValue: (r) => r.enrolledAt,
      cell: (r) => formatAccessDateTime(r.enrolledAt),
    },
  ];

  protected readonly faceColumns: DataTableColumn<FaceWebcamEnrollResponse>[] = [
    {
      id: 'name',
      header: 'Afiliado',
      sortable: true,
      sortValue: (r) => r.memberName,
      cell: (r) => r.memberName,
    },
    {
      id: 'doc',
      header: 'Cédula',
      sortable: true,
      sortValue: (r) => r.documentId ?? '',
      cell: (r) => r.documentId ?? '—',
    },
    {
      id: 'date',
      header: 'Registrado',
      sortable: true,
      sortValue: (r) => r.enrolledAt,
      cell: (r) => formatAccessDateTime(r.enrolledAt),
    },
  ];

  protected readonly logColumns: DataTableColumn<AccessLogEntry>[] = [
    {
      id: 'date',
      header: 'Fecha',
      sortable: true,
      sortValue: (r) => r.createdAt,
      cell: (r) => formatAccessDateTime(r.createdAt),
    },
    {
      id: 'type',
      header: 'Tipo',
      sortable: true,
      sortValue: (r) => r.credentialTypeLabel,
      cell: (r) => r.credentialTypeLabel,
    },
    {
      id: 'member',
      header: 'Afiliado',
      sortable: true,
      sortValue: (r) => r.memberName ?? '',
      cell: (r) => r.memberName ?? '—',
    },
    {
      id: 'device',
      header: 'ID lector',
      cell: (r) => r.deviceUserId ?? r.fingerprintUserId ?? '—',
    },
    {
      id: 'message',
      header: 'Mensaje',
      cell: (r) => r.message,
    },
    {
      id: 'gate',
      header: 'Torniquete',
      cell: (r) => (r.gateOpened ? '✓ Abierto' : '—'),
    },
  ];

  protected readonly enrollForm = this.fb.nonNullable.group({
    memberId: [null as number | null, Validators.required],
    deviceUserId: ['', [Validators.required, Validators.maxLength(64)]],
    credentialType: ['FINGERPRINT' as BiometricCredentialType, Validators.required],
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
    this.accessService.listWebcamEnrollments().subscribe({
      next: (w) => this.faceEnrollments.set(w),
    });
  }

  protected readonly grantedRowClass = (): string => 'row-granted';

  setSection(next: 'register' | 'registered' | 'logs'): void {
    this.section.set(next);
  }

  async enrollFace(): Promise<void> {
    const memberId = this.faceMemberId();
    if (memberId == null) {
      this.message.set('Selecciona un afiliado para registrar su rostro');
      return;
    }
    const capture = this.faceCapture();
    if (!capture) {
      return;
    }
    this.saving.set(true);
    const descriptor = await capture.captureDescriptor();
    if (!descriptor) {
      this.saving.set(false);
      this.message.set('No se pudo capturar el rostro. Mejora la luz y mira de frente.');
      return;
    }
    this.accessService.enrollWebcam(memberId, descriptor).subscribe({
      next: () => {
        this.message.set('Rostro registrado en el lector biométrico');
        this.saving.set(false);
        this.loadAll();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo guardar el rostro');
        this.saving.set(false);
      },
    });
  }

  onFaceStatus(status: string): void {
    this.faceStatus.set(status);
  }

  onFaceMemberSelected(memberId: number | null): void {
    this.faceMemberId.set(memberId);
    const member = memberId != null ? this.members().find((m) => m.id === memberId) : undefined;
    if (member) {
      const doc = member.documentId ? ` (CC ${member.documentId})` : '';
      this.faceStatus.set(
        `Registrando a ${member.firstName} ${member.lastName}${doc} en el lector biométrico. Mira la cámara.`,
      );
    } else {
      this.faceStatus.set('Busca un afiliado por nombre o cédula y mira la cámara del lector biométrico.');
    }
  }

  removeFaceEnrollment(memberId: number): void {
    if (!confirm('¿Quitar el rostro del lector biométrico de este afiliado?')) {
      return;
    }
    this.accessService.removeWebcamEnrollment(memberId).subscribe({
      next: () => {
        this.message.set('Rostro eliminado del lector biométrico');
        this.loadAll();
      },
      error: () => this.message.set('No se pudo eliminar'),
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
        deviceUserId: raw.deviceUserId,
        credentialType: raw.credentialType,
        deviceLabel: raw.deviceLabel || undefined,
      })
      .subscribe({
        next: () => {
          const label = this.typeLabels[raw.credentialType];
          this.message.set(`${label} vinculado al afiliado correctamente`);
          this.saving.set(false);
          this.enrollForm.patchValue({ deviceUserId: '', deviceLabel: '' });
          this.loadAll();
        },
        error: (err) => {
          this.message.set(err?.error?.message ?? 'No se pudo registrar');
          this.saving.set(false);
        },
      });
  }

  removeEnrollment(memberId: number, type: BiometricCredentialType): void {
    const label = this.typeLabels[type].toLowerCase();
    if (!confirm(`¿Quitar la ${label} de este afiliado?`)) {
      return;
    }
    this.accessService.removeEnrollment(memberId, type).subscribe({
      next: () => {
        this.message.set(`${this.typeLabels[type]} eliminado`);
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

  clearAccessLogs(): void {
    if (
      !confirm(
        '¿Borrar todo el historial de ingresos? Esta acción no se puede deshacer.',
      )
    ) {
      return;
    }
    this.clearingLogs.set(true);
    this.accessService.clearLogs().subscribe({
      next: () => {
        this.message.set('Historial de ingresos limpiado');
        this.clearingLogs.set(false);
        this.logs.set([]);
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo limpiar el historial');
        this.clearingLogs.set(false);
      },
    });
  }
}
