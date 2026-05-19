package com.gym.management.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record BusinessDayScheduleDto(
        @NotNull DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        @NotNull Boolean closed
) {}
