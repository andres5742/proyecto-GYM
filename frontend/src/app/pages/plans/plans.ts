import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MembershipPlan,
  MembershipPlanKind,
  MembershipPlanRequest,
} from '../../core/models/plan.model';
import { PlanService } from '../../core/services/plan.service';

@Component({
  selector: 'app-plans',
  imports: [ReactiveFormsModule, CopCurrencyPipe],
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
  protected readonly editingId = signal<number | null>(null);

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    description: [''],
    durationDays: [30, [Validators.required, Validators.min(1)]],
    planKind: ['REGULAR' as MembershipPlanKind, Validators.required],
    monthlyEntryLimit: [null as number | null],
    price: [0, [Validators.required, Validators.min(0)]],
    active: [true],
  });

  protected readonly isTiqueteraForm = () => this.form.controls.planKind.value === 'TIQUETERA';

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

  startCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      name: '',
      description: '',
      durationDays: 30,
      planKind: 'REGULAR',
      monthlyEntryLimit: null,
      price: 0,
      active: true,
    });
  }

  startEdit(plan: MembershipPlan): void {
    this.editingId.set(plan.id);
    this.form.patchValue({
      name: plan.name,
      description: plan.description ?? '',
      durationDays: plan.durationDays,
      planKind: plan.planKind ?? 'REGULAR',
      monthlyEntryLimit: plan.monthlyEntryLimit ?? null,
      price: plan.price,
      active: plan.active,
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const planKind = raw.planKind ?? 'REGULAR';
    if (planKind === 'TIQUETERA' && (raw.monthlyEntryLimit == null || raw.monthlyEntryLimit < 1)) {
      this.message.set('Indica el número de entrenos de la tiquetera (por ejemplo 16).');
      return;
    }

    const request: MembershipPlanRequest = {
      name: raw.name ?? '',
      description: raw.description ?? '',
      durationDays: raw.durationDays ?? 30,
      planKind,
      monthlyEntryLimit: planKind === 'TIQUETERA' ? raw.monthlyEntryLimit : null,
      price: raw.price ?? 0,
      active: raw.active ?? true,
    };
    this.saving.set(true);
    const id = this.editingId();
    const action = id ? this.planService.update(id, request) : this.planService.create(request);

    action.subscribe({
      next: () => {
        this.message.set(id ? 'Plan actualizado' : 'Plan creado correctamente');
        this.saving.set(false);
        this.startCreate();
        this.loadPlans();
      },
      error: () => {
        this.message.set(id ? 'No se pudo actualizar el plan' : 'No se pudo crear el plan');
        this.saving.set(false);
      },
    });
  }

  remove(plan: MembershipPlan): void {
    if (
      !confirm(
        `¿Eliminar el plan «${plan.name}»?\n\nLos afiliados con este plan quedarán sin plan asignado.`,
      )
    ) {
      return;
    }
    this.planService.delete(plan.id).subscribe({
      next: () => {
        this.message.set('Plan eliminado');
        if (this.editingId() === plan.id) {
          this.startCreate();
        }
        this.loadPlans();
      },
      error: () => this.message.set('No se pudo eliminar el plan'),
    });
  }
}
