package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BillingCashRegisterOtherIncomeRequest(
        @NotNull @DecimalMin(value = "0.01", message = "El valor del ingreso debe ser mayor a cero")
                BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank @Size(max = 500) String observation) {}
