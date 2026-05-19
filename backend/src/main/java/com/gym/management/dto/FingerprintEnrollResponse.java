package com.gym.management.dto;

import java.time.Instant;

public record FingerprintEnrollResponse(
        Long memberId,
        String memberName,
        String fingerprintUserId,
        String deviceLabel,
        Instant enrolledAt
) {}
