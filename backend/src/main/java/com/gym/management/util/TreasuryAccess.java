package com.gym.management.util;

import com.gym.management.dto.BillingCashRegisterClosePreviewResponse;
import com.gym.management.dto.BillingCashRegisterResponse;
import com.gym.management.dto.DigitalAccountBalanceLine;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.util.List;

/** Oculta saldos de tesorería (efectivo total en caja, Nequi, Bancolombia) a quien no es administración. */
public final class TreasuryAccess {

    private TreasuryAccess() {}

    public static boolean canViewTreasuryBalances() {
        return SecurityUtils.canViewPaymentInfo();
    }

    public static BillingCashRegisterResponse maskRegisterResponse(BillingCashRegisterResponse response) {
        if (canViewTreasuryBalances() || response == null) {
            return response;
        }
        return new BillingCashRegisterResponse(
                response.id(),
                response.registerDate(),
                response.openedByEmployeeId(),
                response.openedByEmployeeName(),
                response.openingCashAmount(),
                null,
                null,
                response.openedAt(),
                response.closedAt(),
                response.status(),
                response.sessionTotal(),
                response.sessionIncomeByMethod(),
                response.paymentCount(),
                response.sessionExpensesTotal(),
                response.sessionExpensesByMethod(),
                response.expenseCount(),
                response.dayProductSalesTotal(),
                response.dayProductSalesCount(),
                response.dayProductSalesShiftCount(),
                response.dayProductUnitsSold(),
                response.dayProductSalesCash(),
                response.sessionCashMembership(),
                response.sessionCashDayWorkout(),
                response.sessionCashSportsDance(),
                response.dayFiadoCollectedTotal(),
                response.dayFiadoCollectedByMethod(),
                response.dayFiadoPaymentCount(),
                response.dayOtherIncomesTotal(),
                response.dayOtherIncomesByMethod(),
                response.dayOtherIncomeCount(),
                response.dayIncomeByMethod(),
                response.dayIncomeTotal(),
                null,
                List.<DigitalAccountBalanceLine>of());
    }

    public static BillingCashRegisterClosePreviewResponse maskClosePreview(
            BillingCashRegisterClosePreviewResponse preview) {
        if (canViewTreasuryBalances() || preview == null) {
            return preview;
        }
        return new BillingCashRegisterClosePreviewResponse(
                null,
                preview.fiadoCashCollected(),
                null,
                preview.products(),
                List.of());
    }
}
