package com.gym.management.dto;

import com.gym.management.model.AccessResult;
import java.time.Instant;

public record AccessLogResponse(
        Long id,
        String fingerprintUserId,
        Long memberId,
        String memberName,
        AccessResult result,
        String resultLabel,
        String message,
        boolean gateOpened,
        Instant createdAt
) {}
