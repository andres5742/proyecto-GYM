/** Límites de fecha/hora solo para el día actual (hora local). */
export function todayIsoDate(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function todayDateTimeMin(): string {
  return `${todayIsoDate()}T00:00`;
}

export function todayDateTimeMax(): string {
  return `${todayIsoDate()}T23:59`;
}
