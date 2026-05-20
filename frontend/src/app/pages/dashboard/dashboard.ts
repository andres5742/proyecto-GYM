import { CopCurrencyPipe } from '../../core/pipes/cop-currency.pipe';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
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

  protected readonly apiStatus = signal('Comprobando...');
  protected readonly memberCount = signal(0);
  protected readonly planCount = signal(0);
  protected readonly productCount = signal(0);
  protected readonly inventoryValue = signal(0);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
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
}
