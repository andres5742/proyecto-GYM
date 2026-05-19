package com.gym.management.model;

public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    TRAINER,
    AFFILIATE;

    public String displayLabel() {
        return switch (this) {
            case SUPER_ADMIN -> "Super administrador";
            case ADMIN -> "Administrador";
            case TRAINER -> "Entrenador";
            case AFFILIATE -> "Afiliado";
        };
    }
}
