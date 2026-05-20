package com.gym.management.dto;

import com.gym.management.model.AccessPersonType;
import com.gym.management.model.BiometricCredentialType;
import java.time.Instant;

public record BiometricEnrollResponse(
        Long memberId,
        Long employeeId,
        AccessPersonType personType,
        String memberName,
        String deviceUserId,
        BiometricCredentialType credentialType,
        String credentialTypeLabel,
        String deviceLabel,
        Instant enrolledAt) {}
