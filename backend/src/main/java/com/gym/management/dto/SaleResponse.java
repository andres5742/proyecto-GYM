package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record SaleResponse(
        Long id,
        Long workShiftId,
        String workShiftName,
        Long employeeId,
        String employeeName,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        LocalDateTime saleDate,
        String notes,
        Instant createdAt
) {}
