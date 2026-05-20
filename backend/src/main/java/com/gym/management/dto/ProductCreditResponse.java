package com.gym.management.dto;

import com.gym.management.model.ProductCreditStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record ProductCreditResponse(
        Long id,
        Long memberId,
        String memberName,
        String memberDocumentId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        BigDecimal balance,
        BigDecimal paidAmount,
        ProductCreditStatus status,
        String statusLabel,
        Long workShiftId,
        String workShiftName,
        Long employeeId,
        String employeeName,
        LocalDateTime creditedAt,
        String notes,
        Instant createdAt,
        List<ProductCreditPaymentResponse> payments) {}
