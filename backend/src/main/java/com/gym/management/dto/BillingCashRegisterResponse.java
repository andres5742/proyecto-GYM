package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import com.gym.management.model.ShiftStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record BillingCashRegisterResponse(
        Long id,
        LocalDate registerDate,
        Long openedByEmployeeId,
        String openedByEmployeeName,
        BigDecimal openingCashAmount,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        ShiftStatus status,
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
        /** Cobros de abonos a productos fiados del día (todos los medios). */
        BigDecimal dayFiadoCollectedTotal,
        Map<PaymentMethod, BigDecimal> dayFiadoCollectedByMethod,
        long dayFiadoPaymentCount,
        /** Inicio + facturación efectivo + productos efectivo + fiado efectivo − gastos en efectivo. */
        BigDecimal cashInDrawer) {}
