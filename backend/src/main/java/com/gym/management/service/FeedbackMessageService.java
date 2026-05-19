package com.gym.management.service;

import com.gym.management.dto.FeedbackMessageRequest;
import com.gym.management.dto.FeedbackMessageResponse;
import com.gym.management.dto.FeedbackStatusUpdateRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.FeedbackMessage;
import com.gym.management.model.FeedbackMessageImage;
import com.gym.management.model.FeedbackStatus;
import com.gym.management.model.FeedbackType;
import com.gym.management.repository.FeedbackMessageRepository;
import com.gym.management.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackMessageService {

    private static final int MAX_IMAGES = 4;

    private final FeedbackMessageRepository feedbackMessageRepository;

    @Transactional
    public FeedbackMessageResponse create(FeedbackMessageRequest request) {
        boolean anonymous = Boolean.TRUE.equals(request.anonymous());
        String name = anonymous ? null : blankToNull(request.authorName());
        if (!anonymous && name == null) {
            throw new BusinessException("Indica tu nombre o marca la opción anónima");
        }

        FeedbackMessage message = FeedbackMessage.builder()
                .type(request.type())
                .message(request.message().trim())
                .anonymous(anonymous)
                .authorName(name)
                .status(FeedbackStatus.PENDING)
                .build();
        syncImages(message, request.imageUrls());
        return toResponse(feedbackMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<FeedbackMessageResponse> findAllForAdmin() {
        return feedbackMessageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return feedbackMessageRepository.countByStatus(FeedbackStatus.PENDING);
    }

    @Transactional
    public FeedbackMessageResponse updateStatus(Long id, FeedbackStatusUpdateRequest request) {
        if (request.status() == FeedbackStatus.PENDING) {
            throw new BusinessException("El estado debe ser resuelta o no resuelta");
        }
        FeedbackMessage message = feedbackMessageRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado: " + id));
        message.setStatus(request.status());
        message.setAdminNote(blankToNull(request.adminNote()));
        message.setResolvedAt(Instant.now());
        return toResponse(feedbackMessageRepository.save(message));
    }

    @Transactional
    public void delete(Long id) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException("Solo el super administrador puede eliminar mensajes del buzón");
        }
        if (!feedbackMessageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Mensaje no encontrado: " + id);
        }
        feedbackMessageRepository.deleteById(id);
    }

    private void syncImages(FeedbackMessage message, List<String> imageUrls) {
        message.getImages().clear();
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
            message.getImages()
                    .add(FeedbackMessageImage.builder()
                            .feedbackMessage(message)
                            .imageUrl(url)
                            .displayOrder(order++)
                            .build());
        }
    }

    private FeedbackMessageResponse toResponse(FeedbackMessage message) {
        List<String> imageUrls = message.getImages().stream()
                .map(FeedbackMessageImage::getImageUrl)
                .toList();
        return new FeedbackMessageResponse(
                message.getId(),
                message.getType(),
                typeLabel(message.getType()),
                message.getMessage(),
                message.isAnonymous(),
                message.getAuthorName(),
                displayName(message),
                message.getStatus(),
                statusLabel(message.getStatus()),
                message.getAdminNote(),
                message.getCreatedAt(),
                message.getResolvedAt(),
                imageUrls);
    }

    private static String displayName(FeedbackMessage message) {
        if (message.isAnonymous()) {
            return "Anónimo";
        }
        return message.getAuthorName() != null ? message.getAuthorName() : "Sin nombre";
    }

    private static String typeLabel(FeedbackType type) {
        return switch (type) {
            case SUGGESTION -> "Sugerencia";
            case COMPLAINT -> "Queja o reclamo";
            case PRAISE -> "Felicitación";
        };
    }

    private static String statusLabel(FeedbackStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case RESOLVED -> "Solucionada";
            case NOT_RESOLVED -> "No solucionada";
        };
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
