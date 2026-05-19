package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessVerifyRequest(@NotBlank @Size(max = 64) String fingerprintUserId) {}
