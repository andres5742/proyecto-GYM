package com.gym.management.controller;

import com.gym.management.dto.BusinessDayScheduleDto;
import com.gym.management.dto.BusinessHoursResponse;
import com.gym.management.service.BusinessHoursService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business-hours")
@RequiredArgsConstructor
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;

    @GetMapping
    public BusinessHoursResponse get() {
        return businessHoursService.get();
    }

    @PutMapping
    public BusinessHoursResponse update(@Valid @RequestBody List<@Valid BusinessDayScheduleDto> days) {
        return businessHoursService.update(days);
    }
}
