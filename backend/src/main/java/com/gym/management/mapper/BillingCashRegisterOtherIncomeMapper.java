package com.gym.management.mapper;

import com.gym.management.dto.BillingCashRegisterOtherIncomeResponse;
import com.gym.management.model.BillingCashRegisterOtherIncome;

public final class BillingCashRegisterOtherIncomeMapper {

    private BillingCashRegisterOtherIncomeMapper() {}

    public static BillingCashRegisterOtherIncomeResponse toResponse(BillingCashRegisterOtherIncome income) {
        String name = income.getRecordedBy().getFirstName() + " " + income.getRecordedBy().getLastName();
        return new BillingCashRegisterOtherIncomeResponse(
                income.getId(),
                income.getCashRegister().getId(),
                income.getCashRegister().getRegisterDate(),
                income.getAmount(),
                income.getPaymentMethod(),
                SaleMapper.paymentMethodLabel(income.getPaymentMethod()),
                income.getObservation(),
                income.getRecordedBy().getId(),
                name,
                income.getCreatedAt());
    }
}
