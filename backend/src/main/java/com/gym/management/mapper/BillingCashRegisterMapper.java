package com.gym.management.mapper;

import com.gym.management.dto.BillingCashRegisterResponse;
import com.gym.management.dto.DigitalAccountBalanceLine;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
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
            long expenseCount,
            BigDecimal dayProductSalesTotal,
            long dayProductSalesCount,
            long dayProductSalesShiftCount,
            long dayProductUnitsSold,
            BigDecimal dayProductSalesCash,
            BigDecimal sessionCashMembership,
            BigDecimal sessionCashDayWorkout,
            BigDecimal sessionCashSportsDance,
            BigDecimal dayFiadoCollectedTotal,
            Map<PaymentMethod, BigDecimal> dayFiadoCollectedByMethod,
            long dayFiadoPaymentCount,
            BigDecimal dayOtherIncomesTotal,
            Map<PaymentMethod, BigDecimal> dayOtherIncomesByMethod,
            long dayOtherIncomeCount,
            Map<PaymentMethod, BigDecimal> dayIncomeByMethod,
            BigDecimal dayIncomeTotal,
            BigDecimal cashInDrawer,
            List<DigitalAccountBalanceLine> digitalAccounts) {
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
                register.getOpeningNequiAmount() != null ? register.getOpeningNequiAmount() : BigDecimal.ZERO,
                register.getOpeningBancolombiaAmount() != null
                        ? register.getOpeningBancolombiaAmount()
                        : BigDecimal.ZERO,
                register.getOpenedAt(),
                register.getClosedAt(),
                register.getStatus(),
                sessionTotal != null ? sessionTotal : BigDecimal.ZERO,
                sessionIncomeByMethod,
                paymentCount,
                sessionExpensesTotal != null ? sessionExpensesTotal : BigDecimal.ZERO,
                sessionExpensesByMethod,
                expenseCount,
                dayProductSalesTotal != null ? dayProductSalesTotal : BigDecimal.ZERO,
                dayProductSalesCount,
                dayProductSalesShiftCount,
                dayProductUnitsSold,
                dayProductSalesCash != null ? dayProductSalesCash : BigDecimal.ZERO,
                sessionCashMembership != null ? sessionCashMembership : BigDecimal.ZERO,
                sessionCashDayWorkout != null ? sessionCashDayWorkout : BigDecimal.ZERO,
                sessionCashSportsDance != null ? sessionCashSportsDance : BigDecimal.ZERO,
                dayFiadoCollectedTotal != null ? dayFiadoCollectedTotal : BigDecimal.ZERO,
                dayFiadoCollectedByMethod != null ? dayFiadoCollectedByMethod : Map.of(),
                dayFiadoPaymentCount,
                dayOtherIncomesTotal != null ? dayOtherIncomesTotal : BigDecimal.ZERO,
                dayOtherIncomesByMethod != null ? dayOtherIncomesByMethod : Map.of(),
                dayOtherIncomeCount,
                dayIncomeByMethod != null ? dayIncomeByMethod : Map.of(),
                dayIncomeTotal != null ? dayIncomeTotal : BigDecimal.ZERO,
                cashInDrawer != null ? cashInDrawer : BigDecimal.ZERO,
                digitalAccounts != null ? digitalAccounts : List.of());
    }
}
