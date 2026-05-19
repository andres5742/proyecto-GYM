import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

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
      const needsAuth =
        (req.url.includes('/api/home/') &&
          (req.url.includes('/all') || req.method !== 'GET' || req.url.includes('/upload'))) ||
        (req.url.includes('/api/feedback') && req.method !== 'POST') ||
        (req.url.includes('/api/trainer-ratings') &&
          req.method !== 'POST' &&
          !req.url.includes('/participants')) ||
        (req.url.includes('/api/modules') && !req.url.includes('/public'));
      if (!isLogin && token && (error.status === 401 || (error.status === 403 && needsAuth))) {
        auth.logout();
        router.navigate(['/']);
      }
      return throwError(() => error);
    }),
  );
};
