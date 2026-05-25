package com.gym.management.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Inventario físico al entregar turno: stock en sistema vs. unidades que quedaron contadas. */
public record HandoverDeliveredProductLine(
        Long productId,
        String productName,
        String category,
        /** Stock según sistema antes del conteo de entrega (null en registros antiguos). */
        Integer expectedInSystem,
        /** Unidades que quedaron en bodega tras el conteo (stock real dejado). */
        @JsonAlias("countedQuantity") int stockRemaining) {}
