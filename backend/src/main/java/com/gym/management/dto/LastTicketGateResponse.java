package com.gym.management.dto;

public record LastTicketGateResponse(
        boolean gateOpened, String message, String speechText, SaleResponse sale) {}
