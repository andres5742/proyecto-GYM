/**
 * Teclado mural ZKT en el torniquete: no usa Digit1/Digit2 estándar.
 * En el gym confirmado: tecla 1 → keyCode 253, 2 → 252, 3 → 251… (254 − n).
 */
export function zktKeypadSelectionIndex(event: KeyboardEvent): number | null {
  const fromCode = event.code.match(/^Digit([1-9])$/) ?? event.code.match(/^Numpad([1-9])$/);
  if (fromCode) {
    return Number(fromCode[1]);
  }
  if (/^[1-9]$/.test(event.key)) {
    return Number(event.key);
  }
  const legacyCode = event.keyCode || event.which;
  if (legacyCode >= 245 && legacyCode <= 253) {
    return 254 - legacyCode;
  }
  return null;
}
