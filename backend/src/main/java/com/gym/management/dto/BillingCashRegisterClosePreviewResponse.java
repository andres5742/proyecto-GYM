package com.gym.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record BillingCashRegisterClosePreviewResponse(
        BigDecimal cashInDrawer,
        BigDecimal fiadoCashCollected,
        BigDecimal expectedCashTotal,
        List<ProductInventoryLineResponse> products) {}
