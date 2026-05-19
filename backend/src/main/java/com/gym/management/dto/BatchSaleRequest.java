package com.gym.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchSaleRequest(
        @NotNull Long workShiftId,
        @NotEmpty @Valid List<BatchSaleLineRequest> lines
) {}
