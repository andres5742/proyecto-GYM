package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;

public record ProductInventoryReportLine(
        Long productId,
        String name,
        String category,
        int quantityInStock,
        int minStock,
        boolean lowStock,
        long unitsSoldToday,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal salesAmountToday) {}
