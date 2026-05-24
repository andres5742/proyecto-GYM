package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.PaymentMethod;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record MonthlyBusinessReportResponse(
        int year,
        int month,
        LocalDate startDate,
        LocalDate endDate,
        long cashRegisterDays,
        long billingPaymentCount,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal billingIncomeTotal,
        Map<PaymentMethod, BigDecimal> billingIncomeByMethod,
        ProductSalesReportSection productSales,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal fiadoCollectedTotal,
        Map<PaymentMethod, BigDecimal> fiadoCollectedByMethod,
        long expenseCount,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal expensesTotal,
        Map<PaymentMethod, BigDecimal> expensesByMethod,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal totalIncome,
        Map<PaymentMethod, BigDecimal> totalIncomeByMethod,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal netResult,
        List<ProductInventoryReportLine> inventory,
        BusinessReportBreakdown breakdown) {}
