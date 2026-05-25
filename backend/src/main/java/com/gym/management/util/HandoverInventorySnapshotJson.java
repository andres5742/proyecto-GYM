package com.gym.management.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.dto.HandoverDeliveredProductLine;
import com.gym.management.dto.HandoverInventorySnapshot;
import java.util.List;

public final class HandoverInventorySnapshotJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HandoverInventorySnapshotJson() {}

    public static String write(HandoverInventorySnapshot snapshot) {
        if (snapshot == null || snapshot.lines() == null || snapshot.lines().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo guardar el inventario entregado", ex);
        }
    }

    public static HandoverInventorySnapshot read(String json) {
        if (json == null || json.isBlank()) {
            return new HandoverInventorySnapshot(0, List.of());
        }
        try {
            HandoverInventorySnapshot snapshot = MAPPER.readValue(json, HandoverInventorySnapshot.class);
            if (snapshot.lines() == null) {
                return new HandoverInventorySnapshot(snapshot.unitsTotal(), List.of());
            }
            return snapshot;
        } catch (Exception ex) {
            return new HandoverInventorySnapshot(0, List.of());
        }
    }
}
