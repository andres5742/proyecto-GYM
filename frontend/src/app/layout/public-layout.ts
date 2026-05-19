import { Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { FeedbackModal } from '../components/feedback-modal/feedback-modal';
import { LoginPanel } from '../components/login-panel/login-panel';
import { ModuleService } from '../core/services/module.service';
import { PublicFooter } from '../components/public-footer/public-footer';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, LoginPanel, PublicFooter, FeedbackModal],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  protected readonly auth = inject(AuthService);
  protected readonly modules = inject(ModuleService);
  protected readonly loginOpen = signal(false);

  @HostListener('document:click')
  onDocumentClick(): void {
    this.loginOpen.set(false);
  }

  toggleLogin(event: Event): void {
    event.stopPropagation();
    this.loginOpen.update((v) => !v);
  }

  onLoginPanelClick(event: Event): void {
    event.stopPropagation();
  }

  closeLogin(): void {
    this.loginOpen.set(false);
  }

  logout(): void {
    this.auth.logout();
  }
}
