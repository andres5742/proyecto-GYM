package com.gym.management.dto;

import java.math.BigDecimal;

public record ShiftHandoverExpenseResponse(Long id, String description, BigDecimal amount) {}
