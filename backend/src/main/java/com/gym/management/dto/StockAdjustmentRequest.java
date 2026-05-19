package com.gym.management.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull @jakarta.validation.constraints.Min(1) Integer delta,
        String reason
) {}
