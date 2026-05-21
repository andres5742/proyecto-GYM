package com.gym.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record BillingCashRegisterCloseResultResponse(
        BillingCashRegisterResponse register,
        BigDecimal declaredCashTotal,
        BigDecimal expectedCashTotal,
        CashShortfallResponse cashShortfall,
        CashShortfallResponse inventoryShortfall,
        List<InventoryMissingLineDto> inventoryMissingLines) {}
