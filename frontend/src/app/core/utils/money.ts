/** Pesos colombianos sin centavos. */
export function roundCop(value: number | string | null | undefined): number {
  if (value === null || value === undefined || value === '') {
    return 0;
  }
  const num = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(num)) {
    return 0;
  }
  return Math.round(num);
}
