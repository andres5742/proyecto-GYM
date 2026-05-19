const MONTH_NAMES = [
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

export function monthYearLabel(month: number, year: number): string {
  const name = MONTH_NAMES[month - 1] ?? String(month);
  return `${name} ${year}`;
}

export function currentYearMonth(): { year: number; month: number } {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

export function historyYearOptions(): number[] {
  const current = new Date().getFullYear();
  const start = 2024;
  const years: number[] = [];
  for (let y = current; y >= start; y--) {
    years.push(y);
  }
  return years;
}

export const MONTH_OPTIONS = MONTH_NAMES.map((label, index) => ({
  value: index + 1,
  label,
}));
