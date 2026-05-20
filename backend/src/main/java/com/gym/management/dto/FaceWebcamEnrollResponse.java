package com.gym.management.dto;

import java.time.Instant;

public record FaceWebcamEnrollResponse(
        Long memberId, String memberName, String documentId, Instant enrolledAt) {}
