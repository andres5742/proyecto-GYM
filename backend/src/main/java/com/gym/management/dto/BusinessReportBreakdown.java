package com.gym.management.dto;

import java.util.List;

public record BusinessReportBreakdown(
        BillingTypeReportSection dayWorkout,
        BillingTypeReportSection sportsDance,
        BillingTypeReportSection membership,
        BillingTypeReportSection otherIncomes,
        List<MembershipPlanReportLine> membershipByPlan,
        List<ProductSaleByPaymentLine> productSalesByPayment) {}
