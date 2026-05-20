package com.gym.management.dto;

import com.gym.management.model.MembershipPlanKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MembershipPlanRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description,
        @NotNull @Min(1) Integer durationDays,
        MembershipPlanKind planKind,
        @Min(1) Integer monthlyEntryLimit,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        Boolean active
) {}
