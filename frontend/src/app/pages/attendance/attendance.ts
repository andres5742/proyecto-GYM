import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
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
import { todayIsoDate } from '../../core/utils/today-date';
import { AttendanceService } from '../../core/services/attendance.service';
import { EmployeeService } from '../../core/services/employee.service';

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

  protected readonly hourOptions = ATTENDANCE_HOUR_OPTIONS;
  protected readonly formatHour = formatAttendanceHour;
  protected readonly hourLabel = hourOptionLabel;

  protected readonly form = this.fb.nonNullable.group({
    employeeId: [null as number | null, Validators.required],
    workDate: [{ value: todayIsoDate(), disabled: true }, Validators.required],
    clockInHour: [null as number | null, Validators.required],
    clockOutHour: [null as number | null],
    notes: [''],
  });

  ngOnInit(): void {
    this.employeeService.findActive().subscribe({
      next: (employees) => this.employees.set(employees),
    });
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.attendanceService.findAll().subscribe({
      next: (records) => {
        this.records.set(records);
        this.loading.set(false);
      },
    });
    this.attendanceService.getSummary().subscribe({
      next: (summary) => this.summary.set(summary),
    });
  }

  isToday(workDate: string): boolean {
    return workDate === todayIsoDate();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.modalOpen()) {
      this.closeModal();
    }
  }

  openRegisterModal(): void {
    this.startCreate();
    this.modalOpen.set(true);
  }

  openEditModal(record: WorkAttendance): void {
    if (!this.isToday(record.workDate)) {
      this.message.set('Solo se pueden editar jornadas del día actual');
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
    this.form.reset({
      employeeId: null,
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
    if (record && !this.isToday(record.workDate)) {
      this.message.set('Solo se pueden eliminar jornadas del día actual');
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
      error: () => this.message.set('No se pudo eliminar'),
    });
  }
}
