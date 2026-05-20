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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bloquea APIs del panel si el módulo no está habilitado para el rol del usuario
 * (según configuración global + permisos por rol).
 */
@RequiredArgsConstructor
public class ModuleAccessFilter extends OncePerRequestFilter {

    private final AppModuleService appModuleService;

    private record ApiModuleRule(String pathPrefix, String moduleCode, Set<HttpMethod> methods) {}

    /** F2/F3 y caja: recepción sin módulo Facturación habilitado en BD. */
    private static final List<String> BILLING_RECEPTION_PATH_PREFIXES = List.of(
            "/api/billing/day-workout/",
            "/api/billing/sports-dance/",
            "/api/billing/cash-registers");

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
            new ApiModuleRule("/api/billing", "FACTURACION", Set.of()),
            new ApiModuleRule("/api/shifts", "VENTAS", Set.of()),
            new ApiModuleRule("/api/shift-handovers", "ENTREGA_TURNO", Set.of()),
            new ApiModuleRule("/api/cash-shortfalls", "DESCUADRES_CAJA", Set.of()),
            new ApiModuleRule("/api/product-credits", "FIADO", Set.of()),
            new ApiModuleRule("/api/attendance", "JORNADA", Set.of()));

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isBillingReceptionPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isBillingSupportCatalogRead(path, method) && appModuleService.isEnabled("FACTURACION")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isFiadoSupportCatalogRead(path, method) && appModuleService.isEnabled("FIADO")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isVentasSupportCatalogRead(path, method) && appModuleService.isEnabled("VENTAS")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || SecurityUtils.isSuperAdmin()
                || SecurityUtils.currentRole() == UserRole.AFFILIATE) {
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
            if (isBillingDayPassShortcut(path)) {
                break;
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

    private static boolean isBillingReceptionPath(String path) {
        for (String prefix : BILLING_RECEPTION_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBillingDayPassShortcut(String path) {
        return path.startsWith("/api/billing/day-workout/")
                || path.startsWith("/api/billing/sports-dance/");
    }

    /** Listados de afiliados y planes para facturar membresías (sin abrir módulos Socios/Planes). */
    private static boolean isBillingSupportCatalogRead(String path, HttpMethod method) {
        if (method != HttpMethod.GET) {
            return false;
        }
        return "/api/members".equals(path) || "/api/plans".equals(path);
    }

    /** Listado de afiliados para registrar fiado (sin módulo Socios). */
    private static boolean isFiadoSupportCatalogRead(String path, HttpMethod method) {
        return method == HttpMethod.GET && "/api/members".equals(path);
    }

    /** Afiliados al registrar ventas en pendiente/deuda (sin módulo Socios). */
    private static boolean isVentasSupportCatalogRead(String path, HttpMethod method) {
        return method == HttpMethod.GET && "/api/members".equals(path);
    }
}
