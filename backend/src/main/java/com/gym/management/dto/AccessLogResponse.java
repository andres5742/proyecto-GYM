package com.gym.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.management.model.AccessResult;
import com.gym.management.model.BiometricCredentialType;
import java.time.Instant;

public record AccessLogResponse(
        Long id,
        String deviceUserId,
        BiometricCredentialType credentialType,
        String credentialTypeLabel,
        Long memberId,
        String memberName,
        AccessResult result,
        String resultLabel,
        String message,
        boolean gateOpened,
        Instant createdAt) {

    @JsonProperty("fingerprintUserId")
    public String fingerprintUserId() {
        return deviceUserId;
    }
}
