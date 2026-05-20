package com.gym.management.dto;

import com.gym.management.model.CashShortfallStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CashShortfallResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long workShiftId,
        String workShiftName,
        Long shiftHandoverId,
        LocalDate recordDate,
        BigDecimal expectedAmount,
        BigDecimal declaredAmount,
        BigDecimal shortfallAmount,
        CashShortfallStatus status,
        String statusLabel,
        String notes,
        Instant settledAt,
        String settledByName,
        Instant createdAt) {}
