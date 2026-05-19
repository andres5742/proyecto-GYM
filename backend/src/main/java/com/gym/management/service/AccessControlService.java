package com.gym.management.service;

import com.gym.management.dto.AccessLogResponse;
import com.gym.management.dto.AccessVerifyRequest;
import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.FingerprintEnrollRequest;
import com.gym.management.dto.FingerprintEnrollResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.AccessLog;
import com.gym.management.model.AccessResult;
import com.gym.management.model.Member;
import com.gym.management.model.MemberFingerprint;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.AccessLogRepository;
import com.gym.management.repository.MemberFingerprintRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.service.MemberMembershipRules;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final MemberRepository memberRepository;
    private final MemberFingerprintRepository fingerprintRepository;
    private final AccessLogRepository accessLogRepository;
    private final TurnstileGatewayService turnstileGatewayService;

    @Transactional
    public AccessVerifyResponse verifyAndOpen(AccessVerifyRequest request) {
        String fpId = request.fingerprintUserId().trim();
        return fingerprintRepository
                .findByFingerprintUserId(fpId)
                .map(fp -> evaluateMember(fp, fpId))
                .orElseGet(() -> deny(fpId, null, "Huella no registrada en el sistema"));
    }

    @Transactional
    public FingerprintEnrollResponse enroll(FingerprintEnrollRequest request) {
        Member member = memberRepository
                .findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));

        String fpId = request.fingerprintUserId().trim();
        fingerprintRepository.findByFingerprintUserId(fpId).ifPresent(existing -> {
            if (!existing.getMember().getId().equals(member.getId())) {
                throw new BusinessException("Ese ID de huella ya está asignado a otro afiliado");
            }
        });

        MemberFingerprint fingerprint = fingerprintRepository
                .findByMemberId(member.getId())
                .orElse(MemberFingerprint.builder().member(member).build());

        fingerprint.setFingerprintUserId(fpId);
        fingerprint.setDeviceLabel(blankToNull(request.deviceLabel()));
        fingerprint = fingerprintRepository.save(fingerprint);

        return new FingerprintEnrollResponse(
                member.getId(),
                member.getFirstName() + " " + member.getLastName(),
                fingerprint.getFingerprintUserId(),
                fingerprint.getDeviceLabel(),
                fingerprint.getEnrolledAt());
    }

    @Transactional
    public void removeEnrollment(Long memberId) {
        MemberFingerprint fp = fingerprintRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Este afiliado no tiene huella registrada"));
        fingerprintRepository.delete(fp);
    }

    @Transactional(readOnly = true)
    public List<FingerprintEnrollResponse> listEnrollments() {
        return fingerprintRepository.findAllWithMember().stream()
                .map(f -> new FingerprintEnrollResponse(
                        f.getMember().getId(),
                        f.getMember().getFirstName() + " " + f.getMember().getLastName(),
                        f.getFingerprintUserId(),
                        f.getDeviceLabel(),
                        f.getEnrolledAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccessLogResponse> recentLogs() {
        return accessLogRepository.findRecent().stream().map(this::toLogResponse).toList();
    }

    @Transactional
    public AccessVerifyResponse manualOpen(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + memberId));
        String fpId = fingerprintRepository
                .findByMemberId(memberId)
                .map(MemberFingerprint::getFingerprintUserId)
                .orElse("MANUAL-" + memberId);
        MemberFingerprint fp = fingerprintRepository.findByMemberId(memberId).orElse(null);
        return evaluateMember(fp, fpId, member, true);
    }

    private AccessVerifyResponse evaluateMember(MemberFingerprint fp, String fpId) {
        return evaluateMember(fp, fpId, fp.getMember(), false);
    }

    private AccessVerifyResponse evaluateMember(
            MemberFingerprint fp, String fpId, Member member, boolean manual) {
        MembershipStatus status = MemberMembershipRules.effectiveStatus(member);
        if (status != MembershipStatus.ACTIVE) {
            String message = status == MembershipStatus.SUSPENDED
                    ? "Membresía suspendida. Acércate a recepción."
                    : "Membresía vencida. Renueva tu plan para ingresar.";
            return deny(fpId, member, message);
        }
        LocalDate today = LocalDate.now();
        if (member.getMembershipStart() != null && member.getMembershipStart().isAfter(today)) {
            return deny(fpId, member, "Tu membresía aún no ha iniciado.");
        }

        String name = member.getFirstName() + " " + member.getLastName();
        boolean opened = turnstileGatewayService.openGate(name, member.getId());
        String msg = manual ? "Apertura manual autorizada" : "¡Bienvenido/a, " + member.getFirstName() + "!";
        saveLog(fpId, member, AccessResult.GRANTED, msg, opened);
        return new AccessVerifyResponse(AccessResult.GRANTED, opened, msg, member.getId(), name, fpId);
    }

    private AccessVerifyResponse deny(String fpId, Member member, String message) {
        saveLog(fpId, member, AccessResult.DENIED, message, false);
        return new AccessVerifyResponse(
                AccessResult.DENIED,
                false,
                message,
                member != null ? member.getId() : null,
                member != null ? member.getFirstName() + " " + member.getLastName() : null,
                fpId);
    }

    private void saveLog(String fpId, Member member, AccessResult result, String message, boolean gateOpened) {
        accessLogRepository.save(AccessLog.builder()
                .fingerprintUserId(fpId)
                .member(member)
                .result(result)
                .message(message)
                .gateOpened(gateOpened)
                .build());
    }

    private AccessLogResponse toLogResponse(AccessLog log) {
        String memberName = log.getMember() != null
                ? log.getMember().getFirstName() + " " + log.getMember().getLastName()
                : null;
        return new AccessLogResponse(
                log.getId(),
                log.getFingerprintUserId(),
                log.getMember() != null ? log.getMember().getId() : null,
                memberName,
                log.getResult(),
                log.getResult() == AccessResult.GRANTED ? "Ingreso permitido" : "Ingreso denegado",
                log.getMessage(),
                log.isGateOpened(),
                log.getCreatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
