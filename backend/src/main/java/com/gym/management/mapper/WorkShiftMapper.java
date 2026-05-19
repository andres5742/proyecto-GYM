package com.gym.management.mapper;

import com.gym.management.dto.WorkShiftResponse;
import com.gym.management.model.WorkShift;

public final class WorkShiftMapper {

    private WorkShiftMapper() {}

    public static WorkShiftResponse toResponse(WorkShift shift) {
        return new WorkShiftResponse(
                shift.getId(),
                shift.getShiftDate(),
                shift.getName(),
                shift.getOpenedAt(),
                shift.getClosedAt(),
                shift.getStatus(),
                shift.getCreatedAt()
        );
    }
}
