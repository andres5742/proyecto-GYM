package com.gym.management.dto;

import java.math.BigDecimal;

public record AttendanceSummaryResponse(
        long totalRecords,
        long openRecords,
        BigDecimal totalHours,
        BigDecimal totalOwed
) {}
