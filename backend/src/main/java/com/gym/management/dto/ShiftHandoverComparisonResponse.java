package com.gym.management.dto;

import java.math.BigDecimal;

public record ShiftHandoverComparisonResponse(
        String label,
        BigDecimal declared,
        BigDecimal expected,
        BigDecimal difference) {}
