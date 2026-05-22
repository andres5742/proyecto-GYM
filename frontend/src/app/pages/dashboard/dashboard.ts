import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UpcomingBirthday } from '../../core/models/dashboard.model';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { HealthService } from '../../core/services/health.service';
import { MemberService } from '../../core/services/member.service';
import { ModuleService } from '../../core/services/module.service';
import { PlanService } from '../../core/services/plan.service';
import { ProductService } from '../../core/services/product.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CopCurrencyPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly modules = inject(ModuleService);
  private readonly healthService = inject(HealthService);
  private readonly memberService = inject(MemberService);
  private readonly planService = inject(PlanService);
  private readonly productService = inject(ProductService);
  private readonly dashboardService = inject(DashboardService);

  protected readonly apiStatus = signal('Comprobando...');
  protected readonly upcomingBirthdays = signal<UpcomingBirthday[]>([]);
  protected readonly birthdaysLoading = signal(true);
  protected readonly showMemberBirthdays = signal(false);

  protected readonly trainerBirthdays = computed(() =>
    this.upcomingBirthdays().filter((b) => b.personType === 'EMPLOYEE'),
  );
  protected readonly memberBirthdays = computed(() =>
    this.upcomingBirthdays().filter((b) => b.personType === 'MEMBER'),
  );
  protected readonly memberCount = signal(0);
  protected readonly planCount = signal(0);
  protected readonly productCount = signal(0);
  protected readonly inventoryValue = signal(0);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.dashboardService.upcomingBirthdays(7).subscribe({
      next: (items) => {
        this.upcomingBirthdays.set(items);
        this.birthdaysLoading.set(false);
      },
      error: () => this.birthdaysLoading.set(false),
    });

    this.healthService.check().subscribe({
      next: (res) => this.apiStatus.set(res.status),
      error: () => this.apiStatus.set('Sin conexión'),
    });

    if (this.modules.isEnabled('SOCIOS')) {
      this.memberService.findAll().subscribe({
        next: (members) => this.memberCount.set(members.length),
        error: () => this.error.set('No se pudieron cargar los afiliados'),
      });
    }

    if (this.modules.isEnabled('PLANES')) {
      this.planService.findAll().subscribe({
        next: (plans) => this.planCount.set(plans.length),
      });
    }

    if (this.modules.isEnabled('INVENTARIO')) {
      this.productService.findAll().subscribe({
        next: (products) => {
          this.productCount.set(products.length);
          this.inventoryValue.set(products.reduce((sum, p) => sum + p.stockValue, 0));
        },
      });
    }
  }

  protected trainerBirthdayHeadline(): string {
    const count = this.trainerBirthdays().length;
    if (count === 0) {
      return 'Cumpleaños del equipo';
    }
    return count === 1
      ? '1 entrenador cumple años pronto'
      : `${count} entrenadores cumplen años pronto`;
  }

  protected memberBirthdaysToggleLabel(): string {
    const count = this.memberBirthdays().length;
    if (this.showMemberBirthdays()) {
      return 'Ocultar cumpleaños de afiliados';
    }
    if (count === 0) {
      return 'Ver cumpleaños próximos de afiliados';
    }
    return count === 1
      ? 'Ver 1 cumpleaños próximo de afiliados'
      : `Ver ${count} cumpleaños próximos de afiliados`;
  }

  protected toggleMemberBirthdays(): void {
    this.showMemberBirthdays.update((open) => !open);
  }

  protected birthdayMessage(item: UpcomingBirthday): string {
    const dateLabel = this.formatCelebrationDate(item.celebrationDate);
    if (item.daysUntil === 0) {
      return `Hoy cumple ${item.turningAge} años · ${dateLabel}`;
    }
    if (item.daysUntil === 1) {
      return `Mañana cumple ${item.turningAge} años · ${dateLabel}`;
    }
    return `En ${item.daysUntil} días cumple ${item.turningAge} años · ${dateLabel}`;
  }

  protected formatCelebrationDate(iso: string): string {
    const [y, m, d] = iso.split('-').map(Number);
    if (!y || !m || !d) {
      return iso;
    }
    return new Date(y, m - 1, d).toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    });
  }
}
