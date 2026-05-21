package com.gym.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductInventoryCountItem(
        @NotNull Long productId, @NotNull @Min(0) Integer countedQuantity) {}
