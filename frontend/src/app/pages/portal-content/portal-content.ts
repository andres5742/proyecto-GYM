import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { BusinessDaySchedule, DayOfWeek } from '../../core/models/business-hours.model';
import { Holiday, HolidayRequest } from '../../core/models/holiday.model';
import { PostCategory, WallPost, WallPostRequest } from '../../core/models/wall-post.model';
import { BusinessHoursService } from '../../core/services/business-hours.service';
import { HolidayService } from '../../core/services/holiday.service';
import { HomeContentService } from '../../core/services/home-content.service';
import { WallPostService } from '../../core/services/wall-post.service';
import {
  AmPm,
  DAY_LABELS,
  formatTime12,
  HOUR_12_OPTIONS,
  parseTime12Parts,
  toTime24,
} from '../../core/utils/time-format';
import { EmojiPickerComponent } from '../../components/emoji-picker/emoji-picker';
import { UploadUrlPipe } from '../../core/pipes/upload-url.pipe';
import { PortalHomeAdmin } from './portal-home-admin';

type Tab = 'hours' | 'holidays' | 'posts' | 'slider' | 'media' | 'footer';

/** Fila de horario con campos 12h enlazados al formulario. */
type ScheduleRow = BusinessDaySchedule & {
  openHour12: number;
  openPeriod: AmPm;
  closeHour12: number;
  closePeriod: AmPm;
};

const DAY_ORDER: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

const POST_CATEGORIES: { value: PostCategory; label: string }[] = [
  { value: 'AVISO', label: 'Aviso' },
  { value: 'PROMO', label: 'Promoción' },
  { value: 'HORARIO', label: 'Horario' },
  { value: 'MOTIVACION', label: 'Motivación' },
];

@Component({
  selector: 'app-portal-content',
  imports: [ReactiveFormsModule, FormsModule, DatePipe, PortalHomeAdmin, EmojiPickerComponent, UploadUrlPipe],
  templateUrl: './portal-content.html',
  styleUrl: './portal-content.scss',
})
export class PortalContentPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly businessHoursService = inject(BusinessHoursService);
  private readonly holidayService = inject(HolidayService);
  private readonly wallPostService = inject(WallPostService);
  private readonly homeContent = inject(HomeContentService);

  protected readonly dayLabels = DAY_LABELS;
  protected readonly hour12Options = HOUR_12_OPTIONS;
  protected readonly postCategories = POST_CATEGORIES;
  protected readonly activeTab = signal<Tab>('hours');
  protected readonly message = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly scheduleRows = signal<ScheduleRow[]>([]);
  protected readonly formatTime12 = formatTime12;
  protected readonly holidays = signal<Holiday[]>([]);
  protected readonly posts = signal<WallPost[]>([]);
  protected readonly editingHolidayId = signal<number | null>(null);
  protected readonly editingPostId = signal<number | null>(null);
  protected readonly postImageUrls = signal<string[]>([]);
  protected readonly postPhotoUploading = signal(false);
  protected readonly maxPostPhotos = 8;

  protected readonly holidayForm = this.fb.nonNullable.group({
    date: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
  });

  protected readonly postForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    body: ['', [Validators.required, Validators.maxLength(5000)]],
    emoji: ['📌'],
    category: ['AVISO' as PostCategory, Validators.required],
    permanent: [true],
    displayDays: [7, [Validators.min(1)]],
  });

  ngOnInit(): void {
    this.startPostCreate();
    this.startHolidayCreate();
    this.loadHours();
    this.loadHolidays();
    this.loadPosts();
    this.postForm.get('permanent')?.valueChanges.subscribe((permanent) => {
      const daysCtrl = this.postForm.controls.displayDays;
      if (permanent) {
        daysCtrl.disable();
      } else {
        daysCtrl.enable();
      }
    });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.message.set(null);
    if (tab === 'hours') {
      this.loadHours();
    }
  }

  loadHours(): void {
    this.businessHoursService.get().subscribe({
      next: (res) => {
        const days = res.days?.length === 7 ? res.days : this.defaultSchedule();
        const sorted = days
          .map((d) => this.toScheduleRow(d))
          .sort((a, b) => DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek));
        this.scheduleRows.set(sorted);
      },
      error: () => {
        this.scheduleRows.set(this.defaultSchedule().map((d) => this.toScheduleRow(d)));
        this.message.set('No se pudieron cargar los horarios. Puede configurarlos y guardar.');
      },
    });
  }

  private defaultSchedule(): BusinessDaySchedule[] {
    return DAY_ORDER.map((dayOfWeek) => ({
      dayOfWeek,
      openTime: dayOfWeek === 'SUNDAY' ? '07:00' : dayOfWeek === 'SATURDAY' ? '06:00' : '05:00',
      closeTime: dayOfWeek === 'SUNDAY' ? '14:00' : dayOfWeek === 'SATURDAY' ? '20:00' : '22:00',
      closed: false,
    }));
  }

  patchOpenHour(day: ScheduleRow, hour12: number): void {
    this.patchRow(day.dayOfWeek, (r) => ({
      ...r,
      openHour12: hour12,
      openTime: toTime24(hour12, r.openPeriod),
    }));
  }

  patchOpenPeriod(day: ScheduleRow, period: AmPm): void {
    this.patchRow(day.dayOfWeek, (r) => ({
      ...r,
      openPeriod: period,
      openTime: toTime24(r.openHour12, period),
    }));
  }

  patchCloseHour(day: ScheduleRow, hour12: number): void {
    this.patchRow(day.dayOfWeek, (r) => ({
      ...r,
      closeHour12: hour12,
      closeTime: toTime24(hour12, r.closePeriod),
    }));
  }

  patchClosePeriod(day: ScheduleRow, period: AmPm): void {
    this.patchRow(day.dayOfWeek, (r) => ({
      ...r,
      closePeriod: period,
      closeTime: toTime24(r.closeHour12, period),
    }));
  }

  toggleClosed(day: ScheduleRow): void {
    this.scheduleRows.update((rows) =>
      rows.map((r) => {
        if (r.dayOfWeek !== day.dayOfWeek) {
          return r;
        }
        const closed = !r.closed;
        if (closed) {
          return { ...r, closed: true, openTime: null, closeTime: null };
        }
        const defaults = this.toScheduleRow(
          this.defaultSchedule().find((d) => d.dayOfWeek === r.dayOfWeek)!,
        );
        return { ...defaults, closed: false };
      }),
    );
  }

  rowPreview(day: ScheduleRow): string {
    if (day.closed) {
      return 'Cerrado';
    }
    return `${formatTime12(day.openTime)} – ${formatTime12(day.closeTime)}`;
  }

  saveHours(): void {
    if (this.scheduleRows().length !== 7) {
      this.message.set('Faltan días en el horario. Recargue la página.');
      this.loadHours();
      return;
    }
    this.saving.set(true);
    const payload = this.scheduleRows().map((row) => ({
      dayOfWeek: row.dayOfWeek,
      openTime: row.closed ? null : this.normalizeTime(row.openTime),
      closeTime: row.closed ? null : this.normalizeTime(row.closeTime),
      closed: row.closed,
    }));
    this.businessHoursService.update(payload).subscribe({
      next: (res) => {
        const days = res.days ?? [];
        this.scheduleRows.set(
          days.map((d) => this.toScheduleRow(d)).sort(
            (a, b) => DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek),
          ),
        );
        this.message.set('Horarios de atención guardados');
        this.saving.set(false);
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudieron guardar los horarios');
        this.saving.set(false);
      },
    });
  }

  loadHolidays(): void {
    const now = new Date();
    this.holidayService.findByMonth(now.getFullYear(), now.getMonth() + 1).subscribe({
      next: (list) => this.holidays.set(list),
    });
  }

  startHolidayCreate(): void {
    this.editingHolidayId.set(null);
    this.holidayForm.reset({ date: '', name: '', description: '' });
  }

  startHolidayEdit(h: Holiday): void {
    this.editingHolidayId.set(h.id);
    this.holidayForm.patchValue({
      date: h.date,
      name: h.name,
      description: h.description ?? '',
    });
  }

  saveHoliday(): void {
    if (this.holidayForm.invalid) {
      this.holidayForm.markAllAsTouched();
      return;
    }
    const raw = this.holidayForm.getRawValue();
    const request: HolidayRequest = {
      date: raw.date,
      name: raw.name,
      description: raw.description || undefined,
    };
    const id = this.editingHolidayId();
    this.saving.set(true);
    const action = id
      ? this.holidayService.update(id, request)
      : this.holidayService.create(request);
    action.subscribe({
      next: () => {
        this.message.set(id ? 'Festivo actualizado' : 'Festivo registrado');
        this.saving.set(false);
        this.startHolidayCreate();
        this.loadHolidays();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo guardar el festivo');
        this.saving.set(false);
      },
    });
  }

  removeHoliday(id: number): void {
    if (!confirm('¿Eliminar este día festivo?')) {
      return;
    }
    this.holidayService.delete(id).subscribe({
      next: () => {
        this.message.set('Festivo eliminado');
        this.loadHolidays();
      },
      error: () => this.message.set('No se pudo eliminar'),
    });
  }

  loadPosts(): void {
    this.wallPostService.findAllForAdmin().subscribe({
      next: (posts) => this.posts.set(posts),
    });
  }

  startPostCreate(): void {
    this.editingPostId.set(null);
    this.postImageUrls.set([]);
    this.postForm.reset({
      title: '',
      body: '',
      emoji: '📌',
      category: 'AVISO',
      permanent: true,
      displayDays: 7,
    });
    this.postForm.controls.displayDays.disable();
  }

  startPostEdit(post: WallPost): void {
    this.editingPostId.set(post.id);
    this.postImageUrls.set([...(post.imageUrls ?? [])]);
    this.postForm.patchValue({
      title: post.title,
      body: post.body,
      emoji: post.emoji ?? '📌',
      category: post.category,
      permanent: post.permanent,
      displayDays: post.displayDays ?? 7,
    });
    if (post.permanent) {
      this.postForm.controls.displayDays.disable();
    } else {
      this.postForm.controls.displayDays.enable();
    }
  }

  savePost(): void {
    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }
    const raw = this.postForm.getRawValue();
    const request: WallPostRequest = {
      title: raw.title,
      body: raw.body,
      emoji: raw.emoji || undefined,
      category: raw.category,
      permanent: raw.permanent,
      displayDays: raw.permanent ? undefined : raw.displayDays,
      imageUrls: this.postImageUrls().length > 0 ? this.postImageUrls() : [],
    };
    const id = this.editingPostId();
    this.saving.set(true);
    const action = id ? this.wallPostService.update(id, request) : this.wallPostService.create(request);
    action.subscribe({
      next: () => {
        this.message.set(
          id ? 'Publicación actualizada en el muro' : 'Publicación publicada en el muro de inicio',
        );
        this.saving.set(false);
        this.startPostCreate();
        this.loadPosts();
      },
      error: (err) => {
        this.message.set(err?.error?.message ?? 'No se pudo guardar la publicación');
        this.saving.set(false);
      },
    });
  }

  removePost(id: number): void {
    if (!confirm('¿Eliminar esta publicación?')) {
      return;
    }
    this.wallPostService.delete(id).subscribe({
      next: () => {
        this.message.set('Publicación eliminada');
        this.loadPosts();
      },
      error: () => this.message.set('No se pudo eliminar'),
    });
  }

  private toScheduleRow(d: BusinessDaySchedule): ScheduleRow {
    const open = parseTime12Parts(d.openTime);
    const close = parseTime12Parts(d.closeTime);
    return {
      dayOfWeek: d.dayOfWeek,
      openTime: d.openTime ? d.openTime.slice(0, 5) : null,
      closeTime: d.closeTime ? d.closeTime.slice(0, 5) : null,
      closed: d.closed,
      openHour12: open.hour12,
      openPeriod: open.period,
      closeHour12: close.hour12,
      closePeriod: close.period,
    };
  }

  private patchRow(dayOfWeek: DayOfWeek, patch: (row: ScheduleRow) => ScheduleRow): void {
    this.scheduleRows.update((rows) =>
      rows.map((r) => (r.dayOfWeek === dayOfWeek ? patch(r) : r)),
    );
  }

  private normalizeTime(value?: string | null): string | null {
    if (!value) {
      return null;
    }
    return value.length === 5 ? `${value}:00` : value;
  }

  onPostPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files?.length) {
      return;
    }
    const slots = this.maxPostPhotos - this.postImageUrls().length;
    if (slots <= 0) {
      this.message.set(`Máximo ${this.maxPostPhotos} fotos por publicación`);
      input.value = '';
      return;
    }
    const toUpload = Array.from(files).slice(0, slots);
    this.postPhotoUploading.set(true);
    let completed = 0;
    const newUrls: string[] = [];
    toUpload.forEach((file) => {
      this.homeContent.uploadImage(file).subscribe({
        next: (res) => {
          newUrls.push(res.url);
          completed++;
          if (completed === toUpload.length) {
            this.postImageUrls.update((urls) => [...urls, ...newUrls]);
            this.postPhotoUploading.set(false);
            input.value = '';
          }
        },
        error: () => {
          this.message.set('No se pudo subir una de las fotos');
          this.postPhotoUploading.set(false);
          input.value = '';
        },
      });
    });
  }

  removePostImage(index: number): void {
    this.postImageUrls.update((urls) => urls.filter((_, i) => i !== index));
  }

  postVisibility(post: WallPost): string {
    if (post.permanent) {
      return 'Permanente';
    }
    const until = post.expiresAt
      ? new Date(post.expiresAt).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' })
      : '—';
    return `${post.displayDays} días · vence ${until}`;
  }
}
