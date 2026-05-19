import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const staffGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/ingresar']);
  }
  if (auth.isAffiliate()) {
    return router.createUrlTree(['/mi-cuenta']);
  }
  return true;
};
