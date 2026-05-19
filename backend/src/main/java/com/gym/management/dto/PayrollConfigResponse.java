package com.gym.management.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PayrollConfigResponse(
        Long id,
        BigDecimal weekdayHourlyRate,
        BigDecimal sundayHourlyRate,
        Instant updatedAt
) {}
