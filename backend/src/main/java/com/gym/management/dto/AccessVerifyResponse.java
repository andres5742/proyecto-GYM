package com.gym.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.management.model.AccessPersonType;
import com.gym.management.model.AccessResult;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Gender;

public record AccessVerifyResponse(
        AccessResult result,
        boolean gateOpened,
        String message,
        Long memberId,
        Long employeeId,
        AccessPersonType personType,
        String memberName,
        String deviceUserId,
        BiometricCredentialType credentialType,
        Gender gender) {

    public AccessVerifyResponse {
        if (personType == null) {
            personType = employeeId != null ? AccessPersonType.STAFF : AccessPersonType.MEMBER;
        }
    }

    @JsonProperty("fingerprintUserId")
    public String fingerprintUserId() {
        return deviceUserId;
    }
}
