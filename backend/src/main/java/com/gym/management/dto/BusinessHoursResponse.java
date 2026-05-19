package com.gym.management.dto;

import java.util.List;

public record BusinessHoursResponse(List<BusinessDayScheduleDto> days) {}
