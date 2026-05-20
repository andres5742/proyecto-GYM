import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Gender, Member, MemberRequest, MembershipStatus } from '../../core/models/member.model';
import { MembershipPlan } from '../../core/models/plan.model';
import { AuthService } from '../../core/services/auth.service';
import { AccessService } from '../../core/services/access.service';
import { MemberService } from '../../core/services/member.service';
import { PlanService } from '../../core/services/plan.service';
import { buildMemberAccessMap } from '../../core/utils/member-access-status';
import { MemberAccessBadgesComponent } from '../../components/member-access-badges/member-access-badges';

type SortColumn = 'name' | 'documentId' | 'gender' | 'planName' | 'status' | 'membershipEnd';

const PAGE_SIZES = [10, 25, 50, 100] as const;

@Component({
  selector: 'app-members',
  imports: [ReactiveFormsModule, DatePipe, MemberAccessBadgesComponent, RouterLink],
  templateUrl: './members.html',
  styleUrl: './members.scss',
})
export class Members implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly memberService = inject(MemberService);
  private readonly planService = inject(PlanService);
  private readonly accessService = inject(AccessService);

  protected readonly isSuperAdmin = () => this.auth.isSuperAdmin();
  protected readonly isAdmin = () => this.auth.isAdmin();
  protected readonly canEditMembershipEnd = () => this.auth.isAdmin();

  protected readonly members = signal<Member[]>([]);
  protected readonly accessByMemberId = signal(buildMemberAccessMap([], []));
  protected readonly plans = signal<MembershipPlan[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly editingMember = signal<Member | null>(null);
  protected readonly freezeAction = signal(false);
  protected readonly importing = signal(false);
  protected readonly clearingAll = signal(false);
  protected readonly selectedFileName = signal<string | null>(null);
  protected readonly importErrors = signal<string[]>([]);
  protected readonly portalPassword = signal('');
  protected readonly portalPasswordSaving = signal(false);

  protected readonly searchQuery = signal('');
  protected readonly statusFilter = signal<'ALL' | MembershipStatus>('ALL');
  protected readonly sortColumn = signal<SortColumn>('name');
  protected readonly sortDirection = signal<'asc' | 'desc'>('asc');
  protected readonly page = signal(1);
  protected readonly pageSize = signal<number>(10);
  protected readonly pageSizeOptions = PAGE_SIZES;

  protected readonly statuses: MembershipStatus[] = ['ACTIVE', 'EXPIRED', 'SUSPENDED'];
  protected readonly genders: { value: Gender | ''; label: string }[] = [
    { value: '', label: 'Sin especificar' },
    { value: 'MALE', label: 'Masculino' },
    { value: 'FEMALE', label: 'Femenino' },
    { value: 'OTHER', label: 'Otro' },
  ];

  protected readonly filteredMembers = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const statusFilter = this.statusFilter();
    let list = [...this.members()];

    if (query) {
      list = list.filter((member) => {
        const haystack = [
          member.firstName,
          member.lastName,
          this.genderLabel(member.gender),
          member.documentId ?? '',
          member.phone ?? '',
          member.planName ?? '',
        ]
          .join(' ')
          .toLowerCase();
        return haystack.includes(query);
      });
    }

    if (statusFilter !== 'ALL') {
      list = list.filter((member) => this.effectiveStatus(member) === statusFilter);
    }

    const column = this.sortColumn();
    const dir = this.sortDirection() === 'asc' ? 1 : -1;
    list.sort((a, b) => dir * this.compareMembers(a, b, column));
    return list;
  });

  protected readonly totalFiltered = computed(() => this.filteredMembers().length);

  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalFiltered() / this.pageSize())),
  );

  protected readonly paginatedMembers = computed(() => {
    const totalPages = this.totalPages();
    const page = Math.min(this.page(), totalPages);
    const start = (page - 1) * this.pageSize();
    return this.filteredMembers().slice(start, start + this.pageSize());
  });

  protected readonly pageStart = computed(() =>
    this.totalFiltered() === 0 ? 0 : (Math.min(this.page(), this.totalPages()) - 1) * this.pageSize() + 1,
  );

  protected readonly pageEnd = computed(() =>
    Math.min(this.pageStart() + this.pageSize() - 1, this.totalFiltered()),
  );

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    gender: ['' as Gender | ''],
    phone: [''],
    documentId: [''],
  });

  ngOnInit(): void {
    this.loadData();
    this.loadAccessFlags();
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
        this.page.set(1);
      },
      error: () => {
        this.message.set('Error al cargar afiliados. ¿Está el backend en marcha?');
        this.loading.set(false);
      },
    });
  }

  loadAccessFlags(): void {
    this.accessService.listEnrollments().subscribe({
      next: (enrollments) => {
        this.accessService.listWebcamEnrollments().subscribe({
          next: (webcam) => this.accessByMemberId.set(buildMemberAccessMap(enrollments, webcam)),
        });
      },
    });
  }

  protected accessFlags(memberId: number) {
    return this.accessByMemberId()[memberId];
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.page.set(1);
  }

  onStatusFilterChange(value: string): void {
    this.statusFilter.set(value as 'ALL' | MembershipStatus);
    this.page.set(1);
  }

  onPageSizeChange(value: string): void {
    this.pageSize.set(Number(value));
    this.page.set(1);
  }

  sortBy(column: SortColumn): void {
    if (this.sortColumn() === column) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(column);
      this.sortDirection.set('asc');
    }
    this.page.set(1);
  }

  sortIndicator(column: SortColumn): string {
    if (this.sortColumn() !== column) {
      return '';
    }
    return this.sortDirection() === 'asc' ? ' ▲' : ' ▼';
  }

  goToPage(page: number): void {
    const clamped = Math.max(1, Math.min(page, this.totalPages()));
    this.page.set(clamped);
  }

  clearEdit(): void {
    this.editingId.set(null);
    this.editingMember.set(null);
    this.portalPassword.set('');
    this.form.reset({
      firstName: '',
      lastName: '',
      gender: '',
      phone: '',
      documentId: '',
    });
  }

  startEdit(member: Member): void {
    this.editingId.set(member.id);
    this.editingMember.set(member);
    this.portalPassword.set('');
    this.form.patchValue({
      firstName: member.firstName,
      lastName: member.lastName,
      gender: member.gender ?? '',
      phone: member.phone ?? '',
      documentId: member.documentId ?? '',
    });
  }

  save(): void {
    const id = this.editingId();
    if (id == null) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const existing = this.members().find((m) => m.id === id);
    const request: MemberRequest = {
      firstName: raw.firstName,
      lastName: raw.lastName,
      gender: raw.gender || null,
      phone: raw.phone || undefined,
      documentId: raw.documentId || undefined,
      planId: existing?.planId ?? null,
      status: existing?.status ?? 'ACTIVE',
      membershipStart: existing?.membershipStart,
      membershipEnd: existing?.membershipEnd,
    };

    this.saving.set(true);
    this.memberService.update(id, request).subscribe({
      next: () => {
        this.message.set('Cambios guardados');
        this.saving.set(false);
        this.loadData();
      },
      error: () => {
        this.message.set('No se pudo guardar el afiliado');
        this.saving.set(false);
      },
    });
  }

  remove(id: number): void {
    if (!this.isSuperAdmin()) {
      return;
    }
    if (!confirm('¿Eliminar este afiliado?')) {
      return;
    }
    this.memberService.delete(id).subscribe({
      next: () => {
        this.message.set('Afiliado eliminado');
        this.loadData();
      },
      error: () => this.message.set('No se pudo eliminar el afiliado'),
    });
  }

  clearAllMembers(): void {
    const total = this.members().length;
    if (total === 0) {
      this.message.set('No hay afiliados que borrar');
      return;
    }
    const confirmed = confirm(
      `¿Borrar los ${total} afiliados?\n\nSe eliminarán también sus huellas registradas y el historial de acceso.\nEsta acción no se puede deshacer.`,
    );
    if (!confirmed) {
      return;
    }
    const typed = prompt('Escribe BORRAR para confirmar:');
    if (typed?.trim().toUpperCase() !== 'BORRAR') {
      this.message.set('Cancelado: no se escribió la confirmación');
      return;
    }

    this.clearingAll.set(true);
    this.memberService.deleteAll().subscribe({
      next: (result) => {
        this.clearingAll.set(false);
        this.message.set(`Se eliminaron ${result.deleted} afiliado(s)`);
        this.clearEdit();
        this.loadData();
      },
      error: () => {
        this.clearingAll.set(false);
        this.message.set('No se pudo borrar la lista de afiliados');
      },
    });
  }

  savePortalPassword(): void {
    const id = this.editingId();
    const password = this.portalPassword().trim();
    if (!id || !password) {
      this.message.set('Escribe una contraseña para el portal del afiliado');
      return;
    }
    this.portalPasswordSaving.set(true);
    this.memberService.setPortalPassword(id, password).subscribe({
      next: () => {
        this.portalPasswordSaving.set(false);
        this.portalPassword.set('');
        this.message.set('Contraseña del portal actualizada');
      },
      error: (err) => {
        this.portalPasswordSaving.set(false);
        this.message.set(err?.error?.message ?? 'No se pudo cambiar la contraseña');
      },
    });
  }

  resetPortalPassword(): void {
    const id = this.editingId();
    if (!id) {
      return;
    }
    const doc = this.form.controls.documentId.value?.trim();
    const hint = doc ? ` (quedará como el documento: ${doc})` : ' (quedará igual al documento)';
    if (!confirm(`¿Restablecer la contraseña del portal${hint}?`)) {
      return;
    }
    this.portalPasswordSaving.set(true);
    this.memberService.resetPortalPassword(id).subscribe({
      next: () => {
        this.portalPasswordSaving.set(false);
        this.portalPassword.set('');
        this.message.set('Contraseña restablecida al número de documento');
      },
      error: (err) => {
        this.portalPasswordSaving.set(false);
        this.message.set(err?.error?.message ?? 'No se pudo restablecer la contraseña');
      },
    });
  }

  onExcelSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    this.selectedFileName.set(file.name);
    this.importing.set(true);
    this.importErrors.set([]);
    this.memberService.importFromExcel(file).subscribe({
      next: (result) => {
        this.importing.set(false);
        const parts: string[] = [];
        if (result.created > 0) {
          parts.push(`${result.created} nuevo(s)`);
        }
        if (result.updated > 0) {
          parts.push(`${result.updated} actualizado(s)`);
        }
        if (result.skipped > 0) {
          parts.push(`${result.skipped} fila(s) omitida(s)`);
        }
        this.message.set(
          parts.length > 0
            ? `Importación lista: ${parts.join(', ')}.`
            : 'No se importó ningún registro.',
        );
        this.importErrors.set(result.errors);
        if (result.created > 0 || result.updated > 0) {
          this.loadData();
        }
      },
      error: () => {
        this.importing.set(false);
        this.message.set('No se pudo importar el archivo. Revisa el formato e inténtalo de nuevo.');
      },
    });
  }

  effectiveStatus(member: Member): MembershipStatus {
    if (member.membershipFrozen) {
      return 'SUSPENDED';
    }
    if (member.status === 'SUSPENDED') {
      return 'SUSPENDED';
    }
    if (member.membershipEnd && member.membershipEnd < this.todayIso()) {
      return 'EXPIRED';
    }
    return member.status;
  }

  displayStatusLabel(member: Member): string {
    if (member.membershipFrozen) {
      const days = member.frozenRemainingDays ?? 0;
      return `Congelada (${days} ${days === 1 ? 'día' : 'días'} guardados)`;
    }
    return this.statusLabel(this.effectiveStatus(member));
  }

  canFreezeMember(member: Member): boolean {
    if (member.membershipFrozen || member.status === 'SUSPENDED') {
      return false;
    }
    const end = member.membershipEnd;
    if (!end) {
      return false;
    }
    return end >= this.todayIso();
  }

  freezeFromList(member: Member): void {
    this.startEdit(member);
    this.freezeMembership();
  }

  unfreezeFromList(member: Member): void {
    this.startEdit(member);
    this.unfreezeMembership();
  }

  freezeMembership(): void {
    const id = this.editingId();
    if (id == null) {
      return;
    }
    if (!confirm('¿Congelar la membresía? Se guardarán los días restantes y no podrá ingresar al gimnasio hasta descongelar.')) {
      return;
    }
    this.freezeAction.set(true);
    this.memberService.freezeMembership(id).subscribe({
      next: (updated) => {
        this.freezeAction.set(false);
        this.message.set('Membresía congelada');
        this.applyUpdatedMember(updated);
      },
      error: (err) => {
        this.freezeAction.set(false);
        this.message.set(this.freezeErrorMessage(err));
      },
    });
  }

  unfreezeMembership(): void {
    const id = this.editingId();
    if (id == null) {
      return;
    }
    const days = this.editingMember()?.frozenRemainingDays ?? 0;
    if (
      !confirm(
        `¿Descongelar la membresía? Se extenderá la fecha de vencimiento ${days} día(s) a partir de hoy.`,
      )
    ) {
      return;
    }
    this.freezeAction.set(true);
    this.memberService.unfreezeMembership(id).subscribe({
      next: (updated) => {
        this.freezeAction.set(false);
        this.message.set('Membresía descongelada');
        this.applyUpdatedMember(updated);
      },
      error: (err) => {
        this.freezeAction.set(false);
        this.message.set(this.freezeErrorMessage(err));
      },
    });
  }

  private applyUpdatedMember(updated: Member): void {
    this.members.update((list) => list.map((m) => (m.id === updated.id ? updated : m)));
    this.startEdit(updated);
  }

  private freezeErrorMessage(err: { error?: { message?: string } }): string {
    return err.error?.message ?? 'No se pudo actualizar el estado de congelación';
  }

  genderLabel(gender?: Gender): string {
    const labels: Record<Gender, string> = {
      MALE: 'Masculino',
      FEMALE: 'Femenino',
      OTHER: 'Otro',
    };
    return gender ? labels[gender] : '—';
  }

  statusLabel(status: MembershipStatus): string {
    const labels: Record<MembershipStatus, string> = {
      ACTIVE: 'Activo',
      EXPIRED: 'Inactivo',
      SUSPENDED: 'Suspendido',
    };
    return labels[status];
  }

  private todayIso(): string {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private compareMembers(a: Member, b: Member, column: SortColumn): number {
    switch (column) {
      case 'documentId':
        return (a.documentId ?? '').localeCompare(b.documentId ?? '', 'es');
      case 'gender':
        return this.genderLabel(a.gender).localeCompare(this.genderLabel(b.gender), 'es');
      case 'planName':
        return (a.planName ?? '').localeCompare(b.planName ?? '', 'es');
      case 'status':
        return this.effectiveStatus(a).localeCompare(this.effectiveStatus(b), 'es');
      case 'membershipEnd':
        return (a.membershipEnd ?? '').localeCompare(b.membershipEnd ?? '');
      case 'name':
      default:
        return `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`, 'es');
    }
  }
}
