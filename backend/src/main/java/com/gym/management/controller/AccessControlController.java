package com.gym.management.controller;

import com.gym.management.dto.AccessLogResponse;
import com.gym.management.dto.AccessVerifyRequest;
import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.BiometricEnrollRequest;
import com.gym.management.dto.BiometricEnrollResponse;
import com.gym.management.dto.CardCredentialMigrationResponse;
import com.gym.management.dto.CardSelectMemberRequest;
import com.gym.management.dto.KioskAccessEventResponse;
import com.gym.management.dto.KioskOpenGateRequest;
import com.gym.management.dto.LastDeviceReadResponse;
import com.gym.management.dto.ZktAccessEventRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.service.AccessControlService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessControlController {

    private final AccessControlService accessControlService;

    @Value("${app.access.device-api-key:gym-turnstile-dev-key}")
    private String deviceApiKey;

    /** Llamado por lector de huella, tarjeta o torniquete al identificar al afiliado. */
    @PostMapping("/verify")
    public AccessVerifyResponse verify(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @Valid @RequestBody AccessVerifyRequest request) {
        validateDeviceKey(deviceKey);
        return accessControlService.verifyAndOpen(request);
    }

    /**
     * Evento desde terminal ZKTeco (tarjeta, PIN o huella). Acepta JSON {@code {"pin":"..."}} o
     * parámetro de formulario {@code Pin=} como envía ADMS/Push. Si el Pin es la cédula del afiliado
     * activo, permite ingreso sin credencial vinculada en el panel.
     */
    @PostMapping("/zkt/event")
    public AccessVerifyResponse zktEvent(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @RequestParam(value = "Pin", required = false) String pinUpper,
            @RequestParam(value = "pin", required = false) String pinLower,
            @RequestBody(required = false) ZktAccessEventRequest body) {
        validateDeviceKey(deviceKey);
        String pin = body != null && body.pin() != null && !body.pin().isBlank()
                ? body.pin().trim()
                : pinUpper != null && !pinUpper.isBlank()
                        ? pinUpper.trim()
                        : pinLower != null ? pinLower.trim() : "";
        if (pin.isEmpty()) {
            throw new BusinessException("Falta Pin (número de tarjeta o usuario en el ZKTeco)");
        }
        return accessControlService.verifyZktEvent(pin);
    }

    /** Pantalla /acceso: el afiliado elige con teclado cuando varias personas comparten el código de tarjeta. */
    @PostMapping("/zkt/select-member")
    public AccessVerifyResponse zktSelectMember(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @Valid @RequestBody CardSelectMemberRequest body) {
        validateDeviceKey(deviceKey);
        return accessControlService.verifyCardMemberSelection(body.pin().trim(), body.memberId());
    }

    /** Pantalla /acceso: consulta ingresos recientes (tarjeta/huella vía ZKTeco). */
    /** Recepción: última tarjeta/huella leída en el ZKT (para vincular al afiliado). */
    @GetMapping("/last-read")
    public LastDeviceReadResponse lastRead(@RequestParam(required = false) String since) {
        Instant sinceInstant =
                since != null && !since.isBlank()
                        ? Instant.parse(since)
                        : Instant.now().minus(30, java.time.temporal.ChronoUnit.MINUTES);
        return accessControlService
                .lastDeviceReadSince(sinceInstant)
                .orElse(null);
    }

    /** F2 / F3 en pantalla /acceso o app de escritorio: pulso de torniquete (entreno / bailes). */
    @PostMapping("/kiosk/open-gate")
    public AccessVerifyResponse kioskOpenGate(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @Valid @RequestBody KioskOpenGateRequest request) {
        validateDeviceKey(deviceKey);
        return accessControlService.kioskOpenGate(request.reason());
    }

    @GetMapping("/kiosk/events")
    public List<KioskAccessEventResponse> kioskEvents(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) Long afterId) {
        validateDeviceKey(deviceKey);
        Instant sinceInstant =
                since != null && !since.isBlank() ? Instant.parse(since) : Instant.now().minusSeconds(2);
        return accessControlService.kioskEventsSince(sinceInstant, afterId);
    }

    @GetMapping("/enrollments")
    public List<BiometricEnrollResponse> listEnrollments() {
        return accessControlService.listEnrollments();
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public BiometricEnrollResponse enroll(@Valid @RequestBody BiometricEnrollRequest request) {
        return accessControlService.enroll(request);
    }

    /** Una vez: tarjetas ya registradas pasan a formato codigoLector|cedula. */
    @PostMapping("/migrate-card-credentials")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public CardCredentialMigrationResponse migrateCardCredentials() {
        return accessControlService.migrateCardCredentialsToDocumentSuffix();
    }

    @DeleteMapping("/enroll/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEnrollment(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "FINGERPRINT") BiometricCredentialType type) {
        accessControlService.removeEnrollment(memberId, type);
    }

    @DeleteMapping("/enroll/staff/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStaffEnrollment(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "FINGERPRINT") BiometricCredentialType type) {
        accessControlService.removeStaffEnrollment(employeeId, type);
    }

    @GetMapping("/logs")
    public List<AccessLogResponse> logs() {
        return accessControlService.recentLogs();
    }

    @DeleteMapping("/logs")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void clearLogs() {
        accessControlService.clearAllLogs();
    }

    @PostMapping("/manual-open/{memberId}")
    public AccessVerifyResponse manualOpen(@PathVariable Long memberId) {
        return accessControlService.manualOpen(memberId);
    }

    private void validateDeviceKey(String deviceKey) {
        if (deviceKey == null || !deviceApiKey.equals(deviceKey)) {
            throw new BusinessException("Dispositivo de acceso no autorizado");
        }
    }
}
