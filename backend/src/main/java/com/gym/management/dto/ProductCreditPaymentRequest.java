package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductCreditPaymentRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        Long workShiftId,
        String notes) {}
