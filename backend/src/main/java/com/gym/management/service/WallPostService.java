package com.gym.management.service;

import com.gym.management.dto.WallPostRequest;
import com.gym.management.dto.WallPostResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.WallPostMapper;
import com.gym.management.model.Employee;
import com.gym.management.model.WallPost;
import com.gym.management.model.WallPostImage;
import com.gym.management.repository.WallPostRepository;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.SecurityUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WallPostService {

    private static final int MAX_IMAGES = 8;

    private final WallPostRepository wallPostRepository;
    private final EmployeeService employeeService;
    private final FileStorageService fileStorageService;

    @Transactional
    public List<WallPostResponse> findActive() {
        Instant now = Instant.now();
        wallPostRepository.deleteExpired(now);
        return wallPostRepository.findActivePosts(now).stream()
                .map(post -> WallPostMapper.toPublicResponse(post, fileStorageService::localUploadExists))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WallPostResponse> findAllForAdmin() {
        return wallPostRepository.findAllWithAuthor().stream()
                .map(WallPostMapper::toResponse)
                .toList();
    }

    @Transactional
    public WallPostResponse create(WallPostRequest request) {
        Employee author = getCurrentAuthor();
        WallPost post = buildPost(new WallPost(), request, author);
        post.setPublishedAt(Instant.now());
        return WallPostMapper.toResponse(wallPostRepository.save(post));
    }

    @Transactional
    public WallPostResponse update(Long id, WallPostRequest request) {
        WallPost post = getPost(id);
        buildPost(post, request, post.getAuthor());
        return WallPostMapper.toResponse(wallPostRepository.save(post));
    }

    @Transactional
    public void delete(Long id) {
        if (!wallPostRepository.existsById(id)) {
            throw new ResourceNotFoundException("Publicación no encontrada: " + id);
        }
        wallPostRepository.deleteById(id);
    }

    @Transactional
    public int purgeExpired() {
        return wallPostRepository.deleteExpired(Instant.now());
    }

    private WallPost buildPost(WallPost post, WallPostRequest request, Employee author) {
        boolean permanent = Boolean.TRUE.equals(request.permanent());
        Integer displayDays = request.displayDays();

        if (!permanent && (displayDays == null || displayDays < 1)) {
            throw new BusinessException("Indique cuántos días estará visible la publicación (mínimo 1)");
        }

        post.setAuthor(author);
        post.setTitle(request.title().trim());
        post.setBody(request.body().trim());
        post.setEmoji(blankToNull(request.emoji()));
        post.setCategory(request.category());
        post.setPermanent(permanent);
        post.setDisplayDays(permanent ? null : displayDays);
        Instant publishedAt = post.getPublishedAt() != null ? post.getPublishedAt() : Instant.now();
        post.setExpiresAt(permanent ? null : publishedAt.plus(displayDays, ChronoUnit.DAYS));
        syncImages(post, request.imageUrls());
        return post;
    }

    private void syncImages(WallPost post, List<String> imageUrls) {
        post.getImages().clear();
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        List<String> cleaned = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .distinct()
                .limit(MAX_IMAGES)
                .toList();
        int order = 0;
        for (String url : cleaned) {
            post.getImages()
                    .add(WallPostImage.builder()
                            .wallPost(post)
                            .imageUrl(url)
                            .displayOrder(order++)
                            .build());
        }
    }

    private Employee getCurrentAuthor() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new BusinessException("Debe iniciar sesión para publicar");
        }
        return employeeService.getEmployee(user.employeeId());
    }

    private WallPost getPost(Long id) {
        return wallPostRepository
                .findByIdWithAuthorAndImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada: " + id));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
