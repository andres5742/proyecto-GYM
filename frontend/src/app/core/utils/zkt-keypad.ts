/** Teclado mural ZKT: tecla 1 = FD (253), 2 = FA (250), 3 = F7… paso de 3 en decimal. */
const ZKT_KEYPAD_BASE = 253;
const ZKT_KEYPAD_STEP = 3;
const HEX_PREFIX_MS = 450;

/** Par hex de dos letras → opción en pantalla (1, 2, 3…). */
const ZKT_HEX_PAIR_TO_INDEX: Record<string, number> = buildZktHexPairMap();

let pendingHexPrefix = false;
let hexPrefixTimer: ReturnType<typeof setTimeout> | null = null;

function buildZktHexPairMap(): Record<string, number> {
  const map: Record<string, number> = {};
  for (let index = 1; index <= 9; index++) {
    const code = ZKT_KEYPAD_BASE - ZKT_KEYPAD_STEP * (index - 1);
    const hex = code.toString(16).toUpperCase().padStart(2, '0');
    map[hex] = index;
  }
  return map;
}

function clearHexPrefix(): void {
  pendingHexPrefix = false;
  if (hexPrefixTimer) {
    clearTimeout(hexPrefixTimer);
    hexPrefixTimer = null;
  }
}

function armHexPrefix(): void {
  clearHexPrefix();
  pendingHexPrefix = true;
  hexPrefixTimer = setTimeout(() => clearHexPrefix(), HEX_PREFIX_MS);
}

function indexFromLegacyKeyCode(code: number): number | null {
  if (code > ZKT_KEYPAD_BASE || code < ZKT_KEYPAD_BASE - ZKT_KEYPAD_STEP * 8) {
    return null;
  }
  const diff = ZKT_KEYPAD_BASE - code;
  if (diff % ZKT_KEYPAD_STEP !== 0) {
    return null;
  }
  const index = diff / ZKT_KEYPAD_STEP + 1;
  return index >= 1 && index <= 9 ? index : null;
}

/**
 * Teclado mural ZKT en el torniquete: envía códigos hex (FD, FA…) o keyCode 253, 250…
 * También acepta teclado PC normal (Digit1, Numpad1…).
 */
export function zktKeypadSelectionIndex(event: KeyboardEvent): number | null {
  const fromCode = event.code.match(/^Digit([1-9])$/) ?? event.code.match(/^Numpad([1-9])$/);
  if (fromCode) {
    clearHexPrefix();
    return Number(fromCode[1]);
  }
  if (/^[1-9]$/.test(event.key)) {
    clearHexPrefix();
    return Number(event.key);
  }

  const legacyCode = event.keyCode || event.which;
  const fromKeyCode = indexFromLegacyKeyCode(legacyCode);
  if (fromKeyCode != null) {
    clearHexPrefix();
    return fromKeyCode;
  }

  const key = event.key?.length === 1 ? event.key.toUpperCase() : '';
  if (!key || !/^[0-9A-F]$/.test(key)) {
    return null;
  }

  if (pendingHexPrefix && key !== 'F') {
    const pair = `F${key}`;
    clearHexPrefix();
    return ZKT_HEX_PAIR_TO_INDEX[pair] ?? null;
  }

  if (key === 'F') {
    armHexPrefix();
    return null;
  }

  const pair = key.length === 2 ? key : null;
  if (pair && ZKT_HEX_PAIR_TO_INDEX[pair] != null) {
    clearHexPrefix();
    return ZKT_HEX_PAIR_TO_INDEX[pair];
  }

  return null;
}
