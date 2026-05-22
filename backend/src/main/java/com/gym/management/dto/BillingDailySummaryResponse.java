package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.PaymentMethod;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record BillingDailySummaryResponse(
        LocalDate date,
        long dayWorkoutCount,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal dayWorkoutTotal,
        Map<PaymentMethod, BigDecimal> dayWorkoutByMethod,
        long sportsDanceCount,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal sportsDanceTotal,
        Map<PaymentMethod, BigDecimal> sportsDanceByMethod,
        long membershipCount,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal membershipTotal,
        Map<PaymentMethod, BigDecimal> membershipByMethod,
        Map<PaymentMethod, BigDecimal> incomeByMethod,
        long expenseCount,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal expensesTotal,
        Map<PaymentMethod, BigDecimal> expensesByMethod,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal grandTotal) {}
