package com.gym.management.dto;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record ProductCreditRequest(
        Long memberId,
        Long employeeDebtorId,
        Long productId,
        @Min(1) Integer quantity,
        BigDecimal manualAmount,
        Boolean priorDebt,
        String concept,
        Long workShiftId,
        String notes) {}
