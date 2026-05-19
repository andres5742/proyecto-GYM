import { Component, input, output, signal } from '@angular/core';
import { FeedbackBox } from '../feedback-box/feedback-box';

@Component({
  selector: 'app-feedback-modal',
  imports: [FeedbackBox],
  templateUrl: './feedback-modal.html',
  styleUrl: './feedback-modal.scss',
})
export class FeedbackModal {
  readonly inHeader = input(false);
  readonly closed = output<void>();

  protected readonly open = signal(false);

  show(): void {
    this.open.set(true);
    document.body.style.overflow = 'hidden';
  }

  close(): void {
    this.open.set(false);
    document.body.style.overflow = '';
    this.closed.emit();
  }

  onSubmitted(): void {
    setTimeout(() => this.close(), 2500);
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }
}
