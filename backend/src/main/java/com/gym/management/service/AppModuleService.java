package com.gym.management.service;

import com.gym.management.dto.AppModuleResponse;
import com.gym.management.dto.ModuleToggleRequest;
import com.gym.management.dto.RoleModulePermissionItemRequest;
import com.gym.management.dto.RoleModulePermissionResponse;
import com.gym.management.dto.RoleModulePermissionsBatchRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.AppModule;
import com.gym.management.model.ModuleCategory;
import com.gym.management.model.RoleModulePermission;
import com.gym.management.model.UserRole;
import com.gym.management.repository.AppModuleRepository;
import com.gym.management.repository.RoleModulePermissionRepository;
import com.gym.management.security.SecurityUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppModuleService {

    public static final String CODE_MODULE_ADMIN = "MODULOS_SISTEMA";

    private static final Set<String> TRAINER_DEFAULT_ALLOWED =
            Set.of("VENTAS", "FACTURACION", "ENTREGA_TURNO", "FIADO", "JORNADA");

    private final AppModuleRepository appModuleRepository;
    private final RoleModulePermissionRepository roleModulePermissionRepository;

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

    @Transactional(readOnly = true)
    public List<RoleModulePermissionResponse> findPermissionsForRole(UserRole role) {
        ensureConfigurableRole(role);
        return panelModulesForRoleConfig().stream()
                .map(module -> toRolePermissionResponse(role, module))
                .toList();
    }

    @Transactional
    public RoleModulePermissionResponse updatePermissionForRole(
            UserRole role, String moduleCode, boolean allowed) {
        ensureConfigurableRole(role);
        if (CODE_MODULE_ADMIN.equals(moduleCode)) {
            throw new BusinessException("No se pueden asignar permisos del módulo de configuración");
        }
        AppModule module = appModuleRepository
                .findById(moduleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + moduleCode));
        if (module.getCategory() != ModuleCategory.PANEL) {
            throw new BusinessException("Solo se configuran módulos del panel para cada rol");
        }
        RoleModulePermission permission = roleModulePermissionRepository
                .findById(new com.gym.management.model.RoleModulePermissionId(role, moduleCode))
                .orElse(RoleModulePermission.builder()
                        .role(role)
                        .moduleCode(moduleCode)
                        .build());
        permission.setAllowed(allowed);
        roleModulePermissionRepository.save(permission);
        return toRolePermissionResponse(role, module);
    }

    @Transactional
    public List<RoleModulePermissionResponse> updatePermissionsForRole(
            UserRole role, RoleModulePermissionsBatchRequest request) {
        ensureConfigurableRole(role);
        for (RoleModulePermissionItemRequest item : request.permissions()) {
            if (CODE_MODULE_ADMIN.equals(item.moduleCode())) {
                throw new BusinessException("No se pueden asignar permisos del módulo de configuración");
            }
            AppModule module = appModuleRepository
                    .findById(item.moduleCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + item.moduleCode()));
            if (module.getCategory() != ModuleCategory.PANEL) {
                throw new BusinessException("Solo se configuran módulos del panel para cada rol");
            }
            RoleModulePermission permission = roleModulePermissionRepository
                    .findById(new com.gym.management.model.RoleModulePermissionId(role, item.moduleCode()))
                    .orElse(RoleModulePermission.builder()
                            .role(role)
                            .moduleCode(item.moduleCode())
                            .build());
            permission.setAllowed(Boolean.TRUE.equals(item.allowed()));
            roleModulePermissionRepository.save(permission);
        }
        return findPermissionsForRole(role);
    }

    @Transactional
    public void ensureDefaultRolePermissions() {
        for (UserRole role : List.of(UserRole.TRAINER, UserRole.ADMIN)) {
            for (AppModule module : panelModulesForRoleConfig()) {
                if (!roleModulePermissionRepository.existsByRoleAndModuleCode(role, module.getCode())) {
                    roleModulePermissionRepository.save(RoleModulePermission.builder()
                            .role(role)
                            .moduleCode(module.getCode())
                            .allowed(defaultAllowedForRole(role, module.getCode()))
                            .build());
                }
            }
        }
    }

    @Transactional
    public void ensureFacturacionStaffAccess() {
        ensureStaffModuleAccess("FACTURACION");
    }

    /** Recepción: entrega de turno y arqueo de caja del entrenador. */
    @Transactional
    public void ensureEntregaTurnoStaffAccess() {
        ensureStaffModuleAccess("ENTREGA_TURNO");
    }

    private void ensureStaffModuleAccess(String moduleCode) {
        if (appModuleRepository.findById(moduleCode).isEmpty()) {
            return;
        }
        for (UserRole role : List.of(UserRole.TRAINER, UserRole.ADMIN)) {
            RoleModulePermission permission = roleModulePermissionRepository
                    .findById(new com.gym.management.model.RoleModulePermissionId(role, moduleCode))
                    .orElse(RoleModulePermission.builder()
                            .role(role)
                            .moduleCode(moduleCode)
                            .build());
            permission.setAllowed(true);
            roleModulePermissionRepository.save(permission);
        }
        appModuleRepository.findById(moduleCode).ifPresent(module -> {
            if (!Boolean.TRUE.equals(module.getEnabled())) {
                module.setEnabled(true);
                appModuleRepository.save(module);
            }
        });
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
            return false;
        }
        if (!enabled) {
            return false;
        }
        UserRole role = SecurityUtils.currentRole();
        if (role == null) {
            return false;
        }
        if (role == UserRole.ADMIN) {
            return isAllowedForRole(UserRole.ADMIN, code);
        }
        if (role == UserRole.TRAINER) {
            return isAllowedForRole(UserRole.TRAINER, code);
        }
        return false;
    }

    private boolean isAllowedForRole(UserRole role, String code) {
        return roleModulePermissionRepository
                .findById(new com.gym.management.model.RoleModulePermissionId(role, code))
                .map(RoleModulePermission::getAllowed)
                .map(Boolean::booleanValue)
                .orElse(defaultAllowedForRole(role, code));
    }

    private boolean defaultAllowedForRole(UserRole role, String code) {
        if (role == UserRole.ADMIN) {
            return true;
        }
        if (role == UserRole.TRAINER) {
            return TRAINER_DEFAULT_ALLOWED.contains(code);
        }
        return false;
    }

    private List<AppModule> panelModulesForRoleConfig() {
        return appModuleRepository.findByCategoryOrderBySortOrderAsc(ModuleCategory.PANEL).stream()
                .filter(m -> !CODE_MODULE_ADMIN.equals(m.getCode()))
                .toList();
    }

    private RoleModulePermissionResponse toRolePermissionResponse(UserRole role, AppModule module) {
        boolean globallyEnabled = Boolean.TRUE.equals(module.getEnabled());
        boolean allowed = isAllowedForRole(role, module.getCode());
        return new RoleModulePermissionResponse(
                module.getCode(),
                module.getName(),
                module.getDescription(),
                allowed,
                globallyEnabled);
    }

    private void ensureConfigurableRole(UserRole role) {
        if (role == UserRole.SUPER_ADMIN) {
            throw new BusinessException("El super administrador siempre tiene acceso a todos los módulos");
        }
        if (role != UserRole.TRAINER && role != UserRole.ADMIN) {
            throw new BusinessException("Rol no configurable: " + role);
        }
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
