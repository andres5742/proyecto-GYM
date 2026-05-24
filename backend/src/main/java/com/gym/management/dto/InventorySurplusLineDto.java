package com.gym.management.dto;

import java.math.BigDecimal;

public record InventorySurplusLineDto(
        Long productId,
        String productName,
        String category,
        int expectedQuantity,
        int countedQuantity,
        int surplusQuantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount) {}
