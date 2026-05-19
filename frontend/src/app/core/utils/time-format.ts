export type AmPm = 'AM' | 'PM';

export interface Time12Parts {
  hour12: number;
  period: AmPm;
}

export const HOUR_12_OPTIONS = Array.from({ length: 12 }, (_, i) => i + 1);

/** Convierte hora 12h + AM/PM a "HH:mm" (24h). */
export function toTime24(hour12: number, period: AmPm): string {
  const h12 = Math.min(12, Math.max(1, hour12));
  let hour24 = h12 % 12;
  if (period === 'PM') {
    hour24 += 12;
  }
  return `${String(hour24).padStart(2, '0')}:00`;
}

/** Parsea "HH:mm" o "HH:mm:ss" a hora 12h + AM/PM. */
export function parseTime12Parts(time?: string | null): Time12Parts {
  if (!time) {
    return { hour12: 8, period: 'AM' };
  }
  const [hourStr] = time.split(':');
  const hour24 = parseInt(hourStr, 10);
  const period: AmPm = hour24 >= 12 ? 'PM' : 'AM';
  const hour12 = hour24 % 12 === 0 ? 12 : hour24 % 12;
  return { hour12, period };
}

/** Formatea "HH:mm" o "HH:mm:ss" a texto 12h. */
export function formatTime12(time?: string | null): string {
  if (!time) {
    return '—';
  }
  const [h, m] = time.split(':').map(Number);
  const period = h < 12 ? 'AM' : 'PM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  const mins = m > 0 ? `:${String(m).padStart(2, '0')}` : ':00';
  return `${hour12}${mins} ${period}`;
}

export const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

export const MONTH_LABELS = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
];

/** Semana iniciando en lunes (estándar Colombia). */
export const WEEKDAY_SHORT = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
