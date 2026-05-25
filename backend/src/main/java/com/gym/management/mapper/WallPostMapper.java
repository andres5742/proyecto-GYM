package com.gym.management.mapper;

import com.gym.management.dto.WallPostResponse;
import com.gym.management.model.Employee;
import com.gym.management.model.UserRole;
import com.gym.management.model.WallPost;
import com.gym.management.model.WallPostImage;
import java.util.List;
import java.util.function.Predicate;

public final class WallPostMapper {

    private WallPostMapper() {}

    public static WallPostResponse toResponse(WallPost post) {
        Employee author = post.getAuthor();
        return new WallPostResponse(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getEmoji(),
                post.getCategory(),
                categoryLabel(post.getCategory()),
                author.getId(),
                author.getFirstName() + " " + author.getLastName(),
                author.getRole().displayLabel(),
                post.getPublishedAt(),
                post.isPermanent(),
                post.getDisplayDays(),
                post.getExpiresAt(),
                post.getCreatedAt(),
                imageUrls(post));
    }

    /** Muro público: el super administrador no se muestra por nombre personal. */
    public static WallPostResponse toPublicResponse(WallPost post) {
        return toPublicResponse(post, url -> true);
    }

    public static WallPostResponse toPublicResponse(WallPost post, Predicate<String> includeImageUrl) {
        Employee author = post.getAuthor();
        return new WallPostResponse(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getEmoji(),
                post.getCategory(),
                categoryLabel(post.getCategory()),
                author.getId(),
                publicAuthorName(author),
                publicAuthorRoleLabel(author),
                post.getPublishedAt(),
                post.isPermanent(),
                post.getDisplayDays(),
                post.getExpiresAt(),
                post.getCreatedAt(),
                imageUrls(post, includeImageUrl));
    }

    private static String publicAuthorName(Employee author) {
        if (author.getRole() == UserRole.SUPER_ADMIN) {
            return "Administración";
        }
        return author.getFirstName() + " " + author.getLastName();
    }

    private static String publicAuthorRoleLabel(Employee author) {
        if (author.getRole() == UserRole.SUPER_ADMIN) {
            return "Administración";
        }
        return author.getRole().displayLabel();
    }

    private static List<String> imageUrls(WallPost post) {
        return imageUrls(post, url -> true);
    }

    private static List<String> imageUrls(WallPost post, Predicate<String> includeImageUrl) {
        if (post.getImages() == null || post.getImages().isEmpty()) {
            return List.of();
        }
        return post.getImages().stream()
                .map(WallPostImage::getImageUrl)
                .filter(includeImageUrl)
                .toList();
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
