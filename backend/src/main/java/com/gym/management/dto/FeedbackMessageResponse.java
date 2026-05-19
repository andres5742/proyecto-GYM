package com.gym.management.dto;

import com.gym.management.model.FeedbackStatus;
import com.gym.management.model.FeedbackType;
import java.time.Instant;
import java.util.List;

public record FeedbackMessageResponse(
        Long id,
        FeedbackType type,
        String typeLabel,
        String message,
        boolean anonymous,
        String authorName,
        String displayName,
        FeedbackStatus status,
        String statusLabel,
        String adminNote,
        Instant createdAt,
        Instant resolvedAt,
        List<String> imageUrls
) {}
