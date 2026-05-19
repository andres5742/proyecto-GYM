package com.gym.management.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MemberProgressEntryResponse(
        Long id,
        LocalDate recordedAt,
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
        String notes,
        List<String> imageUrls,
        Instant createdAt) {}
