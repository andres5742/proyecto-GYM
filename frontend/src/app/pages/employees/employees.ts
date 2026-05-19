import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserRole } from '../../core/models/auth.model';
import { Employee, EmployeeRequest } from '../../core/models/employee.model';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeService } from '../../core/services/employee.service';

@Component({
  selector: 'app-employees',
  imports: [ReactiveFormsModule],
  templateUrl: './employees.html',
  styleUrl: './employees.scss',
})
export class Employees implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  protected readonly auth = inject(AuthService);

  protected readonly employees = signal<Employee[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);

  private readonly allRoleOptions: { value: UserRole; label: string }[] = [
    { value: 'TRAINER', label: 'Entrenador' },
    { value: 'ADMIN', label: 'Administrador' },
    { value: 'SUPER_ADMIN', label: 'Super administrador' },
  ];

  protected get roleOptions(): { value: UserRole; label: string }[] {
    if (this.auth.hasRole('SUPER_ADMIN')) {
      return this.allRoleOptions;
    }
    return this.allRoleOptions.filter((r) => r.value !== 'SUPER_ADMIN');
  }

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: [''],
    username: [''],
    password: [''],
    role: [null as UserRole | null],
    nequiNumber: [''],
    bankName: [''],
    bankAccountNumber: [''],
    active: [true],
  });

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.loading.set(true);
    this.employeeService.findAll().subscribe({
      next: (employees) => {
        this.employees.set(employees);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Error al cargar entrenadores');
        this.loading.set(false);
      },
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      firstName: '',
      lastName: '',
      phone: '',
      username: '',
      password: '',
      role: 'TRAINER',
      nequiNumber: '',
      bankName: '',
      bankAccountNumber: '',
      active: true,
    });
  }

  startEdit(employee: Employee): void {
    this.editingId.set(employee.id);
    this.form.patchValue({
      firstName: employee.firstName,
      lastName: employee.lastName,
      phone: employee.phone ?? '',
      username: employee.username ?? '',
      password: '',
      role: employee.role ?? 'TRAINER',
      nequiNumber: employee.nequiNumber ?? '',
      bankName: employee.bankName ?? '',
      bankAccountNumber: employee.bankAccountNumber ?? '',
      active: employee.active,
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: EmployeeRequest = {
      firstName: raw.firstName,
      lastName: raw.lastName,
      phone: raw.phone || undefined,
      username: raw.username || undefined,
      password: raw.password || undefined,
      role: raw.role ?? undefined,
      nequiNumber: raw.nequiNumber || undefined,
      bankName: raw.bankName || undefined,
      bankAccountNumber: raw.bankAccountNumber || undefined,
      active: raw.active,
    };

    this.saving.set(true);
    const id = this.editingId();
    const action = id
      ? this.employeeService.update(id, request)
      : this.employeeService.create(request);

    action.subscribe({
      next: () => {
        this.message.set(id ? 'Entrenador actualizado' : 'Entrenador registrado');
        this.saving.set(false);
        this.startCreate();
        this.loadEmployees();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo guardar el entrenador');
        this.saving.set(false);
      },
    });
  }

  remove(id: number): void {
    if (!confirm('¿Eliminar este entrenador?')) {
      return;
    }
    this.employeeService.delete(id).subscribe({
      next: () => {
        this.message.set('Entrenador eliminado');
        this.loadEmployees();
      },
      error: () => this.message.set('No se pudo eliminar. Puede tener ventas asociadas.'),
    });
  }
}
