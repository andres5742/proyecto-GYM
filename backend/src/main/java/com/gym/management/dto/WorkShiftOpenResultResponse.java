package com.gym.management.dto;

import java.math.BigDecimal;

public record WorkShiftOpenResultResponse(
        WorkShiftResponse shift,
        boolean inventoryAdjusted,
        boolean inventoryShortfallRegistered,
        BigDecimal inventoryShortfallAmount,
        String inventoryShortfallNotes,
        boolean cashShortfallRegistered,
        BigDecimal cashShortfallAmount,
        String cashShortfallNotes) {}
