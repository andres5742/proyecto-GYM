package com.gym.management.dto;

import java.time.Instant;
import java.time.LocalDate;

public record HolidayResponse(
        Long id, LocalDate date, String name, String description, Instant createdAt) {}
