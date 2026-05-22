package com.gym.management.mapper;

import com.gym.management.dto.EmployeeResponse;
import com.gym.management.model.Employee;
import com.gym.management.security.SecurityUtils;

public final class EmployeeMapper {

    private EmployeeMapper() {}

    public static EmployeeResponse toResponse(Employee employee) {
        boolean showPayment = SecurityUtils.canViewPaymentInfo() || isOwnRecord(employee);
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getBirthDate(),
                employee.getPhone(),
                employee.getUsername(),
                employee.getRole(),
                employee.getRole() != null ? employee.getRole().displayLabel() : null,
                showPayment ? employee.getNequiNumber() : null,
                showPayment ? employee.getBankName() : null,
                showPayment ? employee.getBankAccountNumber() : null,
                employee.getActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }

    private static boolean isOwnRecord(Employee employee) {
        var user = SecurityUtils.currentUser();
        return user != null && employee.getId().equals(user.employeeId());
    }
}
