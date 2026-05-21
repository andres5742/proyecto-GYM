package com.gym.management.dto;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record CashDenominationCount(
        @Min(0) int bill2000,
        @Min(0) int bill5000,
        @Min(0) int bill10000,
        @Min(0) int bill20000,
        @Min(0) int bill50000,
        @Min(0) int bill100000,
        @Min(0) int coin1000,
        @Min(0) int coin500,
        @Min(0) int coin200,
        @Min(0) int coin100,
        @Min(0) int coin50) {

    public BigDecimal totalCash() {
        return BigDecimal.valueOf(
                (long) bill2000 * 2000L
                        + (long) bill5000 * 5000L
                        + (long) bill10000 * 10000L
                        + (long) bill20000 * 20000L
                        + (long) bill50000 * 50000L
                        + (long) bill100000 * 100000L
                        + (long) coin1000 * 1000L
                        + (long) coin500 * 500L
                        + (long) coin200 * 200L
                        + (long) coin100 * 100L
                        + (long) coin50 * 50L);
    }
}
