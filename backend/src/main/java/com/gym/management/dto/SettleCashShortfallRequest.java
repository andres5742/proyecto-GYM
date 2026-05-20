package com.gym.management.dto;

import jakarta.validation.constraints.Size;

public record SettleCashShortfallRequest(@Size(max = 500) String notes) {}
