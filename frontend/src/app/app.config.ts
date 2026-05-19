import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  DEFAULT_CURRENCY_CODE,
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import localeEsCo from '@angular/common/locales/es-CO';

import { APP_CURRENCY, APP_LOCALE } from './core/constants/currency';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { ModuleService } from './core/services/module.service';
import { routes } from './app.routes';

registerLocaleData(localeEsCo);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(() => {
      const modules = inject(ModuleService);
      return modules.loadPublic();
    }),
    { provide: LOCALE_ID, useValue: APP_LOCALE },
    { provide: DEFAULT_CURRENCY_CODE, useValue: APP_CURRENCY },
  ],
};
