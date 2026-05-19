package com.gym.management.dto;

import com.gym.management.model.PostCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WallPostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String body,
        @Size(max = 10) String emoji,
        @NotNull PostCategory category,
        @NotNull Boolean permanent,
        @Min(1) Integer displayDays,
        List<@Size(max = 500) String> imageUrls
) {}
