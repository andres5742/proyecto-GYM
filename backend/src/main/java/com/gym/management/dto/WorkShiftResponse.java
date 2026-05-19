package com.gym.management.dto;

import com.gym.management.model.ShiftStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkShiftResponse(
        Long id,
        LocalDate shiftDate,
        String name,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        ShiftStatus status,
        Instant createdAt
) {}
