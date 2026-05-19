import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MembershipPlan, MembershipPlanRequest } from '../../core/models/plan.model';
import { PlanService } from '../../core/services/plan.service';

@Component({
  selector: 'app-plans',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './plans.html',
  styleUrl: './plans.scss',
})
export class Plans implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly planService = inject(PlanService);

  protected readonly plans = signal<MembershipPlan[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    description: [''],
    durationDays: [30, [Validators.required, Validators.min(1)]],
    price: [0, [Validators.required, Validators.min(0)]],
    active: [true],
  });

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.loading.set(true);
    this.planService.findAll().subscribe({
      next: (plans) => {
        this.plans.set(plans);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Error al cargar planes');
        this.loading.set(false);
      },
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request: MembershipPlanRequest = this.form.getRawValue();
    this.saving.set(true);
    this.planService.create(request).subscribe({
      next: () => {
        this.message.set('Plan creado correctamente');
        this.saving.set(false);
        this.form.reset({ name: '', description: '', durationDays: 30, price: 0, active: true });
        this.loadPlans();
      },
      error: () => {
        this.message.set('No se pudo crear el plan');
        this.saving.set(false);
      },
    });
  }
}
