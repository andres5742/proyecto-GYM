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
        Gender gender,
        /** Cédula del afiliado cuando aplica (ingreso por documento o afiliado identificado). */
        String documentId,
        /** ID del registro en access_logs (pantalla /acceso). */
        Long accessLogId) {

    public AccessVerifyResponse(
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
        this(result, gateOpened, message, memberId, employeeId, personType, memberName, deviceUserId, credentialType, gender, null, null);
    }

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
