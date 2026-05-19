import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { ModuleService } from '../core/services/module.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly modules = inject(ModuleService);

  ngOnInit(): void {
    this.refreshModules();
    if (this.auth.isLoggedIn()) {
      this.auth.loadProfile().subscribe({
        next: () => this.refreshModules(),
        error: () => this.auth.logout(),
      });
    }
  }

  private refreshModules(): void {
    this.modules.reloadPanelForUser().subscribe();
  }

  logout(): void {
    this.modules.resetPanel();
    this.auth.logout();
  }
}
