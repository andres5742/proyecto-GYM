package com.gym.management.mapper;

import com.gym.management.dto.ProductCreditPaymentResponse;
import com.gym.management.dto.ProductCreditResponse;
import com.gym.management.model.ProductCredit;
import com.gym.management.model.ProductCreditPayment;
import com.gym.management.model.ProductCreditStatus;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class ProductCreditMapper {

    private ProductCreditMapper() {}

    public static ProductCreditResponse toResponse(ProductCredit credit, boolean includePayments) {
        BigDecimal paid = credit.getTotalAmount().subtract(credit.getBalance());
        List<ProductCreditPaymentResponse> payments = List.of();
        if (includePayments && credit.getPayments() != null) {
            payments = credit.getPayments().stream()
                    .sorted(Comparator.comparing(ProductCreditPayment::getPaidAt).reversed())
                    .map(ProductCreditMapper::toPaymentResponse)
                    .toList();
        }
        String debtorName = credit.getDebtorType() == com.gym.management.model.ProductCreditDebtorType.STAFF
                ? credit.getDebtorEmployee().getFirstName() + " " + credit.getDebtorEmployee().getLastName()
                : credit.getMember().getFirstName() + " " + credit.getMember().getLastName();
        String memberDocumentId = credit.getMember() != null ? credit.getMember().getDocumentId() : null;
        String productName = credit.getProduct() != null
                ? credit.getProduct().getName()
                : (credit.getConcept() != null && !credit.getConcept().isBlank() ? credit.getConcept() : "Deuda anterior");
        return new ProductCreditResponse(
                credit.getId(),
                credit.getDebtorType(),
                credit.getMember() != null ? credit.getMember().getId() : null,
                credit.getDebtorEmployee() != null ? credit.getDebtorEmployee().getId() : null,
                debtorName,
                memberDocumentId,
                credit.getProduct() != null ? credit.getProduct().getId() : null,
                productName,
                credit.getQuantity(),
                credit.getUnitPrice(),
                credit.getTotalAmount(),
                credit.getBalance(),
                paid,
                credit.getStatus(),
                statusLabel(credit.getStatus()),
                credit.getWorkShift().getId(),
                credit.getWorkShift().getName(),
                credit.getEmployee().getId(),
                credit.getEmployee().getFirstName() + " " + credit.getEmployee().getLastName(),
                credit.getCreditedAt(),
                credit.isPriorDebt(),
                credit.getConcept(),
                credit.getNotes(),
                credit.getCreatedAt(),
                payments);
    }

    public static ProductCreditPaymentResponse toPaymentResponse(ProductCreditPayment payment) {
        return new ProductCreditPaymentResponse(
                payment.getId(),
                payment.getCredit().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                SaleMapper.paymentMethodLabel(payment.getPaymentMethod()),
                payment.getWorkShift().getId(),
                payment.getWorkShift().getName(),
                payment.getEmployee().getId(),
                payment.getEmployee().getFirstName() + " " + payment.getEmployee().getLastName(),
                payment.getPaidAt(),
                payment.getNotes(),
                payment.getCreatedAt());
    }

    public static String statusLabel(ProductCreditStatus status) {
        return switch (status) {
            case OPEN -> "Pendiente";
            case PAID -> "Pagado";
            case CANCELLED -> "Anulado";
        };
    }
}
