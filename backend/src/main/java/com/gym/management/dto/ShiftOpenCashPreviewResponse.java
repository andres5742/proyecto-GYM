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
        BigDecimal expectedCashTotal) {}
