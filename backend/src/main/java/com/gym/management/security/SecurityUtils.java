package com.gym.management.security;

import com.gym.management.model.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    public static UserRole currentRole() {
        AuthenticatedUser user = currentUser();
        return user != null ? user.role() : null;
    }

    public static boolean canViewPaymentInfo() {
        UserRole role = currentRole();
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    public static boolean isSuperAdmin() {
        return currentRole() == UserRole.SUPER_ADMIN;
    }
}
