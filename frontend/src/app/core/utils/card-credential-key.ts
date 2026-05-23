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

export function normalizeDocumentDigits(documentId: string | null | undefined): string {
  if (!documentId?.trim()) {
    return '';
  }
  return documentId.replace(/\D/g, '');
}

export function composeMemberCardKey(cardPin: string, documentId: string | null | undefined): string | null {
  const card = extractCardPin(cardPin);
  const doc = normalizeDocumentDigits(documentId);
  if (!card || !doc) {
    return null;
  }
  return `${card}${CARD_CREDENTIAL_SEP}${doc}`;
}

export function composeStaffCardKey(cardPin: string, employeeId: number): string | null {
  const card = extractCardPin(cardPin);
  if (!card || employeeId == null) {
    return null;
  }
  return `${card}${CARD_CREDENTIAL_SEP}E${employeeId}`;
}
