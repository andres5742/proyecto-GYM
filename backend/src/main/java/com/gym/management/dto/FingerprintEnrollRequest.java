package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FingerprintEnrollRequest(
        @NotNull Long memberId,
        @NotBlank @Size(max = 64) String fingerprintUserId,
        @Size(max = 120) String deviceLabel
) {}
