package com.gym.management.dto;

import com.gym.management.model.Gender;
import com.gym.management.model.MembershipStatus;
import java.time.LocalDate;

public record MemberPortalProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String documentId,
        Gender gender,
        String phone,
        String planName,
        MembershipStatus status,
        String statusLabel,
        LocalDate membershipStart,
        LocalDate membershipEnd
) {}
