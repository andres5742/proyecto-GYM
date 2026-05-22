import { formatDate } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import Swal from 'sweetalert2';
import { DataTableComponent } from '../../components/data-table/data-table';
import { DataTableColumn } from '../../components/data-table/data-table.model';
import { MemberSearchSelectComponent } from '../../components/member-search-select/member-search-select';
import {
  AccessLogEntry,
  BIOMETRIC_TYPE_LABELS,
  BiometricCredentialType,
  BiometricEnrollResponse,
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
/** Tu PC (panel web) ve al instante lo que pasa en el .exe de entrada. */
const LOGS_POLL_MS = 2000;

@Component({
  selector: 'app-access-control',
  imports: [ReactiveFormsModule, RouterLink, MemberSearchSelectComponent, DataTableComponent],
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
  protected readonly logs = signal<AccessLogEntry[]>([]);
  protected readonly lastCapturedPin = signal<string | null>(null);
  protected readonly captureWaiting = signal(false);

  private capturePollTimer: ReturnType<typeof setInterval> | null = null;
  private logsPollTimer: ReturnType<typeof setInterval> | null = null;
  private captureSinceIso = new Date().toISOString();
  private lastCaptureLogId = 0;
  private lastSeenLogId = 0;
  private logsPollInitialized = false;
  protected readonly section = signal<'register' | 'registered' | 'logs'>('register');
  protected readonly registerAudience = signal<'members' | 'staff'>('members');
  protected readonly registerMethod = signal<'fingerprint' | 'card'>('card');

  protected readonly fingerprintCount = computed(() => this.memberFingerprintEnrollments().length);
  protected readonly cardCount = computed(() => this.memberCardEnrollments().length);
  protected readonly staffFingerprintCount = computed(() => this.staffFingerprintEnrollments().length);
  protected readonly staffCardCount = computed(() => this.staffCardEnrollments().length);
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

  /** @deprecated use memberFingerprintEnrollments */
  protected readonly fingerprintEnrollments = this.memberFingerprintEnrollments;

  protected readonly accessByMemberId = computed(() => buildMemberAccessMap(this.enrollments()));

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
      cell: (r) => this.logPersonLabel(r),
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
    credentialType: ['CARD' as BiometricCredentialType, Validators.required],
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
    this.syncLogsPolling();
  }

  ngOnDestroy(): void {
    this.stopCapturePolling();
    this.stopLogsPolling();
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
  }

  setSection(next: 'register' | 'registered' | 'logs'): void {
    this.section.set(next);
    this.syncCapturePolling();
    this.syncLogsPolling();
  }

  setRegisterAudience(audience: 'members' | 'staff'): void {
    this.registerAudience.set(audience);
    this.syncCapturePolling();
  }

  setRegisterMethod(method: 'fingerprint' | 'card'): void {
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
    const t = code.trim();
    if (t === 'F2-ENTRENO') {
      return 'F2 · Entreno del día';
    }
    if (t === 'F3-BAILES') {
      return 'F3 · Bailes deportivos';
    }
    return t || '—';
  }

  protected logPersonLabel(row: AccessLogEntry): string {
    if (row.memberName?.trim()) {
      return row.memberName;
    }
    const code = (row.deviceUserId ?? '').trim();
    if (code === 'F2-ENTRENO') {
      return 'Pase entreno (PC entrada)';
    }
    if (code === 'F3-BAILES') {
      return 'Pase bailes (PC entrada)';
    }
    return '—';
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
    this.setRegisterMethod('card');
    this.enrollForm.patchValue({ deviceUserId: pin, credentialType: 'CARD' });
    this.message.set(`Código ${pin} listo para vincular al afiliado`);
  }

  private syncCapturePolling(): void {
    if (this.captureActive()) {
      this.startCapturePolling();
    } else {
      this.stopCapturePolling();
    }
  }

  private syncLogsPolling(): void {
    if (this.section() === 'logs') {
      this.startLogsPolling();
    } else {
      this.stopLogsPolling();
    }
  }

  private startLogsPolling(): void {
    this.stopLogsPolling();
    void this.refreshLogs(false);
    this.logsPollTimer = setInterval(() => void this.refreshLogs(true), LOGS_POLL_MS);
  }

  private stopLogsPolling(): void {
    if (this.logsPollTimer) {
      clearInterval(this.logsPollTimer);
      this.logsPollTimer = null;
    }
  }

  private refreshLogs(notifyOnNew: boolean): void {
    this.accessService.logs().subscribe({
      next: (list) => {
        const maxId = list.reduce((m, row) => Math.max(m, row.id ?? 0), 0);
        if (notifyOnNew && this.logsPollInitialized && maxId > this.lastSeenLogId) {
          const latest = list.find((r) => r.id === maxId);
          const who = latest ? this.logPersonLabel(latest) : 'Entrada';
          this.message.set(`Nuevo ingreso en la puerta: ${who}`);
        }
        this.lastSeenLogId = maxId;
        this.logsPollInitialized = true;
        this.logs.set(list);
      },
    });
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
        const isSpecialPass = pin === 'F2-ENTRENO' || pin === 'F3-BAILES';
        this.lastCapturedPin.set(pin);
        this.captureWaiting.set(false);
        if (!isSpecialPass) {
          if (this.section() === 'logs') {
            this.setSection('register');
            this.setRegisterAudience('members');
          }
          this.setRegisterMethod('card');
        }
        if (this.registerAudience() === 'members') {
          this.enrollForm.patchValue({
            deviceUserId: pin,
            credentialType: isSpecialPass ? this.enrollForm.getRawValue().credentialType : 'CARD',
          });
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
          this.saving.set(false);
          this.loadAll();
          if (credType === 'CARD') {
            this.resetStaffLinkForm();
            this.showLinkSuccessSwal();
          } else {
            this.message.set(`${this.typeLabels[credType]} vinculada al entrenador correctamente`);
            this.staffEnrollForm.patchValue({ deviceUserId: '', deviceLabel: '' });
          }
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
          this.saving.set(false);
          this.loadAll();
          if (raw.credentialType === 'CARD') {
            this.resetMemberLinkForm();
            this.showLinkSuccessSwal();
          } else {
            const label = this.typeLabels[raw.credentialType];
            this.message.set(`${label} vinculado al afiliado correctamente`);
            this.enrollForm.patchValue({ deviceUserId: '', deviceLabel: '' });
          }
        },
        error: (err) => {
          this.message.set(err?.error?.message ?? 'No se pudo registrar');
          this.saving.set(false);
        },
      });
  }

  private showLinkSuccessSwal(): void {
    void Swal.fire({
      icon: 'success',
      title: 'Vinculación exitosa',
      confirmButtonText: 'Aceptar',
      confirmButtonColor: '#d4623a',
    });
  }

  private resetMemberLinkForm(): void {
    this.enrollForm.reset({
      memberId: null,
      deviceUserId: '',
      credentialType: 'CARD',
      deviceLabel: '',
    });
    this.lastCapturedPin.set(null);
    this.message.set(null);
    if (this.captureActive()) {
      this.captureWaiting.set(true);
    }
  }

  private resetStaffLinkForm(): void {
    this.staffEnrollForm.reset({
      employeeId: null,
      deviceUserId: '',
      deviceLabel: '',
    });
    this.lastCapturedPin.set(null);
    this.message.set(null);
    if (this.captureActive()) {
      this.captureWaiting.set(true);
    }
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
    entry: Pick<BiometricEnrollResponse, 'personType' | 'employeeId'>,
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
