package com.gym.management.dto;

import com.gym.management.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewMemberOnboardingData(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 20) String documentId,
        @Size(max = 20) String phone,
        Gender gender) {}
