package com.gym.management.dto;

import com.gym.management.model.MediaType;
import java.time.Instant;

public record GymMediaItemResponse(
        Long id,
        MediaType mediaType,
        String mediaUrl,
        String thumbnailUrl,
        String title,
        int displayOrder,
        boolean active,
        Instant createdAt
) {}
