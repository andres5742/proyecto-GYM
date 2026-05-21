package com.gym.management.dto;

import java.util.List;

public record ShiftOpenInventoryPreviewResponse(
        boolean inventoryCheckRequired,
        Long previousShiftId,
        String previousShiftName,
        String previousEmployeeName,
        List<ProductInventoryLineResponse> products) {}
