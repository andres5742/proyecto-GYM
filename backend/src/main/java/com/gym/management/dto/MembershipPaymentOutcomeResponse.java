package com.gym.management.dto;

public record MembershipPaymentOutcomeResponse(
        BillingPaymentResponse payment,
        MembershipObligationResponse obligation,
        boolean membershipActivated,
        long balanceRemaining,
        String message) {}
