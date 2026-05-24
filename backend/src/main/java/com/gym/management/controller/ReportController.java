package com.gym.management.controller;

import com.gym.management.dto.DailyBusinessReportResponse;
import com.gym.management.dto.MonthlyBusinessReportResponse;
import com.gym.management.service.BusinessReportService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final BusinessReportService businessReportService;

    @GetMapping("/daily")
    public DailyBusinessReportResponse daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        return businessReportService.dailyReport(date);
    }

    @GetMapping("/monthly")
    public MonthlyBusinessReportResponse monthly(
            @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) {
        YearMonth period = resolveYearMonth(year, month);
        return businessReportService.monthlyReport(period.getYear(), period.getMonthValue());
    }

    private static YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        YearMonth now = YearMonth.now(GYM_ZONE);
        return YearMonth.of(year != null ? year : now.getYear(), month != null ? month : now.getMonthValue());
    }
}
