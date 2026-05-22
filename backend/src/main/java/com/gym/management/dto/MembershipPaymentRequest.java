package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MembershipPaymentRequest(
        @NotNull Long memberId,
        @NotNull Long planId,
        @NotNull PaymentMethod paymentMethod,
        @NotNull @Min(1) @Max(36) Integer monthsPaid,
        @NotNull @Min(1) Long amount,
        Long obligationId) {}
