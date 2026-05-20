package com.gym.management.controller;

import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.FaceWebcamEnrollRequest;
import com.gym.management.dto.FaceWebcamEnrollResponse;
import com.gym.management.dto.FaceWebcamVerifyRequest;
import com.gym.management.service.FaceWebcamService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access/webcam")
@RequiredArgsConstructor
public class FaceWebcamController {

    private final FaceWebcamService faceWebcamService;

    @Value("${app.access.device-api-key:gym-turnstile-dev-key}")
    private String deviceApiKey;

    @PostMapping("/verify")
    public AccessVerifyResponse verify(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @Valid @RequestBody FaceWebcamVerifyRequest request) {
        validateDeviceKey(deviceKey);
        return faceWebcamService.verify(request);
    }

    @GetMapping("/enrollments")
    public List<FaceWebcamEnrollResponse> listEnrollments() {
        return faceWebcamService.listEnrollments();
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public FaceWebcamEnrollResponse enroll(@Valid @RequestBody FaceWebcamEnrollRequest request) {
        return faceWebcamService.enroll(request);
    }

    @DeleteMapping("/enroll/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMemberEnrollment(@PathVariable Long memberId) {
        faceWebcamService.removeEnrollment(memberId);
    }

    @DeleteMapping("/enroll/staff/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStaffEnrollment(@PathVariable Long employeeId) {
        faceWebcamService.removeStaffEnrollment(employeeId);
    }

    private void validateDeviceKey(String deviceKey) {
        if (deviceKey == null || !deviceApiKey.equals(deviceKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dispositivo de acceso no autorizado");
        }
    }
}
