package com.gym.management.dto;

import com.gym.management.model.AccessResult;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Gender;
import java.time.Instant;

/** Evento de acceso para la pantalla /acceso (polling tras lectura ZKTeco). */
public record KioskAccessEventResponse(
        Long id,
        String deviceUserId,
        BiometricCredentialType credentialType,
        String credentialTypeLabel,
        Long memberId,
        String memberName,
        AccessResult result,
        String message,
        boolean gateOpened,
        Instant createdAt,
        Gender gender,
        String documentId) {}
