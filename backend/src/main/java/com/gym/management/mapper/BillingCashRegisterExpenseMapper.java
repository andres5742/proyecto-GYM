package com.gym.management.mapper;

import com.gym.management.dto.BillingCashRegisterExpenseResponse;
import com.gym.management.model.BillingCashRegisterExpense;

public final class BillingCashRegisterExpenseMapper {

    private BillingCashRegisterExpenseMapper() {}

    public static BillingCashRegisterExpenseResponse toResponse(BillingCashRegisterExpense expense) {
        String name = expense.getRecordedBy().getFirstName() + " " + expense.getRecordedBy().getLastName();
        return new BillingCashRegisterExpenseResponse(
                expense.getId(),
                expense.getCashRegister().getId(),
                expense.getCashRegister().getRegisterDate(),
                expense.getAmount(),
                expense.getPaymentMethod(),
                SaleMapper.paymentMethodLabel(expense.getPaymentMethod()),
                expense.getObservation(),
                expense.getRecordedBy().getId(),
                name,
                expense.getCreatedAt());
    }
}
