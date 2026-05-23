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

/** Numérico: sin ceros a la izquierda. Alfanumérico (AA1): mayúsculas. */
export function normalizeCardPin(storedOrRaw: string | null | undefined): string {
  const raw = extractCardPin(storedOrRaw);
  if (!raw) {
    return '';
  }
  const alphanumeric = raw.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
  if (!alphanumeric) {
    return raw;
  }
  if (/^\d+$/.test(alphanumeric)) {
    const trimmed = alphanumeric.replace(/^0+/, '');
    return trimmed || '0';
  }
  return alphanumeric;
}

export function normalizeDocumentDigits(documentId: string | null | undefined): string {
  if (!documentId?.trim()) {
    return '';
  }
  return documentId.replace(/\D/g, '');
}

/** Número del chip en decimal (ej. 2198114): único por tarjeta física; no requiere cédula en la clave. */
export function isChipCardUid(cardPin: string | null | undefined): boolean {
  const card = normalizeCardPin(cardPin);
  if (card.length < 8) {
    return false;
  }
  if (/[A-Z]/.test(card)) {
    return true;
  }
  return card.length >= 10;
}

export function composeMemberCardKey(cardPin: string, documentId: string | null | undefined): string | null {
  const card = normalizeCardPin(cardPin);
  const doc = normalizeDocumentDigits(documentId);
  if (!card || !doc) {
    return null;
  }
  return `${card}${CARD_CREDENTIAL_SEP}${doc}`;
}

export function resolveMemberCardKey(
  cardPin: string,
  documentId: string | null | undefined,
): string | null {
  const card = normalizeCardPin(cardPin);
  if (!card) {
    return null;
  }
  if (isChipCardUid(card)) {
    return card;
  }
  return composeMemberCardKey(cardPin, documentId);
}

export function composeStaffCardKey(cardPin: string, employeeId: number): string | null {
  const card = normalizeCardPin(cardPin);
  if (!card || employeeId == null) {
    return null;
  }
  return `${card}${CARD_CREDENTIAL_SEP}E${employeeId}`;
}

export function resolveStaffCardKey(cardPin: string, employeeId: number): string | null {
  const card = normalizeCardPin(cardPin);
  if (!card || employeeId == null) {
    return null;
  }
  if (isChipCardUid(card)) {
    return card;
  }
  return composeStaffCardKey(cardPin, employeeId);
}
