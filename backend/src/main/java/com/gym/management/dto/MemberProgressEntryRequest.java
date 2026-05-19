package com.gym.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MemberProgressEntryRequest(
        @NotNull LocalDate recordedAt,
        BigDecimal weightKg,
        BigDecimal chestCm,
        BigDecimal waistCm,
        BigDecimal hipsCm,
        BigDecimal armRightCm,
        BigDecimal armLeftCm,
        BigDecimal thighUpperRightCm,
        BigDecimal thighUpperLeftCm,
        BigDecimal thighLowerRightCm,
        BigDecimal thighLowerLeftCm,
        BigDecimal calfRightCm,
        BigDecimal calfLeftCm,
        BigDecimal bodyFatPercent,
        @Size(max = 500) String notes,
        List<@Size(max = 500) String> imageUrls) {}
