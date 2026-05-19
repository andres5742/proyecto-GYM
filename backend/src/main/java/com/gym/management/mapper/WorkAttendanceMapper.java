package com.gym.management.mapper;

import com.gym.management.dto.WorkAttendanceResponse;
import com.gym.management.model.Employee;
import com.gym.management.model.WorkAttendance;
import com.gym.management.security.SecurityUtils;

public final class WorkAttendanceMapper {

    private WorkAttendanceMapper() {}

    public static WorkAttendanceResponse toResponse(WorkAttendance attendance) {
        Employee employee = attendance.getEmployee();
        return new WorkAttendanceResponse(
                attendance.getId(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                SecurityUtils.canViewPaymentInfo() ? paymentInfo(employee) : null,
                attendance.getWorkDate(),
                attendance.getClockIn(),
                attendance.getClockOut(),
                attendance.getHoursWorked(),
                attendance.getHourlyRateApplied(),
                attendance.getAmountOwed(),
                attendance.getSunday(),
                Boolean.TRUE.equals(attendance.getSunday()) ? "Domingo" : "Día de semana",
                attendance.getNotes(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }

    private static String paymentInfo(Employee employee) {
        StringBuilder info = new StringBuilder();
        if (employee.getNequiNumber() != null && !employee.getNequiNumber().isBlank()) {
            info.append("Nequi ").append(employee.getNequiNumber().trim());
        }
        if (employee.getBankName() != null
                && !employee.getBankName().isBlank()
                && employee.getBankAccountNumber() != null
                && !employee.getBankAccountNumber().isBlank()) {
            if (!info.isEmpty()) {
                info.append(" · ");
            }
            info.append(employee.getBankName().trim())
                    .append(" ")
                    .append(employee.getBankAccountNumber().trim());
        }
        return info.isEmpty() ? null : info.toString();
    }
}
