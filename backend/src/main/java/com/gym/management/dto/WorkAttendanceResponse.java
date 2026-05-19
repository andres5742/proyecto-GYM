package com.gym.management.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkAttendanceResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String employeePaymentInfo,
        LocalDate workDate,
        LocalDateTime clockIn,
        LocalDateTime clockOut,
        BigDecimal hoursWorked,
        BigDecimal hourlyRateApplied,
        BigDecimal amountOwed,
        Boolean sunday,
        String dayTypeLabel,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
