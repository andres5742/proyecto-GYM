package com.gym.management.dto;

import com.gym.management.model.ProductCreditStatus;
import com.gym.management.model.ProductCreditDebtorType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record ProductCreditResponse(
        Long id,
        ProductCreditDebtorType debtorType,
        Long memberId,
        Long debtorEmployeeId,
        String debtorName,
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
        boolean priorDebt,
        String concept,
        String notes,
        Instant createdAt,
        List<ProductCreditPaymentResponse> payments) {}
