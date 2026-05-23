package com.gym.management.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Teclado mural ZKT en torniquete: tecla 1 = FD (253), 2 = FA (250), 3 = F7… (paso −3).
 * El panel envía ese Pin al API como si fuera tarjeta; hay que interpretarlo como selección.
 */
public final class ZktKeypadSelection {

    private static final int KEYPAD_BASE = 253;
    private static final int KEYPAD_STEP = 3;

    private static final Map<String, Integer> HEX_PAIR_TO_INDEX = buildHexPairMap();

    private ZktKeypadSelection() {}

    private static Map<String, Integer> buildHexPairMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int index = 1; index <= 9; index++) {
            int code = KEYPAD_BASE - KEYPAD_STEP * (index - 1);
            map.put(String.format(Locale.ROOT, "%02X", code), index);
        }
        return Map.copyOf(map);
    }

    /** Índice 1–9 si el Pin es tecla del ZKT (FD, FA, 1…); vacío si parece tarjeta real. */
    public static OptionalInt selectionIndexFromPin(String rawPin) {
        if (rawPin == null || rawPin.isBlank()) {
            return OptionalInt.empty();
        }
        String pin = rawPin.trim().toUpperCase(Locale.ROOT);
        if (pin.length() == 2 && HEX_PAIR_TO_INDEX.containsKey(pin)) {
            return OptionalInt.of(HEX_PAIR_TO_INDEX.get(pin));
        }
        if (pin.length() == 1 && pin.charAt(0) >= '1' && pin.charAt(0) <= '9') {
            return OptionalInt.of(pin.charAt(0) - '0');
        }
        if (pin.chars().allMatch(Character::isDigit)) {
            try {
                return selectionIndexFromKeyCode(Integer.parseInt(pin));
            } catch (NumberFormatException ex) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.empty();
    }

    public static OptionalInt selectionIndexFromKeyCode(int keyCode) {
        if (keyCode > KEYPAD_BASE || keyCode < KEYPAD_BASE - KEYPAD_STEP * 8) {
            return OptionalInt.empty();
        }
        int diff = KEYPAD_BASE - keyCode;
        if (diff % KEYPAD_STEP != 0) {
            return OptionalInt.empty();
        }
        int index = diff / KEYPAD_STEP + 1;
        if (index < 1 || index > 9) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(index);
    }

    public static boolean isKeypadPin(String rawPin) {
        return selectionIndexFromPin(rawPin).isPresent();
    }
}
