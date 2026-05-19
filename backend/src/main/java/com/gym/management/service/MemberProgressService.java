package com.gym.management.service;

import com.gym.management.dto.MemberProgressEntryRequest;
import com.gym.management.dto.MemberProgressEntryResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.Member;
import com.gym.management.model.MemberProgressEntry;
import com.gym.management.model.MemberProgressPhoto;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.MemberProgressEntryRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberProgressService {

    private static final int MAX_IMAGES = 4;

    private final MemberProgressEntryRepository progressRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<MemberProgressEntryResponse> listMine() {
        Member member = getCurrentMember();
        return progressRepository.findByMemberIdOrderByRecordedAtDescIdDesc(member.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MemberProgressEntryResponse create(MemberProgressEntryRequest request) {
        validateRequest(request);
        Member member = getCurrentMember();
        MemberProgressEntry entry = MemberProgressEntry.builder()
                .member(member)
                .recordedAt(request.recordedAt())
                .build();
        applyMeasurements(entry, request);
        syncPhotos(entry, request.imageUrls());
        return toResponse(progressRepository.save(entry));
    }

    @Transactional
    public MemberProgressEntryResponse update(Long id, MemberProgressEntryRequest request) {
        validateRequest(request);
        Member member = getCurrentMember();
        MemberProgressEntry entry = progressRepository
                .findByIdAndMemberId(id, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registro de avance no encontrado"));
        applyMeasurements(entry, request);
        syncPhotos(entry, request.imageUrls());
        return toResponse(entry);
    }

    @Transactional
    public void delete(Long id) {
        Member member = getCurrentMember();
        MemberProgressEntry entry = progressRepository
                .findByIdAndMemberId(id, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registro de avance no encontrado"));
        progressRepository.delete(entry);
    }

    private void applyMeasurements(MemberProgressEntry entry, MemberProgressEntryRequest request) {
        entry.setRecordedAt(request.recordedAt());
        entry.setWeightKg(normalizeMetric(request.weightKg()));
        entry.setChestCm(normalizeMetric(request.chestCm()));
        entry.setWaistCm(normalizeMetric(request.waistCm()));
        entry.setHipsCm(normalizeMetric(request.hipsCm()));
        entry.setArmRightCm(normalizeMetric(request.armRightCm()));
        entry.setArmLeftCm(normalizeMetric(request.armLeftCm()));
        entry.setThighUpperRightCm(normalizeMetric(request.thighUpperRightCm()));
        entry.setThighUpperLeftCm(normalizeMetric(request.thighUpperLeftCm()));
        entry.setThighLowerRightCm(normalizeMetric(request.thighLowerRightCm()));
        entry.setThighLowerLeftCm(normalizeMetric(request.thighLowerLeftCm()));
        entry.setCalfRightCm(normalizeMetric(request.calfRightCm()));
        entry.setCalfLeftCm(normalizeMetric(request.calfLeftCm()));
        entry.setBodyFatPercent(normalizeMetric(request.bodyFatPercent()));
        entry.setNotes(trimToNull(request.notes()));
    }

    private static BigDecimal normalizeMetric(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value;
    }

    private void validateRequest(MemberProgressEntryRequest request) {
        List<String> problems = new ArrayList<>();
        checkNonNegative(problems, "peso", request.weightKg());
        checkNonNegative(problems, "pecho", request.chestCm());
        checkNonNegative(problems, "cintura", request.waistCm());
        checkNonNegative(problems, "cadera", request.hipsCm());
        checkNonNegative(problems, "brazo derecho", request.armRightCm());
        checkNonNegative(problems, "brazo izquierdo", request.armLeftCm());
        checkNonNegative(problems, "muslo superior derecho", request.thighUpperRightCm());
        checkNonNegative(problems, "muslo superior izquierdo", request.thighUpperLeftCm());
        checkNonNegative(problems, "muslo inferior derecho", request.thighLowerRightCm());
        checkNonNegative(problems, "muslo inferior izquierdo", request.thighLowerLeftCm());
        checkNonNegative(problems, "pantorrilla derecha", request.calfRightCm());
        checkNonNegative(problems, "pantorrilla izquierda", request.calfLeftCm());
        checkNonNegative(problems, "grasa corporal", request.bodyFatPercent());
        if (!problems.isEmpty()) {
            throw new BusinessException(String.join(". ", problems));
        }

        if (!hasAnyContent(request)) {
            throw new BusinessException("Registra al menos un dato: peso, alguna medida, una nota o una foto");
        }
    }

    private static boolean hasAnyContent(MemberProgressEntryRequest request) {
        boolean hasMetric = normalizeMetric(request.weightKg()) != null
                || normalizeMetric(request.chestCm()) != null
                || normalizeMetric(request.waistCm()) != null
                || normalizeMetric(request.hipsCm()) != null
                || normalizeMetric(request.armRightCm()) != null
                || normalizeMetric(request.armLeftCm()) != null
                || normalizeMetric(request.thighUpperRightCm()) != null
                || normalizeMetric(request.thighUpperLeftCm()) != null
                || normalizeMetric(request.thighLowerRightCm()) != null
                || normalizeMetric(request.thighLowerLeftCm()) != null
                || normalizeMetric(request.calfRightCm()) != null
                || normalizeMetric(request.calfLeftCm()) != null
                || normalizeMetric(request.bodyFatPercent()) != null;
        boolean hasNotes = request.notes() != null && !request.notes().isBlank();
        boolean hasPhotos = request.imageUrls() != null && !request.imageUrls().isEmpty();
        return hasMetric || hasNotes || hasPhotos;
    }

    private static void checkNonNegative(List<String> problems, String label, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            problems.add("El valor de " + label + " no puede ser negativo");
        }
    }

    private void syncPhotos(MemberProgressEntry entry, List<String> imageUrls) {
        entry.getPhotos().clear();
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        List<String> cleaned = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .filter(this::isAllowedUploadUrl)
                .distinct()
                .limit(MAX_IMAGES)
                .toList();
        int order = 0;
        for (String url : cleaned) {
            entry.getPhotos()
                    .add(MemberProgressPhoto.builder()
                            .entry(entry)
                            .imageUrl(url)
                            .displayOrder(order++)
                            .build());
        }
    }

    private boolean isAllowedUploadUrl(String url) {
        return url.startsWith("/uploads/") && !url.contains("..");
    }

    private Member getCurrentMember() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null || user.memberId() == null) {
            throw new BusinessException("Sesión de afiliado no válida");
        }
        Member member = memberRepository
                .findById(user.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado"));
        if (MemberMembershipRules.effectiveStatus(member) != MembershipStatus.ACTIVE) {
            throw new BusinessException("Tu membresía no está activa");
        }
        return member;
    }

    private MemberProgressEntryResponse toResponse(MemberProgressEntry entry) {
        List<String> imageUrls = entry.getPhotos().stream()
                .map(MemberProgressPhoto::getImageUrl)
                .toList();
        return new MemberProgressEntryResponse(
                entry.getId(),
                entry.getRecordedAt(),
                entry.getWeightKg(),
                entry.getChestCm(),
                entry.getWaistCm(),
                entry.getHipsCm(),
                entry.getArmRightCm(),
                entry.getArmLeftCm(),
                entry.getThighUpperRightCm(),
                entry.getThighUpperLeftCm(),
                entry.getThighLowerRightCm(),
                entry.getThighLowerLeftCm(),
                entry.getCalfRightCm(),
                entry.getCalfLeftCm(),
                entry.getBodyFatPercent(),
                entry.getNotes(),
                imageUrls,
                entry.getCreatedAt());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
