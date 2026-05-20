package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ProductCreditPaymentResponse(
        Long id,
        Long creditId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        Long workShiftId,
        String workShiftName,
        Long employeeId,
        String employeeName,
        LocalDateTime paidAt,
        String notes,
        Instant createdAt) {}
