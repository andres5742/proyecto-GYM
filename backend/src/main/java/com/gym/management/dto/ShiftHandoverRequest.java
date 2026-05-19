package com.gym.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ShiftHandoverRequest(
        @NotNull Long workShiftId,
        @Min(0) int bill2000,
        @Min(0) int bill5000,
        @Min(0) int bill10000,
        @Min(0) int bill20000,
        @Min(0) int bill50000,
        @Min(0) int bill100000,
        @Min(0) int coin1000,
        @Min(0) int coin500,
        @Min(0) int coin200,
        @Min(0) int coin100,
        @Min(0) int coin50,
        @NotNull @DecimalMin("0") BigDecimal auxAmount,
        @NotNull @DecimalMin("0") BigDecimal nequiAmount,
        @NotNull @DecimalMin("0") BigDecimal bankAmount,
        @Size(max = 1000) String notes,
        @Valid List<ShiftHandoverExpenseRequest> expenses,
        @Valid List<ShiftHandoverPriorPaymentRequest> priorPayments) {}
