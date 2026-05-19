package com.gym.management.dto;

import com.gym.management.model.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackMessageRequest(
        @NotNull FeedbackType type,
        @NotBlank @Size(max = 3000) String message,
        @NotNull Boolean anonymous,
        @Size(max = 120) String authorName
) {}
