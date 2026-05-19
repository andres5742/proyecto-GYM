package com.gym.management.controller;

import com.gym.management.dto.AppModuleResponse;
import com.gym.management.dto.ModuleToggleRequest;
import com.gym.management.service.AppModuleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class AppModuleController {

    private final AppModuleService appModuleService;

    @GetMapping("/public")
    public Map<String, Boolean> publicModules() {
        return appModuleService.publicModuleFlags();
    }

    @GetMapping
    public Map<String, Boolean> panelModules() {
        return appModuleService.panelModuleFlags();
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<AppModuleResponse> manage() {
        return appModuleService.findAllForManagement();
    }

    @PatchMapping("/{code}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AppModuleResponse update(
            @PathVariable String code, @Valid @RequestBody ModuleToggleRequest request) {
        return appModuleService.updateEnabled(code, request);
    }
}
