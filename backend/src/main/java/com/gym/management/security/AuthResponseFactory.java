package com.gym.management.security;

import com.gym.management.dto.AuthResponse;
import com.gym.management.model.Employee;
import com.gym.management.model.Member;
import com.gym.management.model.UserRole;

/** Construye respuestas de autenticación (único lugar — DRY). */
public final class AuthResponseFactory {

    private AuthResponseFactory() {}

    public static AuthResponse fromAuthenticatedUser(AuthenticatedUser user, String token) {
        return new AuthResponse(
                token,
                user.employeeId(),
                user.memberId(),
                user.fullName(),
                user.getUsername(),
                user.role(),
                user.role().displayLabel());
    }

    public static AuthResponse fromEmployee(String token, Employee employee) {
        return new AuthResponse(
                token,
                employee.getId(),
                null,
                fullName(employee.getFirstName(), employee.getLastName()),
                employee.getUsername(),
                employee.getRole(),
                employee.getRole().displayLabel());
    }

    public static AuthResponse fromMember(String token, Member member) {
        return new AuthResponse(
                token,
                null,
                member.getId(),
                fullName(member.getFirstName(), member.getLastName()),
                member.getDocumentId(),
                UserRole.AFFILIATE,
                UserRole.AFFILIATE.displayLabel());
    }

    private static String fullName(String first, String last) {
        return (first != null ? first : "").trim() + " " + (last != null ? last : "").trim();
    }
}
