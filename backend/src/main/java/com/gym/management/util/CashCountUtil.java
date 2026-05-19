package com.gym.management.util;

import com.gym.management.model.ShiftHandover;
import java.math.BigDecimal;

public final class CashCountUtil {

    private CashCountUtil() {}

    public static BigDecimal totalCash(ShiftHandover handover) {
        return BigDecimal.valueOf(handover.getBill2000() * 2000L
                        + handover.getBill5000() * 5000L
                        + handover.getBill10000() * 10000L
                        + handover.getBill20000() * 20000L
                        + handover.getBill50000() * 50000L
                        + handover.getBill100000() * 100000L
                        + handover.getCoin1000() * 1000L
                        + handover.getCoin500() * 500L
                        + handover.getCoin200() * 200L
                        + handover.getCoin100() * 100L
                        + handover.getCoin50() * 50L);
    }
}
