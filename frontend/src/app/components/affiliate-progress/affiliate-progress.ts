import { DatePipe } from '@angular/common';
import { Component, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  MemberProgressEntry,
  MemberProgressEntryRequest,
  ProgressEntryChip,
  ProgressMetricGroup,
} from '../../core/models/member-progress.model';
import { MemberPortalService } from '../../core/services/member-portal.service';
import { UploadUrlPipe } from '../../core/pipes/upload-url.pipe';

interface PendingPhoto {
  id: string;
  previewUrl: string;
  serverUrl?: string;
  uploading: boolean;
  error?: string;
}

const EMPTY_FORM = {
  recordedAt: '',
  weightKg: null as number | null,
  chestCm: null as number | null,
  waistCm: null as number | null,
  hipsCm: null as number | null,
  armRightCm: null as number | null,
  armLeftCm: null as number | null,
  thighUpperRightCm: null as number | null,
  thighUpperLeftCm: null as number | null,
  thighLowerRightCm: null as number | null,
  thighLowerLeftCm: null as number | null,
  calfRightCm: null as number | null,
  calfLeftCm: null as number | null,
  bodyFatPercent: null as number | null,
  notes: '',
};

@Component({
  selector: 'app-affiliate-progress',
  imports: [ReactiveFormsModule, DatePipe, UploadUrlPipe],
  templateUrl: './affiliate-progress.html',
  styleUrl: './affiliate-progress.scss',
})
export class AffiliateProgress implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly portal = inject(MemberPortalService);

  /** Vista compacta dentro de pestaña en Mi cuenta */
  readonly embedded = input(false);

  protected readonly entries = signal<MemberProgressEntry[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly showForm = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly pendingPhotos = signal<PendingPhoto[]>([]);
  protected readonly viewingEntry = signal<MemberProgressEntry | null>(null);

  protected readonly maxPhotos = 4;
  protected readonly acceptedTypes = 'image/jpeg,image/png,image/webp,image/gif';

  protected readonly form = this.fb.nonNullable.group({
    recordedAt: [this.todayIso()],
    weightKg: [null as number | null],
    chestCm: [null as number | null],
    waistCm: [null as number | null],
    hipsCm: [null as number | null],
    armRightCm: [null as number | null],
    armLeftCm: [null as number | null],
    thighUpperRightCm: [null as number | null],
    thighUpperLeftCm: [null as number | null],
    thighLowerRightCm: [null as number | null],
    thighLowerLeftCm: [null as number | null],
    calfRightCm: [null as number | null],
    calfLeftCm: [null as number | null],
    bodyFatPercent: [null as number | null],
    notes: [''],
  });

  private objectUrls: string[] = [];

  ngOnInit(): void {
    this.loadEntries();
  }

  ngOnDestroy(): void {
    this.revokeObjectUrls();
  }

  loadEntries(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.portal.listProgress().subscribe({
      next: (entries) => {
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(err?.error?.message ?? 'No se pudieron cargar tus avances');
        this.loading.set(false);
      },
    });
  }

  openView(entry: MemberProgressEntry): void {
    this.viewingEntry.set(entry);
  }

  closeView(): void {
    this.viewingEntry.set(null);
  }

  openCreate(): void {
    this.closeView();
    this.editingId.set(null);
    this.clearPendingPhotos();
    this.form.reset({ ...EMPTY_FORM, recordedAt: this.todayIso() });
    this.showForm.set(true);
    this.message.set(null);
    this.error.set(null);
  }

  openEdit(entry: MemberProgressEntry): void {
    this.closeView();
    this.editingId.set(entry.id);
    this.clearPendingPhotos();
    this.form.patchValue({
      recordedAt: entry.recordedAt,
      weightKg: entry.weightKg,
      chestCm: entry.chestCm,
      waistCm: entry.waistCm,
      hipsCm: entry.hipsCm,
      armRightCm: entry.armRightCm,
      armLeftCm: entry.armLeftCm,
      thighUpperRightCm: entry.thighUpperRightCm,
      thighUpperLeftCm: entry.thighUpperLeftCm,
      thighLowerRightCm: entry.thighLowerRightCm,
      thighLowerLeftCm: entry.thighLowerLeftCm,
      calfRightCm: entry.calfRightCm,
      calfLeftCm: entry.calfLeftCm,
      bodyFatPercent: entry.bodyFatPercent,
      notes: entry.notes ?? '',
    });
    for (const url of entry.imageUrls) {
      this.pendingPhotos.update((list) => [
        ...list,
        { id: crypto.randomUUID(), previewUrl: url, serverUrl: url, uploading: false },
      ]);
    }
    this.showForm.set(true);
    this.message.set(null);
    this.error.set(null);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.clearPendingPhotos();
  }

  onPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files?.length) {
      return;
    }
    const remaining = this.maxPhotos - this.pendingPhotos().length;
    const toAdd = Array.from(files).slice(0, remaining);
    input.value = '';

    for (const file of toAdd) {
      const previewUrl = URL.createObjectURL(file);
      this.objectUrls.push(previewUrl);
      const photo: PendingPhoto = {
        id: crypto.randomUUID(),
        previewUrl,
        uploading: true,
      };
      this.pendingPhotos.update((list) => [...list, photo]);
      this.portal.uploadProgressPhoto(file).subscribe({
        next: ({ url }) => {
          this.pendingPhotos.update((list) =>
            list.map((p) => (p.id === photo.id ? { ...p, serverUrl: url, uploading: false } : p)),
          );
        },
        error: () => {
          this.pendingPhotos.update((list) =>
            list.map((p) =>
              p.id === photo.id ? { ...p, uploading: false, error: 'No se pudo subir' } : p,
            ),
          );
        },
      });
    }
  }

  removePhoto(id: string): void {
    const photo = this.pendingPhotos().find((p) => p.id === id);
    if (photo?.previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(photo.previewUrl);
      this.objectUrls = this.objectUrls.filter((u) => u !== photo.previewUrl);
    }
    this.pendingPhotos.update((list) => list.filter((p) => p.id !== id));
  }

  save(): void {
    if (this.pendingPhotos().some((p) => p.uploading)) {
      this.error.set('Espera un momento: aún se están subiendo las fotos');
      return;
    }
    const raw = this.form.getRawValue();
    const request: MemberProgressEntryRequest = {
      recordedAt: raw.recordedAt,
      weightKg: this.toNumberOrNull(raw.weightKg),
      chestCm: this.toNumberOrNull(raw.chestCm),
      waistCm: this.toNumberOrNull(raw.waistCm),
      hipsCm: this.toNumberOrNull(raw.hipsCm),
      armRightCm: this.toNumberOrNull(raw.armRightCm),
      armLeftCm: this.toNumberOrNull(raw.armLeftCm),
      thighUpperRightCm: this.toNumberOrNull(raw.thighUpperRightCm),
      thighUpperLeftCm: this.toNumberOrNull(raw.thighUpperLeftCm),
      thighLowerRightCm: this.toNumberOrNull(raw.thighLowerRightCm),
      thighLowerLeftCm: this.toNumberOrNull(raw.thighLowerLeftCm),
      calfRightCm: this.toNumberOrNull(raw.calfRightCm),
      calfLeftCm: this.toNumberOrNull(raw.calfLeftCm),
      bodyFatPercent: this.toNumberOrNull(raw.bodyFatPercent),
      notes: raw.notes?.trim() || null,
      imageUrls: this.pendingPhotos()
        .map((p) => p.serverUrl)
        .filter((url): url is string => !!url),
    };

    const id = this.editingId();
    this.saving.set(true);
    this.error.set(null);
    const action = id
      ? this.portal.updateProgress(id, request)
      : this.portal.createProgress(request);

    action.subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set(id ? '¡Listo! Actualizamos tu registro' : '¡Genial! Guardamos tu avance');
        this.showForm.set(false);
        this.editingId.set(null);
        this.clearPendingPhotos();
        this.loadEntries();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(this.formatError(err));
      },
    });
  }

  deleteEntry(entry: MemberProgressEntry): void {
    if (!confirm('¿Quieres eliminar este registro de tu historial?')) {
      return;
    }
    this.portal.deleteProgress(entry.id).subscribe({
      next: () => {
        this.message.set('Registro eliminado');
        this.loadEntries();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo eliminar');
      },
    });
  }

  protected metricGroups(entry: MemberProgressEntry): ProgressMetricGroup[] {
    const cm = (v: number | null | undefined) =>
      v != null ? `${v.toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} cm` : null;
    const groups: ProgressMetricGroup[] = [];

    const general: { label: string; value: string }[] = [];
    if (entry.weightKg != null) {
      general.push({
        label: 'Peso',
        value: `${entry.weightKg.toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} kg`,
      });
    }
    if (entry.bodyFatPercent != null) {
      general.push({
        label: 'Grasa corporal',
        value: `${entry.bodyFatPercent.toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} %`,
      });
    }
    if (general.length) {
      groups.push({
        title: 'General',
        icon: '⚖️',
        hint: 'Peso y porcentaje de grasa corporal',
        items: general,
      });
    }

    const torso: { label: string; value: string }[] = [];
    const chest = cm(entry.chestCm);
    if (chest) torso.push({ label: 'Pecho', value: chest });
    const waist = cm(entry.waistCm);
    if (waist) torso.push({ label: 'Cintura', value: waist });
    const hips = cm(entry.hipsCm);
    if (hips) torso.push({ label: 'Cadera', value: hips });
    if (torso.length) {
      groups.push({ title: 'Torso', icon: '🧍', hint: 'Pecho, cintura y cadera', items: torso });
    }

    const arms: { label: string; value: string }[] = [];
    const armR = cm(entry.armRightCm);
    if (armR) arms.push({ label: 'Brazo derecho', value: armR });
    const armL = cm(entry.armLeftCm);
    if (armL) arms.push({ label: 'Brazo izquierdo', value: armL });
    if (arms.length) {
      groups.push({ title: 'Brazos', icon: '💪', hint: 'Medida del brazo flexionado', items: arms });
    }

    const thighUpper: { label: string; value: string }[] = [];
    const thighUpperR = cm(entry.thighUpperRightCm);
    if (thighUpperR) thighUpper.push({ label: 'Superior derecho', value: thighUpperR });
    const thighUpperL = cm(entry.thighUpperLeftCm);
    if (thighUpperL) thighUpper.push({ label: 'Superior izquierdo', value: thighUpperL });
    if (thighUpper.length) {
      groups.push({
        title: 'Muslo superior',
        icon: '🦵',
        hint: 'Parte alta del muslo (der. / izq.)',
        items: thighUpper,
      });
    }

    const thighLower: { label: string; value: string }[] = [];
    const thighLowerR = cm(entry.thighLowerRightCm);
    if (thighLowerR) thighLower.push({ label: 'Inferior derecho', value: thighLowerR });
    const thighLowerL = cm(entry.thighLowerLeftCm);
    if (thighLowerL) thighLower.push({ label: 'Inferior izquierdo', value: thighLowerL });
    if (thighLower.length) {
      groups.push({
        title: 'Muslo inferior',
        icon: '🦿',
        hint: 'Parte baja del muslo (der. / izq.)',
        items: thighLower,
      });
    }

    const calves: { label: string; value: string }[] = [];
    const calfR = cm(entry.calfRightCm);
    if (calfR) calves.push({ label: 'Pantorrilla derecha', value: calfR });
    const calfL = cm(entry.calfLeftCm);
    if (calfL) calves.push({ label: 'Pantorrilla izquierda', value: calfL });
    if (calves.length) {
      groups.push({ title: 'Pantorrillas', icon: '🦶', hint: 'Contorno de la pantorrilla', items: calves });
    }

    return groups;
  }

  protected hasMetrics(entry: MemberProgressEntry): boolean {
    return this.metricGroups(entry).length > 0;
  }

  protected formatWeight(entry: MemberProgressEntry): string {
    if (entry.weightKg == null) {
      return '—';
    }
    return `${entry.weightKg.toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} kg`;
  }

  protected entryChips(entry: MemberProgressEntry): ProgressEntryChip[] {
    const chips: ProgressEntryChip[] = [];
    if (entry.weightKg != null) {
      chips.push({ label: 'Peso', kind: 'weight' });
    }
    const measureCount = this.countMeasures(entry);
    if (measureCount > 0) {
      chips.push({
        label: `${measureCount} medida${measureCount === 1 ? '' : 's'}`,
        kind: 'measures',
      });
    }
    if (entry.notes?.trim()) {
      chips.push({ label: 'Notas', kind: 'notes' });
    }
    if (entry.imageUrls.length > 0) {
      chips.push({
        label: `${entry.imageUrls.length} foto${entry.imageUrls.length === 1 ? '' : 's'}`,
        kind: 'photos',
      });
    }
    return chips;
  }

  private countMeasures(entry: MemberProgressEntry): number {
    const fields = [
      entry.chestCm,
      entry.waistCm,
      entry.hipsCm,
      entry.armRightCm,
      entry.armLeftCm,
      entry.thighUpperRightCm,
      entry.thighUpperLeftCm,
      entry.thighLowerRightCm,
      entry.thighLowerLeftCm,
      entry.calfRightCm,
      entry.calfLeftCm,
      entry.bodyFatPercent,
    ];
    return fields.filter((v) => v != null).length;
  }

  private toNumberOrNull(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const n = Number(value);
    if (!Number.isFinite(n) || n < 0) {
      return null;
    }
    if (n === 0) {
      return null;
    }
    return n;
  }

  private formatError(err: { error?: { message?: string; errors?: Record<string, string> } }): string {
    const errors = err?.error?.errors;
    if (errors && typeof errors === 'object') {
      const messages = Object.values(errors);
      if (messages.length > 0) {
        return messages.join('. ');
      }
    }
    return err?.error?.message ?? 'No se pudo guardar. Intenta de nuevo';
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private clearPendingPhotos(): void {
    this.revokeObjectUrls();
    this.pendingPhotos.set([]);
  }

  private revokeObjectUrls(): void {
    for (const url of this.objectUrls) {
      URL.revokeObjectURL(url);
    }
    this.objectUrls = [];
  }
}
