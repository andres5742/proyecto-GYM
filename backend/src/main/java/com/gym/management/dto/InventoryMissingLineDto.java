package com.gym.management.dto;

import java.math.BigDecimal;

public record InventoryMissingLineDto(
        Long productId,
        String productName,
        String category,
        int expectedQuantity,
        int countedQuantity,
        int missingQuantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount) {}
