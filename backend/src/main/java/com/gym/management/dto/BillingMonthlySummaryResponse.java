package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.Map;

public record BillingMonthlySummaryResponse(
        int year,
        int month,
        long totalPayments,
        BigDecimal grandTotal,
        BigDecimal totalExpenses,
        long expenseCount,
        Map<PaymentMethod, BigDecimal> byMethod,
        Map<PaymentMethod, BigDecimal> dayWorkoutByMethod,
        Map<PaymentMethod, BigDecimal> sportsDanceByMethod,
        Map<PaymentMethod, BigDecimal> membershipByMethod,
        Map<PaymentMethod, BigDecimal> expensesByMethod) {}
