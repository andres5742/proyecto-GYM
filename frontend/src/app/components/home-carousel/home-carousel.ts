import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CarouselSlide } from '../../core/models/home-content.model';
import { UploadUrlPipe } from '../../core/pipes/upload-url.pipe';
import { HomeContentService } from '../../core/services/home-content.service';

@Component({
  selector: 'app-home-carousel',
  imports: [UploadUrlPipe],
  templateUrl: './home-carousel.html',
  styleUrl: './home-carousel.scss',
})
export class HomeCarousel implements OnInit, OnDestroy {
  private readonly homeContent = inject(HomeContentService);
  private timerId: ReturnType<typeof setInterval> | null = null;

  protected readonly slides = signal<CarouselSlide[]>([]);
  protected readonly currentIndex = signal(0);

  ngOnInit(): void {
    this.homeContent.findCarousel().subscribe({
      next: (slides) => {
        this.slides.set(slides);
        if (slides.length > 1) {
          this.startAutoPlay();
        }
      },
      error: () => this.slides.set([]),
    });
  }

  ngOnDestroy(): void {
    this.stopAutoPlay();
  }

  goTo(index: number): void {
    const total = this.slides().length;
    if (total === 0) {
      return;
    }
    this.currentIndex.set((index + total) % total);
    this.restartAutoPlay();
  }

  next(): void {
    this.goTo(this.currentIndex() + 1);
  }

  prev(): void {
    this.goTo(this.currentIndex() - 1);
  }

  private startAutoPlay(): void {
    this.stopAutoPlay();
    this.timerId = setInterval(() => this.next(), 5000);
  }

  private stopAutoPlay(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  private restartAutoPlay(): void {
    if (this.slides().length > 1) {
      this.startAutoPlay();
    }
  }
}
