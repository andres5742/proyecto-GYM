package com.gym.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePaymentAccountSettingsRequest(
        @NotNull @DecimalMin(value = "0", message = "El saldo inicial de Nequi no puede ser negativo")
                BigDecimal nequiInitialBalance,
        @NotNull @DecimalMin(value = "0", message = "El saldo inicial de Bancolombia no puede ser negativo")
                BigDecimal bancolombiaInitialBalance) {}
