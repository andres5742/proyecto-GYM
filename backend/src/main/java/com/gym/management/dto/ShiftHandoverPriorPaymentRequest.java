package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ShiftHandoverPriorPaymentRequest(
        @NotBlank @Size(max = 200) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 300) String notes) {}
