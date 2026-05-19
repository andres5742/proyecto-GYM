import { Component, inject, input, output, signal } from '@angular/core';
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

@Component({
  selector: 'app-feedback-box',
  imports: [ReactiveFormsModule],
  templateUrl: './feedback-box.html',
  styleUrl: './feedback-box.scss',
})
export class FeedbackBox {
  private readonly fb = inject(FormBuilder);
  private readonly feedbackService = inject(FeedbackService);

  readonly submitted = output<void>();
  readonly inModal = input(false);

  protected readonly saving = signal(false);
  protected readonly success = signal(false);

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

  setAnonymous(anonymous: boolean): void {
    this.form.controls.anonymous.setValue(anonymous);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.success.set(false);
    this.feedbackService
      .submit({
        type: raw.type,
        message: raw.message,
        anonymous: raw.anonymous,
        authorName: raw.anonymous ? undefined : raw.authorName || undefined,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.success.set(true);
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
}
