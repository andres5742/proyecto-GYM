import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Adjunta JWT y cierra sesión si el token ya no es válido (KISS). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      const isLogin = req.url.includes('/auth/login');
      if (!isLogin && token && error.status === 401) {
        auth.logout();
        router.navigate(['/']);
      }
      return throwError(() => error);
    }),
  );
};
