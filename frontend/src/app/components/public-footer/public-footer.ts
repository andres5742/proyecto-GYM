import { Component, OnInit, inject, signal } from '@angular/core';
import { SiteFooter } from '../../core/models/home-content.model';
import { HomeContentService } from '../../core/services/home-content.service';

@Component({
  selector: 'app-public-footer',
  imports: [],
  templateUrl: './public-footer.html',
  styleUrl: './public-footer.scss',
})
export class PublicFooter implements OnInit {
  private readonly homeContent = inject(HomeContentService);

  protected readonly footer = signal<SiteFooter | null>(null);
  protected readonly currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.homeContent.getFooter().subscribe({
      next: (data) => this.footer.set(data),
      error: () => this.footer.set(null),
    });
  }

  hasSocials(data: SiteFooter): boolean {
    return !!(
      data.instagramUrl ||
      data.facebookUrl ||
      data.tiktokUrl ||
      data.youtubeUrl ||
      data.whatsappUrl
    );
  }
}
