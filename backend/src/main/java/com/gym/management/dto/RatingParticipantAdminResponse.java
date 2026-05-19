package com.gym.management.dto;

import com.gym.management.model.UserRole;

public record RatingParticipantAdminResponse(
        Long id,
        String fullName,
        UserRole role,
        String roleLabel,
        Boolean active,
        Boolean ratingEligible,
        String photoUrl) {}
