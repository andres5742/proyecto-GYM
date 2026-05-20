package com.gym.management.dto;

public record DayWorkoutRegisterResponse(
        boolean gateOpened, String message, String speechText, BillingPaymentResponse payment) {}
