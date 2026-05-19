package com.gym.management.security;

import com.gym.management.model.Employee;
import com.gym.management.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        if (employee.getPasswordHash() == null || employee.getRole() == null) {
            throw new UsernameNotFoundException("Usuario sin acceso configurado");
        }
        return new AuthenticatedUser(
                employee.getId(),
                employee.getUsername(),
                employee.getPasswordHash(),
                employee.getRole(),
                Boolean.TRUE.equals(employee.getActive()));
    }
}
