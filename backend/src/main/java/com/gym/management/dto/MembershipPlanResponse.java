package com.gym.management.dto;

import com.gym.management.model.MembershipPlanKind;
import java.math.BigDecimal;
import java.time.Instant;

public record MembershipPlanResponse(
        Long id,
        String name,
        String description,
        Integer durationDays,
        MembershipPlanKind planKind,
        Integer monthlyEntryLimit,
        BigDecimal price,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
