package com.gym.management.mapper;

import com.gym.management.dto.ShiftHandoverExpenseResponse;
import com.gym.management.dto.ShiftHandoverPriorPaymentResponse;
import com.gym.management.dto.ShiftHandoverResponse;
import com.gym.management.model.ShiftHandover;
import com.gym.management.model.ShiftHandoverExpense;
import com.gym.management.model.ShiftHandoverPriorPayment;
import com.gym.management.util.CashCountUtil;
import java.math.BigDecimal;
import java.util.List;

public final class ShiftHandoverMapper {

    private ShiftHandoverMapper() {}

    public static ShiftHandoverResponse toResponse(
            ShiftHandover handover,
            BigDecimal expensesTotal,
            BigDecimal priorPaymentsTotal,
            com.gym.management.dto.ShiftDetailResponse shiftDetail,
            BigDecimal billingCashExpected,
            BigDecimal billingCashBase,
            BigDecimal billingOtherIncomesCash,
            BigDecimal previousShiftSalesCash,
            BigDecimal previousShiftShortfallsDeducted,
            String previousShiftName,
            BigDecimal salesCashExpected,
            BigDecimal previousShiftCreditPaymentsCash,
            BigDecimal creditPaymentsCashExpected,
            BigDecimal expectedCashTotal,
            BigDecimal lastHandoverCashTotal,
            BigDecimal cashSinceLastHandover,
            java.util.List<com.gym.management.dto.ProductInventoryLineResponse> inventoryProducts,
            int inventoryUnitsDelivered,
            int inventoryProductKindsDelivered,
            java.util.List<com.gym.management.dto.HandoverDeliveredProductLine> deliveredInventory,
            BigDecimal pendingInventoryShortfallTotal,
            List<com.gym.management.dto.ShiftHandoverComparisonResponse> comparisons,
            BigDecimal registeredShortfallAmount,
            Long cashShortfallId,
            boolean inventorySurplusResolved,
            String inventorySurplusResolutionNote,
            boolean cashSurplusRegistered,
            BigDecimal registeredSurplusAmount,
            Long cashSurplusOtherIncomeId) {
        BigDecimal cashTotal = CashCountUtil.totalCash(handover);
        BigDecimal declaredGrand = cashTotal
                .add(nullToZero(handover.getAuxAmount()))
                .add(nullToZero(handover.getNequiAmount()))
                .add(nullToZero(handover.getBankAmount()))
                .add(priorPaymentsTotal)
                .subtract(expensesTotal);

        return new ShiftHandoverResponse(
                handover.getId(),
                handover.getWorkShift().getId(),
                handover.getWorkShift().getName(),
                handover.getWorkShift().getShiftDate(),
                handover.getEmployee().getId(),
                handover.getEmployee().getFirstName() + " " + handover.getEmployee().getLastName(),
                handover.getSubmittedAt(),
                handover.getBill2000(),
                handover.getBill5000(),
                handover.getBill10000(),
                handover.getBill20000(),
                handover.getBill50000(),
                handover.getBill100000(),
                handover.getCoin1000(),
                handover.getCoin500(),
                handover.getCoin200(),
                handover.getCoin100(),
                handover.getCoin50(),
                cashTotal,
                billingCashExpected,
                billingCashBase,
                billingOtherIncomesCash,
                previousShiftSalesCash,
                previousShiftShortfallsDeducted,
                previousShiftName,
                salesCashExpected,
                previousShiftCreditPaymentsCash,
                creditPaymentsCashExpected,
                expectedCashTotal,
                lastHandoverCashTotal,
                cashSinceLastHandover,
                handover.getAuxAmount(),
                handover.getNequiAmount(),
                handover.getBankAmount(),
                expensesTotal,
                priorPaymentsTotal,
                declaredGrand,
                handover.getNotes(),
                handover.getExpenses().stream().map(ShiftHandoverMapper::toExpenseResponse).toList(),
                handover.getPriorPayments().stream().map(ShiftHandoverMapper::toPriorPaymentResponse).toList(),
                shiftDetail,
                inventoryProducts != null ? inventoryProducts : java.util.List.of(),
                inventoryUnitsDelivered,
                inventoryProductKindsDelivered,
                deliveredInventory != null ? deliveredInventory : java.util.List.of(),
                pendingInventoryShortfallTotal != null ? pendingInventoryShortfallTotal : BigDecimal.ZERO,
                comparisons,
                registeredShortfallAmount,
                cashShortfallId,
                inventorySurplusResolved,
                inventorySurplusResolutionNote,
                cashSurplusRegistered,
                registeredSurplusAmount,
                cashSurplusOtherIncomeId);
    }

    public static ShiftHandoverExpenseResponse toExpenseResponse(ShiftHandoverExpense expense) {
        return new ShiftHandoverExpenseResponse(expense.getId(), expense.getDescription(), expense.getAmount());
    }

    public static ShiftHandoverPriorPaymentResponse toPriorPaymentResponse(ShiftHandoverPriorPayment payment) {
        return new ShiftHandoverPriorPaymentResponse(
                payment.getId(),
                payment.getDescription(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                SaleMapper.paymentMethodLabel(payment.getPaymentMethod()),
                payment.getNotes());
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
