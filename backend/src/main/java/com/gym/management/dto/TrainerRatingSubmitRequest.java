package com.gym.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainerRatingSubmitRequest(
        @NotNull Long employeeId,
        @NotBlank @Size(min = 5, max = 20) String identificationNumber,
        @Min(1) @Max(10) int score) {}
