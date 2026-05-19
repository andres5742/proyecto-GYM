import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ModuleService } from '../../core/services/module.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly modules = inject(ModuleService);
  private readonly router = inject(Router);

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
          this.router.navigate(['/mi-cuenta']);
          return;
        }
        this.modules.reloadPanelForUser().subscribe({
          next: () => {
            this.saving.set(false);
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
