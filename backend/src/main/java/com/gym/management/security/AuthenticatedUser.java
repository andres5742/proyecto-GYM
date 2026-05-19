package com.gym.management.security;

import com.gym.management.model.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(
        Long employeeId,
        Long memberId,
        String username,
        String passwordHash,
        UserRole role,
        boolean active,
        String fullName
) implements UserDetails {

    public String fullName() {
        return fullName != null ? fullName.trim() : username;
    }

    public boolean isAffiliate() {
        return role == UserRole.AFFILIATE;
    }

    public boolean isEmployee() {
        return employeeId != null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
