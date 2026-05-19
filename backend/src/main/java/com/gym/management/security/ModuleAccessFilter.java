package com.gym.management.security;

import com.gym.management.model.UserRole;
import com.gym.management.service.AppModuleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bloquea APIs del panel si el módulo no está habilitado para el rol del usuario
 * (según configuración global + permisos por rol).
 */
@Component
@RequiredArgsConstructor
public class ModuleAccessFilter extends OncePerRequestFilter {

    private final AppModuleService appModuleService;

    private record ApiModuleRule(String pathPrefix, String moduleCode, Set<HttpMethod> methods) {}

    private static final List<ApiModuleRule> RULES = List.of(
            new ApiModuleRule("/api/feedback", "BUZON", Set.of(HttpMethod.GET, HttpMethod.PATCH)),
            new ApiModuleRule("/api/members", "SOCIOS", Set.of()),
            new ApiModuleRule("/api/plans", "PLANES", Set.of()),
            new ApiModuleRule("/api/products", "INVENTARIO", Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)),
            new ApiModuleRule("/api/employees", "ENTRENADORES", Set.of()),
            new ApiModuleRule("/api/payroll-config", "NOMINA", Set.of()),
            new ApiModuleRule("/api/home", "CONTENIDO_INICIO", Set.of()),
            new ApiModuleRule("/api/holidays", "CONTENIDO_INICIO", Set.of()),
            new ApiModuleRule("/api/wall-posts", "CONTENIDO_INICIO", Set.of()),
            new ApiModuleRule("/api/business-hours", "CONTENIDO_INICIO", Set.of(HttpMethod.PUT, HttpMethod.PATCH)),
            new ApiModuleRule("/api/trainer-ratings/monthly", "CALIFICACIONES", Set.of()),
            new ApiModuleRule("/api/access", "ACCESO", Set.of()),
            new ApiModuleRule("/api/sales", "VENTAS", Set.of()),
            new ApiModuleRule("/api/shifts", "VENTAS", Set.of()),
            new ApiModuleRule("/api/shift-handovers", "ENTREGA_TURNO", Set.of()),
            new ApiModuleRule("/api/attendance", "JORNADA", Set.of()));

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityUtils.isSuperAdmin()
                || SecurityUtils.currentUser() == null
                || SecurityUtils.currentRole() == UserRole.AFFILIATE) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/employees/active")) {
            filterChain.doFilter(request, response);
            return;
        }

        for (ApiModuleRule rule : RULES) {
            if (!path.startsWith(rule.pathPrefix())) {
                continue;
            }
            if (!rule.methods().isEmpty() && !rule.methods().contains(method)) {
                continue;
            }
            if (!appModuleService.isEnabled(rule.moduleCode())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes acceso a este módulo");
                return;
            }
            break;
        }

        filterChain.doFilter(request, response);
    }
}
