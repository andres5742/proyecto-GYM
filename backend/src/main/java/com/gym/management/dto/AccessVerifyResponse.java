package com.gym.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.management.model.AccessPersonType;
import com.gym.management.model.AccessResult;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Gender;
import java.time.LocalDate;
import java.util.List;

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
        Long accessLogId,
        /** Días hasta vencimiento (1–5); null si no aplica. */
        Integer membershipDaysRemaining,
        /** Fecha de vencimiento de la membresía cuando aplica. */
        LocalDate membershipEndDate,
        /** Entrenos tiquetera tras este ingreso; null si no es tiquetera. */
        Integer tiqueteraEntriesRemainingAfter,
        Boolean tiqueteraPlan,
        /** Afiliados posibles cuando varias personas comparten el mismo código de tarjeta. */
        List<CardSelectionCandidate> cardSelectionCandidates) {

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
        this(
                result,
                gateOpened,
                message,
                memberId,
                employeeId,
                personType,
                memberName,
                deviceUserId,
                credentialType,
                gender,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public AccessVerifyResponse {
        if (personType == null) {
            personType = employeeId != null ? AccessPersonType.STAFF : AccessPersonType.MEMBER;
        }
        if (cardSelectionCandidates == null) {
            cardSelectionCandidates = List.of();
        }
    }

    @JsonProperty("fingerprintUserId")
    public String fingerprintUserId() {
        return deviceUserId;
    }
}
