import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  CarouselSlide,
  GymMediaItem,
  MediaType,
  SiteFooter,
} from '../../core/models/home-content.model';
import { AuthService } from '../../core/services/auth.service';
import { UploadUrlPipe } from '../../core/pipes/upload-url.pipe';
import { HomeContentService } from '../../core/services/home-content.service';

export type HomeAdminTab = 'slider' | 'media' | 'footer';

@Component({
  selector: 'app-portal-home-admin',
  imports: [ReactiveFormsModule, UploadUrlPipe],
  templateUrl: './portal-home-admin.html',
  styleUrl: './portal-home-admin.scss',
})
export class PortalHomeAdmin implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly homeContent = inject(HomeContentService);

  readonly tab = input.required<HomeAdminTab>();
  readonly notify = output<string | null>();

  protected readonly saving = signal(false);
  protected readonly uploading = signal(false);
  protected readonly slides = signal<CarouselSlide[]>([]);
  protected readonly mediaItems = signal<GymMediaItem[]>([]);
  protected readonly editingSlideId = signal<number | null>(null);
  protected readonly editingMediaId = signal<number | null>(null);

  protected readonly slideForm = this.fb.nonNullable.group({
    imageUrl: ['', [Validators.required, Validators.maxLength(500)]],
    title: [''],
    caption: [''],
    linkUrl: [''],
    active: [true],
  });

  protected readonly mediaForm = this.fb.nonNullable.group({
    mediaType: ['PHOTO' as MediaType, Validators.required],
    mediaUrl: ['', [Validators.required, Validators.maxLength(500)]],
    thumbnailUrl: [''],
    title: [''],
    active: [true],
  });

  protected readonly footerForm = this.fb.nonNullable.group({
    tagline: [''],
    address: [''],
    phone: [''],
    instagramUrl: [''],
    facebookUrl: [''],
    tiktokUrl: [''],
    youtubeUrl: [''],
    whatsappUrl: [''],
  });

  ngOnInit(): void {
    if (!this.auth.isAdmin()) {
      this.flash('Solo administradores pueden gestionar slider, fotos y redes.');
      return;
    }
    if (!this.auth.getToken()) {
      this.flash('Inicia sesión como administrador (andres.perez o superadmin).');
      return;
    }
    this.auth.loadProfile().subscribe({
      next: () => this.initTab(),
      error: () => {
        this.flash('Sesión expirada o inválida. Cierra sesión e inicia de nuevo.');
        this.auth.logout();
      },
    });
  }

  private initTab(): void {
    if (this.tab() === 'slider') {
      this.loadSlides();
      this.resetSlideForm();
    } else if (this.tab() === 'media') {
      this.loadMedia();
      this.resetMediaForm();
    } else {
      this.loadFooter();
    }
  }

  private flash(msg: string | null): void {
    this.notify.emit(msg);
  }

  private apiError(err: unknown, fallback: string): string {
    const http = err as HttpErrorResponse;
    if (http?.status === 403) {
      return 'Acceso denegado. Cierra sesión e inicia con andres.perez / andres.perez574 (admin) o superadmin / admin123. No uses cuenta de entrenador.';
    }
    if (http?.status === 401) {
      return 'Sesión expirada. Cierra sesión e inicia de nuevo.';
    }
    return http?.error?.message ?? fallback;
  }

  onSlideFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.homeContent.uploadImage(file).subscribe({
      next: ({ url }) => {
        this.slideForm.patchValue({ imageUrl: url });
        this.flash('Imagen subida correctamente');
        this.uploading.set(false);
        input.value = '';
      },
      error: (err) => {
        this.flash(this.apiError(err, 'No se pudo subir la imagen'));
        this.uploading.set(false);
        input.value = '';
      },
    });
  }

  onMediaPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.homeContent.uploadImage(file).subscribe({
      next: ({ url }) => {
        this.mediaForm.patchValue({ mediaType: 'PHOTO', mediaUrl: url });
        this.flash('Foto subida correctamente');
        this.uploading.set(false);
        input.value = '';
      },
      error: (err) => {
        this.flash(this.apiError(err, 'No se pudo subir la foto'));
        this.uploading.set(false);
        input.value = '';
      },
    });
  }

  loadSlides(): void {
    this.homeContent.findAllCarousel().subscribe({
      next: (list) => this.slides.set(list),
      error: (err) => this.flash(this.apiError(err, 'No se pudo cargar el slider')),
    });
  }

  resetSlideForm(): void {
    this.editingSlideId.set(null);
    this.slideForm.reset({ imageUrl: '', title: '', caption: '', linkUrl: '', active: true });
  }

  editSlide(s: CarouselSlide): void {
    this.editingSlideId.set(s.id);
    this.slideForm.patchValue({
      imageUrl: s.imageUrl,
      title: s.title ?? '',
      caption: s.caption ?? '',
      linkUrl: s.linkUrl ?? '',
      active: s.active,
    });
  }

  saveSlide(): void {
    if (this.slideForm.invalid) {
      this.slideForm.markAllAsTouched();
      return;
    }
    const raw = this.slideForm.getRawValue();
    const id = this.editingSlideId();
    this.saving.set(true);
    const req = {
      imageUrl: raw.imageUrl,
      title: raw.title || undefined,
      caption: raw.caption || undefined,
      linkUrl: raw.linkUrl || undefined,
      active: raw.active,
    };
    const action = id
      ? this.homeContent.updateCarousel(id, req)
      : this.homeContent.createCarousel(req);
    action.subscribe({
      next: () => {
        this.flash(id ? 'Imagen del slider actualizada' : 'Imagen agregada al slider');
        this.saving.set(false);
        this.resetSlideForm();
        this.loadSlides();
      },
      error: (err) => {
        this.flash(this.apiError(err, 'No se pudo guardar'));
        this.saving.set(false);
      },
    });
  }

  removeSlide(id: number): void {
    if (!confirm('¿Eliminar esta imagen del slider?')) {
      return;
    }
    this.homeContent.deleteCarousel(id).subscribe({
      next: () => {
        this.flash('Imagen eliminada');
        this.loadSlides();
      },
      error: () => this.flash('No se pudo eliminar'),
    });
  }

  loadMedia(): void {
    this.homeContent.findAllMedia().subscribe({
      next: (list) => this.mediaItems.set(list),
      error: (err) => this.flash(this.apiError(err, 'No se pudo cargar fotos y videos')),
    });
  }

  resetMediaForm(): void {
    this.editingMediaId.set(null);
    this.mediaForm.reset({
      mediaType: 'PHOTO',
      mediaUrl: '',
      thumbnailUrl: '',
      title: '',
      active: true,
    });
  }

  editMedia(m: GymMediaItem): void {
    this.editingMediaId.set(m.id);
    this.mediaForm.patchValue({
      mediaType: m.mediaType,
      mediaUrl: m.mediaUrl,
      thumbnailUrl: m.thumbnailUrl ?? '',
      title: m.title ?? '',
      active: m.active,
    });
  }

  saveMedia(): void {
    if (this.mediaForm.invalid) {
      this.mediaForm.markAllAsTouched();
      return;
    }
    const raw = this.mediaForm.getRawValue();
    const id = this.editingMediaId();
    this.saving.set(true);
    const req = {
      mediaType: raw.mediaType,
      mediaUrl: raw.mediaUrl,
      thumbnailUrl: raw.thumbnailUrl || undefined,
      title: raw.title || undefined,
      active: raw.active,
    };
    const action = id ? this.homeContent.updateMedia(id, req) : this.homeContent.createMedia(req);
    action.subscribe({
      next: () => {
        this.flash(id ? 'Medio actualizado' : 'Medio agregado');
        this.saving.set(false);
        this.resetMediaForm();
        this.loadMedia();
      },
      error: (err) => {
        this.flash(this.apiError(err, 'No se pudo guardar'));
        this.saving.set(false);
      },
    });
  }

  removeMedia(id: number): void {
    if (!confirm('¿Eliminar este foto/video?')) {
      return;
    }
    this.homeContent.deleteMedia(id).subscribe({
      next: () => {
        this.flash('Medio eliminado');
        this.loadMedia();
      },
      error: () => this.flash('No se pudo eliminar'),
    });
  }

  loadFooter(): void {
    this.homeContent.getFooter().subscribe({
      next: (f) => this.footerForm.patchValue({
        tagline: f.tagline ?? '',
        address: f.address ?? '',
        phone: f.phone ?? '',
        instagramUrl: f.instagramUrl ?? '',
        facebookUrl: f.facebookUrl ?? '',
        tiktokUrl: f.tiktokUrl ?? '',
        youtubeUrl: f.youtubeUrl ?? '',
        whatsappUrl: f.whatsappUrl ?? '',
      }),
      error: (err) => this.flash(this.apiError(err, 'No se pudo cargar el pie de página')),
    });
  }

  saveFooter(): void {
    this.saving.set(true);
    this.homeContent.updateFooter(this.footerForm.getRawValue() as SiteFooter).subscribe({
      next: () => {
        this.flash('Pie de página y redes guardados');
        this.saving.set(false);
      },
      error: () => {
        this.flash('No se pudo guardar');
        this.saving.set(false);
      },
    });
  }
}
