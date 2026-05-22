package com.gym.management.model;

public enum BirthdayPersonType {
    MEMBER("Afiliado"),
    EMPLOYEE("Entrenador");

    private final String label;

    BirthdayPersonType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
