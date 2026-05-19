package com.gym.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 80) String category,
        @NotNull @Min(0) Integer quantity,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
        @Min(0) Integer minStock,
        Boolean active
) {}
