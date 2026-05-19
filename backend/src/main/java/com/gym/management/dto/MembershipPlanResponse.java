package com.gym.management.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MembershipPlanResponse(
        Long id,
        String name,
        String description,
        Integer durationDays,
        BigDecimal price,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
