package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BillingCashRegisterExpenseResponse(
        Long id,
        Long cashRegisterId,
        LocalDate registerDate,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        String observation,
        Long recordedByEmployeeId,
        String recordedByEmployeeName,
        Instant createdAt) {}
