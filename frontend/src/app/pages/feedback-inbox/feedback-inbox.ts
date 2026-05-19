import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { FeedbackMessage, FeedbackStatus } from '../../core/models/feedback.model';
import { FeedbackService } from '../../core/services/feedback.service';

type FilterStatus = 'ALL' | FeedbackStatus;

@Component({
  selector: 'app-feedback-inbox',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './feedback-inbox.html',
  styleUrl: './feedback-inbox.scss',
})
export class FeedbackInboxPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly feedbackService = inject(FeedbackService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly items = signal<FeedbackMessage[]>([]);
  protected readonly filter = signal<FilterStatus>('ALL');
  protected readonly selectedId = signal<number | null>(null);

  protected readonly noteForm = this.fb.nonNullable.group({
    adminNote: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.feedbackService.findAll().subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('No se pudieron cargar los mensajes del buzón.');
        this.loading.set(false);
      },
    });
  }

  filteredItems(): FeedbackMessage[] {
    const f = this.filter();
    if (f === 'ALL') {
      return this.items();
    }
    return this.items().filter((i) => i.status === f);
  }

  pendingCount(): number {
    return this.items().filter((i) => i.status === 'PENDING').length;
  }

  setFilter(status: FilterStatus): void {
    this.filter.set(status);
  }

  select(item: FeedbackMessage): void {
    this.selectedId.set(item.id);
    this.noteForm.patchValue({ adminNote: item.adminNote ?? '' });
  }

  selected(): FeedbackMessage | null {
    const id = this.selectedId();
    return this.items().find((i) => i.id === id) ?? null;
  }

  mark(status: 'RESOLVED' | 'NOT_RESOLVED'): void {
    const item = this.selected();
    if (!item) {
      return;
    }
    this.saving.set(true);
    this.feedbackService
      .updateStatus(item.id, {
        status,
        adminNote: this.noteForm.controls.adminNote.value || undefined,
      })
      .subscribe({
        next: (updated) => {
          this.items.update((list) => list.map((i) => (i.id === updated.id ? updated : i)));
          this.message.set(
            status === 'RESOLVED' ? 'Marcada como solucionada' : 'Marcada como no solucionada',
          );
          this.saving.set(false);
        },
        error: (err) => {
          this.message.set(err?.error?.message ?? 'No se pudo actualizar');
          this.saving.set(false);
        },
      });
  }

  remove(id: number): void {
    if (!confirm('¿Eliminar este mensaje del buzón?')) {
      return;
    }
    this.feedbackService.delete(id).subscribe({
      next: () => {
        this.items.update((list) => list.filter((i) => i.id !== id));
        if (this.selectedId() === id) {
          this.selectedId.set(null);
        }
        this.message.set('Mensaje eliminado');
      },
      error: () => this.message.set('No se pudo eliminar'),
    });
  }

  statusClass(status: FeedbackStatus): string {
    return `status-${status.toLowerCase()}`;
  }
}
