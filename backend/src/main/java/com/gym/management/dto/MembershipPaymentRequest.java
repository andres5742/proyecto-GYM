package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record MembershipPaymentRequest(
        @NotNull Long memberId, @NotNull Long planId, @NotNull PaymentMethod paymentMethod) {}
