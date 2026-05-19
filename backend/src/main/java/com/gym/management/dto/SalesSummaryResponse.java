package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.Map;

public record SalesSummaryResponse(
        long totalSales,
        long totalUnits,
        BigDecimal totalAmount,
        Map<PaymentMethod, BigDecimal> amountByPaymentMethod
) {}
