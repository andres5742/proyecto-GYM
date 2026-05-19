package com.gym.management.dto;

import jakarta.validation.constraints.Size;

public record RatingParticipantUpdateRequest(Boolean ratingEligible, @Size(max = 500) String photoUrl) {}
