package com.gym.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CloseBillingCashRegisterRequest(
        @NotNull @Valid CashDenominationCount cashCount,
        @NotNull List<@Valid ProductInventoryCountItem> inventoryCounts,
        @Size(max = 1000) String notes) {}
