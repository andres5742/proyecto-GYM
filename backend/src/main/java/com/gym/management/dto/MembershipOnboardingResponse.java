package com.gym.management.dto;

public record MembershipOnboardingResponse(
        MemberResponse member,
        BillingPaymentResponse payment,
        MembershipObligationResponse openObligation,
        boolean membershipActivated,
        long balanceRemaining,
        String paymentMessage,
        boolean accessRegistered,
        String accessMessage) {}
