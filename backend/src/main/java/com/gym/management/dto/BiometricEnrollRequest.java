package com.gym.management.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.gym.management.model.BiometricCredentialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BiometricEnrollRequest(
        @NotNull Long memberId,
        @NotBlank @Size(max = 64) @JsonAlias("fingerprintUserId") String deviceUserId,
        BiometricCredentialType credentialType,
        @Size(max = 120) String deviceLabel) {

    public BiometricEnrollRequest {
        if (credentialType == null) {
            credentialType = BiometricCredentialType.FINGERPRINT;
        }
    }
}
