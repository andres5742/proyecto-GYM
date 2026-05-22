package com.gym.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record WorkShiftRequest(
        @NotBlank @Size(max = 80) String name,
        LocalDate shiftDate,
        Long employeeId,
        @Valid List<ProductInventoryCountItem> inventoryCounts,
        @Valid CashDenominationCount cashCount) {}
