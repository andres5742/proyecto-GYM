package com.gym.management.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class BirthdayUtils {

    private BirthdayUtils() {}

    /** Fecha del próximo cumpleaños (hoy o en el futuro). */
    public static LocalDate nextCelebrationDate(LocalDate birthDate, LocalDate today) {
        LocalDate candidate = birthDate.withYear(today.getYear());
        if (candidate.isBefore(today)) {
            candidate = candidate.plusYears(1);
        }
        return candidate;
    }

    public static int daysUntilNextBirthday(LocalDate birthDate, LocalDate today) {
        return (int) ChronoUnit.DAYS.between(today, nextCelebrationDate(birthDate, today));
    }

    public static int ageTurningOn(LocalDate birthDate, LocalDate celebrationDate) {
        return celebrationDate.getYear() - birthDate.getYear();
    }
}
