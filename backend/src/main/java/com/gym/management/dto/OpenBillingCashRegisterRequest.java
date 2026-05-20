package com.gym.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpenBillingCashRegisterRequest(
        @NotNull @DecimalMin(value = "0", message = "El efectivo inicial no puede ser negativo")
                BigDecimal openingCashAmount) {}
