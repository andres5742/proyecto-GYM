import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HomeCarousel } from '../../components/home-carousel/home-carousel';
import { TrainerRatingModal } from '../../components/trainer-rating-modal/trainer-rating-modal';
import { HomeMediaGallery } from '../../components/home-media-gallery/home-media-gallery';
import { BusinessDaySchedule } from '../../core/models/business-hours.model';
import { Holiday } from '../../core/models/holiday.model';
import { WallPost } from '../../core/models/wall-post.model';
import { BusinessHoursService } from '../../core/services/business-hours.service';
import { HolidayService } from '../../core/services/holiday.service';
import { ModuleService } from '../../core/services/module.service';
import { WallPostService } from '../../core/services/wall-post.service';
import { UploadUrlPipe } from '../../core/pipes/upload-url.pipe';
import { DAY_LABELS, formatTime12, MONTH_LABELS, WEEKDAY_SHORT } from '../../core/utils/time-format';

export interface CalendarCell {
  day: number;
  iso: string;
  isToday: boolean;
  isHoliday: boolean;
  holidayName?: string;
}

@Component({
  selector: 'app-home-wall',
  imports: [DatePipe, HomeCarousel, HomeMediaGallery, TrainerRatingModal, UploadUrlPipe],
  templateUrl: './home-wall.html',
  styleUrl: './home-wall.scss',
})
export class HomeWall implements OnInit {
  protected readonly modules = inject(ModuleService);
  private readonly holidayService = inject(HolidayService);
  private readonly businessHoursService = inject(BusinessHoursService);
  private readonly wallPostService = inject(WallPostService);

  protected readonly monthLabels = MONTH_LABELS;
  protected readonly weekdayShort = WEEKDAY_SHORT;
  protected readonly dayLabels = DAY_LABELS;

  protected readonly loading = signal(true);
  protected readonly calendarYear = signal(new Date().getFullYear());
  protected readonly calendarMonth = signal(new Date().getMonth() + 1);
  protected readonly holidays = signal<Holiday[]>([]);
  protected readonly businessDays = signal<BusinessDaySchedule[]>([]);
  protected readonly posts = signal<WallPost[]>([]);

  protected readonly calendarTitle = computed(
    () => `${MONTH_LABELS[this.calendarMonth() - 1]} ${this.calendarYear()}`,
  );

  protected readonly sundaySchedule = computed(() =>
    this.businessDays().find((d) => d.dayOfWeek === 'SUNDAY'),
  );

  protected readonly todayHolidayNotice = computed(() => {
    const today = this.todayIso();
    const holiday = this.holidays().find((h) => h.date === today);
    if (!holiday || !this.isWeekdayHoliday(holiday.date)) {
      return null;
    }
    const sunday = this.sundaySchedule();
    const hours = sunday ? this.formatHours(sunday) : 'ver horario de domingo';
    return `Hoy es festivo (${holiday.name}). Atención con horario de domingo: ${hours}.`;
  });

  protected readonly calendarCells = computed(() => {
    const year = this.calendarYear();
    const month = this.calendarMonth();
    const first = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0).getDate();
    // Lunes = primer día de la semana (getDay: 0=Dom … 6=Sáb)
    const dow = first.getDay();
    const pad = dow === 0 ? 6 : dow - 1;
    const today = this.todayIso();
    const holidayMap = new Map(this.holidays().map((h) => [h.date, h]));

    const cells: (CalendarCell | null)[] = Array.from({ length: pad }, () => null);
    for (let day = 1; day <= lastDay; day++) {
      const iso = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const holiday = holidayMap.get(iso);
      cells.push({
        day,
        iso,
        isToday: iso === today,
        isHoliday: !!holiday,
        holidayName: holiday?.name,
      });
    }
    return cells;
  });

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.holidayService.findByMonth(this.calendarYear(), this.calendarMonth()).subscribe({
      next: (holidays) => this.holidays.set(holidays),
    });
    this.businessHoursService.get().subscribe({
      next: (hours) => this.businessDays.set(hours.days),
    });
    this.wallPostService.findActive().subscribe({
      next: (posts) => {
        this.posts.set(posts);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  prevMonth(): void {
    if (this.calendarMonth() === 1) {
      this.calendarYear.update((y) => y - 1);
      this.calendarMonth.set(12);
    } else {
      this.calendarMonth.update((m) => m - 1);
    }
    this.loadHolidays();
  }

  nextMonth(): void {
    if (this.calendarMonth() === 12) {
      this.calendarYear.update((y) => y + 1);
      this.calendarMonth.set(1);
    } else {
      this.calendarMonth.update((m) => m + 1);
    }
    this.loadHolidays();
  }

  loadHolidays(): void {
    this.holidayService.findByMonth(this.calendarYear(), this.calendarMonth()).subscribe({
      next: (holidays) => this.holidays.set(holidays),
    });
  }

  formatHours(day: BusinessDaySchedule): string {
    if (day.closed) {
      return 'Cerrado';
    }
    return `${formatTime12(day.openTime)} – ${formatTime12(day.closeTime)}`;
  }

  /** Festivo entre semana (lun–sáb): aplica horario de domingo. */
  isWeekdayHoliday(dateIso: string): boolean {
    const [year, month, day] = dateIso.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    const dow = date.getDay();
    return dow >= 1 && dow <= 6;
  }

  holidayUsesSundayHours(holiday: Holiday): boolean {
    return this.isWeekdayHoliday(holiday.date);
  }

  holidaySundayHoursLabel(holiday: Holiday): string {
    const sunday = this.sundaySchedule();
    if (!sunday) {
      return 'Horario de domingo';
    }
    return `Horario de domingo: ${this.formatHours(sunday)}`;
  }

  cellHolidayTitle(cell: CalendarCell): string {
    if (!cell.isHoliday) {
      return '';
    }
    const parts = [cell.holidayName ?? 'Festivo'];
    if (this.isWeekdayHoliday(cell.iso)) {
      const sunday = this.sundaySchedule();
      parts.push(
        sunday
          ? `Atención con horario de domingo (${this.formatHours(sunday)})`
          : 'Atención con horario de domingo',
      );
    }
    return parts.join(' · ');
  }

  categoryClass(category: WallPost['category']): string {
    return category.toLowerCase();
  }

  postMeta(post: WallPost): string {
    const date = new Date(post.publishedAt).toLocaleDateString('es-CO', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
    return `${post.authorRoleLabel} · ${date}`;
  }

  visibilityLabel(post: WallPost): string {
    if (post.permanent) {
      return 'Permanente';
    }
    if (post.expiresAt) {
      const until = new Date(post.expiresAt).toLocaleDateString('es-CO', {
        day: 'numeric',
        month: 'short',
      });
      return `Visible ${post.displayDays} días · hasta ${until}`;
    }
    return `Visible ${post.displayDays} días`;
  }

  private todayIso(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}
