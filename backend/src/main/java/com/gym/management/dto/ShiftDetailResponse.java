package com.gym.management.dto;

import java.util.List;

public record ShiftDetailResponse(
        WorkShiftResponse shift,
        SalesSummaryResponse summary,
        List<ProductSalesRowResponse> productRows,
        List<SaleResponse> sales
) {}
