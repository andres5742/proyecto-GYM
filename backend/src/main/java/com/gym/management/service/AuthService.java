package com.gym.management.service;

import com.gym.management.dto.AuthResponse;
import com.gym.management.dto.LoginRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.Employee;
import com.gym.management.model.UserRole;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.JwtService;
import com.gym.management.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Employee employee = employeeRepository
                .findById(user.employeeId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        String token = jwtService.generateToken(employee.getId(), employee.getUsername(), employee.getRole());
        return toAuthResponse(token, employee);
    }

    @Transactional(readOnly = true)
    public AuthResponse me() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new BusinessException("Sesión no válida");
        }
        Employee employee = employeeRepository
                .findById(user.employeeId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        return toAuthResponse(null, employee);
    }

    private AuthResponse toAuthResponse(String token, Employee employee) {
        return new AuthResponse(
                token,
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getUsername(),
                employee.getRole(),
                roleLabel(employee.getRole()));
    }

    public static String roleLabel(UserRole role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case SUPER_ADMIN -> "Super administrador";
            case ADMIN -> "Administrador";
            case TRAINER -> "Entrenador";
        };
    }
}
