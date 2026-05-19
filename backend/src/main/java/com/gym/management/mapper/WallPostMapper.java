package com.gym.management.mapper;

import com.gym.management.dto.WallPostResponse;
import com.gym.management.model.WallPost;
import com.gym.management.model.WallPostImage;
import java.util.List;

public final class WallPostMapper {

    private WallPostMapper() {}

    public static WallPostResponse toResponse(WallPost post) {
        return new WallPostResponse(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getEmoji(),
                post.getCategory(),
                categoryLabel(post.getCategory()),
                post.getAuthor().getId(),
                post.getAuthor().getFirstName() + " " + post.getAuthor().getLastName(),
                post.getAuthor().getRole().displayLabel(),
                post.getPublishedAt(),
                post.isPermanent(),
                post.getDisplayDays(),
                post.getExpiresAt(),
                post.getCreatedAt(),
                imageUrls(post));
    }

    private static List<String> imageUrls(WallPost post) {
        if (post.getImages() == null || post.getImages().isEmpty()) {
            return List.of();
        }
        return post.getImages().stream().map(WallPostImage::getImageUrl).toList();
    }

    public static String categoryLabel(com.gym.management.model.PostCategory category) {
        return switch (category) {
            case AVISO -> "Aviso";
            case PROMO -> "Promoción";
            case HORARIO -> "Horario";
            case MOTIVACION -> "Motivación";
        };
    }
}
