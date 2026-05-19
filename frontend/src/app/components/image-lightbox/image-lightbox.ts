import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-image-lightbox',
  templateUrl: './image-lightbox.html',
  styleUrl: './image-lightbox.scss',
})
export class ImageLightbox {
  readonly src = input.required<string>();
  readonly alt = input('Imagen ampliada');
  readonly closed = output<void>();

  close(): void {
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }
}
