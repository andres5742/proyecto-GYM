package com.gym.management.dto;

import com.gym.management.model.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FeedbackMessageRequest(
        @NotNull FeedbackType type,
        @NotBlank @Size(max = 3000) String message,
        @NotNull Boolean anonymous,
        @Size(max = 120) String authorName,
        @Size(max = 4) List<@NotBlank @Size(max = 500) String> imageUrls
) {}
