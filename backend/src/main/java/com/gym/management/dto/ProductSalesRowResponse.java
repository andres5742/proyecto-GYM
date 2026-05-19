package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.Map;

public record ProductSalesRowResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Map<PaymentMethod, PaymentMethodTotals> byPaymentMethod,
        int totalQuantity,
        BigDecimal totalAmount
) {}
