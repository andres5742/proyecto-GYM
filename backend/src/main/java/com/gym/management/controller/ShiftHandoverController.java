package com.gym.management.controller;

import com.gym.management.dto.ShiftHandoverRequest;
import com.gym.management.dto.ShiftHandoverResponse;
import com.gym.management.service.ShiftHandoverService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shift-handovers")
@RequiredArgsConstructor
public class ShiftHandoverController {

    private final ShiftHandoverService handoverService;

    @GetMapping
    public List<ShiftHandoverResponse> findAll() {
        return handoverService.findAll();
    }

    @GetMapping("/shift/{workShiftId}")
    public ShiftHandoverResponse previewForShift(@PathVariable Long workShiftId) {
        return handoverService.previewForShift(workShiftId);
    }

    @GetMapping("/{id:\\d+}")
    public ShiftHandoverResponse findById(@PathVariable Long id) {
        return handoverService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftHandoverResponse submit(@Valid @RequestBody ShiftHandoverRequest request) {
        return handoverService.submit(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable Long id) {
        handoverService.delete(id);
    }
}
