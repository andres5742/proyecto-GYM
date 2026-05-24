package com.gym.management.dto;

import java.math.BigDecimal;
import java.util.Optional;

public record SurplusInventoryResolutionResult(
        BigDecimal appliedToInventory,
        BigDecimal remainingInventoryDebt,
        BigDecimal remainingCashSurplus,
        boolean inventoryFullyCovered,
        Optional<String> userMessage) {}
