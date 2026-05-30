package com.gym.management.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Teclado mural ZKT en torniquete: algunas versiones envían 1=FD,2=FA,3=F7...
 * y otras 1=FC,2=F9,3=F6... (paso -3).
 * El panel envía ese Pin al API como si fuera tarjeta; hay que interpretarlo como selección.
 */
public final class ZktKeypadSelection {

    private static final int KEYPAD_BASE_A = 253; // FD family
    private static final int KEYPAD_BASE_B = 252; // FC family
    private static final int KEYPAD_STEP = 3;

    private static final Map<String, Integer> HEX_PAIR_TO_INDEX = buildHexPairMap();

    private ZktKeypadSelection() {}

    private static Map<String, Integer> buildHexPairMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        addKeypadFamily(map, KEYPAD_BASE_A);
        addKeypadFamily(map, KEYPAD_BASE_B);
        return Map.copyOf(map);
    }

    private static void addKeypadFamily(Map<String, Integer> map, int base) {
        for (int index = 1; index <= 9; index++) {
            int code = base - KEYPAD_STEP * (index - 1);
            map.put(String.format(Locale.ROOT, "%02X", code), index);
        }
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
        OptionalInt fromA = selectionIndexFromKeyCodeFamily(keyCode, KEYPAD_BASE_A);
        if (fromA.isPresent()) {
            return fromA;
        }
        return selectionIndexFromKeyCodeFamily(keyCode, KEYPAD_BASE_B);
    }

    private static OptionalInt selectionIndexFromKeyCodeFamily(int keyCode, int base) {
        if (keyCode > base || keyCode < base - KEYPAD_STEP * 8) {
            return OptionalInt.empty();
        }
        int diff = base - keyCode;
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
