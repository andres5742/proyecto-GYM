package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import com.gym.management.model.ShiftStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BillingCashRegisterResponse(
        Long id,
        LocalDate registerDate,
        Long openedByEmployeeId,
        String openedByEmployeeName,
        BigDecimal openingCashAmount,
        BigDecimal openingNequiAmount,
        BigDecimal openingBancolombiaAmount,
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
        /** Otros ingresos del día (no facturación ni productos). */
        BigDecimal dayOtherIncomesTotal,
        Map<PaymentMethod, BigDecimal> dayOtherIncomesByMethod,
        /** Parte de otros ingresos marcada automáticamente como sobrante. */
        Map<PaymentMethod, BigDecimal> dayAutoSurplusByMethod,
        long dayOtherIncomeCount,
        /** Facturación + productos + fiado + otros ingresos hoy, por medio (sin base inicial). */
        Map<PaymentMethod, BigDecimal> dayIncomeByMethod,
        BigDecimal dayIncomeTotal,
        /**
         * Efectivo físico esperado en caja: si hubo entrega de turno, último conteo entregado + movimientos en
         * Facturación desde entonces; si no, inicio + cobros − gastos (efectivo).
         */
        BigDecimal cashInDrawer,
        /** Efectivo contado en la última entrega de turno (null si aún no hay entrega hoy). */
        BigDecimal lastHandoverCashTotal,
        /** Cobros/gastos en efectivo en Facturación después de esa entrega. */
        BigDecimal cashSinceLastHandover,
        List<DigitalAccountBalanceLine> digitalAccounts) {}
