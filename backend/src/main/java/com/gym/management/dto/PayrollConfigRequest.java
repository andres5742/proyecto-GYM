package com.gym.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PayrollConfigRequest(
        @NotNull @DecimalMin("0.0") BigDecimal weekdayHourlyRate,
        @NotNull @DecimalMin("0.0") BigDecimal sundayHourlyRate
) {}
