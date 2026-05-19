package com.gym.management.dto;

import jakarta.validation.constraints.Size;

public record SiteFooterRequest(
        @Size(max = 300) String tagline,
        @Size(max = 300) String address,
        @Size(max = 30) String phone,
        @Size(max = 300) String instagramUrl,
        @Size(max = 300) String facebookUrl,
        @Size(max = 300) String tiktokUrl,
        @Size(max = 300) String youtubeUrl,
        @Size(max = 300) String whatsappUrl
) {}
