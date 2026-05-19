package com.gym.management.mapper;

import com.gym.management.dto.PayrollConfigResponse;
import com.gym.management.model.PayrollConfig;

public final class PayrollConfigMapper {

    private PayrollConfigMapper() {}

    public static PayrollConfigResponse toResponse(PayrollConfig config) {
        return new PayrollConfigResponse(
                config.getId(),
                config.getWeekdayHourlyRate(),
                config.getSundayHourlyRate(),
                config.getUpdatedAt()
        );
    }
}
