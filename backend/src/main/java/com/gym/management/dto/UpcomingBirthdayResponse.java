package com.gym.management.dto;

import com.gym.management.model.BirthdayPersonType;
import java.time.LocalDate;

public record UpcomingBirthdayResponse(
        BirthdayPersonType personType,
        String personTypeLabel,
        Long personId,
        String fullName,
        LocalDate celebrationDate,
        int daysUntil,
        int turningAge) {}
