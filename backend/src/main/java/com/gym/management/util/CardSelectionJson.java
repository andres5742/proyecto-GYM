package com.gym.management.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.dto.CardSelectionCandidate;
import java.util.Collections;
import java.util.List;

public final class CardSelectionJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CardSelectionJson() {}

    public static String write(List<CardSelectionCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(candidates);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar candidatos de tarjeta", ex);
        }
    }

    public static List<CardSelectionCandidate> read(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<CardSelectionCandidate>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    public static List<CardSelectionCandidate> emptyIfNull(List<CardSelectionCandidate> candidates) {
        return candidates == null ? Collections.emptyList() : candidates;
    }
}
