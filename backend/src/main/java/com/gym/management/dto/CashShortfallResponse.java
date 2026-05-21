package com.gym.management.dto;

import com.gym.management.model.CashShortfallKind;
import com.gym.management.model.CashShortfallStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        CashShortfallKind kind,
        String kindLabel,
        String notes,
        List<InventoryMissingLineDto> inventoryMissingLines,
        Instant settledAt,
        String settledByName,
        Instant createdAt) {}
