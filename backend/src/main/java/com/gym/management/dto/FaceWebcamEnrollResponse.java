package com.gym.management.dto;

import com.gym.management.model.AccessPersonType;
import java.time.Instant;

public record FaceWebcamEnrollResponse(
        Long memberId,
        Long employeeId,
        AccessPersonType personType,
        String memberName,
        String documentId,
        Instant enrolledAt) {}
