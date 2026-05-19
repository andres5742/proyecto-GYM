package com.gym.management.service;

import com.gym.management.dto.AuthResponse;
import com.gym.management.dto.LoginRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.Employee;
import com.gym.management.model.Member;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.security.AuthResponseFactory;
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
    private final MemberRepository memberRepository;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        String token = user.isAffiliate()
                ? jwtService.generateMemberToken(user.memberId(), user.getUsername())
                : jwtService.generateEmployeeToken(user.employeeId(), user.getUsername(), user.role());

        return AuthResponseFactory.fromAuthenticatedUser(user, token);
    }

    @Transactional(readOnly = true)
    public AuthResponse me() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new BusinessException("Sesión no válida");
        }
        if (user.isAffiliate()) {
            Member member = memberRepository
                    .findById(user.memberId())
                    .orElseThrow(() -> new BusinessException("Afiliado no encontrado"));
            return AuthResponseFactory.fromMember(null, member);
        }
        Employee employee = employeeRepository
                .findById(user.employeeId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        return AuthResponseFactory.fromEmployee(null, employee);
    }
}
