import { todayIsoDate } from './today-date';

/** Horas del día (0–23) para jornada sin minutos. */
export const ATTENDANCE_HOUR_OPTIONS = Array.from({ length: 24 }, (_, hour) => hour);

/** Convierte hora 24h a texto 12h con AM/PM (ej. 14 → "2:00 PM"). */
export function formatHour12(hour24: number): string {
  const period = hour24 < 12 ? 'AM' : 'PM';
  const hour12 = hour24 % 12 === 0 ? 12 : hour24 % 12;
  return `${hour12}:00 ${period}`;
}

export function formatAttendanceHour(iso?: string | null): string {
  if (!iso) {
    return '—';
  }
  return formatHour12(new Date(iso).getHours());
}

export function hourFromIso(iso?: string | null): number | null {
  if (!iso) {
    return null;
  }
  return new Date(iso).getHours();
}

export function dateTimeFromHour(hour: number): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${todayIsoDate()}T${pad(hour)}:00`;
}

export function hourOptionLabel(hour: number): string {
  return formatHour12(hour);
}
