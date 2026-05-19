import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, HostListener, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WorkAttendance } from '../../core/models/attendance.model';
import { Employee } from '../../core/models/employee.model';
import {
  ATTENDANCE_HOUR_OPTIONS,
  dateTimeFromHour,
  formatAttendanceHour,
  hourFromIso,
  hourOptionLabel,
} from '../../core/utils/attendance-hour';
import {
  currentYearMonth,
  historyYearOptions,
  monthYearLabel,
  MONTH_OPTIONS,
} from '../../core/utils/attendance-period';
import { todayIsoDate } from '../../core/utils/today-date';
import { AttendanceQuery, AttendanceService } from '../../core/services/attendance.service';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeService } from '../../core/services/employee.service';

type AttendanceView = 'day' | 'month' | 'history';

@Component({
  selector: 'app-attendance',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './attendance.html',
  styleUrl: './attendance.scss',
})
export class Attendance implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly attendanceService = inject(AttendanceService);
  private readonly employeeService = inject(EmployeeService);
  protected readonly auth = inject(AuthService);

  protected readonly employees = signal<Employee[]>([]);
  protected readonly records = signal<WorkAttendance[]>([]);
  protected readonly summary = signal({
    totalRecords: 0,
    openRecords: 0,
    totalHours: 0,
    totalOwed: 0,
  });
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly modalOpen = signal(false);
  protected readonly historyModalOpen = signal(false);

  protected readonly view = signal<AttendanceView>('day');
  protected readonly historyYear = signal(currentYearMonth().year);
  protected readonly historyMonth = signal(currentYearMonth().month);

  protected readonly hourOptions = ATTENDANCE_HOUR_OPTIONS;
  protected readonly formatHour = formatAttendanceHour;
  protected readonly hourLabel = hourOptionLabel;
  protected readonly monthOptions = MONTH_OPTIONS;
  protected readonly historyYears = historyYearOptions();

  protected readonly periodLabel = computed(() => {
    const mode = this.view();
    if (mode === 'day') {
      return 'Hoy';
    }
    const y = this.historyYear();
    const m = this.historyMonth();
    return monthYearLabel(m, y);
  });

  protected readonly canManageToday = computed(() => this.view() === 'day');

  protected readonly form = this.fb.nonNullable.group({
    employeeId: [null as number | null, Validators.required],
    workDate: [{ value: todayIsoDate(), disabled: true }, Validators.required],
    clockInHour: [null as number | null, Validators.required],
    clockOutHour: [null as number | null],
    notes: [''],
  });

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.employeeService.findActive().subscribe({
        next: (employees) => this.employees.set(employees),
      });
    } else {
      const user = this.auth.currentUser();
      if (user) {
        this.form.controls.employeeId.setValue(user.employeeId);
      }
    }
    this.loadData();
  }

  loadData(): void {
    const query = this.buildQuery();
    this.loading.set(true);
    this.attendanceService.findAll(query).subscribe({
      next: (records) => {
        this.records.set(records);
        this.loading.set(false);
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudieron cargar las jornadas');
        this.loading.set(false);
      },
    });
    this.attendanceService.getSummary(query).subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => {},
    });
  }

  setView(mode: AttendanceView): void {
    this.view.set(mode);
    if (mode === 'month') {
      const { year, month } = currentYearMonth();
      this.historyYear.set(year);
      this.historyMonth.set(month);
    }
    this.loadData();
  }

  openHistoryModal(): void {
    const { year, month } = currentYearMonth();
    const prev = month === 1 ? { year: year - 1, month: 12 } : { year, month: month - 1 };
    this.historyYear.set(prev.year);
    this.historyMonth.set(prev.month);
    this.historyModalOpen.set(true);
  }

  applyHistory(): void {
    this.view.set('history');
    this.historyModalOpen.set(false);
    this.loadData();
  }

  backToCurrentMonth(): void {
    this.setView('month');
  }

  isToday(workDate: string): boolean {
    return workDate === todayIsoDate();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.historyModalOpen()) {
      this.historyModalOpen.set(false);
      return;
    }
    if (this.modalOpen()) {
      this.closeModal();
    }
  }

  openRegisterModal(): void {
    if (!this.canManageToday()) {
      return;
    }
    this.startCreate();
    this.modalOpen.set(true);
  }

  openEditModal(record: WorkAttendance): void {
    if (!this.canManageToday() || !this.isToday(record.workDate)) {
      this.message.set('Solo se pueden editar jornadas del día actual');
      return;
    }
    if (!this.auth.isAdmin() && record.employeeId !== this.auth.currentUser()?.employeeId) {
      this.message.set('Solo puedes editar tus propias jornadas');
      return;
    }
    this.editingId.set(record.id);
    this.form.patchValue({
      employeeId: record.employeeId,
      workDate: todayIsoDate(),
      clockInHour: hourFromIso(record.clockIn),
      clockOutHour: hourFromIso(record.clockOut),
      notes: record.notes ?? '',
    });
    this.form.controls.workDate.disable();
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.startCreate();
  }

  startCreate(): void {
    this.editingId.set(null);
    const defaultEmployee = this.auth.isAdmin()
      ? null
      : (this.auth.currentUser()?.employeeId ?? null);
    this.form.reset({
      employeeId: defaultEmployee,
      workDate: todayIsoDate(),
      clockInHour: null,
      clockOutHour: null,
      notes: '',
    });
    this.form.controls.workDate.disable();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    if (raw.employeeId == null || raw.clockInHour == null) {
      return;
    }

    if (raw.clockOutHour != null && raw.clockOutHour <= raw.clockInHour) {
      this.message.set('La hora de salida debe ser posterior a la de entrada');
      return;
    }

    const request = {
      employeeId: raw.employeeId,
      workDate: todayIsoDate(),
      clockIn: dateTimeFromHour(raw.clockInHour),
      clockOut: raw.clockOutHour != null ? dateTimeFromHour(raw.clockOutHour) : undefined,
      notes: raw.notes || undefined,
    };

    this.saving.set(true);
    const id = this.editingId();
    const action = id
      ? this.attendanceService.update(id, request)
      : this.attendanceService.create(request);

    action.subscribe({
      next: () => {
        this.message.set(id ? 'Jornada actualizada' : 'Jornada registrada');
        this.saving.set(false);
        this.modalOpen.set(false);
        this.startCreate();
        this.loadData();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo guardar');
        this.saving.set(false);
      },
    });
  }

  remove(id: number): void {
    const record = this.records().find((r) => r.id === id);
    if (!record || !this.canManageToday() || !this.isToday(record.workDate)) {
      this.message.set('Solo se pueden eliminar jornadas del día actual');
      return;
    }
    if (!this.auth.isAdmin() && record.employeeId !== this.auth.currentUser()?.employeeId) {
      this.message.set('Solo puedes eliminar tus propias jornadas');
      return;
    }
    if (!confirm('¿Eliminar este registro de jornada?')) {
      return;
    }
    this.attendanceService.delete(id).subscribe({
      next: () => {
        this.message.set('Registro eliminado');
        this.loadData();
      },
      error: (err) => this.message.set(err?.error?.message ?? 'No se pudo eliminar'),
    });
  }

  canEditRecord(record: WorkAttendance): boolean {
    return (
      this.canManageToday() &&
      this.isToday(record.workDate) &&
      (this.auth.isAdmin() || record.employeeId === this.auth.currentUser()?.employeeId)
    );
  }

  employeeInitial(fullName: string): string {
    const parts = fullName.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }

  private buildQuery(): AttendanceQuery {
    if (this.view() === 'day') {
      return { date: todayIsoDate() };
    }
    return {
      year: this.historyYear(),
      month: this.historyMonth(),
    };
  }
}
