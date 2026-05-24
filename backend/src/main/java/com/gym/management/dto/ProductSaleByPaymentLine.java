package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.PaymentMethod;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;

public record ProductSaleByPaymentLine(
        Long productId,
        String productName,
        PaymentMethod paymentMethod,
        long units,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal amount) {}
