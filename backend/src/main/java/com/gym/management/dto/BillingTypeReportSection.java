package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.PaymentMethod;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;
import java.util.Map;

public record BillingTypeReportSection(
        long paymentCount,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal totalAmount,
        Map<PaymentMethod, BigDecimal> amountByMethod) {}
