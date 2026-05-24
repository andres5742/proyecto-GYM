package com.gym.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record HandoverInventoryResult(
        boolean inventoryProcessed,
        BigDecimal newMissingShortfallTotal,
        BigDecimal productSurplusCredit,
        List<InventoryMissingLineDto> missingLines,
        List<InventorySurplusLineDto> surplusLines,
        CashShortfallResponse newInventoryShortfall) {}
