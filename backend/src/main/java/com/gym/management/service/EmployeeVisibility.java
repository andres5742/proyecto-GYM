package com.gym.management.service;

import com.gym.management.model.Employee;
import com.gym.management.model.UserRole;
import com.gym.management.security.SecurityUtils;

/** Quién aparece en listas de equipo, acceso biométrico de staff, etc. */
public final class EmployeeVisibility {

    private EmployeeVisibility() {}

    public static boolean visibleInTeamDirectory(Employee employee) {
        if (employee == null) {
            return false;
        }
        if (employee.getRole() != UserRole.SUPER_ADMIN) {
            return true;
        }
        return SecurityUtils.isSuperAdmin();
    }
}
