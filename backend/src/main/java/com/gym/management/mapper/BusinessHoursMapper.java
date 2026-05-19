package com.gym.management.mapper;

import com.gym.management.dto.BusinessDayScheduleDto;
import com.gym.management.dto.BusinessHoursResponse;
import com.gym.management.model.BusinessDaySchedule;
import java.util.Comparator;
import java.util.List;

public final class BusinessHoursMapper {

    private BusinessHoursMapper() {}

    public static BusinessDayScheduleDto toDto(BusinessDaySchedule schedule) {
        return new BusinessDayScheduleDto(
                schedule.getDayOfWeek(),
                schedule.getOpenTime(),
                schedule.getCloseTime(),
                schedule.isClosed());
    }

    public static BusinessHoursResponse toResponse(List<BusinessDaySchedule> schedules) {
        List<BusinessDayScheduleDto> days = schedules.stream()
                .sorted(Comparator.comparing(s -> s.getDayOfWeek().getValue()))
                .map(BusinessHoursMapper::toDto)
                .toList();
        return new BusinessHoursResponse(days);
    }
}
