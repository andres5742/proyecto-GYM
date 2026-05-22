/** Mensaje legible desde respuestas de error del API (400, 401, etc.). */
export function httpErrorMessage(err: unknown, fallback = 'Error de conexión con el servidor'): string {
  const raw = (err as { error?: unknown })?.error;
  if (typeof raw === 'string' && raw.trim()) {
    return raw.trim();
  }
  const body = raw as { message?: string; errors?: Record<string, string> } | undefined;
  if (body?.errors) {
    const first = Object.values(body.errors).find((m) => m?.trim());
    if (first) {
      return first;
    }
  }
  return body?.message?.trim() || fallback;
}
