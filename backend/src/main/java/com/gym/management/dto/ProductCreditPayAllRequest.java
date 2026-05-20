package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record ProductCreditPayAllRequest(
        @NotNull PaymentMethod paymentMethod,
        Long workShiftId,
        String notes) {}
