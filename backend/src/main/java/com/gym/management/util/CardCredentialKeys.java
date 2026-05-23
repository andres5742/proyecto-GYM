package com.gym.management.util;

import com.gym.management.exception.BusinessException;

/** Clave de tarjeta en BD: {@code codigoLector|cedula} (afiliado) o {@code codigoLector|E{id}} (staff). */
public final class CardCredentialKeys {

    public static final String SEPARATOR = "|";
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
     * Código de tarjeta canónico: solo dígitos, sin ceros a la izquierda.
     * Acepta el impreso en la tarjeta ({@code 0000035979}, {@code 000,35979}) o lo que envíe el lector.
     */
    public static String normalizeCardPin(String storedOrRaw) {
        String raw = extractCardPin(storedOrRaw);
        if (raw.isEmpty()) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return raw;
        }
        String withoutLeadingZeros = digits.replaceFirst("^0+", "");
        return withoutLeadingZeros.isEmpty() ? "0" : withoutLeadingZeros;
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
