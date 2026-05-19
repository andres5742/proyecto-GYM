import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PayrollService } from '../../core/services/payroll.service';

@Component({
  selector: 'app-payroll-config',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './payroll-config.html',
  styleUrl: './payroll-config.scss',
})
export class PayrollConfigPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly payrollService = inject(PayrollService);

  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly lastUpdated = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    weekdayHourlyRate: [0, [Validators.required, Validators.min(0)]],
    sundayHourlyRate: [0, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.payrollService.get().subscribe({
      next: (config) => {
        this.form.patchValue({
          weekdayHourlyRate: config.weekdayHourlyRate,
          sundayHourlyRate: config.sundayHourlyRate,
        });
        this.lastUpdated.set(config.updatedAt);
      },
      error: () => this.message.set('No se pudo cargar la configuración'),
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.payrollService.update(this.form.getRawValue()).subscribe({
      next: (config) => {
        this.message.set('Tarifas actualizadas');
        this.lastUpdated.set(config.updatedAt);
        this.saving.set(false);
      },
      error: () => {
        this.message.set('No se pudo guardar');
        this.saving.set(false);
      },
    });
  }
}
