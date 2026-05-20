package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MembershipOnboardingRequest(
        Long memberId,
        @Valid NewMemberOnboardingData newMember,
        @NotNull Long planId,
        @NotNull PaymentMethod paymentMethod,
        @NotNull @Min(1) @Max(36) Integer monthsPaid,
        @Valid AccessOnboardingData access) {}
