package com.gym.management.dto;

import com.gym.management.model.PostCategory;
import java.time.Instant;
import java.util.List;

public record WallPostResponse(
        Long id,
        String title,
        String body,
        String emoji,
        PostCategory category,
        String categoryLabel,
        Long authorId,
        String authorName,
        String authorRoleLabel,
        Instant publishedAt,
        boolean permanent,
        Integer displayDays,
        Instant expiresAt,
        Instant createdAt,
        List<String> imageUrls
) {}
