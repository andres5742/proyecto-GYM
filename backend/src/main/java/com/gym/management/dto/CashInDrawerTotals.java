package com.gym.management.dto;

import java.math.BigDecimal;

/** Mismo criterio que «Efectivo en caja» en Facturación. */
public record CashInDrawerTotals(
        BigDecimal total, BigDecimal lastHandoverCashTotal, BigDecimal cashSinceLastHandover) {}
