import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ModuleCode } from '../models/module.model';
import { AuthService } from '../services/auth.service';
import { ModuleService } from '../services/module.service';

export const moduleGuard: CanActivateFn = (route) => {
  const modules = inject(ModuleService);
  const auth = inject(AuthService);
  const router = inject(Router);
  const key = route.data['moduleKey'] as ModuleCode | undefined;

  if (!key) {
    return true;
  }
  if (auth.hasRole('SUPER_ADMIN')) {
    return true;
  }
  const scope = key.startsWith('PUBLIC_') ? 'public' : 'panel';
  if (modules.isEnabled(key, scope)) {
    return true;
  }
  return router.createUrlTree([key.startsWith('PUBLIC_') ? '/' : '/panel']);
};
