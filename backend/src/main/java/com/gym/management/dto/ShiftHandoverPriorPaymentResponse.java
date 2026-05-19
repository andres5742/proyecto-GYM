package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;

public record ShiftHandoverPriorPaymentResponse(
        Long id,
        String description,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        String notes) {}
