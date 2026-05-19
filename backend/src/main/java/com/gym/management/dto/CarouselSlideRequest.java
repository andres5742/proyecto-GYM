package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CarouselSlideRequest(
        @NotBlank @Size(max = 500) String imageUrl,
        @Size(max = 200) String title,
        @Size(max = 400) String caption,
        @Size(max = 500) String linkUrl,
        Integer displayOrder,
        Boolean active
) {}
