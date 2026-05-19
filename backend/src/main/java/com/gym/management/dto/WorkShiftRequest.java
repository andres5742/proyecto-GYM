package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record WorkShiftRequest(
        @NotBlank @Size(max = 80) String name,
        LocalDate shiftDate,
        Long employeeId
) {}
