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
        return "¡" + welcomeWord(gender) + ", " + name + "!";
    }
}
