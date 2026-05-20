package com.gym.management.mapper;

import com.gym.management.dto.BillingPaymentResponse;
import com.gym.management.model.BillingPayment;
import com.gym.management.model.BillingPaymentType;

public final class BillingPaymentMapper {

    private BillingPaymentMapper() {}

    public static BillingPaymentResponse toResponse(BillingPayment payment) {
        Long memberId = payment.getMember() != null ? payment.getMember().getId() : null;
        String memberName = payment.getMember() != null
                ? payment.getMember().getFirstName() + " " + payment.getMember().getLastName()
                : (payment.getGuestLabel() != null ? payment.getGuestLabel() : "Invitado");
        String planName = payment.getPlan() != null ? payment.getPlan().getName() : null;
        Long planId = payment.getPlan() != null ? payment.getPlan().getId() : null;
        Long saleId = payment.getSale() != null ? payment.getSale().getId() : null;
        Long recordedById = payment.getEmployee() != null ? payment.getEmployee().getId() : null;
        String recordedByName = employeeDisplayName(payment);
        return new BillingPaymentResponse(
                payment.getId(),
                payment.getPaymentType(),
                paymentTypeLabel(payment.getPaymentType()),
                memberId,
                memberName,
                planId,
                planName,
                saleId,
                payment.getPaymentMethod(),
                SaleMapper.paymentMethodLabel(payment.getPaymentMethod()),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMembershipStart(),
                payment.getMembershipEnd(),
                recordedById,
                recordedByName,
                payment.getCreatedAt());
    }

    private static String employeeDisplayName(BillingPayment payment) {
        if (payment.getEmployee() == null) {
            return "—";
        }
        return payment.getEmployee().getFirstName() + " " + payment.getEmployee().getLastName();
    }

    public static String paymentTypeLabel(BillingPaymentType type) {
        return switch (type) {
            case DAY_WORKOUT -> "Entreno del día";
            case SPORTS_DANCE -> "Bailes deportivos";
            case MEMBERSHIP -> "Membresía";
        };
    }
}
