package com.gym.management.dto;

import java.math.BigDecimal;

/** Desglose de efectivo en caja de facturación para entrega de turno (sin productos ni fiado por turno). */
public record BillingHandoverCashBreakdown(
        BigDecimal cashBase,
        BigDecimal otherIncomesCash,
        BigDecimal total) {}
