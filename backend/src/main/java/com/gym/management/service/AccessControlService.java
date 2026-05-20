package com.gym.management.service;

import com.gym.management.dto.AccessLogResponse;
import com.gym.management.dto.AccessVerifyRequest;
import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.BiometricEnrollRequest;
import com.gym.management.dto.BiometricEnrollResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.AccessLog;
import com.gym.management.model.AccessPersonType;
import com.gym.management.model.AccessResult;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Employee;
import com.gym.management.model.EmployeeBiometricCredential;
import com.gym.management.model.Member;
import com.gym.management.model.MemberBiometricCredential;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.AccessLogRepository;
import com.gym.management.repository.EmployeeBiometricCredentialRepository;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.repository.MemberBiometricCredentialRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.util.WelcomeMessageUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final MemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;
    private final MemberBiometricCredentialRepository credentialRepository;
    private final EmployeeBiometricCredentialRepository employeeCredentialRepository;
    private final AccessLogRepository accessLogRepository;
    private final TurnstileGatewayService turnstileGatewayService;

    @Transactional
    public AccessVerifyResponse verifyAndOpen(AccessVerifyRequest request) {
        BiometricCredentialType type = request.credentialType();
        String deviceUserId = request.deviceUserId().trim();

        var memberCred = credentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId);
        if (memberCred.isPresent()) {
            return evaluateMember(memberCred.get(), deviceUserId, type, false);
        }

        var staffCred = employeeCredentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId);
        if (staffCred.isPresent()) {
            return evaluateStaff(staffCred.get(), deviceUserId, type, false);
        }

        return deny(deviceUserId, type, null, null, notRegisteredMessage(type));
    }

    @Transactional
    public BiometricEnrollResponse enroll(BiometricEnrollRequest request) {
        BiometricCredentialType type = request.credentialType();
        if (type == BiometricCredentialType.FACE) {
            throw new BusinessException(
                    "El rostro se registra con el lector biométrico (cámara en recepción), no con ID de terminal.");
        }
        String deviceUserId = request.deviceUserId().trim();

        if (request.employeeId() != null) {
            return enrollStaffFingerprint(request.employeeId(), deviceUserId, request.deviceLabel());
        }
        if (request.memberId() == null) {
            throw new BusinessException("Indica el afiliado o el entrenador a registrar.");
        }
        return enrollMemberFingerprint(request.memberId(), deviceUserId, type, request.deviceLabel());
    }

    private BiometricEnrollResponse enrollMemberFingerprint(
            Long memberId, String deviceUserId, BiometricCredentialType type, String deviceLabel) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + memberId));

        credentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId).ifPresent(existing -> {
            if (!existing.getMember().getId().equals(member.getId())) {
                throw new BusinessException(alreadyAssignedMessage(type));
            }
        });
        employeeCredentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId).ifPresent(existing -> {
            throw new BusinessException("Ese ID de huella ya está asignado a un entrenador.");
        });

        MemberBiometricCredential credential = credentialRepository
                .findByMemberIdAndCredentialType(member.getId(), type)
                .orElse(MemberBiometricCredential.builder()
                        .member(member)
                        .credentialType(type)
                        .build());

        credential.setDeviceUserId(deviceUserId);
        credential.setDeviceLabel(blankToNull(deviceLabel));
        credential = credentialRepository.save(credential);
        return toMemberEnrollResponse(credential);
    }

    private BiometricEnrollResponse enrollStaffFingerprint(Long employeeId, String deviceUserId, String deviceLabel) {
        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado: " + employeeId));

        employeeCredentialRepository
                .findByCredentialTypeAndDeviceUserId(BiometricCredentialType.FINGERPRINT, deviceUserId)
                .ifPresent(existing -> {
                    if (!existing.getEmployee().getId().equals(employee.getId())) {
                        throw new BusinessException("Ese ID de huella ya está asignado a otro entrenador.");
                    }
                });
        credentialRepository
                .findByCredentialTypeAndDeviceUserId(BiometricCredentialType.FINGERPRINT, deviceUserId)
                .ifPresent(existing -> {
                    throw new BusinessException("Ese ID de huella ya está asignado a un afiliado.");
                });

        EmployeeBiometricCredential credential = employeeCredentialRepository
                .findByEmployeeIdAndCredentialType(employee.getId(), BiometricCredentialType.FINGERPRINT)
                .orElse(EmployeeBiometricCredential.builder()
                        .employee(employee)
                        .credentialType(BiometricCredentialType.FINGERPRINT)
                        .build());

        credential.setDeviceUserId(deviceUserId);
        credential.setDeviceLabel(blankToNull(deviceLabel));
        credential = employeeCredentialRepository.save(credential);
        return toStaffEnrollResponse(credential);
    }

    @Transactional
    public void removeEnrollment(Long memberId, BiometricCredentialType credentialType) {
        MemberBiometricCredential credential = credentialRepository
                .findByMemberIdAndCredentialType(memberId, credentialType)
                .orElseThrow(() -> new ResourceNotFoundException(notEnrolledMessage(credentialType, false)));
        credentialRepository.delete(credential);
    }

    @Transactional
    public void removeStaffEnrollment(Long employeeId, BiometricCredentialType credentialType) {
        EmployeeBiometricCredential credential = employeeCredentialRepository
                .findByEmployeeIdAndCredentialType(employeeId, credentialType)
                .orElseThrow(() -> new ResourceNotFoundException(notEnrolledMessage(credentialType, true)));
        employeeCredentialRepository.delete(credential);
    }

    @Transactional(readOnly = true)
    public List<BiometricEnrollResponse> listEnrollments() {
        List<BiometricEnrollResponse> all = new ArrayList<>();
        credentialRepository.findAllWithMember().stream()
                .map(this::toMemberEnrollResponse)
                .forEach(all::add);
        employeeCredentialRepository.findAllWithActiveEmployee().stream()
                .map(this::toStaffEnrollResponse)
                .forEach(all::add);
        return all;
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

    @Transactional
    public AccessVerifyResponse processMemberAccess(
            Member member, String deviceUserId, BiometricCredentialType type, boolean manual) {
        return evaluateMember(null, deviceUserId, type, member, manual);
    }

    @Transactional
    public AccessVerifyResponse processStaffAccess(
            Employee employee, String deviceUserId, BiometricCredentialType type, boolean manual) {
        return evaluateStaff(null, deviceUserId, type, employee, manual);
    }

    @Transactional
    public AccessVerifyResponse denyWebcamAccess(String deviceUserId, String message) {
        return deny(deviceUserId, BiometricCredentialType.FACE, null, null, message);
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
        if (MembershipFreezeService.isFrozen(member)) {
            int days = member.getFrozenRemainingDays() != null ? member.getFrozenRemainingDays() : 0;
            return deny(
                    deviceUserId,
                    type,
                    member,
                    null,
                    "Membresía congelada. Tienes "
                            + days
                            + " día"
                            + (days == 1 ? "" : "s")
                            + " guardados. Descongela en recepción para ingresar.");
        }

        MembershipStatus status = MemberMembershipRules.effectiveStatus(member);
        if (status != MembershipStatus.ACTIVE) {
            String message = status == MembershipStatus.SUSPENDED
                    ? "Membresía suspendida. Acércate a recepción."
                    : "Membresía vencida. Renueva tu plan para ingresar.";
            return deny(deviceUserId, type, member, null, message);
        }
        LocalDate today = LocalDate.now(GYM_ZONE);
        if (member.getMembershipStart() != null && member.getMembershipStart().isAfter(today)) {
            return deny(deviceUserId, type, member, null, "Tu membresía aún no ha iniciado.");
        }

        var tiqueteraDeny = TicketBookAccessRules.denyReasonIfMonthlyLimitReached(
                member, accessLogRepository, GYM_ZONE);
        if (tiqueteraDeny.isPresent()) {
            return deny(deviceUserId, type, member, null, tiqueteraDeny.get());
        }

        MembershipPlan plan = member.getPlan();
        if (!manual && alreadyEnteredToday(member)) {
            return deny(
                    deviceUserId,
                    type,
                    member,
                    null,
                    "Ya ingresaste hoy. Solo se permite un ingreso por día.");
        }

        String welcomeMessage = manual
                ? "Apertura manual autorizada"
                : buildMemberWelcomeMessage(member, plan);

        return grantAccess(
                deviceUserId,
                type,
                member.getFirstName() + " " + member.getLastName(),
                member.getId(),
                null,
                AccessPersonType.MEMBER,
                member.getId(),
                welcomeMessage,
                member,
                null);
    }

    private String buildMemberWelcomeMessage(Member member, MembershipPlan plan) {
        String base = WelcomeMessageUtils.welcomeWithFirstName(member.getGender(), member.getFirstName());
        if (!TicketBookAccessRules.isTiqueteraPlan(plan)) {
            return base;
        }
        int remainingAfter =
                TicketBookAccessRules.remainingEntries(member, accessLogRepository, GYM_ZONE) - 1;
        if (remainingAfter < 0) {
            return base;
        }
        if (remainingAfter == 0) {
            return base + " Este era tu último entreno de la tiquetera.";
        }
        return base
                + " Te quedan "
                + remainingAfter
                + " entreno"
                + (remainingAfter == 1 ? "" : "s")
                + " en tu tiquetera.";
    }

    private AccessVerifyResponse evaluateStaff(
            EmployeeBiometricCredential cred, String deviceUserId, BiometricCredentialType type, boolean manual) {
        return evaluateStaff(cred, deviceUserId, type, cred.getEmployee(), manual);
    }

    private AccessVerifyResponse evaluateStaff(
            EmployeeBiometricCredential cred,
            String deviceUserId,
            BiometricCredentialType type,
            Employee employee,
            boolean manual) {
        if (!Boolean.TRUE.equals(employee.getActive())) {
            return deny(deviceUserId, type, null, employee, "Entrenador inactivo. Acércate a administración.");
        }

        return grantAccess(
                deviceUserId,
                type,
                employee.getFirstName() + " " + employee.getLastName(),
                null,
                employee.getId(),
                AccessPersonType.STAFF,
                employee.getId(),
                manual
                        ? "Apertura manual autorizada"
                        : WelcomeMessageUtils.welcomeWithFirstName(null, employee.getFirstName()),
                null,
                employee);
    }

    private AccessVerifyResponse grantAccess(
            String deviceUserId,
            BiometricCredentialType type,
            String fullName,
            Long memberId,
            Long employeeId,
            AccessPersonType personType,
            Long gatePersonId,
            String message,
            Member member,
            Employee employee) {
        boolean opened = turnstileGatewayService.openGate(fullName, gatePersonId);
        if (employee != null) {
            saveStaffLog(deviceUserId, type, employee, AccessResult.GRANTED, message, opened);
        } else {
            saveMemberLog(deviceUserId, type, member, AccessResult.GRANTED, message, opened);
        }
        return new AccessVerifyResponse(
                AccessResult.GRANTED,
                opened,
                message,
                memberId,
                employeeId,
                personType,
                fullName,
                deviceUserId,
                type,
                member != null ? member.getGender() : null);
    }

    private AccessVerifyResponse deny(
            String deviceUserId,
            BiometricCredentialType type,
            Member member,
            Employee employee,
            String message) {
        if (employee != null) {
            saveStaffLog(deviceUserId, type, employee, AccessResult.DENIED, message, false);
        } else {
            saveMemberLog(deviceUserId, type, member, AccessResult.DENIED, message, false);
        }
        String displayName = member != null
                ? member.getFirstName() + " " + member.getLastName()
                : employee != null ? employee.getFirstName() + " " + employee.getLastName() : null;
        return new AccessVerifyResponse(
                AccessResult.DENIED,
                false,
                message,
                member != null ? member.getId() : null,
                employee != null ? employee.getId() : null,
                employee != null ? AccessPersonType.STAFF : AccessPersonType.MEMBER,
                displayName,
                deviceUserId,
                type,
                member != null ? member.getGender() : null);
    }

    private void saveMemberLog(
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

    private void saveStaffLog(
            String deviceUserId,
            BiometricCredentialType type,
            Employee employee,
            AccessResult result,
            String message,
            boolean gateOpened) {
        accessLogRepository.save(AccessLog.builder()
                .fingerprintUserId(deviceUserId)
                .credentialType(type)
                .employee(employee)
                .result(result)
                .message(message)
                .gateOpened(gateOpened)
                .build());
    }

    private BiometricEnrollResponse toMemberEnrollResponse(MemberBiometricCredential credential) {
        Member member = credential.getMember();
        return new BiometricEnrollResponse(
                member.getId(),
                null,
                AccessPersonType.MEMBER,
                member.getFirstName() + " " + member.getLastName(),
                credential.getDeviceUserId(),
                credential.getCredentialType(),
                credential.getCredentialType().displayLabel(),
                credential.getDeviceLabel(),
                credential.getEnrolledAt());
    }

    private BiometricEnrollResponse toStaffEnrollResponse(EmployeeBiometricCredential credential) {
        Employee employee = credential.getEmployee();
        return new BiometricEnrollResponse(
                null,
                employee.getId(),
                AccessPersonType.STAFF,
                employee.getFirstName() + " " + employee.getLastName(),
                credential.getDeviceUserId(),
                credential.getCredentialType(),
                credential.getCredentialType().displayLabel(),
                credential.getDeviceLabel(),
                credential.getEnrolledAt());
    }

    private AccessLogResponse toLogResponse(AccessLog log) {
        String personName = null;
        if (log.getMember() != null) {
            personName = log.getMember().getFirstName() + " " + log.getMember().getLastName();
        } else if (log.getEmployee() != null) {
            personName = log.getEmployee().getFirstName() + " " + log.getEmployee().getLastName();
        }
        BiometricCredentialType type = log.getCredentialType() != null
                ? log.getCredentialType()
                : BiometricCredentialType.FINGERPRINT;
        return new AccessLogResponse(
                log.getId(),
                log.getFingerprintUserId(),
                type,
                type.displayLabel(),
                log.getMember() != null ? log.getMember().getId() : null,
                personName,
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

    private static String notEnrolledMessage(BiometricCredentialType type, boolean staff) {
        if (staff) {
            return switch (type) {
                case FINGERPRINT -> "Este entrenador no tiene huella registrada";
                case FACE -> "Este entrenador no tiene rostro registrado";
            };
        }
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
