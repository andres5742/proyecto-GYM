package com.gym.management.service;

import com.gym.management.dto.AccessLogResponse;
import com.gym.management.dto.AccessVerifyRequest;
import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.BiometricEnrollRequest;
import com.gym.management.dto.BiometricEnrollResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.AccessLog;
import com.gym.management.model.AccessResult;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Member;
import com.gym.management.model.MemberBiometricCredential;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.AccessLogRepository;
import com.gym.management.repository.MemberBiometricCredentialRepository;
import com.gym.management.repository.MemberRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final MemberRepository memberRepository;
    private final MemberBiometricCredentialRepository credentialRepository;
    private final AccessLogRepository accessLogRepository;
    private final TurnstileGatewayService turnstileGatewayService;

    @Transactional
    public AccessVerifyResponse verifyAndOpen(AccessVerifyRequest request) {
        BiometricCredentialType type = request.credentialType();
        String deviceUserId = request.deviceUserId().trim();
        return credentialRepository
                .findByCredentialTypeAndDeviceUserId(type, deviceUserId)
                .map(cred -> evaluateMember(cred, deviceUserId, type, false))
                .orElseGet(() -> deny(deviceUserId, type, null, notRegisteredMessage(type)));
    }

    @Transactional
    public BiometricEnrollResponse enroll(BiometricEnrollRequest request) {
        Member member = memberRepository
                .findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));

        BiometricCredentialType type = request.credentialType();
        if (type == BiometricCredentialType.FACE) {
            throw new BusinessException(
                    "El rostro se registra con el lector biométrico (cámara en recepción), no con ID de terminal.");
        }
        String deviceUserId = request.deviceUserId().trim();

        credentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId).ifPresent(existing -> {
            if (!existing.getMember().getId().equals(member.getId())) {
                throw new BusinessException(alreadyAssignedMessage(type));
            }
        });

        MemberBiometricCredential credential = credentialRepository
                .findByMemberIdAndCredentialType(member.getId(), type)
                .orElse(MemberBiometricCredential.builder()
                        .member(member)
                        .credentialType(type)
                        .build());

        credential.setDeviceUserId(deviceUserId);
        credential.setDeviceLabel(blankToNull(request.deviceLabel()));
        credential = credentialRepository.save(credential);

        return toEnrollResponse(credential);
    }

    @Transactional
    public void removeEnrollment(Long memberId, BiometricCredentialType credentialType) {
        MemberBiometricCredential credential = credentialRepository
                .findByMemberIdAndCredentialType(memberId, credentialType)
                .orElseThrow(() -> new ResourceNotFoundException(notEnrolledMessage(credentialType)));
        credentialRepository.delete(credential);
    }

    @Transactional(readOnly = true)
    public List<BiometricEnrollResponse> listEnrollments() {
        return credentialRepository.findAllWithMember().stream()
                .map(this::toEnrollResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccessLogResponse> recentLogs() {
        return accessLogRepository.findRecentGranted().stream().map(this::toLogResponse).toList();
    }

    @Transactional
    public void clearAllLogs() {
        accessLogRepository.deleteAllInBatch();
    }

    @Transactional
    public AccessVerifyResponse manualOpen(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + memberId));

        List<MemberBiometricCredential> credentials = credentialRepository.findByMemberId(memberId);
        MemberBiometricCredential cred = credentials.isEmpty() ? null : credentials.get(0);
        BiometricCredentialType type =
                cred != null ? cred.getCredentialType() : BiometricCredentialType.FINGERPRINT;
        String deviceUserId = cred != null ? cred.getDeviceUserId() : "MANUAL-" + memberId;

        return evaluateMember(cred, deviceUserId, type, member, true);
    }

    /** Acceso tras reconocimiento facial por webcam u otro identificador directo. */
    @Transactional
    public AccessVerifyResponse processMemberAccess(
            Member member, String deviceUserId, BiometricCredentialType type, boolean manual) {
        return evaluateMember(null, deviceUserId, type, member, manual);
    }

    @Transactional
    public AccessVerifyResponse denyWebcamAccess(String deviceUserId, String message) {
        return deny(deviceUserId, BiometricCredentialType.FACE, null, message);
    }

    private AccessVerifyResponse evaluateMember(
            MemberBiometricCredential cred, String deviceUserId, BiometricCredentialType type, boolean manual) {
        return evaluateMember(cred, deviceUserId, type, cred.getMember(), manual);
    }

    private AccessVerifyResponse evaluateMember(
            MemberBiometricCredential cred,
            String deviceUserId,
            BiometricCredentialType type,
            Member member,
            boolean manual) {
        MembershipStatus status = MemberMembershipRules.effectiveStatus(member);
        if (status != MembershipStatus.ACTIVE) {
            String message = status == MembershipStatus.SUSPENDED
                    ? "Membresía suspendida. Acércate a recepción."
                    : "Membresía vencida. Renueva tu plan para ingresar.";
            return deny(deviceUserId, type, member, message);
        }
        LocalDate today = LocalDate.now();
        if (member.getMembershipStart() != null && member.getMembershipStart().isAfter(today)) {
            return deny(deviceUserId, type, member, "Tu membresía aún no ha iniciado.");
        }

        if (!manual && alreadyEnteredToday(member)) {
            return deny(
                    deviceUserId,
                    type,
                    member,
                    "Ya ingresaste hoy. Solo se permite un ingreso por día.");
        }

        String name = member.getFirstName() + " " + member.getLastName();
        boolean opened = turnstileGatewayService.openGate(name, member.getId());
        String msg = manual
                ? "Apertura manual autorizada"
                : "¡Bienvenido/a, " + member.getFirstName() + "!";
        saveLog(deviceUserId, type, member, AccessResult.GRANTED, msg, opened);
        return new AccessVerifyResponse(AccessResult.GRANTED, opened, msg, member.getId(), name, deviceUserId, type);
    }

    private AccessVerifyResponse deny(
            String deviceUserId, BiometricCredentialType type, Member member, String message) {
        saveLog(deviceUserId, type, member, AccessResult.DENIED, message, false);
        return new AccessVerifyResponse(
                AccessResult.DENIED,
                false,
                message,
                member != null ? member.getId() : null,
                member != null ? member.getFirstName() + " " + member.getLastName() : null,
                deviceUserId,
                type);
    }

    private void saveLog(
            String deviceUserId,
            BiometricCredentialType type,
            Member member,
            AccessResult result,
            String message,
            boolean gateOpened) {
        accessLogRepository.save(AccessLog.builder()
                .fingerprintUserId(deviceUserId)
                .credentialType(type)
                .member(member)
                .result(result)
                .message(message)
                .gateOpened(gateOpened)
                .build());
    }

    private BiometricEnrollResponse toEnrollResponse(MemberBiometricCredential credential) {
        Member member = credential.getMember();
        return new BiometricEnrollResponse(
                member.getId(),
                member.getFirstName() + " " + member.getLastName(),
                credential.getDeviceUserId(),
                credential.getCredentialType(),
                credential.getCredentialType().displayLabel(),
                credential.getDeviceLabel(),
                credential.getEnrolledAt());
    }

    private AccessLogResponse toLogResponse(AccessLog log) {
        String memberName = log.getMember() != null
                ? log.getMember().getFirstName() + " " + log.getMember().getLastName()
                : null;
        BiometricCredentialType type = log.getCredentialType() != null
                ? log.getCredentialType()
                : BiometricCredentialType.FINGERPRINT;
        return new AccessLogResponse(
                log.getId(),
                log.getFingerprintUserId(),
                type,
                type.displayLabel(),
                log.getMember() != null ? log.getMember().getId() : null,
                memberName,
                log.getResult(),
                log.getResult() == AccessResult.GRANTED ? "Ingreso permitido" : "Ingreso denegado",
                log.getMessage(),
                log.isGateOpened(),
                log.getCreatedAt());
    }

    private static String notRegisteredMessage(BiometricCredentialType type) {
        return switch (type) {
            case FINGERPRINT -> "Huella no registrada en el sistema";
            case FACE -> "Rostro no registrado en el sistema";
        };
    }

    private static String notEnrolledMessage(BiometricCredentialType type) {
        return switch (type) {
            case FINGERPRINT -> "Este afiliado no tiene huella registrada";
            case FACE -> "Este afiliado no tiene rostro registrado";
        };
    }

    private static String alreadyAssignedMessage(BiometricCredentialType type) {
        return switch (type) {
            case FINGERPRINT -> "Ese ID de huella ya está asignado a otro afiliado";
            case FACE -> "Ese ID de rostro ya está asignado a otro afiliado";
        };
    }

    private boolean alreadyEnteredToday(Member member) {
        LocalDate today = LocalDate.now(GYM_ZONE);
        Instant from = today.atStartOfDay(GYM_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(GYM_ZONE).toInstant();
        return accessLogRepository.existsByMemberIdAndResultBetween(
                member.getId(), AccessResult.GRANTED, from, to);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
