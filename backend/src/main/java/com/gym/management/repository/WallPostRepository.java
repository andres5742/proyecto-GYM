package com.gym.management.repository;

import com.gym.management.model.WallPost;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WallPostRepository extends JpaRepository<WallPost, Long> {

    @Query(
            """
            SELECT DISTINCT p FROM WallPost p
            JOIN FETCH p.author
            LEFT JOIN FETCH p.images
            WHERE p.permanent = true OR p.expiresAt IS NULL OR p.expiresAt > :now
            ORDER BY p.publishedAt DESC
            """)
    List<WallPost> findActivePosts(Instant now);

    @Query(
            """
            SELECT DISTINCT p FROM WallPost p
            JOIN FETCH p.author
            LEFT JOIN FETCH p.images
            ORDER BY p.publishedAt DESC
            """)
    List<WallPost> findAllWithAuthor();

    @Query(
            """
            SELECT DISTINCT p FROM WallPost p
            JOIN FETCH p.author
            LEFT JOIN FETCH p.images
            WHERE p.id = :id
            """)
    java.util.Optional<WallPost> findByIdWithAuthorAndImages(Long id);

    @Modifying
    @Query("DELETE FROM WallPost p WHERE p.permanent = false AND p.expiresAt IS NOT NULL AND p.expiresAt <= :now")
    int deleteExpired(Instant now);
}
