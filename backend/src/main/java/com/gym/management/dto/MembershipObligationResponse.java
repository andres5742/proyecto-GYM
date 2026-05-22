package com.gym.management.dto;

import com.gym.management.model.MembershipObligationStatus;
import java.time.LocalDate;

public record MembershipObligationResponse(
        Long id,
        Long memberId,
        String memberName,
        Long planId,
        String planName,
        int monthsPaid,
        long totalAmount,
        long amountPaid,
        long balance,
        MembershipObligationStatus status,
        LocalDate plannedMembershipStart,
        LocalDate plannedMembershipEnd) {}
