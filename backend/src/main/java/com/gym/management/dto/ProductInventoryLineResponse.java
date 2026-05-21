package com.gym.management.dto;

import java.math.BigDecimal;

public record ProductInventoryLineResponse(
        Long productId,
        String productName,
        String category,
        int expectedQuantity,
        BigDecimal unitPrice) {}
