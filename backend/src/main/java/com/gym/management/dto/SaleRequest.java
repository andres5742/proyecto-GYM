package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record SaleRequest(
        @NotNull Long workShiftId,
        @NotNull Long employeeId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotNull PaymentMethod paymentMethod,
        LocalDateTime saleDate,
        @Size(max = 500) String notes
) {}
