import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Gender } from '../../core/models/member.model';
import { MemberPortalProfile } from '../../core/models/member-portal.model';
import { AuthService } from '../../core/services/auth.service';
import { MemberPortalService } from '../../core/services/member-portal.service';
import { AffiliateProgress } from '../../components/affiliate-progress/affiliate-progress';

export type AccountTab = 'perfil' | 'avances' | 'seguridad';

@Component({
  selector: 'app-my-account',
  imports: [ReactiveFormsModule, DatePipe, RouterLink, AffiliateProgress],
  templateUrl: './my-account.html',
  styleUrl: './my-account.scss',
})
export class MyAccount implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly portal = inject(MemberPortalService);
  protected readonly auth = inject(AuthService);

  protected readonly activeTab = signal<AccountTab>('perfil');
  protected readonly loading = signal(true);
  protected readonly profile = signal<MemberPortalProfile | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly passwordSaving = signal(false);
  protected readonly passwordMessage = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);

  protected readonly passwordForm = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(4)]],
      confirmPassword: ['', Validators.required],
    },
    {
      validators: (group) =>
        group.get('newPassword')?.value === group.get('confirmPassword')?.value
          ? null
          : { passwordMismatch: true },
    },
  );

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.portal.getProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(err?.error?.message ?? 'No se pudo cargar tu información');
        this.loading.set(false);
      },
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    const raw = this.passwordForm.getRawValue();
    this.passwordSaving.set(true);
    this.passwordMessage.set(null);
    this.passwordError.set(null);
    this.portal
      .changePassword({ currentPassword: raw.currentPassword, newPassword: raw.newPassword })
      .subscribe({
        next: () => {
          this.passwordSaving.set(false);
          this.passwordMessage.set('Contraseña actualizada correctamente');
          this.passwordForm.reset();
        },
        error: (err) => {
          this.passwordSaving.set(false);
          this.passwordError.set(err?.error?.message ?? 'No se pudo cambiar la contraseña');
        },
      });
  }

  logout(): void {
    this.auth.logout();
  }

  protected selectTab(tab: AccountTab): void {
    this.activeTab.set(tab);
  }

  protected genderLabel(gender: Gender | null | undefined): string {
    switch (gender) {
      case 'MALE':
        return 'Masculino';
      case 'FEMALE':
        return 'Femenino';
      case 'OTHER':
        return 'Otro';
      default:
        return 'Sin especificar';
    }
  }
}
