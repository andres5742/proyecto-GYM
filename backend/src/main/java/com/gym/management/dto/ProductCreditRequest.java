package com.gym.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductCreditRequest(
        @NotNull Long memberId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        Long workShiftId,
        String notes) {}
