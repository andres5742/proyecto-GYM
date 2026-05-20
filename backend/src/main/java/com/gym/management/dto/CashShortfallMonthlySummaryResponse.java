package com.gym.management.dto;

import java.math.BigDecimal;

public record CashShortfallMonthlySummaryResponse(
        Long employeeId,
        String employeeName,
        BigDecimal pendingTotal,
        BigDecimal settledTotal,
        long recordCount) {}
