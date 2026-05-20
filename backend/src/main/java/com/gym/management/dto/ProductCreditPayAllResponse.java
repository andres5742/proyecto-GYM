package com.gym.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreditPayAllResponse(
        int creditsPaid,
        BigDecimal totalAmount,
        List<ProductCreditPaymentResponse> payments) {}
