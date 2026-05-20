package com.gym.management.dto;

public record MembershipOnboardingResponse(
        MemberResponse member,
        BillingPaymentResponse payment,
        boolean accessRegistered,
        String accessMessage) {}
