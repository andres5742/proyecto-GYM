package com.gym.management.dto;

import com.gym.management.model.FeedbackStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackStatusUpdateRequest(
        @NotNull FeedbackStatus status,
        @Size(max = 1000) String adminNote
) {}
