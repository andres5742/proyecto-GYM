import { Component, OnInit, inject, signal } from '@angular/core';
import { GymMediaItem } from '../../core/models/home-content.model';
import { UploadUrlPipe } from '../../core/pipes/upload-url.pipe';
import { HomeContentService } from '../../core/services/home-content.service';

@Component({
  selector: 'app-home-media-gallery',
  imports: [UploadUrlPipe],
  templateUrl: './home-media-gallery.html',
  styleUrl: './home-media-gallery.scss',
})
export class HomeMediaGallery implements OnInit {
  private readonly homeContent = inject(HomeContentService);

  protected readonly items = signal<GymMediaItem[]>([]);
  protected readonly selectedVideo = signal<GymMediaItem | null>(null);

  ngOnInit(): void {
    this.homeContent.findMedia().subscribe({
      next: (items) => this.items.set(items),
      error: () => this.items.set([]),
    });
  }

  openVideo(item: GymMediaItem): void {
    this.selectedVideo.set(item);
  }

  closeVideo(): void {
    this.selectedVideo.set(null);
  }

  isYoutube(url: string): boolean {
    return url.includes('youtube.com') || url.includes('youtu.be');
  }
}
