package com.gym.management.dto;

import java.time.Instant;

public record CarouselSlideResponse(
        Long id,
        String imageUrl,
        String title,
        String caption,
        String linkUrl,
        int displayOrder,
        boolean active,
        Instant createdAt
) {}
