package com.gym.management.controller;

import com.gym.management.dto.AccessLogResponse;
import com.gym.management.dto.AccessVerifyRequest;
import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.FingerprintEnrollRequest;
import com.gym.management.dto.FingerprintEnrollResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.service.AccessControlService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessControlController {

    private final AccessControlService accessControlService;

    @Value("${app.access.device-api-key:gym-turnstile-dev-key}")
    private String deviceApiKey;

    /** Llamado por el lector de huellas / torniquete al detectar una huella. */
    @PostMapping("/verify")
    public AccessVerifyResponse verify(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @Valid @RequestBody AccessVerifyRequest request) {
        validateDeviceKey(deviceKey);
        return accessControlService.verifyAndOpen(request);
    }

    @GetMapping("/enrollments")
    public List<FingerprintEnrollResponse> listEnrollments() {
        return accessControlService.listEnrollments();
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public FingerprintEnrollResponse enroll(@Valid @RequestBody FingerprintEnrollRequest request) {
        return accessControlService.enroll(request);
    }

    @DeleteMapping("/enroll/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEnrollment(@PathVariable Long memberId) {
        accessControlService.removeEnrollment(memberId);
    }

    @GetMapping("/logs")
    public List<AccessLogResponse> logs() {
        return accessControlService.recentLogs();
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
