import { Routes } from '@angular/router';
import { affiliateGuard } from './core/guards/affiliate.guard';
import { guestGuard } from './core/guards/auth.guard';
import { moduleGuard } from './core/guards/module.guard';
import { roleGuard } from './core/guards/role.guard';
import { staffGuard } from './core/guards/staff.guard';
import { MainLayout } from './layout/main-layout';
import { PublicLayout } from './layout/public-layout';
import { HomeWall } from './pages/home/home-wall';
import { Dashboard } from './pages/dashboard/dashboard';
import { Members } from './pages/members/members';
import { Employees } from './pages/employees/employees';
import { Inventory } from './pages/inventory/inventory';
import { Plans } from './pages/plans/plans';
import { Attendance } from './pages/attendance/attendance';
import { PayrollConfigPage } from './pages/payroll-config/payroll-config';
import { AccessControlPage } from './pages/access-control/access-control';
import { AccessKiosk } from './pages/access-kiosk/access-kiosk';
import { FeedbackInboxPage } from './pages/feedback-inbox/feedback-inbox';
import { TrainerRatingsAdminPage } from './pages/trainer-ratings-admin/trainer-ratings-admin';
import { ModulesAdminPage } from './pages/modules-admin/modules-admin';
import { PortalContentPage } from './pages/portal-content/portal-content';
import { BillingPage } from './pages/billing/billing';
import { Sales } from './pages/sales/sales';
import { ShiftHandoverPage } from './pages/shift-handover/shift-handover';
import { CashShortfallsPage } from './pages/cash-shortfalls/cash-shortfalls';
import { ProductCreditsPage } from './pages/product-credits/product-credits';
import { Login } from './pages/login/login';
import { MyAccount } from './pages/my-account/my-account';

const superAdminRoles = ['SUPER_ADMIN'] as const;

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [{ path: '', component: HomeWall }],
  },
  /** Torniquete: sin login, solo verificación biométrica. */
  {
    path: 'acceso',
    component: AccessKiosk,
  },
  {
    path: 'login',
    redirectTo: 'ingresar',
    pathMatch: 'full',
  },
  {
    path: 'ingresar',
    component: Login,
    canActivate: [guestGuard],
  },
  {
    path: 'mi-cuenta',
    component: MyAccount,
    canActivate: [affiliateGuard],
  },
  {
    path: 'panel',
    component: MainLayout,
    canActivate: [staffGuard],
    children: [
      { path: '', component: Dashboard },
      { path: 'socios', redirectTo: 'afiliados', pathMatch: 'full' },
      {
        path: 'afiliados',
        component: Members,
        canActivate: [moduleGuard],
        data: { moduleKey: 'SOCIOS' },
      },
      {
        path: 'planes',
        component: Plans,
        canActivate: [moduleGuard],
        data: { moduleKey: 'PLANES' },
      },
      {
        path: 'inventario',
        component: Inventory,
        canActivate: [moduleGuard],
        data: { moduleKey: 'INVENTARIO' },
      },
      {
        path: 'entrenadores',
        component: Employees,
        canActivate: [moduleGuard],
        data: { moduleKey: 'ENTRENADORES' },
      },
      { path: 'empleados', redirectTo: 'entrenadores', pathMatch: 'full' },
      {
        path: 'ventas',
        component: Sales,
        canActivate: [moduleGuard],
        data: { moduleKey: 'VENTAS' },
      },
      {
        path: 'facturacion',
        component: BillingPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'FACTURACION' },
      },
      {
        path: 'entrega-turno',
        component: ShiftHandoverPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'ENTREGA_TURNO' },
      },
      {
        path: 'descuadres-caja',
        component: CashShortfallsPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'DESCUADRES_CAJA' },
      },
      {
        path: 'fiado',
        component: ProductCreditsPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'FIADO' },
      },
      {
        path: 'jornada',
        component: Attendance,
        canActivate: [moduleGuard],
        data: { moduleKey: 'JORNADA' },
      },
      {
        path: 'configuracion-nomina',
        component: PayrollConfigPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'NOMINA' },
      },
      {
        path: 'contenido-inicio',
        component: PortalContentPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'CONTENIDO_INICIO' },
      },
      {
        path: 'buzon',
        component: FeedbackInboxPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'BUZON' },
      },
      {
        path: 'calificaciones',
        component: TrainerRatingsAdminPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'CALIFICACIONES' },
      },
      {
        path: 'acceso',
        component: AccessControlPage,
        canActivate: [moduleGuard],
        data: { moduleKey: 'ACCESO' },
      },
      {
        path: 'modulos',
        component: ModulesAdminPage,
        canActivate: [roleGuard],
        data: { roles: superAdminRoles },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
