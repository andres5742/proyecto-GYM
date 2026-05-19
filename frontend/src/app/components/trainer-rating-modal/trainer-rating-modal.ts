import { Component, inject, OnInit, signal } from '@angular/core';
import { TrainerRatingParticipant } from '../../core/models/trainer-rating.model';
import { resolveUploadUrl } from '../../core/utils/media-url';
import { TrainerRatingService } from '../../core/services/trainer-rating.service';

@Component({
  selector: 'app-trainer-rating-modal',
  templateUrl: './trainer-rating-modal.html',
  styleUrl: './trainer-rating-modal.scss',
})
export class TrainerRatingModal implements OnInit {
  private readonly ratingService = inject(TrainerRatingService);

  protected readonly open = signal(false);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly participants = signal<TrainerRatingParticipant[]>([]);
  protected readonly selectedId = signal<number | null>(null);
  protected readonly identificationNumber = signal('');
  protected readonly selectedScore = signal<number | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly success = signal(false);
  protected readonly scores = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] as const;

  ngOnInit(): void {
    this.loadParticipants();
  }

  show(): void {
    this.open.set(true);
    this.resetForm();
    document.body.style.overflow = 'hidden';
    if (this.participants().length === 0) {
      this.loadParticipants();
    }
  }

  close(): void {
    this.open.set(false);
    document.body.style.overflow = '';
    this.resetForm();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  selectTrainer(id: number): void {
    this.selectedId.set(id);
    this.selectedScore.set(null);
    this.message.set(null);
    this.success.set(false);
  }

  selectScore(score: number): void {
    this.selectedScore.set(score);
    this.message.set(null);
  }

  submit(): void {
    const employeeId = this.selectedId();
    const score = this.selectedScore();
    const identificationNumber = this.identificationNumber().trim();
    if (!employeeId) {
      this.message.set('Elige un entrenador');
      return;
    }
    if (identificationNumber.length < 5) {
      this.message.set('Indica tu número de identificación (mínimo 5 caracteres)');
      return;
    }
    if (!score) {
      this.message.set('Elige una puntuación del 1 al 10');
      return;
    }
    this.saving.set(true);
    this.message.set(null);
    this.ratingService.submit({ employeeId, identificationNumber, score }).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(true);
        setTimeout(() => this.close(), 2200);
      },
      error: (err) => {
        this.saving.set(false);
        this.message.set(err.error?.message ?? 'No se pudo enviar la calificación');
      },
    });
  }

  trainerPhoto(p: TrainerRatingParticipant): string | null {
    const url = resolveUploadUrl(p.photoUrl);
    return url || null;
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('');
  }

  private loadParticipants(): void {
    this.loading.set(true);
    this.ratingService.findParticipants().subscribe({
      next: (list) => {
        this.participants.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.participants.set([]);
      },
    });
  }

  private resetForm(): void {
    this.selectedId.set(null);
    this.identificationNumber.set('');
    this.selectedScore.set(null);
    this.message.set(null);
    this.success.set(false);
  }
}
