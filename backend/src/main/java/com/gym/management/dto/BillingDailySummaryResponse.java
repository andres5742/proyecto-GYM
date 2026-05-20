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
        long sportsDanceCount,
        BigDecimal sportsDanceTotal,
        Map<PaymentMethod, BigDecimal> sportsDanceByMethod,
        long membershipCount,
        BigDecimal membershipTotal,
        Map<PaymentMethod, BigDecimal> membershipByMethod,
        Map<PaymentMethod, BigDecimal> incomeByMethod,
        long expenseCount,
        BigDecimal expensesTotal,
        Map<PaymentMethod, BigDecimal> expensesByMethod,
        BigDecimal grandTotal) {}
