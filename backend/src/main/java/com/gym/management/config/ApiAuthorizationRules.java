package com.gym.management.config;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Reglas HTTP centralizadas (SRP). Los módulos deshabilitados se validan en {@link com.gym.management.security.ModuleAccessFilter}.
 */
public final class ApiAuthorizationRules {

    private ApiAuthorizationRules() {}

    public static void apply(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/billing/day-workout/register")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers(HttpMethod.POST, "/api/billing/sports-dance/register")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers("/api/auth/**", "/api/health/**")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/feedback/upload", "/api/feedback")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/trainer-ratings")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/trainer-ratings/participants")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/access/verify")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/access/zkt/event")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/access/zkt/select-member")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/access/kiosk/events")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/access/kiosk/open-gate")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/access/webcam/verify")
                .permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/access/logs")
                .hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/modules/public")
                .permitAll()
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/holidays/**",
                        "/api/business-hours/**",
                        "/api/wall-posts",
                        "/api/home/carousel",
                        "/api/home/media",
                        "/api/home/footer")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/feedback", "/api/feedback/**")
                .authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/feedback/**")
                .authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/feedback/**")
                .hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/trainer-ratings/monthly")
                .authenticated()
                .requestMatchers("/api/trainer-ratings/**")
                .hasRole("SUPER_ADMIN")
                .requestMatchers("/api/access/**")
                .authenticated()
                .requestMatchers(
                        "/api/holidays/**",
                        "/api/business-hours/**",
                        "/api/wall-posts/**",
                        "/api/home/**")
                .authenticated()
                .requestMatchers("/api/member-portal/**")
                .hasRole("AFFILIATE")
                .requestMatchers(HttpMethod.PUT, "/api/members/*/portal-password")
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/members/*/portal-password/reset")
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/employees/active")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers("/api/employees/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers("/api/payroll-config/**")
                .authenticated()
                .requestMatchers(HttpMethod.GET, "/api/modules")
                .authenticated()
                .requestMatchers("/api/modules/**")
                .hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**")
                .authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/shifts/**")
                .hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/shift-handovers/**")
                .hasRole("SUPER_ADMIN")
                .requestMatchers("/api/shift-handovers/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers("/api/shifts/**", "/api/sales/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers(HttpMethod.DELETE, "/api/billing/payments/**")
                .hasRole("SUPER_ADMIN")
                .requestMatchers("/api/billing/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers("/api/members/**", "/api/plans/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN", "TRAINER")
                .requestMatchers(HttpMethod.POST, "/api/products/**")
                .authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/products/**")
                .authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/products/**")
                .authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                .authenticated();
    }
}
