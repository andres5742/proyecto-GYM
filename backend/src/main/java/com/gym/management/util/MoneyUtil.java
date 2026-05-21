package com.gym.management.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Montos en pesos colombianos sin centavos (redondeo al peso más cercano). */
public final class MoneyUtil {

    private MoneyUtil() {}

    public static BigDecimal roundPesos(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    public static String formatPesos(BigDecimal value) {
        return roundPesos(value).toPlainString();
    }
}
