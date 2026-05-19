package com.gym.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkAttendanceRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate workDate,
        @NotNull LocalDateTime clockIn,
        LocalDateTime clockOut,
        @Size(max = 500) String notes
) {}
