package com.gym.management.mapper;

import com.gym.management.dto.EmployeeResponse;
import com.gym.management.model.Employee;
import com.gym.management.security.SecurityUtils;
import com.gym.management.service.AuthService;

public final class EmployeeMapper {

    private EmployeeMapper() {}

    public static EmployeeResponse toResponse(Employee employee) {
        boolean showPayment = SecurityUtils.canViewPaymentInfo();
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getPhone(),
                employee.getUsername(),
                employee.getRole(),
                AuthService.roleLabel(employee.getRole()),
                showPayment ? employee.getNequiNumber() : null,
                showPayment ? employee.getBankName() : null,
                showPayment ? employee.getBankAccountNumber() : null,
                employee.getActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
