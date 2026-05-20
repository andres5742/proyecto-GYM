package com.gym.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.management.model.BiometricCredentialType;
import java.time.Instant;

public record BiometricEnrollResponse(
        Long memberId,
        String memberName,
        String deviceUserId,
        BiometricCredentialType credentialType,
        String credentialTypeLabel,
        String deviceLabel,
        Instant enrolledAt) {

    @JsonProperty("fingerprintUserId")
    public String fingerprintUserId() {
        return deviceUserId;
    }
}
