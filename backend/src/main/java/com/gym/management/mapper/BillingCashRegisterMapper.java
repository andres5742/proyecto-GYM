package com.gym.management.mapper;

import com.gym.management.dto.BillingCashRegisterResponse;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.Map;

public final class BillingCashRegisterMapper {

    private BillingCashRegisterMapper() {}

    public static BillingCashRegisterResponse toResponse(
            BillingCashRegister register,
            BigDecimal sessionTotal,
            Map<PaymentMethod, BigDecimal> sessionIncomeByMethod,
            long paymentCount,
            BigDecimal sessionExpensesTotal,
            Map<PaymentMethod, BigDecimal> sessionExpensesByMethod,
            long expenseCount) {
        String employeeName = register.getOpenedBy() != null
                ? register.getOpenedBy().getFirstName() + " " + register.getOpenedBy().getLastName()
                : "—";
        Long employeeId = register.getOpenedBy() != null ? register.getOpenedBy().getId() : null;
        return new BillingCashRegisterResponse(
                register.getId(),
                register.getRegisterDate(),
                employeeId,
                employeeName,
                register.getOpeningCashAmount(),
                register.getOpenedAt(),
                register.getClosedAt(),
                register.getStatus(),
                sessionTotal != null ? sessionTotal : BigDecimal.ZERO,
                sessionIncomeByMethod,
                paymentCount,
                sessionExpensesTotal != null ? sessionExpensesTotal : BigDecimal.ZERO,
                sessionExpensesByMethod,
                expenseCount);
    }
}
