import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ModuleService } from '../../core/services/module.service';

@Component({
  selector: 'app-login-panel',
  imports: [ReactiveFormsModule],
  templateUrl: './login-panel.html',
  styleUrl: './login-panel.scss',
})
export class LoginPanel {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly modules = inject(ModuleService);
  private readonly router = inject(Router);

  readonly compact = input(false);
  readonly closed = output<void>();

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        if (this.auth.isAffiliate()) {
          this.saving.set(false);
          this.closed.emit();
          this.router.navigate(['/mi-cuenta']);
          return;
        }
        this.modules.reloadPanelForUser().subscribe({
          next: () => {
            this.saving.set(false);
            this.closed.emit();
            this.router.navigate(['/panel']);
          },
          error: () => {
            this.saving.set(false);
            this.error.set('Sesión iniciada, pero no se pudieron cargar los módulos');
          },
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Usuario o contraseña incorrectos');
      },
    });
  }
}
