package com.gym.management.dto;

import java.math.BigDecimal;

public record ShiftOpenCashPreviewResponse(
        Long cashRegisterId,
        String cashRegisterOpenedByName,
        BigDecimal openingCashAmount,
        BigDecimal cashExpenses,
        BigDecimal productCashTotal,
        /** Efectivo contado en la última entrega de turno del día (lo que quedó en caja). */
        BigDecimal lastHandoverCashTotal,
        /** Entradas/salidas en efectivo en Facturación después de esa entrega. */
        BigDecimal cashSinceLastHandover,
        /** Ventas en efectivo de turnos ya cerrados hoy (misma lógica que entrega de turno). */
        BigDecimal closedShiftsCashNet,
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
