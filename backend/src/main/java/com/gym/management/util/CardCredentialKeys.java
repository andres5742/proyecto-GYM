package com.gym.management.util;

import com.gym.management.exception.BusinessException;
import java.util.Locale;

/**
 * Clave de tarjeta en BD: siempre {@code codigo|cedula} (ej. {@code A5AD8AE2|1234567890}).
 */
public final class CardCredentialKeys {

    public static final String SEPARATOR = "|";
    private static final int MIN_CHIP_UID_LENGTH = 8;
    private static final int MIN_NUMERIC_CHIP_UID_LENGTH = 10;
    private static final int MAX_LENGTH = 64;

    private CardCredentialKeys() {}

    /** Solo la parte de tarjeta guardada o leída (sin sufijo de cédula). */
    public static String extractCardPin(String storedOrRaw) {
        if (storedOrRaw == null) {
            return "";
        }
        String trimmed = storedOrRaw.trim();
        int sep = trimmed.indexOf(SEPARATOR);
        if (sep >= 0) {
            return trimmed.substring(0, sep).trim();
        }
        return trimmed;
    }

    /**
     * Código de tarjeta canónico. Numérico: sin ceros a la izquierda ({@code 0000035979} → {@code 35979}).
     * Alfanumérico: mayúsculas ({@code AA1} → {@code AA1}).
     */
    public static String normalizeCardPin(String storedOrRaw) {
        String raw = extractCardPin(storedOrRaw);
        if (raw.isEmpty()) {
            return "";
        }
        String alphanumeric = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (alphanumeric.isEmpty()) {
            return raw.trim();
        }
        if (alphanumeric.matches("\\d+")) {
            String withoutLeadingZeros = alphanumeric.replaceFirst("^0+", "");
            return withoutLeadingZeros.isEmpty() ? "0" : withoutLeadingZeros;
        }
        return alphanumeric;
    }

    public static String normalizeDocumentDigits(String documentId) {
        if (documentId == null) {
            return "";
        }
        return documentId.replaceAll("\\D", "");
    }

    public static boolean isComposite(String deviceUserId) {
        return deviceUserId != null && deviceUserId.contains(SEPARATOR);
    }

    /**
     * UID único del chip (hex, como el lector), p. ej. {@code A5AD8AE2}.
     * No hace falta añadir cédula: dos tarjetas físicas distintas no comparten este valor.
     */
    public static boolean isChipCardUid(String normalizedCardPin) {
        if (normalizedCardPin == null || normalizedCardPin.length() < MIN_CHIP_UID_LENGTH) {
            return false;
        }
        if (normalizedCardPin.matches(".*[A-Z].*")) {
            return true;
        }
        return normalizedCardPin.length() >= MIN_NUMERIC_CHIP_UID_LENGTH;
    }

    /** Clave final a guardar: siempre {@code codigo|cedula} para distinguir afiliados con el mismo código. */
    public static String resolveMemberCardStorage(String cardPinFromReader, String memberDocumentId) {
        return composeMemberCard(cardPinFromReader, memberDocumentId);
    }

    public static String resolveStaffCardStorage(String cardPinFromReader, Long employeeId) {
        String card = normalizeCardPin(cardPinFromReader);
        if (card.isEmpty()) {
            throw new BusinessException("Falta el código de la tarjeta leída");
        }
        if (isChipCardUid(card)) {
            return ensureMaxLength(card);
        }
        return composeStaffCard(cardPinFromReader, employeeId);
    }

    public static String composeMemberCard(String cardPinFromReader, String memberDocumentId) {
        String card = normalizeCardPin(cardPinFromReader);
        if (card.isEmpty()) {
            throw new BusinessException("Falta el código de la tarjeta leída");
        }
        String doc = normalizeDocumentDigits(memberDocumentId);
        if (doc.isEmpty()) {
            throw new BusinessException(
                    "El afiliado debe tener número de documento (cédula) para vincular la tarjeta");
        }
        return ensureMaxLength(card + SEPARATOR + doc);
    }

    public static String composeStaffCard(String cardPinFromReader, Long employeeId) {
        String card = normalizeCardPin(cardPinFromReader);
        if (card.isEmpty()) {
            throw new BusinessException("Falta el código de la tarjeta leída");
        }
        if (employeeId == null) {
            throw new BusinessException("Indica el entrenador");
        }
        return ensureMaxLength(card + SEPARATOR + "E" + employeeId);
    }

    private static String ensureMaxLength(String value) {
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(
                    "La clave de tarjeta es demasiado larga (máx. " + MAX_LENGTH + " caracteres). Acorte documento o use otra tarjeta.");
        }
        return value;
    }
}
