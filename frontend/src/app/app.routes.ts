import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { moduleGuard } from './core/guards/module.guard';
import { roleGuard } from './core/guards/role.guard';
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
import { Sales } from './pages/sales/sales';

const adminRoles = ['ADMIN', 'SUPER_ADMIN'] as const;
const superAdminRoles = ['SUPER_ADMIN'] as const;

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [{ path: '', component: HomeWall }],
  },
  {
    path: 'acceso',
    component: AccessKiosk,
    canActivate: [moduleGuard],
    data: { moduleKey: 'PUBLIC_ACCESO' },
  },
  {
    path: 'login',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: 'panel',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', component: Dashboard },
      {
        path: 'socios',
        component: Members,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'SOCIOS' },
      },
      {
        path: 'planes',
        component: Plans,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'PLANES' },
      },
      {
        path: 'inventario',
        component: Inventory,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'INVENTARIO' },
      },
      {
        path: 'entrenadores',
        component: Employees,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'ENTRENADORES' },
      },
      { path: 'empleados', redirectTo: 'entrenadores', pathMatch: 'full' },
      {
        path: 'ventas',
        component: Sales,
        canActivate: [moduleGuard],
        data: { moduleKey: 'VENTAS' },
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
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'NOMINA' },
      },
      {
        path: 'contenido-inicio',
        component: PortalContentPage,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'CONTENIDO_INICIO' },
      },
      {
        path: 'buzon',
        component: FeedbackInboxPage,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'BUZON' },
      },
      {
        path: 'calificaciones',
        component: TrainerRatingsAdminPage,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'CALIFICACIONES' },
      },
      {
        path: 'acceso',
        component: AccessControlPage,
        canActivate: [roleGuard, moduleGuard],
        data: { roles: adminRoles, moduleKey: 'ACCESO' },
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
