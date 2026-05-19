package com.gym.management.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String category,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal stockValue,
        Integer minStock,
        Boolean lowStock,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
