package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record DayWorkoutRegisterRequest(
        Long memberId, @NotNull PaymentMethod paymentMethod) {}
