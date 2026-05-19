package com.gym.management.config;

import java.time.LocalDate;
import java.util.List;

/** Festivos oficiales de Colombia (fechas ya trasladadas al lunes cuando aplica la Ley Emiliani). */
public final class ColombiaHolidayCatalog {

    private ColombiaHolidayCatalog() {}

    public record HolidayEntry(LocalDate date, String name) {}

    public static List<HolidayEntry> forYear(int year) {
        return switch (year) {
            case 2025 -> holidays2025();
            case 2026 -> holidays2026();
            case 2027 -> holidays2027();
            default -> List.of();
        };
    }

  private static List<HolidayEntry> holidays2025() {
        return List.of(
                entry(2025, 1, 1, "Año Nuevo"),
                entry(2025, 1, 6, "Día de los Reyes Magos"),
                entry(2025, 3, 24, "Día de San José"),
                entry(2025, 4, 17, "Jueves Santo"),
                entry(2025, 4, 18, "Viernes Santo"),
                entry(2025, 5, 1, "Día del Trabajo"),
                entry(2025, 6, 2, "Ascensión del Señor"),
                entry(2025, 6, 23, "Corpus Christi"),
                entry(2025, 6, 30, "Sagrado Corazón y San Pedro y San Pablo"),
                entry(2025, 7, 20, "Día de la Independencia"),
                entry(2025, 8, 7, "Batalla de Boyacá"),
                entry(2025, 8, 18, "La Asunción de la Virgen"),
                entry(2025, 10, 13, "Día de la Raza"),
                entry(2025, 11, 3, "Todos los Santos"),
                entry(2025, 11, 17, "Independencia de Cartagena"),
                entry(2025, 12, 8, "Inmaculada Concepción"),
                entry(2025, 12, 25, "Navidad"));
    }

    private static List<HolidayEntry> holidays2026() {
        return List.of(
                entry(2026, 1, 1, "Año Nuevo"),
                entry(2026, 1, 12, "Día de los Reyes Magos"),
                entry(2026, 3, 23, "Día de San José"),
                entry(2026, 4, 2, "Jueves Santo"),
                entry(2026, 4, 3, "Viernes Santo"),
                entry(2026, 5, 1, "Día del Trabajo"),
                entry(2026, 5, 18, "Ascensión del Señor"),
                entry(2026, 6, 8, "Corpus Christi"),
                entry(2026, 6, 15, "Sagrado Corazón de Jesús"),
                entry(2026, 6, 29, "San Pedro y San Pablo"),
                entry(2026, 7, 20, "Día de la Independencia"),
                entry(2026, 8, 7, "Batalla de Boyacá"),
                entry(2026, 8, 17, "La Asunción de la Virgen"),
                entry(2026, 10, 12, "Día de la Raza"),
                entry(2026, 11, 2, "Todos los Santos"),
                entry(2026, 11, 16, "Independencia de Cartagena"),
                entry(2026, 12, 8, "Inmaculada Concepción"),
                entry(2026, 12, 25, "Navidad"));
    }

    private static List<HolidayEntry> holidays2027() {
        return List.of(
                entry(2027, 1, 1, "Año Nuevo"),
                entry(2027, 1, 11, "Día de los Reyes Magos"),
                entry(2027, 3, 22, "Día de San José"),
                entry(2027, 3, 25, "Jueves Santo"),
                entry(2027, 3, 26, "Viernes Santo"),
                entry(2027, 5, 1, "Día del Trabajo"),
                entry(2027, 5, 10, "Ascensión del Señor"),
                entry(2027, 5, 31, "Corpus Christi"),
                entry(2027, 6, 7, "Sagrado Corazón de Jesús"),
                entry(2027, 6, 28, "San Pedro y San Pablo"),
                entry(2027, 7, 20, "Día de la Independencia"),
                entry(2027, 8, 7, "Batalla de Boyacá"),
                entry(2027, 8, 16, "La Asunción de la Virgen"),
                entry(2027, 10, 18, "Día de la Raza"),
                entry(2027, 11, 1, "Todos los Santos"),
                entry(2027, 11, 15, "Independencia de Cartagena"),
                entry(2027, 12, 8, "Inmaculada Concepción"),
                entry(2027, 12, 25, "Navidad"));
    }

    private static HolidayEntry entry(int year, int month, int day, String name) {
        return new HolidayEntry(LocalDate.of(year, month, day), name);
    }
}
