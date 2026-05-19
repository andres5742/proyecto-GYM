import { formatCurrency, getCurrencySymbol } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { APP_CURRENCY, APP_LOCALE } from '../constants/currency';

/** Pesos colombianos sin decimales (ej. $ 12.000 en lugar de $ 12.000,00). */
@Pipe({ name: 'cop', standalone: true })
export class CopCurrencyPipe implements PipeTransform {
  transform(value: number | string | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    const num = typeof value === 'number' ? value : Number(value);
    if (Number.isNaN(num)) {
      return '—';
    }
    return formatCurrency(
      num,
      APP_LOCALE,
      getCurrencySymbol(APP_CURRENCY, 'narrow'),
      APP_CURRENCY,
      '1.0-0',
    );
  }
}
