import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RatingParticipantAdmin, TrainerRatingMonthlySummary } from '../../core/models/trainer-rating.model';
import { AuthService } from '../../core/services/auth.service';
import { HomeContentService } from '../../core/services/home-content.service';
import { UploadUrlPipe } from '../../core/pipes/upload-url.pipe';
import { TrainerRatingService } from '../../core/services/trainer-rating.service';

@Component({
  selector: 'app-trainer-ratings-admin',
  imports: [FormsModule, UploadUrlPipe],
  templateUrl: './trainer-ratings-admin.html',
  styleUrl: './trainer-ratings-admin.scss',
})
export class TrainerRatingsAdminPage implements OnInit {
  private readonly ratingService = inject(TrainerRatingService);
  private readonly homeContent = inject(HomeContentService);
  protected readonly auth = inject(AuthService);

  protected readonly tab = signal<'ranking' | 'participants'>('ranking');
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly uploading = signal(false);
  protected readonly message = signal<string | null>(null);

  protected readonly participants = signal<RatingParticipantAdmin[]>([]);
  protected readonly monthly = signal<TrainerRatingMonthlySummary[]>([]);

  protected readonly selectedYear = signal(new Date().getFullYear());
  protected readonly selectedMonth = signal(new Date().getMonth() + 1);

  protected readonly monthOptions = [
    { value: 1, label: 'Enero' },
    { value: 2, label: 'Febrero' },
    { value: 3, label: 'Marzo' },
    { value: 4, label: 'Abril' },
    { value: 5, label: 'Mayo' },
    { value: 6, label: 'Junio' },
    { value: 7, label: 'Julio' },
    { value: 8, label: 'Agosto' },
    { value: 9, label: 'Septiembre' },
    { value: 10, label: 'Octubre' },
    { value: 11, label: 'Noviembre' },
    { value: 12, label: 'Diciembre' },
  ];

  ngOnInit(): void {
    this.loadMonthly();
    if (this.auth.hasRole('SUPER_ADMIN')) {
      this.loadParticipants();
    } else {
      this.loading.set(false);
    }
  }

  setTab(value: 'ranking' | 'participants'): void {
    this.tab.set(value);
  }

  loadMonthly(): void {
    this.loading.set(true);
    this.ratingService.monthlySummary(this.selectedYear(), this.selectedMonth()).subscribe({
      next: (rows) => {
        this.monthly.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Error al cargar el ranking del mes');
        this.loading.set(false);
      },
    });
  }

  onPeriodChange(): void {
    this.loadMonthly();
  }

  loadParticipants(): void {
    this.ratingService.findParticipantsForConfig().subscribe({
      next: (list) => this.participants.set(list),
      error: () => this.message.set('Error al cargar entrenadores'),
    });
  }

  setParticipantEligible(p: RatingParticipantAdmin, eligible: boolean): void {
    if (p.ratingEligible === eligible) {
      return;
    }
    this.saving.set(true);
    this.ratingService
      .updateParticipant(p.id, { ratingEligible: eligible })
      .subscribe({
        next: (updated) => {
          this.participants.update((list) =>
            list.map((x) => (x.id === updated.id ? updated : x)),
          );
          this.saving.set(false);
        },
        error: () => {
          this.message.set('No se pudo actualizar');
          this.saving.set(false);
        },
      });
  }

  updatePhotoUrl(p: RatingParticipantAdmin, photoUrl: string): void {
    this.saving.set(true);
    this.ratingService.updateParticipant(p.id, { photoUrl }).subscribe({
      next: (updated) => {
        this.participants.update((list) => list.map((x) => (x.id === updated.id ? updated : x)));
        this.saving.set(false);
      },
      error: () => {
        this.message.set('No se pudo guardar la foto');
        this.saving.set(false);
      },
    });
  }

  onPhotoSelected(p: RatingParticipantAdmin, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.homeContent.uploadImage(file).subscribe({
      next: (res) => {
        this.uploading.set(false);
        this.updatePhotoUrl(p, res.url);
        input.value = '';
      },
      error: () => {
        this.uploading.set(false);
        this.message.set('Error al subir la imagen');
        input.value = '';
      },
    });
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('');
  }
}
