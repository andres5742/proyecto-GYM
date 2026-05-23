/** Mismo formato que el backend: codigoLector|cedula */
export const CARD_CREDENTIAL_SEP = '|';

export function extractCardPin(storedOrRaw: string | null | undefined): string {
  if (!storedOrRaw?.trim()) {
    return '';
  }
  const trimmed = storedOrRaw.trim();
  const sep = trimmed.indexOf(CARD_CREDENTIAL_SEP);
  return sep >= 0 ? trimmed.slice(0, sep).trim() : trimmed;
}

/** Solo dígitos, sin ceros a la izquierda (0000035979 y 000,35979 → 35979). */
export function normalizeCardPin(storedOrRaw: string | null | undefined): string {
  const raw = extractCardPin(storedOrRaw);
  if (!raw) {
    return '';
  }
  const digits = raw.replace(/\D/g, '');
  if (!digits) {
    return raw;
  }
  const trimmed = digits.replace(/^0+/, '');
  return trimmed || '0';
}

export function normalizeDocumentDigits(documentId: string | null | undefined): string {
  if (!documentId?.trim()) {
    return '';
  }
  return documentId.replace(/\D/g, '');
}

export function composeMemberCardKey(cardPin: string, documentId: string | null | undefined): string | null {
  const card = normalizeCardPin(cardPin);
  const doc = normalizeDocumentDigits(documentId);
  if (!card || !doc) {
    return null;
  }
  return `${card}${CARD_CREDENTIAL_SEP}${doc}`;
}

export function composeStaffCardKey(cardPin: string, employeeId: number): string | null {
  const card = normalizeCardPin(cardPin);
  if (!card || employeeId == null) {
    return null;
  }
  return `${card}${CARD_CREDENTIAL_SEP}E${employeeId}`;
}
