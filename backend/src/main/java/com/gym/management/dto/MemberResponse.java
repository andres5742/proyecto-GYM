package com.gym.management.dto;

import com.gym.management.model.MembershipStatus;
import java.time.Instant;
import java.time.LocalDate;

public record MemberResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String documentId,
        Long planId,
        String planName,
        MembershipStatus status,
        LocalDate membershipStart,
        LocalDate membershipEnd,
        Instant createdAt,
        Instant updatedAt
) {}
