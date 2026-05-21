package com.gym.management.model;

public enum BiometricCredentialType {
    FINGERPRINT,
    CARD,
    FACE;

    public String displayLabel() {
        return switch (this) {
            case FINGERPRINT -> "Huella";
            case CARD -> "Tarjeta (ZKTeco)";
            case FACE -> "Rostro (biométrico)";
        };
    }
}
