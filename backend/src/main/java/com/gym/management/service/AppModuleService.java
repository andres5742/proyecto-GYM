package com.gym.management.service;

import com.gym.management.dto.AppModuleResponse;
import com.gym.management.dto.ModuleToggleRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.AppModule;
import com.gym.management.model.ModuleCategory;
import com.gym.management.repository.AppModuleRepository;
import com.gym.management.security.SecurityUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppModuleService {

    public static final String CODE_MODULE_ADMIN = "MODULOS_SISTEMA";

    private final AppModuleRepository appModuleRepository;

    @Transactional(readOnly = true)
    public Map<String, Boolean> publicModuleFlags() {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        appModuleRepository.findByCategoryOrderBySortOrderAsc(ModuleCategory.PUBLIC).stream()
                .map(this::toResponse)
                .forEach(m -> flags.put(m.code(), m.enabled()));
        return flags;
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> panelModuleFlags() {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        appModuleRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .forEach(m -> flags.put(m.code(), effectiveEnabled(m.code(), m.enabled())));
        return flags;
    }

    @Transactional(readOnly = true)
    public List<AppModuleResponse> findAllForManagement() {
        return appModuleRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppModuleResponse updateEnabled(String code, ModuleToggleRequest request) {
        if (CODE_MODULE_ADMIN.equals(code)) {
            throw new BusinessException("El módulo de configuración del sistema no se puede desactivar");
        }
        AppModule module = appModuleRepository
                .findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + code));
        module.setEnabled(request.enabled());
        return toResponse(appModuleRepository.save(module));
    }

    public boolean isEnabled(String code) {
        return appModuleRepository
                .findById(code)
                .map(AppModule::getEnabled)
                .map(enabled -> effectiveEnabled(code, enabled))
                .orElse(false);
    }

    private boolean effectiveEnabled(String code, boolean enabled) {
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }
        if (CODE_MODULE_ADMIN.equals(code)) {
            return SecurityUtils.isSuperAdmin();
        }
        return enabled;
    }

    private AppModuleResponse toResponse(AppModule module) {
        return new AppModuleResponse(
                module.getCode(),
                module.getName(),
                module.getDescription(),
                module.getCategory(),
                categoryLabel(module.getCategory()),
                Boolean.TRUE.equals(module.getEnabled()),
                module.getSortOrder() != null ? module.getSortOrder() : 0);
    }

    private static String categoryLabel(ModuleCategory category) {
        return switch (category) {
            case PANEL -> "Panel de administración";
            case PUBLIC -> "Página pública (inicio)";
        };
    }
}
