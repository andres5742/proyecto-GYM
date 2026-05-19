package com.gym.management.dto;

import java.time.Instant;

public record SiteFooterResponse(
        String tagline,
        String address,
        String phone,
        String instagramUrl,
        String facebookUrl,
        String tiktokUrl,
        String youtubeUrl,
        String whatsappUrl,
        Instant updatedAt
) {}
