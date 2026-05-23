package com.gym.management.service;

import com.gym.management.dto.AccessLogResponse;
import com.gym.management.dto.AccessVerifyRequest;
import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.AccessVoiceHints;
import com.gym.management.dto.BiometricEnrollRequest;
import com.gym.management.dto.BiometricEnrollResponse;
import com.gym.management.dto.CardCredentialMigrationResponse;
import com.gym.management.dto.KioskAccessEventResponse;
import com.gym.management.dto.LastDeviceReadResponse;
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
import com.gym.management.security.SecurityUtils;
import com.gym.management.util.CardCredentialKeys;
import com.gym.management.util.WelcomeMessageUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        if (type == BiometricCredentialType.FACE) {
            return deny(deviceUserId, type, null, null, notRegisteredMessage(type));
        }

        for (BiometricCredentialType candidate : lookupOrder(type)) {
            Optional<AccessVerifyResponse> match = lookupAndVerify(deviceUserId, candidate);
            if (match.isPresent()) {
                return match.get();
            }
        }

        Optional<AccessVerifyResponse> byDocument = lookupByDocumentAndVerify(deviceUserId, type);
        if (byDocument.isPresent()) {
            return byDocument.get();
        }

        return deny(deviceUserId, type, null, null, notRegisteredMessage(type));
    }

    /**
     * Llamado por terminal ZKTeco al leer tarjeta, huella o PIN (mismo campo Pin). Si el Pin es la cédula
     * del afiliado y está activo, permite ingreso aunque no tenga credencial biométrica vinculada en el panel.
     */
    @Transactional
    public AccessVerifyResponse verifyZktEvent(String pin) {
        return verifyAndOpen(new AccessVerifyRequest(pin.trim(), BiometricCredentialType.CARD));
    }

    private Optional<AccessVerifyResponse> lookupAndVerify(String deviceUserId, BiometricCredentialType type) {
        if (type == BiometricCredentialType.CARD) {
            return lookupCardByReaderPin(deviceUserId);
        }
        var memberCred = credentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId);
        if (memberCred.isPresent()) {
            return Optional.of(evaluateMember(memberCred.get(), deviceUserId, type, false));
        }
        var staffCred = employeeCredentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId);
        if (staffCred.isPresent()) {
            return Optional.of(evaluateStaff(staffCred.get(), deviceUserId, type, false));
        }
        return Optional.empty();
    }

    private Optional<AccessVerifyResponse> lookupCardByReaderPin(String readerPin) {
        String pin = CardCredentialKeys.normalizeCardPin(readerPin);
        if (pin.isEmpty()) {
            return Optional.empty();
        }
        List<MemberBiometricCredential> memberMatches = credentialRepository.findMemberCardsByReaderPin(pin);
        if (memberMatches.size() > 1) {
            return Optional.of(deny(
                    pin,
                    BiometricCredentialType.CARD,
                    null,
                    null,
                    "Varias personas comparten esta tarjeta. Identifíquese en recepción."));
        }
        if (memberMatches.size() == 1) {
            MemberBiometricCredential cred = memberMatches.get(0);
            return Optional.of(evaluateMember(cred, cred.getDeviceUserId(), BiometricCredentialType.CARD, false));
        }
        List<EmployeeBiometricCredential> staffMatches = employeeCredentialRepository.findStaffCardsByReaderPin(pin);
        if (staffMatches.size() > 1) {
            return Optional.of(deny(
                    pin,
                    BiometricCredentialType.CARD,
                    null,
                    null,
                    "Varias tarjetas de personal coinciden. Use recepción."));
        }
        if (staffMatches.size() == 1) {
            EmployeeBiometricCredential cred = staffMatches.get(0);
            return Optional.of(evaluateStaff(cred, cred.getDeviceUserId(), BiometricCredentialType.CARD, false));
        }
        return Optional.empty();
    }

    /** Huella/tarjeta en ZKT con ID = cédula: busca afiliado por documento si está activo. */
    private Optional<AccessVerifyResponse> lookupByDocumentAndVerify(
            String pin, BiometricCredentialType type) {
        String key = pin.trim();
        if (key.isBlank()) {
            return Optional.empty();
        }

        Optional<Member> member = memberRepository.findByDocumentId(key);
        if (member.isEmpty()) {
            String digits = digitsOnly(key);
            if (digits.length() >= 5) {
                member = memberRepository.findByDocumentId(digits);
                if (member.isEmpty()) {
                    member = memberRepository.findByDocumentDigitsOnly(digits);
                }
            }
        }

        return member.map(m -> evaluateMember(null, key, type, m, false));
    }

    private static String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    private static List<BiometricCredentialType> lookupOrder(BiometricCredentialType requested) {
        if (requested == BiometricCredentialType.CARD) {
            return List.of(BiometricCredentialType.CARD, BiometricCredentialType.FINGERPRINT);
        }
        return List.of(BiometricCredentialType.FINGERPRINT, BiometricCredentialType.CARD);
    }

    @Transactional
    public BiometricEnrollResponse enroll(BiometricEnrollRequest request) {
        BiometricCredentialType type = request.credentialType();
        if (type == BiometricCredentialType.FACE) {
            throw new BusinessException(
                    "El rostro se registra con el lector biométrico (cámara en recepción), no con ID de terminal.");
        }
        String deviceUserId = request.deviceUserId().trim();
        if (type == BiometricCredentialType.CARD) {
            deviceUserId = CardCredentialKeys.normalizeCardPin(deviceUserId);
        }

        if (request.employeeId() != null) {
            return enrollStaffDeviceCredential(request.employeeId(), deviceUserId, type, request.deviceLabel());
        }
        if (request.memberId() == null) {
            throw new BusinessException("Indica el afiliado o el entrenador a registrar.");
        }
        return enrollMemberDeviceCredential(request.memberId(), deviceUserId, type, request.deviceLabel());
    }

    private BiometricEnrollResponse enrollMemberDeviceCredential(
            Long memberId, String deviceUserId, BiometricCredentialType type, String deviceLabel) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + memberId));

        if (type == BiometricCredentialType.CARD) {
            deviceUserId = CardCredentialKeys.composeMemberCard(deviceUserId, member.getDocumentId());
        }

        credentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId).ifPresent(existing -> {
            if (!existing.getMember().getId().equals(member.getId())) {
                throw new BusinessException(alreadyAssignedMessage(type));
            }
        });
        employeeCredentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId).ifPresent(existing -> {
            throw new BusinessException(staffAlreadyAssignedMessage(type));
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

    private BiometricEnrollResponse enrollStaffDeviceCredential(
            Long employeeId, String deviceUserId, BiometricCredentialType type, String deviceLabel) {
        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado: " + employeeId));

        if (type == BiometricCredentialType.CARD) {
            deviceUserId = CardCredentialKeys.composeStaffCard(deviceUserId, employeeId);
        }

        employeeCredentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId).ifPresent(existing -> {
            if (!existing.getEmployee().getId().equals(employee.getId())) {
                throw new BusinessException(staffAlreadyAssignedMessage(type));
            }
        });
        credentialRepository.findByCredentialTypeAndDeviceUserId(type, deviceUserId).ifPresent(existing -> {
            throw new BusinessException(memberAlreadyAssignedMessage(type));
        });

        EmployeeBiometricCredential credential = employeeCredentialRepository
                .findByEmployeeIdAndCredentialType(employee.getId(), type)
                .orElse(EmployeeBiometricCredential.builder()
                        .employee(employee)
                        .credentialType(type)
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

    /**
     * Afiliados/entrenadores con tarjeta antigua (solo código de lector): guarda {@code codigo|cedula} o {@code codigo|Eid}.
     */
    @Transactional
    public CardCredentialMigrationResponse migrateCardCredentialsToDocumentSuffix() {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException("Solo administración puede actualizar las tarjetas registradas");
        }
        int membersUpdated = 0;
        int membersSkipped = 0;
        int staffUpdated = 0;
        int staffSkipped = 0;

        for (MemberBiometricCredential credential : credentialRepository.findAllWithMember()) {
            if (credential.getCredentialType() != BiometricCredentialType.CARD) {
                continue;
            }
            Member member = credential.getMember();
            String documentId = member.getDocumentId();
            if (documentId == null || documentId.isBlank()) {
                membersSkipped++;
                continue;
            }
            String composite = CardCredentialKeys.composeMemberCard(credential.getDeviceUserId(), documentId);
            if (composite.equals(credential.getDeviceUserId())) {
                membersSkipped++;
                continue;
            }
            var conflict = credentialRepository.findByCredentialTypeAndDeviceUserId(
                    BiometricCredentialType.CARD, composite);
            if (conflict.isPresent() && !conflict.get().getMember().getId().equals(member.getId())) {
                membersSkipped++;
                continue;
            }
            credential.setDeviceUserId(composite);
            credentialRepository.save(credential);
            membersUpdated++;
        }

        for (EmployeeBiometricCredential credential : employeeCredentialRepository.findAllWithActiveEmployee()) {
            if (credential.getCredentialType() != BiometricCredentialType.CARD) {
                continue;
            }
            Employee employee = credential.getEmployee();
            String composite = CardCredentialKeys.composeStaffCard(credential.getDeviceUserId(), employee.getId());
            if (composite.equals(credential.getDeviceUserId())) {
                staffSkipped++;
                continue;
            }
            var conflict = employeeCredentialRepository.findByCredentialTypeAndDeviceUserId(
                    BiometricCredentialType.CARD, composite);
            if (conflict.isPresent()
                    && !conflict.get().getEmployee().getId().equals(employee.getId())) {
                staffSkipped++;
                continue;
            }
            credential.setDeviceUserId(composite);
            employeeCredentialRepository.save(credential);
            staffUpdated++;
        }

        String message =
                "Actualizados: "
                        + membersUpdated
                        + " afiliado(s), "
                        + staffUpdated
                        + " entrenador(es). Omitidos: "
                        + membersSkipped
                        + " afiliado(s), "
                        + staffSkipped
                        + " entrenador(es) (sin cédula, ya actualizados o clave en uso).";
        return new CardCredentialMigrationResponse(
                membersUpdated, membersSkipped, staffUpdated, staffSkipped, message);
    }

    @Transactional(readOnly = true)
    public List<BiometricEnrollResponse> listEnrollments() {
        List<BiometricEnrollResponse> all = new ArrayList<>();
        credentialRepository.findAllWithMember().stream()
                .map(this::toMemberEnrollResponse)
                .forEach(all::add);
        employeeCredentialRepository.findAllWithActiveEmployee().stream()
                .filter(c -> EmployeeVisibility.visibleInTeamDirectory(c.getEmployee()))
                .map(this::toStaffEnrollResponse)
                .forEach(all::add);
        return all;
    }

    @Transactional(readOnly = true)
    public List<AccessLogResponse> recentLogs() {
        return accessLogRepository.findRecent().stream()
                .limit(400)
                .map(this::toLogResponse)
                .toList();
    }

    /** Eventos nuevos para la pantalla /acceso (tras lectura en ZKTeco). */
    /** Última lectura del lector ZKT (para copiar Pin/tarjeta al vincular afiliado). */
    @Transactional(readOnly = true)
    public Optional<LastDeviceReadResponse> lastDeviceReadSince(Instant since) {
        Instant from = since != null ? since : Instant.now().minus(30, ChronoUnit.MINUTES);
        return accessLogRepository.findFirstByCreatedAtAfterOrderByIdDesc(from).map(this::toLastDeviceRead);
    }

    private LastDeviceReadResponse toLastDeviceRead(AccessLog log) {
        BiometricCredentialType type = log.getCredentialType() != null
                ? log.getCredentialType()
                : BiometricCredentialType.FINGERPRINT;
        return new LastDeviceReadResponse(
                log.getId(),
                log.getFingerprintUserId(),
                type,
                type.displayLabel(),
                log.getResult(),
                log.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<KioskAccessEventResponse> kioskEventsSince(Instant since, Long afterId) {
        if (afterId != null && afterId > 0) {
            return accessLogRepository.findAfterId(afterId).stream().map(this::toKioskEvent).toList();
        }
        Instant from = since != null ? since : Instant.now().minusSeconds(2);
        return accessLogRepository.findSince(from).stream().map(this::toKioskEvent).toList();
    }

    @Transactional
    public void clearAllLogs() {
        accessLogRepository.deleteAllInBatch();
    }

    /**
     * Pantalla /acceso o app de escritorio: F2 entreno del día, F3 bailes deportivos.
     * Abre el torniquete vía {@code TURNSTILE_WEBHOOK} sin afiliado (pase invitado en entrada).
     */
    @Transactional
    public AccessVerifyResponse kioskOpenGate(String reason) {
        boolean sports = "sports-dance".equalsIgnoreCase(reason);
        String deviceUserId = sports ? "F3-BAILES" : "F2-ENTRENO";
        String label = sports ? "Bailes deportivos (F3)" : "Entreno del día (F2)";
        String message =
                sports ? "Torniquete: bailes deportivos (F3)" : "Torniquete: entreno del día (F2)";
        boolean opened = turnstileGatewayService.openGate(label, null);
        Long logId =
                saveMemberLog(deviceUserId, BiometricCredentialType.CARD, null, AccessResult.GRANTED, message, opened);
        String resultMessage =
                opened ? "Puerta abierta — " + label : "No se pudo abrir el torniquete. Revise TURNSTILE_WEBHOOK.";
        return new AccessVerifyResponse(
                opened ? AccessResult.GRANTED : AccessResult.DENIED,
                opened,
                resultMessage,
                null,
                null,
                AccessPersonType.MEMBER,
                label,
                deviceUserId,
                BiometricCredentialType.CARD,
                null,
                null,
                logId,
                null,
                null,
                null);
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
        String firstName =
                WelcomeMessageUtils.resolveFirstName(member.getFirstName(), member.getLastName());
        String base = WelcomeMessageUtils.welcomeWithFirstName(member.getGender(), firstName);
        AccessVoiceHints hints =
                AccessVoiceHintsBuilder.forMember(member, accessLogRepository, GYM_ZONE, false);
        StringBuilder msg = new StringBuilder(base);
        if (hints.membershipDaysRemaining() != null) {
            int days = hints.membershipDaysRemaining();
            msg.append(" Te quedan ")
                    .append(days)
                    .append(" día")
                    .append(days == 1 ? "" : "s")
                    .append(" de entreno antes de que venza tu membresía.");
        }
        if (hints.tiqueteraPlan() && hints.tiqueteraEntriesRemainingAfter() != null) {
            int left = hints.tiqueteraEntriesRemainingAfter();
            if (left < AccessVoiceHints.TIQUETERA_LOW_ENTRIES_THRESHOLD) {
                if (left == 0) {
                    msg.append(" Este era tu último entreno de la tiquetera.");
                } else {
                    msg.append(" Te quedan ")
                            .append(left)
                            .append(" entreno")
                            .append(left == 1 ? "" : "s")
                            .append(" en tu tiquetera.");
                }
            }
        }
        return msg.toString();
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
                        : WelcomeMessageUtils.staffWelcomeMessage(null),
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
        Long logId;
        if (employee != null) {
            logId = saveStaffLog(deviceUserId, type, employee, AccessResult.GRANTED, message, opened);
        } else {
            logId = saveMemberLog(deviceUserId, type, member, AccessResult.GRANTED, message, opened);
        }
        AccessVoiceHints voiceHints = employee != null
                ? AccessVoiceHints.none()
                : AccessVoiceHintsBuilder.forMember(member, accessLogRepository, GYM_ZONE, true);
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
                member != null ? member.getGender() : null,
                memberDocumentId(member),
                logId,
                voiceHints.membershipDaysRemaining(),
                voiceHints.tiqueteraEntriesRemainingAfter(),
                voiceHints.tiqueteraPlan() ? Boolean.TRUE : null);
    }

    private AccessVerifyResponse deny(
            String deviceUserId,
            BiometricCredentialType type,
            Member member,
            Employee employee,
            String message) {
        Long logId;
        if (employee != null) {
            logId = saveStaffLog(deviceUserId, type, employee, AccessResult.DENIED, message, false);
        } else {
            logId = saveMemberLog(deviceUserId, type, member, AccessResult.DENIED, message, false);
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
                member != null ? member.getGender() : null,
                memberDocumentId(member),
                logId,
                null,
                null,
                null);
    }

    private Long saveMemberLog(
            String deviceUserId,
            BiometricCredentialType type,
            Member member,
            AccessResult result,
            String message,
            boolean gateOpened) {
        AccessLog log = accessLogRepository.save(AccessLog.builder()
                .fingerprintUserId(deviceUserId)
                .credentialType(type)
                .member(member)
                .result(result)
                .message(message)
                .gateOpened(gateOpened)
                .build());
        return log.getId();
    }

    private Long saveStaffLog(
            String deviceUserId,
            BiometricCredentialType type,
            Employee employee,
            AccessResult result,
            String message,
            boolean gateOpened) {
        AccessLog log = accessLogRepository.save(AccessLog.builder()
                .fingerprintUserId(deviceUserId)
                .credentialType(type)
                .employee(employee)
                .result(result)
                .message(message)
                .gateOpened(gateOpened)
                .build());
        return log.getId();
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

    private KioskAccessEventResponse toKioskEvent(AccessLog log) {
        String personName = null;
        com.gym.management.model.Gender gender = null;
        Long memberId = null;
        Long employeeId = null;
        AccessPersonType personType = AccessPersonType.MEMBER;
        if (log.getMember() != null) {
            personName = log.getMember().getFirstName() + " " + log.getMember().getLastName();
            gender = log.getMember().getGender();
            memberId = log.getMember().getId();
            personType = AccessPersonType.MEMBER;
        } else if (log.getEmployee() != null) {
            personName = log.getEmployee().getFirstName() + " " + log.getEmployee().getLastName();
            employeeId = log.getEmployee().getId();
            personType = AccessPersonType.STAFF;
        }
        BiometricCredentialType type = log.getCredentialType() != null
                ? log.getCredentialType()
                : BiometricCredentialType.FINGERPRINT;
        String documentId = log.getMember() != null ? log.getMember().getDocumentId() : null;
        AccessVoiceHints voiceHints = log.getResult() == AccessResult.GRANTED && log.getMember() != null
                ? AccessVoiceHintsBuilder.forMember(log.getMember(), accessLogRepository, GYM_ZONE, true)
                : AccessVoiceHints.none();
        return new KioskAccessEventResponse(
                log.getId(),
                log.getFingerprintUserId(),
                type,
                type.displayLabel(),
                memberId,
                employeeId,
                personType,
                personName,
                log.getResult(),
                log.getMessage(),
                log.isGateOpened(),
                log.getCreatedAt(),
                gender,
                documentId,
                voiceHints.membershipDaysRemaining(),
                voiceHints.tiqueteraEntriesRemainingAfter(),
                voiceHints.tiqueteraPlan() ? Boolean.TRUE : null);
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
            case FINGERPRINT -> "Huella o cédula no reconocida en el sistema";
            case CARD -> "Tarjeta o cédula no reconocida en el sistema";
            case FACE -> "Rostro no registrado en el sistema";
        };
    }

    private static String notEnrolledMessage(BiometricCredentialType type, boolean staff) {
        if (staff) {
            return switch (type) {
                case FINGERPRINT -> "Este entrenador no tiene huella registrada";
                case CARD -> "Este entrenador no tiene tarjeta registrada";
                case FACE -> "Este entrenador no tiene rostro registrado";
            };
        }
        return switch (type) {
            case FINGERPRINT -> "Este afiliado no tiene huella registrada";
            case CARD -> "Este afiliado no tiene tarjeta registrada";
            case FACE -> "Este afiliado no tiene rostro registrado";
        };
    }

    private static String alreadyAssignedMessage(BiometricCredentialType type) {
        return memberAlreadyAssignedMessage(type);
    }

    private static String memberAlreadyAssignedMessage(BiometricCredentialType type) {
        return switch (type) {
            case FINGERPRINT -> "Ese ID de huella ya está asignado a otro afiliado";
            case CARD -> "Ese número de tarjeta ya está asignado a otro afiliado";
            case FACE -> "Ese ID de rostro ya está asignado a otro afiliado";
        };
    }

    private static String staffAlreadyAssignedMessage(BiometricCredentialType type) {
        return switch (type) {
            case FINGERPRINT -> "Ese ID de huella ya está asignado a otro entrenador";
            case CARD -> "Ese número de tarjeta ya está asignado a otro entrenador";
            case FACE -> "Ese ID de rostro ya está asignado a otro entrenador";
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

    private static String memberDocumentId(Member member) {
        if (member == null || member.getDocumentId() == null || member.getDocumentId().isBlank()) {
            return null;
        }
        return member.getDocumentId().trim();
    }
}
