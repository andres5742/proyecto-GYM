package com.gym.management.security;

import com.gym.management.model.Employee;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipStatus;
import com.gym.management.model.UserRole;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.service.MemberMembershipRules;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GymUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String login = username != null ? username.trim() : "";
        if (login.isBlank()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        var employee = employeeRepository.findByUsernameIgnoreCase(login);
        if (employee.isPresent()) {
            return toEmployeeUser(employee.get());
        }

        Member member = memberRepository
                .findByDocumentId(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return toMemberUser(member);
    }

    private UserDetails toEmployeeUser(Employee employee) {
        if (employee.getPasswordHash() == null || employee.getRole() == null) {
            throw new UsernameNotFoundException("Usuario sin acceso configurado");
        }
        return new AuthenticatedUser(
                employee.getId(),
                null,
                employee.getUsername(),
                employee.getPasswordHash(),
                employee.getRole(),
                Boolean.TRUE.equals(employee.getActive()),
                fullName(employee.getFirstName(), employee.getLastName()));
    }

    private UserDetails toMemberUser(Member member) {
        if (!Boolean.TRUE.equals(member.getPortalAccessEnabled())) {
            throw new DisabledException("El acceso al portal no está habilitado para este afiliado");
        }
        if (member.getPasswordHash() == null || member.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("Afiliado sin contraseña configurada");
        }
        if (member.getDocumentId() == null || member.getDocumentId().isBlank()) {
            throw new UsernameNotFoundException("Afiliado sin documento de acceso");
        }
        MembershipStatus status = MemberMembershipRules.effectiveStatus(member);
        if (status != MembershipStatus.ACTIVE) {
            throw new DisabledException("Tu membresía no está activa. Acércate a recepción.");
        }
        return new AuthenticatedUser(
                null,
                member.getId(),
                member.getDocumentId(),
                member.getPasswordHash(),
                UserRole.AFFILIATE,
                true,
                fullName(member.getFirstName(), member.getLastName()));
    }

    private static String fullName(String first, String last) {
        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
    }
}
