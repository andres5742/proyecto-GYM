package com.gym.management.mapper;

import com.gym.management.dto.SaleResponse;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Sale;

public final class SaleMapper {

    private SaleMapper() {}

    public static SaleResponse toResponse(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getWorkShift() != null ? sale.getWorkShift().getId() : null,
                sale.getWorkShift() != null ? sale.getWorkShift().getName() : null,
                sale.getEmployee().getId(),
                sale.getEmployee().getFirstName() + " " + sale.getEmployee().getLastName(),
                sale.getProduct().getId(),
                sale.getProduct().getName(),
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getTotalAmount(),
                sale.getPaymentMethod(),
                paymentMethodLabel(sale.getPaymentMethod()),
                sale.getSaleDate(),
                sale.getNotes(),
                sale.getCreatedAt()
        );
    }

    public static String paymentMethodLabel(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Efectivo";
            case NEQUI -> "Nequi";
            case BANCOLOMBIA -> "Bancolombia";
            case AUX -> "Sistema AUX";
            case PENDING -> "Pendiente de pago / deuda";
        };
    }
}
