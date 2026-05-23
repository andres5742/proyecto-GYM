package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DayWorkoutRegisterRequest(
        Long memberId, @NotNull PaymentMethod paymentMethod, @Valid List<PaymentSplitLine> paymentSplits) {}
