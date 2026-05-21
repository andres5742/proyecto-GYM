package com.gym.management.controller;

import com.gym.management.dto.AdminDataCleanupRequest;
import com.gym.management.dto.AdminDataCleanupResponse;
import com.gym.management.dto.AdminDataCleanupScopeResponse;
import com.gym.management.service.AdminDataCleanupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/data-cleanup")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminDataCleanupController {

    private final AdminDataCleanupService cleanupService;

    public AdminDataCleanupController(AdminDataCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @GetMapping("/scopes")
    public List<AdminDataCleanupScopeResponse> scopes() {
        return cleanupService.listScopes();
    }

    @PostMapping
    public AdminDataCleanupResponse cleanup(@Valid @RequestBody AdminDataCleanupRequest request) {
        return cleanupService.cleanup(request.scope(), request.confirmPhrase());
    }
}
