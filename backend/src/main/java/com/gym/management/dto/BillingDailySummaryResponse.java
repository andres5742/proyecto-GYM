package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record BillingDailySummaryResponse(
        LocalDate date,
        long dayWorkoutCount,
        BigDecimal dayWorkoutTotal,
        Map<PaymentMethod, BigDecimal> dayWorkoutByMethod,
        long membershipCount,
        BigDecimal membershipTotal,
        Map<PaymentMethod, BigDecimal> membershipByMethod,
        BigDecimal grandTotal) {}
