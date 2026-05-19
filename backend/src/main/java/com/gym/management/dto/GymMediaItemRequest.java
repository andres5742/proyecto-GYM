package com.gym.management.dto;

import com.gym.management.model.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GymMediaItemRequest(
        @NotNull MediaType mediaType,
        @NotBlank @Size(max = 500) String mediaUrl,
        @Size(max = 500) String thumbnailUrl,
        @Size(max = 200) String title,
        Integer displayOrder,
        Boolean active
) {}
