import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Member, MemberRequest, MembershipStatus } from '../../core/models/member.model';
import { MembershipPlan } from '../../core/models/plan.model';
import { MemberService } from '../../core/services/member.service';
import { PlanService } from '../../core/services/plan.service';

@Component({
  selector: 'app-members',
  imports: [ReactiveFormsModule, DatePipe, CurrencyPipe],
  templateUrl: './members.html',
  styleUrl: './members.scss',
})
export class Members implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly memberService = inject(MemberService);
  private readonly planService = inject(PlanService);

  protected readonly members = signal<Member[]>([]);
  protected readonly plans = signal<MembershipPlan[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);

  protected readonly statuses: MembershipStatus[] = ['ACTIVE', 'EXPIRED', 'SUSPENDED'];

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    documentId: [''],
    planId: [null as number | null],
    status: ['ACTIVE' as MembershipStatus, Validators.required],
    membershipStart: [''],
    membershipEnd: [''],
  });

  ngOnInit(): void {
    this.loadData();
    this.planService.findAll().subscribe({
      next: (plans) => this.plans.set(plans),
    });
  }

  loadData(): void {
    this.loading.set(true);
    this.memberService.findAll().subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Error al cargar socios. ¿Está el backend en marcha?');
        this.loading.set(false);
      },
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      documentId: '',
      planId: null,
      status: 'ACTIVE',
      membershipStart: '',
      membershipEnd: '',
    });
  }

  startEdit(member: Member): void {
    this.editingId.set(member.id);
    this.form.patchValue({
      firstName: member.firstName,
      lastName: member.lastName,
      email: member.email,
      phone: member.phone ?? '',
      documentId: member.documentId ?? '',
      planId: member.planId ?? null,
      status: member.status,
      membershipStart: member.membershipStart ?? '',
      membershipEnd: member.membershipEnd ?? '',
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: MemberRequest = {
      firstName: raw.firstName,
      lastName: raw.lastName,
      email: raw.email,
      phone: raw.phone || undefined,
      documentId: raw.documentId || undefined,
      planId: raw.planId,
      status: raw.status,
      membershipStart: raw.membershipStart || undefined,
      membershipEnd: raw.membershipEnd || undefined,
    };

    this.saving.set(true);
    const id = this.editingId();
    const action = id
      ? this.memberService.update(id, request)
      : this.memberService.create(request);

    action.subscribe({
      next: () => {
        this.message.set(id ? 'Socio actualizado' : 'Socio registrado');
        this.saving.set(false);
        this.startCreate();
        this.loadData();
      },
      error: () => {
        this.message.set('No se pudo guardar el socio');
        this.saving.set(false);
      },
    });
  }

  remove(id: number): void {
    if (!confirm('¿Eliminar este socio?')) {
      return;
    }
    this.memberService.delete(id).subscribe({
      next: () => {
        this.message.set('Socio eliminado');
        this.loadData();
      },
      error: () => this.message.set('No se pudo eliminar el socio'),
    });
  }

  statusLabel(status: MembershipStatus): string {
    const labels: Record<MembershipStatus, string> = {
      ACTIVE: 'Activo',
      EXPIRED: 'Vencido',
      SUSPENDED: 'Suspendido',
    };
    return labels[status];
  }
}
