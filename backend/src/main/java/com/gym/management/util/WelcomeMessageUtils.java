package com.gym.management.util;

import com.gym.management.model.Gender;

public final class WelcomeMessageUtils {

    private WelcomeMessageUtils() {}

    /** "Bienvenida" solo para FEMALE; en los demás casos "Bienvenido". */
    public static String welcomeWord(Gender gender) {
        return gender == Gender.FEMALE ? "Bienvenida" : "Bienvenido";
    }

    public static String welcomeWithFirstName(Gender gender, String firstName) {
        String name = firstName != null ? firstName.trim() : "";
        if (name.isEmpty()) {
            return "¡" + welcomeWord(gender) + "!";
        }
        if (name.length() > 1) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
        return "¡" + welcomeWord(gender) + ", " + name + "!";
    }

    /** Saludo en torniquete / historial para entrenadores. */
    public static String staffWelcomeMessage(Gender gender) {
        return "¡" + welcomeWord(gender) + "! Que tenga un excelente entreno.";
    }

    /** Primer nombre para el saludo si el campo nombre viene vacío. */
    public static String resolveFirstName(String firstName, String lastName) {
        if (firstName != null && !firstName.isBlank()) {
            return firstName.trim();
        }
        if (lastName == null || lastName.isBlank()) {
            return "";
        }
        String[] parts = lastName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }
}
