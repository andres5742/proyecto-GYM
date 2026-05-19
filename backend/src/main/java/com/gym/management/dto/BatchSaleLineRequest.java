package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BatchSaleLineRequest(
        @NotNull Long productId,
        @NotNull PaymentMethod paymentMethod,
        @Min(1) int quantity
) {}
