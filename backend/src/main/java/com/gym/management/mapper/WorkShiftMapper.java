package com.gym.management.mapper;

import com.gym.management.dto.WorkShiftResponse;
import com.gym.management.model.WorkShift;

public final class WorkShiftMapper {

    private WorkShiftMapper() {}

    public static WorkShiftResponse toResponse(WorkShift shift) {
        return toResponse(shift, null, 0L);
    }

    public static WorkShiftResponse toResponse(WorkShift shift, java.math.BigDecimal totalAmount, long totalSales) {
        return new WorkShiftResponse(
                shift.getId(),
                shift.getShiftDate(),
                shift.getName(),
                shift.getEmployee() != null ? shift.getEmployee().getId() : null,
                shift.getEmployee() != null
                        ? shift.getEmployee().getFirstName() + " " + shift.getEmployee().getLastName()
                        : null,
                shift.getOpenedAt(),
                shift.getClosedAt(),
                shift.getStatus(),
                totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO,
                totalSales,
                shift.getCreatedAt()
        );
    }
}
