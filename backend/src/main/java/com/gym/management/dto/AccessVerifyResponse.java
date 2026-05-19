package com.gym.management.dto;

import com.gym.management.model.AccessResult;

public record AccessVerifyResponse(
        AccessResult result,
        boolean gateOpened,
        String message,
        Long memberId,
        String memberName,
        String fingerprintUserId
) {}
