package com.gym.management.dto;

import com.gym.management.model.ShiftStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkShiftResponse(
        Long id,
        LocalDate shiftDate,
        String name,
        Long employeeId,
        String employeeName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        ShiftStatus status,
        BigDecimal totalAmount,
        long totalSales,
        Instant createdAt
) {}
