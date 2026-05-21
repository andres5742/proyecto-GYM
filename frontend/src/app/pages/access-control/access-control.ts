import { formatDate } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
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
  isMemberPerson,
  isStaffPerson,
} from '../../core/models/access.model';
import { Employee } from '../../core/models/employee.model';
import { Member } from '../../core/models/member.model';
import { AccessService } from '../../core/services/access.service';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeService } from '../../core/services/employee.service';
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

const CAPTURE_POLL_MS = 1000;

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
export class AccessControlPage implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly accessService = inject(AccessService);
  private readonly memberService = inject(MemberService);
  private readonly employeeService = inject(EmployeeService);
  private readonly auth = inject(AuthService);

  protected readonly typeLabels = BIOMETRIC_TYPE_LABELS;
  protected readonly isSuperAdmin = () => this.auth.isSuperAdmin();
  protected readonly message = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly clearingLogs = signal(false);
  protected readonly members = signal<Member[]>([]);
  protected readonly trainers = signal<Employee[]>([]);
  protected readonly enrollments = signal<BiometricEnrollResponse[]>([]);
  protected readonly faceEnrollments = signal<FaceWebcamEnrollResponse[]>([]);
  protected readonly logs = signal<AccessLogEntry[]>([]);
  protected readonly faceMemberId = signal<number | null>(null);
  protected readonly staffFaceEmployeeId = signal<number | null>(null);
  protected readonly lastCapturedPin = signal<string | null>(null);
  protected readonly captureWaiting = signal(false);

  private capturePollTimer: ReturnType<typeof setInterval> | null = null;
  private captureSinceIso = new Date().toISOString();
  private lastCaptureLogId = 0;
  protected readonly faceStatus = signal('Busca un afiliado por nombre o cédula y mira la cámara del lector biométrico.');
  protected readonly staffFaceStatus = signal('Selecciona un entrenador y mira la cámara del lector biométrico.');
  protected readonly section = signal<'register' | 'registered' | 'logs'>('register');
  protected readonly registerAudience = signal<'members' | 'staff'>('members');
  protected readonly registerMethod = signal<'fingerprint' | 'card' | 'face'>('fingerprint');

  private readonly faceCapture = viewChild('memberFaceCapture', { read: FaceWebcamCaptureComponent });
  private readonly staffFaceCapture = viewChild('staffFaceCapture', { read: FaceWebcamCaptureComponent });

  protected readonly fingerprintCount = computed(() => this.memberFingerprintEnrollments().length);
  protected readonly cardCount = computed(() => this.memberCardEnrollments().length);
  protected readonly faceCount = computed(() => this.memberFaceEnrollments().length);
  protected readonly staffFingerprintCount = computed(() => this.staffFingerprintEnrollments().length);
  protected readonly staffCardCount = computed(() => this.staffCardEnrollments().length);
  protected readonly staffFaceCount = computed(() => this.staffFaceEnrollments().length);
  protected readonly logsCount = computed(() => this.logs().length);

  protected readonly memberFingerprintEnrollments = computed(() =>
    this.enrollments().filter((e) => e.credentialType === 'FINGERPRINT' && isMemberPerson(e)),
  );

  protected readonly memberCardEnrollments = computed(() =>
    this.enrollments().filter((e) => e.credentialType === 'CARD' && isMemberPerson(e)),
  );

  protected readonly staffFingerprintEnrollments = computed(() =>
    this.enrollments()
      .filter((e) => e.credentialType === 'FINGERPRINT' && isStaffPerson(e))
      .filter((e) => this.isTrainerVisibleInDirectory(e)),
  );

  protected readonly staffCardEnrollments = computed(() =>
    this.enrollments()
      .filter((e) => e.credentialType === 'CARD' && isStaffPerson(e))
      .filter((e) => this.isTrainerVisibleInDirectory(e)),
  );

  protected readonly memberFaceEnrollments = computed(() =>
    this.faceEnrollments().filter((e) => isMemberPerson(e)),
  );

  protected readonly staffFaceEnrollments = computed(() =>
    this.faceEnrollments().filter((e) => isStaffPerson(e) && this.isTrainerVisibleInDirectory(e)),
  );

  /** @deprecated use memberFingerprintEnrollments */
  protected readonly fingerprintEnrollments = this.memberFingerprintEnrollments;

  protected readonly selectedFaceMember = computed(() => {
    const id = this.faceMemberId();
    if (id == null) {
      return undefined;
    }
    return this.members().find((m) => m.id === id);
  });

  protected readonly selectedStaffFace = computed(() => {
    const id = this.staffFaceEmployeeId();
    if (id == null) {
      return undefined;
    }
    return this.trainers().find((t) => t.id === id);
  });

  protected readonly accessByMemberId = computed(() =>
    buildMemberAccessMap(this.enrollments(), this.faceEnrollments()),
  );

  protected readonly cardColumns: DataTableColumn<BiometricEnrollResponse>[] = [
    {
      id: 'name',
      header: 'Afiliado',
      sortable: true,
      sortValue: (r) => r.memberName,
      cell: (r) => r.memberName,
    },
    {
      id: 'device',
      header: 'Nº tarjeta / Pin ZKT',
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

  protected readonly staffCardColumns: DataTableColumn<BiometricEnrollResponse>[] = [
    {
      id: 'name',
      header: 'Entrenador',
      sortable: true,
      sortValue: (r) => r.memberName,
      cell: (r) => r.memberName,
    },
    {
      id: 'device',
      header: 'Nº tarjeta / Pin ZKT',
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

  protected readonly staffFingerprintColumns: DataTableColumn<BiometricEnrollResponse>[] = [
    {
      id: 'name',
      header: 'Entrenador',
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

  protected readonly staffFaceColumns: DataTableColumn<FaceWebcamEnrollResponse>[] = [
    {
      id: 'name',
      header: 'Entrenador',
      sortable: true,
      sortValue: (r) => r.memberName,
      cell: (r) => r.memberName,
    },
    {
      id: 'date',
      header: 'Registrado',
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
      id: 'result',
      header: 'Resultado',
      sortable: true,
      sortValue: (r) => r.result,
      cell: (r) => r.resultLabel,
      cellInnerClass: (r) => (r.result === 'GRANTED' ? 'log-result log-result--ok' : 'log-result log-result--deny'),
    },
    {
      id: 'type',
      header: 'Tipo',
      sortable: true,
      sortValue: (r) => r.credentialTypeLabel,
      cell: (r) => r.credentialTypeLabel,
    },
    {
      id: 'code',
      header: 'Código tarjeta / ID',
      sortable: true,
      sortValue: (r) => this.logDeviceCode(r),
      cell: (r) => this.logDeviceCode(r),
      cellClass: () => 'log-code-cell',
      cellInnerClass: (r) =>
        r.credentialType === 'CARD' ? 'log-code log-code--card' : 'log-code',
    },
    {
      id: 'member',
      header: 'Afiliado',
      sortable: true,
      sortValue: (r) => r.memberName ?? '',
      cell: (r) => r.memberName ?? '—',
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

  protected readonly staffEnrollForm = this.fb.nonNullable.group({
    employeeId: [null as number | null, Validators.required],
    deviceUserId: ['', [Validators.required, Validators.maxLength(64)]],
    deviceLabel: [''],
  });

  ngOnInit(): void {
    this.loadAll();
    this.syncCapturePolling();
  }

  ngOnDestroy(): void {
    this.stopCapturePolling();
  }

  loadAll(): void {
    this.memberService.findAll().subscribe({
      next: (m) => this.members.set(m),
    });
    this.employeeService.findActive().subscribe({
      next: (t) => {
        const visible = this.auth.hasRole('SUPER_ADMIN')
          ? t
          : t.filter((e) => e.role !== 'SUPER_ADMIN');
        this.trainers.set(visible);
      },
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

  setSection(next: 'register' | 'registered' | 'logs'): void {
    this.section.set(next);
    this.syncCapturePolling();
  }

  setRegisterAudience(audience: 'members' | 'staff'): void {
    this.registerAudience.set(audience);
    this.syncCapturePolling();
  }

  setRegisterMethod(method: 'fingerprint' | 'card' | 'face'): void {
    this.registerMethod.set(method);
    if (method === 'card') {
      this.enrollForm.patchValue({ credentialType: 'CARD' });
    } else if (method === 'fingerprint') {
      this.enrollForm.patchValue({ credentialType: 'FINGERPRINT' });
    }
    this.syncCapturePolling();
  }

  protected captureActive(): boolean {
    if (this.section() === 'logs') {
      return true;
    }
    return (
      this.section() === 'register' &&
      (this.registerMethod() === 'card' || this.registerMethod() === 'fingerprint')
    );
  }

  protected logDeviceCode(row: AccessLogEntry): string {
    const code = row.deviceUserId ?? row.fingerprintUserId ?? '';
    return code.trim() || '—';
  }

  protected logRowClass(row: AccessLogEntry): string {
    return row.result === 'GRANTED' ? 'row-granted' : 'row-denied';
  }

  protected goVincularCapturedPin(): void {
    const pin = this.lastCapturedPin();
    if (!pin) {
      return;
    }
    this.setSection('register');
    this.setRegisterAudience('members');
    this.setRegisterMethod('card');
    this.enrollForm.patchValue({ deviceUserId: pin, credentialType: 'CARD' });
    this.message.set(`Código ${pin} listo para vincular al afiliado`);
  }

  protected linkCardFromLog(row: AccessLogEntry): void {
    const pin = this.logDeviceCode(row);
    if (pin === '—') {
      return;
    }
    this.lastCapturedPin.set(pin);
    this.setSection('register');
    this.setRegisterAudience('members');
    this.setRegisterMethod(row.credentialType === 'CARD' ? 'card' : 'fingerprint');
    if (row.credentialType === 'CARD') {
      this.enrollForm.patchValue({ deviceUserId: pin, credentialType: 'CARD' });
    } else {
      this.enrollForm.patchValue({ deviceUserId: pin, credentialType: 'FINGERPRINT' });
    }
    this.message.set(`Código ${pin} listo para vincular al afiliado`);
  }

  private syncCapturePolling(): void {
    if (this.captureActive()) {
      this.startCapturePolling();
    } else {
      this.stopCapturePolling();
    }
  }

  private startCapturePolling(): void {
    this.stopCapturePolling();
    this.captureSinceIso = new Date().toISOString();
    this.lastCaptureLogId = 0;
    this.lastCapturedPin.set(null);
    this.captureWaiting.set(true);
    void this.pollLastRead();
    this.capturePollTimer = setInterval(() => void this.pollLastRead(), CAPTURE_POLL_MS);
  }

  private stopCapturePolling(): void {
    if (this.capturePollTimer) {
      clearInterval(this.capturePollTimer);
      this.capturePollTimer = null;
    }
    this.captureWaiting.set(false);
  }

  private pollLastRead(): void {
    this.accessService.lastDeviceRead(this.captureSinceIso).subscribe({
      next: (read) => {
        if (!read?.pin?.trim() || read.logId <= this.lastCaptureLogId) {
          return;
        }
        this.lastCaptureLogId = read.logId;
        const pin = read.pin.trim();
        this.lastCapturedPin.set(pin);
        this.captureWaiting.set(false);
        if (this.registerAudience() === 'members') {
          this.enrollForm.patchValue({ deviceUserId: pin });
        } else {
          this.staffEnrollForm.patchValue({ deviceUserId: pin });
        }
        this.message.set(`Lectura capturada: ${pin} (${read.credentialTypeLabel})`);
        this.accessService.logs().subscribe({
          next: (l) => this.logs.set(l),
        });
      },
    });
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

  async enrollStaffFace(): Promise<void> {
    const employeeId = this.staffFaceEmployeeId();
    if (employeeId == null) {
      this.message.set('Selecciona un entrenador para registrar su rostro');
      return;
    }
    const capture = this.staffFaceCapture() ?? this.faceCapture();
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
    this.accessService.enrollStaffWebcam(employeeId, descriptor).subscribe({
      next: () => {
        this.message.set('Rostro del entrenador registrado');
        this.saving.set(false);
        this.loadAll();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo guardar el rostro');
        this.saving.set(false);
      },
    });
  }

  onStaffFaceStatus(status: string): void {
    this.staffFaceStatus.set(status);
  }

  onStaffFaceSelected(employeeId: number | null): void {
    this.staffFaceEmployeeId.set(employeeId);
    const trainer = employeeId != null ? this.trainers().find((t) => t.id === employeeId) : undefined;
    if (trainer) {
      this.staffFaceStatus.set(
        `Registrando a ${trainer.firstName} ${trainer.lastName} en el lector biométrico. Mira la cámara.`,
      );
    } else {
      this.staffFaceStatus.set('Selecciona un entrenador y mira la cámara del lector biométrico.');
    }
  }

  removeStaffFaceEnrollment(employeeId: number): void {
    if (!confirm('¿Quitar el rostro del lector biométrico de este entrenador?')) {
      return;
    }
    this.accessService.removeStaffWebcamEnrollment(employeeId).subscribe({
      next: () => {
        this.message.set('Rostro del entrenador eliminado');
        this.loadAll();
      },
      error: () => this.message.set('No se pudo eliminar'),
    });
  }

  enrollStaff(): void {
    if (this.staffEnrollForm.invalid) {
      this.staffEnrollForm.markAllAsTouched();
      return;
    }
    const raw = this.staffEnrollForm.getRawValue();
    if (raw.employeeId == null) {
      return;
    }
    this.saving.set(true);
    const credType: BiometricCredentialType =
      this.registerMethod() === 'card' ? 'CARD' : 'FINGERPRINT';
    this.accessService
      .enrollStaff({
        employeeId: raw.employeeId,
        deviceUserId: raw.deviceUserId,
        credentialType: credType,
        deviceLabel: raw.deviceLabel || undefined,
      })
      .subscribe({
        next: () => {
          this.message.set(`${this.typeLabels[credType]} vinculada al entrenador correctamente`);
          this.saving.set(false);
          this.staffEnrollForm.patchValue({ deviceUserId: '', deviceLabel: '' });
          this.loadAll();
        },
        error: (err) => {
          this.message.set(err?.error?.message ?? 'No se pudo registrar');
          this.saving.set(false);
        },
      });
  }

  removeStaffEnrollment(employeeId: number, type: BiometricCredentialType): void {
    const label = this.typeLabels[type].toLowerCase();
    if (!confirm(`¿Quitar la ${label} de este entrenador?`)) {
      return;
    }
    this.accessService.removeStaffEnrollment(employeeId, type).subscribe({
      next: () => {
        this.message.set(`${this.typeLabels[type]} eliminado`);
        this.loadAll();
      },
      error: () => this.message.set('No se pudo eliminar'),
    });
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

  private isTrainerVisibleInDirectory(
    entry: Pick<BiometricEnrollResponse | FaceWebcamEnrollResponse, 'personType' | 'employeeId'>,
  ): boolean {
    if (!isStaffPerson(entry)) {
      return true;
    }
    if (this.auth.hasRole('SUPER_ADMIN')) {
      return true;
    }
    const empId = entry.employeeId;
    if (empId == null) {
      return true;
    }
    return this.trainers().some((t) => t.id === empId);
  }
}
