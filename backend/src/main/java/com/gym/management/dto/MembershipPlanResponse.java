package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.MembershipPlanKind;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;
import java.time.Instant;

public record MembershipPlanResponse(
        Long id,
        String name,
        String description,
        Integer durationDays,
        MembershipPlanKind planKind,
        Integer monthlyEntryLimit,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal price,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
