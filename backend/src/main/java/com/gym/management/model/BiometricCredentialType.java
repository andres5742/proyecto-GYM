package com.gym.management.model;

public enum BiometricCredentialType {
    FINGERPRINT,
    FACE;

    public String displayLabel() {
        return switch (this) {
            case FINGERPRINT -> "Huella";
            case FACE -> "Rostro (biométrico)";
        };
    }
}
