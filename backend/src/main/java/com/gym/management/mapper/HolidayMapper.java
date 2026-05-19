package com.gym.management.mapper;

import com.gym.management.dto.HolidayResponse;
import com.gym.management.model.Holiday;

public final class HolidayMapper {

    private HolidayMapper() {}

    public static HolidayResponse toResponse(Holiday holiday) {
        return new HolidayResponse(
                holiday.getId(),
                holiday.getDate(),
                holiday.getName(),
                holiday.getDescription(),
                holiday.getCreatedAt());
    }
}
