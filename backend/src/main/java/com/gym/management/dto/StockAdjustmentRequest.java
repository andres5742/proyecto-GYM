package com.gym.management.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull Integer delta,
        String reason
) {}
