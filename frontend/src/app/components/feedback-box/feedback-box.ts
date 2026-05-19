import { Component, inject, input, OnDestroy, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FeedbackType } from '../../core/models/feedback.model';
import { FeedbackService } from '../../core/services/feedback.service';

interface FeedbackTypeOption {
  value: FeedbackType;
  label: string;
  icon: string;
  hint: string;
  example: string;
  cssClass: string;
}

interface PendingPhoto {
  id: string;
  previewUrl: string;
  serverUrl?: string;
  uploading: boolean;
  error?: string;
}

@Component({
  selector: 'app-feedback-box',
  imports: [ReactiveFormsModule],
  templateUrl: './feedback-box.html',
  styleUrl: './feedback-box.scss',
})
export class FeedbackBox implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly feedbackService = inject(FeedbackService);

  readonly submitted = output<void>();
  readonly inModal = input(false);

  protected readonly saving = signal(false);
  protected readonly success = signal(false);
  protected readonly uploadingPhotos = signal(false);
  protected readonly pendingPhotos = signal<PendingPhoto[]>([]);

  protected readonly maxPhotos = 4;
  protected readonly acceptedTypes = 'image/jpeg,image/png,image/webp,image/gif';

  protected readonly types: FeedbackTypeOption[] = [
    {
      value: 'SUGGESTION',
      label: 'Sugerencia',
      icon: '💡',
      hint: 'Ideas para mejorar',
      example: 'Más horarios de cardio, nuevas máquinas…',
      cssClass: 'type-suggestion',
    },
    {
      value: 'COMPLAINT',
      label: 'Queja o reclamo',
      icon: '📢',
      hint: 'Algo que no funcionó bien',
      example: 'Atención, limpieza, equipos dañados…',
      cssClass: 'type-complaint',
    },
    {
      value: 'PRAISE',
      label: 'Felicitación',
      icon: '🎉',
      hint: 'Reconocer al equipo',
      example: 'Un entrenador, la recepción, el ambiente…',
      cssClass: 'type-praise',
    },
  ];

  protected readonly form = this.fb.nonNullable.group({
    type: ['SUGGESTION' as FeedbackType, Validators.required],
    message: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(3000)]],
    anonymous: [false],
    authorName: ['', Validators.maxLength(120)],
  });

  private objectUrls: string[] = [];

  protected messagePlaceholder(): string {
    const value = this.form.controls.type.value;
    const t = this.types.find((x) => x.value === value) ?? this.types[0];
    return `Ejemplo: ${t.example}`;
  }

  constructor() {
    this.form.controls.anonymous.valueChanges.subscribe((anonymous) => {
      const nameCtrl = this.form.controls.authorName;
      if (anonymous) {
        nameCtrl.clearValidators();
        nameCtrl.setValue('');
        nameCtrl.disable();
      } else {
        nameCtrl.setValidators([Validators.required, Validators.maxLength(120)]);
        nameCtrl.enable();
      }
      nameCtrl.updateValueAndValidity();
    });
  }

  ngOnDestroy(): void {
    this.revokeObjectUrls();
  }

  protected photosRemaining(): number {
    return this.maxPhotos - this.pendingPhotos().length;
  }

  protected canAddPhotos(): boolean {
    return this.photosRemaining() > 0 && !this.uploadingPhotos();
  }

  onPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files?.length) {
      return;
    }
    const slots = this.photosRemaining();
    const toProcess = Array.from(files).slice(0, slots);
    input.value = '';

    for (const file of toProcess) {
      if (!file.type.startsWith('image/')) {
        continue;
      }
      if (file.size > 5 * 1024 * 1024) {
        alert(`"${file.name}" supera 5 MB`);
        continue;
      }
      const id = crypto.randomUUID();
      const previewUrl = URL.createObjectURL(file);
      this.objectUrls.push(previewUrl);
      const entry: PendingPhoto = { id, previewUrl, uploading: true };
      this.pendingPhotos.update((list) => [...list, entry]);

      this.uploadingPhotos.set(true);
      this.feedbackService.uploadImage(file).subscribe({
        next: (res) => {
          this.pendingPhotos.update((list) =>
            list.map((p) =>
              p.id === id ? { ...p, serverUrl: res.url, uploading: false, error: undefined } : p,
            ),
          );
          this.uploadingPhotos.set(this.pendingPhotos().some((p) => p.uploading));
        },
        error: () => {
          this.pendingPhotos.update((list) =>
            list.map((p) =>
              p.id === id
                ? { ...p, uploading: false, error: 'No se pudo subir' }
                : p,
            ),
          );
          this.uploadingPhotos.set(this.pendingPhotos().some((p) => p.uploading));
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

  photoDisplayUrl(photo: PendingPhoto): string {
    return photo.previewUrl;
  }

  setAnonymous(anonymous: boolean): void {
    this.form.controls.anonymous.setValue(anonymous);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.pendingPhotos().some((p) => p.uploading)) {
      alert('Espera a que terminen de subirse las fotos');
      return;
    }
    if (this.pendingPhotos().some((p) => p.error)) {
      alert('Quita o vuelve a subir las fotos con error');
      return;
    }

    const raw = this.form.getRawValue();
    const imageUrls = this.pendingPhotos()
      .map((p) => p.serverUrl)
      .filter((url): url is string => !!url);

    this.saving.set(true);
    this.success.set(false);
    this.feedbackService
      .submit({
        type: raw.type,
        message: raw.message,
        anonymous: raw.anonymous,
        authorName: raw.anonymous ? undefined : raw.authorName || undefined,
        imageUrls: imageUrls.length > 0 ? imageUrls : undefined,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.success.set(true);
          this.clearPhotos();
          this.form.reset({ type: 'SUGGESTION', message: '', anonymous: false, authorName: '' });
          this.form.controls.authorName.enable();
          this.submitted.emit();
        },
        error: (err) => {
          this.saving.set(false);
          alert(err?.error?.message ?? 'No se pudo enviar el mensaje. Intenta de nuevo.');
        },
      });
  }

  private clearPhotos(): void {
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
