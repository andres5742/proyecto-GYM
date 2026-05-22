package com.gym.management.controller;

import com.gym.management.dto.UpcomingBirthdayResponse;
import com.gym.management.service.BirthdayReminderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BirthdayReminderService birthdayReminderService;

    @GetMapping("/upcoming-birthdays")
    public List<UpcomingBirthdayResponse> upcomingBirthdays(
            @RequestParam(defaultValue = "7") int withinDays) {
        return birthdayReminderService.findUpcoming(withinDays);
    }
}
