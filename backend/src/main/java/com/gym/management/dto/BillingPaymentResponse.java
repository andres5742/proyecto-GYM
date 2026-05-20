package com.gym.management.dto;

import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BillingPaymentResponse(
        Long id,
        BillingPaymentType paymentType,
        String paymentTypeLabel,
        Long memberId,
        String memberName,
        Long planId,
        String planName,
        Long saleId,
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        BigDecimal amount,
        LocalDate paymentDate,
        LocalDate membershipStart,
        LocalDate membershipEnd,
        Long recordedByEmployeeId,
        String recordedByEmployeeName,
        Instant createdAt) {}
