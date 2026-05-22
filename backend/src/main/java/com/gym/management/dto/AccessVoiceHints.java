package com.gym.management.dto;

/** Datos para anuncios de voz en torniquete (membresía por vencer, cupo tiquetera). */
public record AccessVoiceHints(
        Integer membershipDaysRemaining,
        Integer tiqueteraEntriesRemainingAfter,
        boolean tiqueteraPlan) {

    public static final int MEMBERSHIP_WARNING_DAYS = 5;
    public static final int TIQUETERA_LOW_ENTRIES_THRESHOLD = 3;

    public static AccessVoiceHints none() {
        return new AccessVoiceHints(null, null, false);
    }
}
