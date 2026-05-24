package com.gym.management.dto;

import java.math.BigDecimal;

public record ShiftOpenCashPreviewResponse(
        Long cashRegisterId,
        String cashRegisterOpenedByName,
        BigDecimal openingCashAmount,
        BigDecimal cashExpenses,
        BigDecimal productCashTotal,
        BigDecimal fiadoCashTotal,
        BigDecimal cashMembership,
        BigDecimal cashDayWorkout,
        BigDecimal cashSportsDance,
        BigDecimal otherIncomesCash,
        /** Efectivo según sistema antes de descontar faltantes ya registrados. */
        BigDecimal systemCashTotal,
        /** Faltantes de caja ya cargados (entrega anterior, cierre, etc.). */
        BigDecimal cashShortfallsDeducted,
        BigDecimal expectedCashTotal) {}
